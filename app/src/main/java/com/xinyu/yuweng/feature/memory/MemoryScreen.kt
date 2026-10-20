package com.xinyu.yuweng.feature.memory

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xinyu.yuweng.core.designsystem.XinyuRadius
import com.xinyu.yuweng.core.designsystem.XinyuSpacing
import com.xinyu.yuweng.core.designsystem.theme.DaiGray
import com.xinyu.yuweng.core.designsystem.theme.DuskPurple
import com.xinyu.yuweng.core.designsystem.theme.FogGray
import com.xinyu.yuweng.core.designsystem.theme.InkColor
import com.xinyu.yuweng.core.designsystem.theme.MistBlue
import com.xinyu.yuweng.core.designsystem.theme.MoonWhite
import com.xinyu.yuweng.core.designsystem.theme.MossGreen
import com.xinyu.yuweng.core.designsystem.theme.SecondaryBg
import com.xinyu.yuweng.core.designsystem.theme.SunsetOrange
import com.xinyu.yuweng.core.designsystem.theme.WarmGold
import com.xinyu.yuweng.core.stateengine.StateEngine
import com.xinyu.yuweng.data.local.entity.MemoryCardEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── 卡片类型元数据 ───

private data class CardTypeInfo(
    val emoji: String,
    val label: String,
    val color: Color
)

private val CARD_TYPE_MAP = mapOf(
    "moment" to CardTypeInfo("🌊", "时刻", SunsetOrange),
    "quote" to CardTypeInfo("💬", "话语", WarmGold),
    "question" to CardTypeInfo("❓", "疑问", MistBlue),
    "feeling" to CardTypeInfo("💗", "感受", DuskPurple),
    "song" to CardTypeInfo("🎵", "旋律", MossGreen),
    "self" to CardTypeInfo("🌙", "自我", MoonWhite)
)

private val StarBlue = Color(0xFF2C3E6B)

// ─── ViewModel 包装层 ───

@Composable
fun MemoryScreen(viewModel: MemoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MemoryScreenContent(
        activeCards = state.activeCards,
        forgettingLog = state.forgettingLog,
        forgottenCount = state.forgottenCount,
        selectedView = state.selectedView,
        onViewSelected = { viewModel.selectView(it) },
        onRedeem = { viewModel.redeemCard(it) }
    )
}

// ─── 主屏幕 Composable ───

@Composable
fun MemoryScreenContent(
    activeCards: List<MemoryCardEntity> = emptyList(),
    forgettingLog: List<ForgettingLogEntry> = emptyList(),
    forgottenCount: Int = 0,
    selectedView: String = "card",
    onViewSelected: (String) -> Unit = {},
    onCardClick: (MemoryCardEntity) -> Unit = {},
    onRedeem: (Long) -> Unit = {},
    onSearch: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonWhite)
            .padding(horizontal = XinyuSpacing.xl)
    ) {
        Spacer(modifier = Modifier.height(XinyuSpacing.lg))

        // ── 分段切换 ──
        SegmentedToggle(
            options = listOf("卡片", "树状"),
            selectedIndex = if (selectedView == "tree") 1 else 0,
            onSelect = { idx -> onViewSelected(if (idx == 1) "tree" else "card") }
        )

        Spacer(modifier = Modifier.height(XinyuSpacing.xl))

        if (selectedView == "tree") {
            TreeViewContent(activeCards = activeCards)
        } else {
            CardViewContent(
                activeCards = activeCards,
                forgettingLog = forgettingLog,
                forgottenCount = forgottenCount,
                onCardClick = onCardClick,
                onRedeem = onRedeem
            )
        }
    }
}

// ─── 分段切换组件 ───

@Composable
private fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(XinyuRadius.md))
            .background(SecondaryBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            val animAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "segment_alpha"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(XinyuRadius.sm))
                    .background(
                        if (isSelected) WarmGold.copy(alpha = 0.8f) else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) InkColor else FogGray,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

// ─── 卡片视图 ───

@Composable
private fun CardViewContent(
    activeCards: List<MemoryCardEntity>,
    forgettingLog: List<ForgettingLogEntry>,
    forgottenCount: Int,
    onCardClick: (MemoryCardEntity) -> Unit,
    onRedeem: (Long) -> Unit
) {
    // 统计
    val activeCount = activeCards.count { it.status != "forgotten" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(XinyuSpacing.lg)
    ) {
        // ── 摘要栏 ──
        item {
            SummaryBar(activeCount = activeCount, forgottenCount = forgottenCount)
            Spacer(modifier = Modifier.height(XinyuSpacing.lg))
        }

        // ── 遗忘日志条目（最近一条，如有未赎回的） ──
        forgettingLog.filter { !it.redeemed }.firstOrNull()?.let { entry ->
            item {
                ForgottenLogEntry(
                    entry = entry,
                    onRedeem = { onRedeem(entry.id) }
                )
                Spacer(modifier = Modifier.height(XinyuSpacing.lg))
            }
        }

        // ── 空状态 ──
        if (activeCards.isEmpty()) {
            item {
                EmptyState()
            }
        } else {
            // ── 卡片列表 ──
            items(
                items = activeCards.filter { it.status != "forgotten" },
                key = { it.id }
            ) { card ->
                MemoryCardItem(
                    card = card,
                    onClick = { onCardClick(card) }
                )
            }
        }

        // 底部留白
        item {
            Spacer(modifier = Modifier.height(XinyuSpacing.huge))
        }
    }
}

// ─── 摘要栏 ───

@Composable
private fun SummaryBar(activeCount: Int, forgottenCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(XinyuRadius.md),
        color = SecondaryBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = XinyuSpacing.lg, vertical = XinyuSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "屿翁记得 $activeCount 张 · 忘了 $forgottenCount 张",
                color = DaiGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── 遗忘日志条目 ───

@Composable
private fun ForgottenLogEntry(
    entry: ForgettingLogEntry,
    onRedeem: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM月dd日", Locale.CHINA) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(XinyuRadius.md),
        color = DuskPurple.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(XinyuSpacing.lg))
        ) {
            Text(
                text = dateFormat.format(Date(entry.forgottenAt)),
                color = FogGray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(XinyuSpacing.sm))
            Text(
                text = entry.contentSnapshot,
                color = InkColor,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(XinyuSpacing.xs))
            Text(
                text = entry.whyForgotten,
                color = FogGray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(XinyuSpacing.md))
            Surface(
                shape = RoundedCornerShape(XinyuRadius.pill),
                color = MistBlue.copy(alpha = 0.3f),
                modifier = Modifier
                    .clip(RoundedCornerShape(XinyuRadius.pill))
                    .clickable { onRedeem() }
                    .padding(horizontal = XinyuSpacing.lg, vertical = 8.dp)
            ) {
                Text(
                    text = "赎回这张记忆",
                    color = Color(0xFF2C3E6B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── 空状态 ───

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = XinyuSpacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🌱",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(XinyuSpacing.lg))
        Text(
            text = "这棵树还没长叶子。屿翁还在观察。",
            color = FogGray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ─── 单张记忆卡片项 ───

@Composable
private fun MemoryCardItem(
    card: MemoryCardEntity,
    onClick: () -> Unit
) {
    val typeInfo = CARD_TYPE_MAP[card.cardType] ?: CardTypeInfo("📝", "其他", FogGray)

    var expandedNote by remember { mutableStateOf(false) }

    // 计算遗忘率
    val now = System.currentTimeMillis()
    val ageDays = ((now - card.createdAt) / (1000 * 60 * 60 * 24)).toInt()
    val fadingDays = if (card.status == "fading") {
        card.forgottenAt?.let { ((now - it) / (1000 * 60 * 60 * 24)).toInt() } ?: 0
    } else 0
    val forgettingRate = StateEngine.computeCardForgettingRate(
        card.status, ageDays, fadingDays, card.recallCount
    )

    // 状态徽章
    val statusLabel = when (card.status) {
        "fresh" -> "新生"
        "stable" -> "稳定"
        "fading" -> "褪色"
        "forgotten" -> "遗忘"
        else -> ""
    }
    val statusColor = when (card.status) {
        "fresh" -> MossGreen
        "stable" -> WarmGold
        "fading" -> SunsetOrange
        "forgotten" -> FogGray
        else -> FogGray
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(XinyuRadius.md))
            .clickable { onClick() },
        shape = RoundedCornerShape(XinyuRadius.md),
        color = MoonWhite,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(XinyuSpacing.lg))
        ) {
            // ── 顶部：类型徽章 + 状态徽章 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 类型徽章
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(typeInfo.color.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = typeInfo.emoji,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(XinyuSpacing.sm))
                    Text(
                        text = typeInfo.label,
                        color = typeInfo.color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 状态徽章
                Surface(
                    shape = RoundedCornerShape(XinyuRadius.pill),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = statusLabel,
                        color = DaiGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(XinyuSpacing.md))

            // ── 卡片内容 ──
            Text(
                text = card.content,
                color = InkColor,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(XinyuSpacing.sm))

            // ── 情感权重条 ──
            AnimatedProgressBar(
                progress = card.emotionalWeight,
                color = typeInfo.color,
                label = "情感强度"
            )

            Spacer(modifier = Modifier.height(XinyuSpacing.xs))

            // ── 遗忘率条 ──
            AnimatedProgressBar(
                progress = forgettingRate / 100f,
                color = FogGray,
                label = "遗忘率 ${"%.0f".format(forgettingRate)}%",
                showLabel = true
            )

            Spacer(modifier = Modifier.height(XinyuSpacing.sm))

            // ── 底部信息 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.CHINA) }
                Text(
                    text = "回忆 ${card.recallCount} 次",
                    color = FogGray,
                    fontSize = 12.sp
                )
                Text(
                    text = dateFormat.format(Date(card.createdAt)),
                    color = FogGray,
                    fontSize = 12.sp
                )
            }

            // ── 可展开的屿翁口吻注解 ──
            Spacer(modifier = Modifier.height(XinyuSpacing.sm))
            Text(
                text = if (expandedNote) "收起 ▲" else "屿翁的话 ▼",
                color = FogGray,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(XinyuRadius.sm))
                    .clickable { expandedNote = !expandedNote }
                    .padding(vertical = 4.dp)
            )

            if (expandedNote) {
                val note = StateEngine.getCardLifecycleDescription(
                    card.status, ageDays, fadingDays
                )
                if (note.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(XinyuRadius.sm),
                        color = SecondaryBg.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = XinyuSpacing.xs))
                    ) {
                        Text(
                            text = note,
                            color = DaiGray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(XinyuSpacing.sm))
                        )
                    }
                }
            }
        }
    }
}

// ─── 动画进度条 ───

@Composable
private fun AnimatedProgressBar(
    progress: Float,
    color: Color,
    label: String = "",
    showLabel: Boolean = false
) {
    val animProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progress_anim"
    )

    Column {
        if (showLabel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    color = FogGray,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(XinyuRadius.pill))
                .background(DividerColor())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animProgress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(XinyuRadius.pill))
                    .background(color)
            )
        }
    }
}

// ─── 树视图 ───

@Composable
private fun TreeViewContent(activeCards: List<MemoryCardEntity>) {
    val layout = remember(activeCards) {
        val tree = MemoryTreeLayout.buildTree(activeCards)
        MemoryTreeLayout.simulate(tree)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(XinyuRadius.md))
            .background(StarBlue)
    ) {
        TreeCanvas(layout = layout)

        // 底部图例
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(XinyuSpacing.lg)
        ) {
            TreeLegend()
        }
    }
}

// ─── 树视图画布 ───

@Composable
private fun TreeCanvas(layout: TreeLayout) {
    val nodeMap = remember(layout) { layout.nodes.associateBy { it.id } }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // ── 绘制边 ──
        layout.edges.forEach { edge ->
            val from = nodeMap[edge.from]
            val to = nodeMap[edge.to]
            if (from != null && to != null) {
                val x1 = centerX + from.x
                val y1 = centerY + from.y
                val x2 = centerX + to.x
                val y2 = centerY + to.y

                if (edge.isCrossBranch) {
                    // 跨分支边：虚线效果
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 1.5f
                    )
                } else {
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // ── 绘制节点 ──
        layout.nodes.forEach { node ->
            val cx = centerX + node.x
            val cy = centerY + node.y
            val nodeColor = Color(node.color).let { base ->
                when (node.status) {
                    "fading" -> base.copy(alpha = 0.5f)
                    "fresh" -> base.copy(alpha = 1f)
                    "stable" -> base
                    else -> base.copy(alpha = 0.7f)
                }
            }
            val nodeRadius = node.radius * (size.width / 900f).coerceIn(0.6f, 1.2f)

            // 节点圆形
            drawCircle(
                color = nodeColor,
                radius = nodeRadius,
                center = Offset(cx, cy)
            )

            // 边框
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = nodeRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )

            // 标签文字
            val labelText = node.label
            val textSizePt = (nodeRadius * 0.45f).coerceIn(8f, 14f)

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(230, 45, 45, 45)
                    this.textSize = textSizePt * density
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                drawText(labelText, cx, cy + textSizePt * density * 0.35f, paint)
            }
        }
    }
}

// ─── 树视图图例 ───

@Composable
private fun TreeLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CARD_TYPE_MAP.entries.forEach { (type, info) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(info.color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = info.label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ─── 辅助颜色 ───

private fun DividerColor(): Color = Color(0xFFD4D4D4)

// ─── Preview ───

@Preview(showBackground = true)
@Composable
fun MemoryScreenPreview() {
    val now = System.currentTimeMillis()
    val sampleCards = listOf(
        MemoryCardEntity(
            id = 1L,
            userId = 1L,
            cardType = "moment",
            content = "那天你告诉我你养了一只猫，叫奶茶。你说它总是跳上键盘打断你工作。",
            context = "用户分享了关于宠物的生活细节",
            emotionalWeight = 0.7f,
            status = "stable",
            recallCount = 5,
            createdAt = now - 86400000L * 10
        ),
        MemoryCardEntity(
            id = 2L,
            userId = 1L,
            cardType = "feeling",
            content = "你说下雨天让你想起小时候的夏天，空气里有青草的味道。",
            context = "用户描述了一种感官记忆",
            emotionalWeight = 0.8f,
            status = "fresh",
            recallCount = 1,
            createdAt = now - 86400000L * 3
        ),
        MemoryCardEntity(
            id = 3L,
            userId = 1L,
            cardType = "quote",
            content = "你引用了史铁生：\"人的故乡，并不止于一块特定的土地，而是一种辽阔无比的心情。\"",
            context = "用户分享了文学引用",
            emotionalWeight = 0.6f,
            status = "fading",
            recallCount = 0,
            createdAt = now - 86400000L * 40,
            forgottenAt = now - 86400000L * 5
        ),
        MemoryCardEntity(
            id = 4L,
            userId = 1L,
            cardType = "song",
            content = "你推荐了《晴天》给我，说前奏一响就想回到高中。",
            context = "用户分享了一首喜欢的歌",
            emotionalWeight = 0.5f,
            status = "stable",
            recallCount = 3,
            createdAt = now - 86400000L * 20
        ),
        MemoryCardEntity(
            id = 5L,
            userId = 1L,
            cardType = "question",
            content = "你问我：如果记忆可以删除，你想忘掉什么？",
            context = "用户提出了一个哲学性问题",
            emotionalWeight = 0.9f,
            status = "fresh",
            recallCount = 0,
            createdAt = now - 86400000L * 1
        )
    )

    val forgettingLog = listOf(
        ForgettingLogEntry(
            id = 100L,
            contentSnapshot = "你说你最喜欢的季节是秋天。",
            forgottenAt = now - 86400000L * 2,
            whyForgotten = "很久没有被提起，慢慢褪色了",
            redeemed = false
        )
    )

    MemoryScreenContent(
        activeCards = sampleCards,
        forgettingLog = forgettingLog,
        forgottenCount = 1,
        selectedView = "card"
    )
}

@Preview(showBackground = true)
@Composable
fun MemoryScreenTreePreview() {
    val now = System.currentTimeMillis()
    val sampleCards = listOf(
        MemoryCardEntity(
            id = 1L,
            userId = 1L,
            cardType = "moment",
            content = "那天你告诉我你养了一只猫，叫奶茶。",
            context = "关于宠物",
            emotionalWeight = 0.7f,
            status = "stable",
            recallCount = 5,
            createdAt = now - 86400000L * 10
        ),
        MemoryCardEntity(
            id = 2L,
            userId = 1L,
            cardType = "moment",
            content = "你说下雨天让你想起小时候的夏天。",
            context = "关于雨天",
            emotionalWeight = 0.8f,
            status = "fresh",
            recallCount = 1,
            createdAt = now - 86400000L * 3
        ),
        MemoryCardEntity(
            id = 3L,
            userId = 1L,
            cardType = "feeling",
            content = "你说下雨天让你想起故乡的味道。",
            context = "关于乡愁",
            emotionalWeight = 0.6f,
            status = "fading",
            recallCount = 0,
            createdAt = now - 86400000L * 40
        ),
        MemoryCardEntity(
            id = 4L,
            userId = 1L,
            cardType = "quote",
            content = "你引用了史铁生关于故乡的文字。",
            context = "关于文学",
            emotionalWeight = 0.5f,
            status = "stable",
            recallCount = 3,
            createdAt = now - 86400000L * 20
        ),
        MemoryCardEntity(
            id = 5L,
            userId = 1L,
            cardType = "question",
            content = "你问我：如果记忆可以删除，你想忘掉什么？",
            context = "关于选择",
            emotionalWeight = 0.9f,
            status = "fresh",
            recallCount = 0,
            createdAt = now - 86400000L * 1
        ),
        MemoryCardEntity(
            id = 6L,
            userId = 1L,
            cardType = "song",
            content = "你推荐了《晴天》给我。",
            context = "关于音乐",
            emotionalWeight = 0.5f,
            status = "stable",
            recallCount = 3,
            createdAt = now - 86400000L * 15
        ),
        MemoryCardEntity(
            id = 7L,
            userId = 1L,
            cardType = "self",
            content = "你说你最近在学着和自己相处。",
            context = "关于自我",
            emotionalWeight = 0.8f,
            status = "fresh",
            recallCount = 0,
            createdAt = now - 86400000L * 2
        )
    )

    MemoryScreenContent(
        activeCards = sampleCards,
        forgettingLog = emptyList(),
        forgottenCount = 0,
        selectedView = "tree"
    )
}
