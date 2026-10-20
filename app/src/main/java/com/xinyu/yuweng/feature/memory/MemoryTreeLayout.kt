package com.xinyu.yuweng.feature.memory

import com.xinyu.yuweng.core.stateengine.StateEngine
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 屿翁记忆树 — 纯 Kotlin 力导向布局引擎。
 * 无 Android 依赖，可在 JVM 单元测试中直接调用。
 *
 * 算法概览：
 *  - 斥力（节点间互斥）+ 弹簧力（边约束）+ 向心力（拉向中心）
 *  - 阻尼衰减 + 速度上限 + warmup 收敛
 */

data class TreeNode(
    val id: String,
    val label: String,
    val type: String,      // "root" | cardType
    val emotionWeight: Float,
    val forgettingRate: Float,
    val status: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val radius: Float = 40f,
    val color: Long = 0L   // color as Long for pure Kotlin
)

data class TreeEdge(
    val from: String,
    val to: String,
    val isCrossBranch: Boolean = false,
    val reason: String = ""
)

data class TreeLayout(
    val nodes: List<TreeNode>,
    val edges: List<TreeEdge>
)

object MemoryTreeLayout {

    // ─── 力导向常量 ───
    private const val REP = 1500f           // 斥力系数
    private const val REP_CUTOFF = 600f     // 斥力截止距离
    private const val REP_BOOST_DIST = 40f  // 小于此距离 ×3 斥力

    private const val SPRING_MAIN_REST = 225f    // 主干弹簧原长
    private const val SPRING_BRANCH_REST = 112f   // 分支弹簧原长
    private const val SPRING_CROSS_REST = 255f    // 跨分支弹簧原长
    private const val SPRING_MAIN_K = 0.04f       // 主干刚度
    private const val SPRING_BRANCH_K = 0.05f     // 分支刚度
    private const val SPRING_CROSS_K = 0.04f * 0.4f // 跨分支刚度 ×0.4

    private const val CENTRIPETAL = 0.0022f  // 向心系数
    private const val DAMPING = 0.85f        // 阻尼
    private const val SPEED_LIMIT = 4.5f     // 速度上限

    private const val BRANCH_RADIUS = 250f   // 分支节点初始半径
    private const val LEAF_RADIUS_MIN = 330f // 叶子节点最小半径
    private const val LEAF_RADIUS_MAX = 450f // 叶子节点最大半径

    private const val WARMUP_FRAMES = 700
    private const val CONVERGENCE_THRESHOLD = 0.1f

    // ─── 卡片类型到颜色的映射（ARGB Long） ───
    private val CARD_TYPE_COLORS = mapOf(
        "moment" to 0xFFF5C6A5L,    // SunsetOrange
        "quote" to 0xFFF4E4C1L,     // WarmGold
        "question" to 0xFFB8D8E3L,  // MistBlue
        "feeling" to 0xFFC8B6E2L,   // DuskPurple
        "song" to 0xFFA8C9A1L,      // MossGreen
        "self" to 0xFFFAF7F2L       // MoonWhite
    )

    private val CARD_TYPE_LABELS = mapOf(
        "moment" to "时刻",
        "quote" to "话语",
        "question" to "疑问",
        "feeling" to "感受",
        "song" to "旋律",
        "self" to "自我"
    )

    private val ALL_CARD_TYPES = listOf("moment", "quote", "question", "feeling", "song", "self")

    /**
     * 从记忆卡片列表构建树结构。
     * - 根节点：屿翁
     * - 分支节点：每种存在的 card_type 一个
     * - 叶子节点：每张 active 卡片一个
     * - 跨分支边：内容前三字相同 → 共享主题
     */
    fun buildTree(cards: List<com.xinyu.yuweng.data.local.entity.MemoryCardEntity>): TreeLayout {
        val activeCards = cards.filter { it.status != "forgotten" }
        val nodeList = mutableListOf<TreeNode>()
        val edgeList = mutableListOf<TreeEdge>()

        // ── 根节点 ──
        nodeList.add(
            TreeNode(
                id = "root",
                label = "屿翁",
                type = "root",
                emotionWeight = activeCards.map { it.emotionalWeight }.average().toFloat(),
                forgettingRate = 0f,
                status = "stable",
                x = 0f,
                y = 0f,
                radius = 50f,
                color = 0xFF4A4A4AL // DaiGray
            )
        )

        // ── 分支节点（按 card_type 分组） ──
        val cardsByGroup = activeCards.groupBy { it.cardType }
        val presentTypes = ALL_CARD_TYPES.filter { cardsByGroup.containsKey(it) }
        val branchCount = presentTypes.size

        val branchNodes = presentTypes.mapIndexed { index, cardType ->
            val angle = if (branchCount > 0) (2.0 * Math.PI * index / branchCount) else 0.0
            val groupCards = cardsByGroup[cardType].orEmpty()
            val avgEmotion = groupCards.map { it.emotionalWeight }.average().toFloat()
            val avgForgetting = groupCards.map {
                computeRateForCard(it)
            }.average().toFloat()

            TreeNode(
                id = "branch_$cardType",
                label = CARD_TYPE_LABELS[cardType] ?: cardType,
                type = cardType,
                emotionWeight = avgEmotion,
                forgettingRate = avgForgetting,
                status = "stable",
                x = (BRANCH_RADIUS * cos(angle)).toFloat(),
                y = (BRANCH_RADIUS * sin(angle)).toFloat(),
                radius = 36f,
                color = CARD_TYPE_COLORS[cardType] ?: 0xFF8C8C8CL
            )
        }
        nodeList.addAll(branchNodes)

        // 根→分支 主干边
        branchNodes.forEach { branch ->
            edgeList.add(TreeEdge(from = "root", to = branch.id))
        }

        // ── 叶子节点 ──
        val leafNodes = mutableListOf<TreeNode>()
        presentTypes.forEachIndexed { typeIndex, cardType ->
            val groupCards = cardsByGroup[cardType].orEmpty()
            val branchAngle = if (branchCount > 0) (2.0 * Math.PI * typeIndex / branchCount) else 0.0

            groupCards.forEachIndexed { cardIndex, card ->
                // 叶子角度：在分支角度附近展开 ±30°
                val spread = if (groupCards.size > 1) {
                    val totalSpread = Math.PI / 3.0  // 60° 总展开角
                    branchAngle - totalSpread / 2.0 + totalSpread * cardIndex / (groupCards.size - 1).coerceAtLeast(1)
                } else {
                    branchAngle
                }

                // 半径在 330~450 之间交替分布，避免重叠
                val r = if (groupCards.size > 1) {
                    LEAF_RADIUS_MIN + (LEAF_RADIUS_MAX - LEAF_RADIUS_MIN) * cardIndex / (groupCards.size - 1).coerceAtLeast(1)
                } else {
                    (LEAF_RADIUS_MIN + LEAF_RADIUS_MAX) / 2f
                }

                val rate = computeRateForCard(card)
                val leafAlpha = when (card.status) {
                    "fresh" -> 1f
                    "stable" -> 0.85f
                    "fading" -> 0.5f
                    else -> 0.7f
                }

                val leaf = TreeNode(
                    id = "leaf_${card.id}",
                    label = card.content.take(12) + if (card.content.length > 12) "…" else "",
                    type = card.cardType,
                    emotionWeight = card.emotionalWeight,
                    forgettingRate = rate,
                    status = card.status,
                    x = (r * cos(spread)).toFloat(),
                    y = (r * sin(spread)).toFloat(),
                    radius = 28f + card.emotionalWeight * 12f,
                    color = CARD_TYPE_COLORS[card.cardType] ?: 0xFF8C8C8CL
                )
                leafNodes.add(leaf)

                // 分支→叶子 分支边
                edgeList.add(
                    TreeEdge(
                        from = "branch_${card.cardType}",
                        to = leaf.id,
                        reason = card.context
                    )
                )
            }
        }
        nodeList.addAll(leafNodes)

        // ── 跨分支边：内容前三字相同 ──
        val leafByPrefix = leafNodes.groupBy { it.label.take(3) }
        leafByPrefix.entries.forEach { (_, group) ->
            if (group.size > 1) {
                for (i in 0 until group.size - 1) {
                    for (j in i + 1 until group.size) {
                        val a = group[i]
                        val b = group[j]
                        if (a.type != b.type) {
                            edgeList.add(
                                TreeEdge(
                                    from = a.id,
                                    to = b.id,
                                    isCrossBranch = true,
                                    reason = "共同主题：「${a.label.take(3)}」"
                                )
                            )
                        }
                    }
                }
            }
        }

        return TreeLayout(nodes = nodeList, edges = edgeList)
    }

    /**
     * 运行完整模拟直到收敛或达到最大帧数。
     */
    fun simulate(layout: TreeLayout, frames: Int = WARMUP_FRAMES): TreeLayout {
        var current = layout
        for (i in 0 until frames) {
            current = step(current)
            // 提前收敛检查（每 50 帧检查一次，避免过度计算）
            if (i % 50 == 0 && i > 0) {
                val maxV = current.nodes.maxOfOrNull { sqrt(it.vx * it.vx + it.vy * it.vy) } ?: 0f
                if (maxV < CONVERGENCE_THRESHOLD) break
            }
        }
        return current
    }

    /**
     * 单步力导向计算。返回新布局（不可变）。
     */
    fun step(layout: TreeLayout): TreeLayout {
        val nodeMap = layout.nodes.associateBy { it.id }.toMutableMap()
        val forces = mutableMapOf<String, Pair<Float, Float>>()
        layout.nodes.forEach { forces[it.id] = 0f to 0f }

        // ── 1. 斥力（所有节点对之间） ──
        val nodeArr = layout.nodes.toTypedArray()
        for (i in nodeArr.indices) {
            for (j in i + 1 until nodeArr.size) {
                val a = nodeArr[i]
                val b = nodeArr[j]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < 0.01f) continue

                val effectiveCutoff = REP_CUTOFF
                if (dist > effectiveCutoff) continue

                var repK = REP
                if (dist < REP_BOOST_DIST) {
                    repK *= 3f
                }

                val force = repK / (dist * dist)
                val fx = (dx / dist) * force
                val fy = (dy / dist) * force

                val fa = forces[a.id]!!
                val fb = forces[b.id]!!
                forces[a.id] = fa.first - fx to fa.second - fy
                forces[b.id] = fb.first + fx to fb.second + fy
            }
        }

        // ── 2. 弹簧力（边约束） ──
        layout.edges.forEach { edge ->
            val a = nodeMap[edge.from] ?: return@forEach
            val b = nodeMap[edge.to] ?: return@forEach
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < 0.01f) return@forEach

            val (restLen, k) = when {
                edge.isCrossBranch -> SPRING_CROSS_REST to SPRING_CROSS_K
                a.type == "root" -> SPRING_MAIN_REST to SPRING_MAIN_K
                else -> SPRING_BRANCH_REST to SPRING_BRANCH_K
            }

            val displacement = dist - restLen
            val force = k * displacement
            val fx = (dx / dist) * force
            val fy = (dy / dist) * force

            val fa = forces[a.id]!!
            val fb = forces[b.id]!!
            forces[a.id] = fa.first + fx to fa.second + fy
            forces[b.id] = fb.first - fx to fb.second - fy
        }

        // ── 3. 向心力（拉向原点） ──
        layout.nodes.forEach { node ->
            val dist = sqrt(node.x * node.x + node.y * node.y)
            if (dist > 0.01f) {
                val fx = -node.x * CENTRIPETAL
                val fy = -node.y * CENTRIPETAL
                val f = forces[node.id]!!
                forces[node.id] = f.first + fx to f.second + fy
            }
        }

        // ── 4. 积分（更新速度与位置） ──
        val newNodes = layout.nodes.map { node ->
            val (fx, fy) = forces[node.id]!!
            var nvx = (node.vx + fx) * DAMPING
            var nvy = (node.vy + fy) * DAMPING

            // 速度上限
            val speed = sqrt(nvx * nvx + nvy * nvy)
            if (speed > SPEED_LIMIT) {
                nvx = (nvx / speed) * SPEED_LIMIT
                nvy = (nvy / speed) * SPEED_LIMIT
            }

            // 根节点固定
            if (node.id == "root") {
                node.copy(vx = 0f, vy = 0f, x = 0f, y = 0f)
            } else {
                node.copy(
                    vx = nvx,
                    vy = nvy,
                    x = node.x + nvx,
                    y = node.y + nvy
                )
            }
        }

        return TreeLayout(nodes = newNodes, edges = layout.edges)
    }

    // ─── 工具 ───

    private fun computeRateForCard(card: com.xinyu.yuweng.data.local.entity.MemoryCardEntity): Float {
        val now = System.currentTimeMillis()
        val ageDays = ((now - card.createdAt) / (1000 * 60 * 60 * 24)).toInt()
        val fadingDays = if (card.status == "fading") {
            card.forgottenAt?.let { ((now - it) / (1000 * 60 * 60 * 24)).toInt() } ?: 0
        } else 0
        return StateEngine.computeCardForgettingRate(card.status, ageDays, fadingDays, card.recallCount)
    }
}
