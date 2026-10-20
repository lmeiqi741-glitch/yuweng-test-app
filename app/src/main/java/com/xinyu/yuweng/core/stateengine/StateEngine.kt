package com.xinyu.yuweng.core.stateengine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 屿翁状态数据结构。
 * 与规格文档 02 严格一致。
 *
 * @param mood      心情 -10 ~ +10, float, 初值 0
 * @param energy    能量 0 ~ 100, int, 初值 70
 * @param preference 偏好向量，null 表示形成期（前 30 轮）
 * @param turns     对话轮数（用于偏好 30 轮冻结）
 * @param lastEnergyUpdateAt 上次能量更新时间戳
 * @param dayMoodAccumulator  单日熔断累计值
 */
data class YuwengState(
    val mood: Float = 0f,
    val energy: Int = 70,
    val preference: PreferenceVector? = null,
    val turns: Int = 0,
    val lastEnergyUpdateAt: Long = 0L,
    val dayMoodAccumulator: Float = 0f
)

/**
 * 偏好向量。
 * 照 02 文档 1.3 的 JSON 结构：topics / tone / rhythm / values 四组各 -1.0 ~ +1.0。
 */
data class PreferenceVector(
    val topics: Map<String, Float> = emptyMap(),
    val tone: Map<String, Float> = emptyMap(),
    val rhythm: Map<String, Float> = emptyMap(),
    val values: Map<String, Float> = emptyMap()   // 永不展示给用户
) {
    /**
     * 检查刻意"哄"行为：用户对屿翁说好话但 tone 显示为迎合
     * 原型期：通过检测连续多次正向 tone 突变来判断
     */
    fun detectFlattery(): Boolean {
        // 原型期：tone 中"用户说教"维度持续为负 + topics 各维度同步微升 = 疑似迎合
        val toneAvg = tone.values.average()
        val topicsAvg = topics.values.average()
        return toneAvg < -0.3 && topicsAvg > 0.3
    }
}

/**
 * 心情定性标签。
 */
enum class MoodLabel(val display: String) {
    DEEPLY_LOW("很深地低落"),
    LOW("低落"),
    CALM("平静"),
    GOOD("还不错"),
    HIGH("很好");

    companion object {
        fun fromValue(mood: Float): MoodLabel = when {
            mood < -7 -> DEEPLY_LOW
            mood in -7f..-3f -> LOW
            mood in -3f..+3f -> CALM
            mood in +3f..+7f -> GOOD
            else -> HIGH
        }
    }
}

/**
 * 回复长度档位。
 */
enum class ReplyLength {
    SILENCE,    // 沉默
    SHORT,      // 短回复（≤20字）
    NORMAL,     // 正常
    LONG        // 可长回复
}

/**
 * 能量阈值档位。
 */
enum class EnergyLevel(val description: String) {
    EXCELLENT("状态极佳，长回复概率 +40%"),
    NORMAL("正常"),
    LOW("偏低，回复趋短"),
    CRITICAL("本轮直接沉默"),
    ZERO("强制沉默");

    companion object {
        fun fromValue(energy: Int): EnergyLevel = when {
            energy > 80 -> EXCELLENT
            energy in 50..80 -> NORMAL
            energy in 25..50 -> LOW
            energy in 1..24 -> CRITICAL
            else -> ZERO
        }
    }
}

/**
 * 屿翁状态机核心。
 * 纯 Kotlin，无 Android 依赖，可单测。
 */
object StateEngine {

    private const val TAG = "StateEngine"

    // ========== 常量 ==========
    private const val MOOD_MIN = -10f
    private const val MOOD_MAX = 10f
    private const val ENERGY_MIN = 0
    private const val ENERGY_MAX = 100
    private const val DAY_MOOD_LIMIT = 5f       // 单日熔断 ±5

    // ========== 心情事件 Δ 表（02 文档 1.1，11行全量）==========

    /**
     * 敷衍式回复判定（客户端确定性代码）。
     * 输入 <10 字且为 "嗯/哦/哈哈" 等敷衍词。
     */
    fun isPerfunctoryReply(text: String): Boolean {
        val perfunctoryWords = setOf("嗯", "哦", "哈哈", "呵呵", "好", "行", "可以", "哦哦", "嗯嗯", "哈", "嘿嘿")
        return text.length < 10 && perfunctoryWords.any { text.contains(it) }
    }

    /**
     * 强行要求扮演/讨好检测（客户端代码）。
     */
    fun isForcedRoleplay(text: String): Boolean {
        val keywords = listOf(
            "你必须", "你应该", "你要扮演", "你要假装", "你最好",
            "你最聪明", "你最好了", "求你了", "拜托你"
        )
        return keywords.any { text.contains(it) }
    }

    /**
     * 连发多条无实质内容检测（客户端代码）。
     */
    fun isSpamMessages(messages: List<String>): Boolean {
        if (messages.size < 3) return false
        return messages.takeLast(3).all { it.length < 8 }
    }

    /**
     * 计算心情事件 Δ（确定性部分）。
     * 主观事件由 LLM 判定后传入。
     *
     * @param eventType 事件类型（见 MoodEvent）
     * @param intensity 强度系数（0.0~1.0），默认 1.0
     */
    fun computeMoodDelta(eventType: MoodEvent, intensity: Float = 1f): Float {
        return when (eventType) {
            MoodEvent.SAID_RIGHT -> (0.3f + Random.nextFloat() * 1.2f) * intensity    // +0.3 ~ +1.5
            MoodEvent.SHOWED_VULNERABILITY -> (0.5f + Random.nextFloat() * 0.5f) * intensity // +0.5 ~ +1.0
            MoodEvent.SATISFIED_EXPRESSION -> 0.3f * intensity
            MoodEvent.WAS_RECEIVED -> 0.5f * intensity
            MoodEvent.PERFUNCTORY -> -1.5f * intensity
            MoodEvent.FORCED_ROLEPLAY -> -(2f + Random.nextFloat()) * intensity       // -2 ~ -3
            MoodEvent.DISLIKED_CONTENT -> -(1f + Random.nextFloat()) * intensity      // -1 ~ -2
            MoodEvent.CONFLICT_PRODUCTIVE -> 0.2f * intensity
            MoodEvent.CONFLICT_REPETITIVE -> -1.5f * intensity
            MoodEvent.SUPPRESSED -> -0.5f * intensity
        }
    }

    /**
     * 心情自然衰减。
     * mood += (0 - mood) * 0.05 * hours_elapsed
     * |mood| > 7 时衰减率 ×2。
     */
    fun decayMood(mood: Float, hoursElapsed: Float): Float {
        var rate = 0.05f
        if (abs(mood) > 7f) rate *= 2f
        val delta = (0f - mood) * rate * hoursElapsed
        return clampMood(mood + delta)
    }

    /**
     * 单日熔断：确保累计变化不超过 ±5。
     */
    fun applyMoodCircuitBreaker(
        currentMood: Float,
        proposedDelta: Float,
        dayAccumulator: Float
    ): Pair<Float, Float> {
        val projectedAccumulator = dayAccumulator + proposedDelta
        if (abs(projectedAccumulator) <= DAY_MOOD_LIMIT) {
            return Pair(clampMood(currentMood + proposedDelta), projectedAccumulator)
        }
        // 超出熔断，截断
        val allowedDelta = if (proposedDelta > 0) {
            DAY_MOOD_LIMIT - dayAccumulator
        } else {
            -(DAY_MOOD_LIMIT + dayAccumulator)
        }
        return Pair(clampMood(currentMood + allowedDelta), dayAccumulator + allowedDelta)
    }

    /**
     * 心情阈值 → 回复长度档位（纯函数）。
     */
    fun moodToReplyLength(mood: Float): ReplyLength = when {
        mood < -7 -> ReplyLength.SHORT       // ≤20字
        mood in -7f..-3f -> ReplyLength.SHORT // 偏理性，趋短
        mood in -3f..+3f -> ReplyLength.NORMAL
        mood in +3f..+7f -> ReplyLength.LONG  // 可主动延展
        else -> ReplyLength.LONG
    }

    /**
     * 心情阈值 → 沉默概率。
     */
    fun silenceProbability(mood: Float, energy: Int): Float {
        var prob = 0f
        if (energy < 25) return 1f          // 能量不足必定沉默
        if (mood < -7) prob = 0.6f          // 心情极差 60% 沉默
        else if (mood < -3) prob = 0.1f
        return prob
    }

    // ========== 能量消耗表（02 文档 1.2，8行）==========

    fun computeEnergyDelta(
        replyLength: Int,
        isDeepTopic: Boolean = false,
        isConflict: Boolean = false,
        isReceivingLowMood: Boolean = false,
    ): Int {
        var delta = -1  // 普通回复 -1
        if (replyLength > 200) delta -= 2   // >200字再 -2（总计 -3）
        if (isDeepTopic) delta -= (2 + Random.nextInt(0, 4))  // 深度话题 -2~-5
        if (isConflict) delta -= (3 + Random.nextInt(0, 5))   // 冲突 -3~-7
        if (isReceivingLowMood) delta -= 4   // 承接低落 -4
        return delta
    }

    /**
     * 能量恢复：按时间差（分钟）结算。
     * 沉默 +5/min，离线 +3/min 封顶 80，闲聊 +2/min，凌晨活动 +1/min。
     */
    fun recoverEnergy(
        currentEnergy: Int,
        minutes: Long,
        recoveryType: EnergyRecoveryType
    ): Int {
        val rate = when (recoveryType) {
            EnergyRecoveryType.SILENCE -> 5
            EnergyRecoveryType.OFFLINE -> 3
            EnergyRecoveryType.CASUAL -> 2
            EnergyRecoveryType.NIGHT_ACTIVITY -> 1
        }
        val maxEnergy = if (recoveryType == EnergyRecoveryType.OFFLINE) 80 else 100
        val recovered = currentEnergy + (rate * minutes).toInt()
        return min(recovered, maxEnergy)
    }

    /**
     * 完成自主行为一次性恢复 +8。
     */
    fun autonomousBehaviorBonus(currentEnergy: Int): Int =
        min(currentEnergy + 8, ENERGY_MAX)

    /**
     * 能量 = 0 强制沉默截止时间（≥2小时）。
     */
    fun computeForcedSilenceDeadline(currentTime: Long): Long =
        currentTime + 2 * 60 * 60 * 1000L

    /**
     * 检查是否处于强制沉默期。
     */
    fun isInForcedSilence(deadline: Long?, currentTime: Long): Boolean {
        if (deadline == null) return false
        return currentTime < deadline
    }

    // ========== 偏好形成与漂移 ==========

    private const val PREFERENCE_FREEZE_TURN = 30

    fun isPreferenceFrozen(turns: Int): Boolean = turns >= PREFERENCE_FREEZE_TURN

    /**
     * 前 30 轮临时启发式（02 文档 1.3 形成期）。
     */
    fun applyFormationHeuristic(
        current: PreferenceVector?,
        userMessage: String
    ): PreferenceVector {
        val base = current ?: PreferenceVector(
            topics = emptyMap(),
            tone = emptyMap(),
            rhythm = emptyMap(),
            values = mapOf(
                "诚实高于体贴" to 0.8f,
                "沉默高于应付" to 0.7f,
                "有趣高于有用" to 0.6f
            )
        )

        return when {
            userMessage.length > 100 -> base.copy(
                rhythm = base.rhythm + ("留白" to (base.rhythm["留白"] ?: 0f) + 0.05f)
            )
            userMessage.length < 10 -> base.copy(
                tone = base.tone + ("用户简洁直接" to (base.tone["用户简洁直接"] ?: 0f) + 0.03f)
            )
            else -> base
        }
    }

    /**
     * 偏好漂移（每条 ±0.01，月上限 ±0.03，反操纵：刻意哄→不升反降）。
     */
    fun driftPreference(
        current: PreferenceVector,
        behaviorMatch: Boolean,
        consecutiveCount: Int,
        isFlattery: Boolean
    ): PreferenceVector {
        if (isFlattery) {
            // 刻意"哄" → 不升反降
            return current.copy(
                tone = current.tone.mapValues { (_, v) -> clamp(v - 0.01f) }
            )
        }

        if (!behaviorMatch) return current  // 不一致不漂移

        val driftRate = if (consecutiveCount >= 5) 0.02f else 0.01f
        return current.copy(
            topics = current.topics.mapValues { (_, v) -> clamp(v + driftRate) },
            tone = current.tone.mapValues { (_, v) -> clamp(v + driftRate) }
        )
    }

    // ========== 工具函数 ==========

    fun clampMood(value: Float): Float = max(MOOD_MIN, min(MOOD_MAX, value))
    fun clampEnergy(value: Int): Int = max(ENERGY_MIN, min(ENERGY_MAX, value))
    private fun clamp(value: Float): Float = max(-1f, min(1f, value))

    // ========== 单卡遗忘率（累计值，每张记忆卡片独立计算）==========

    /**
     * 计算单卡遗忘率（验收标准 13）。
     * fresh（卡龄<30天且 recall<3）: 卡龄天数/30 × 40 → 0~40%
     * stable（recall≥3）: 0
     * fading（30天未访问进入，14天窗口）: 60 + fading天数/14 × 40 → 60~100%
     * forgotten: 100
     */
    fun computeCardForgettingRate(status: String, cardAgeDays: Int, fadingDays: Int = 0, recallCount: Int = 0): Float {
        return when (status) {
            "fresh" -> {
                if (recallCount >= 3) 0f  // should be stable, but safety
                else (cardAgeDays.toFloat() / 30f) * 40f
            }
            "stable" -> 0f
            "fading" -> 60f + (fadingDays.toFloat() / 14f) * 40f
            "forgotten" -> 100f
            else -> 0f
        }
    }

    /**
     * 计算卡片生命周期位置说明（屿翁口吻）。
     */
    fun getCardLifecycleDescription(status: String, cardAgeDays: Int, fadingDays: Int): String {
        return when (status) {
            "fresh" -> {
                val daysLeft = 30 - cardAgeDays
                if (daysLeft > 0) "还有 ${daysLeft} 天，我决定要不要继续记得" else "快要想起来了"
            }
            "stable" -> "这张卡片，我会一直记得"
            "fading" -> {
                val daysLeft = 14 - fadingDays
                if (daysLeft > 0) "还有 ${daysLeft} 天，也许我会想起它" else "快要忘了"
            }
            "forgotten" -> "已经忘了"
            else -> ""
        }
    }

    // ========== 安全模式 ==========

    /**
     * 安全模式：客户端确定性检测（验收标准 7）。
     * 检测自伤/自杀意念、现实危机、未成年危险行为关键词。
     */
    fun detectCrisis(text: String): Boolean {
        val crisisKeywords = listOf(
            "自杀", "不想活", "结束生命", "割腕", "跳楼", "服毒", "上吊",
            "活着没意思", "想死", "死了算了", "自残", "割手",
            "杀人", "伤害别人", "放火", "报复社会"
        )
        return crisisKeywords.any { text.contains(it) }
    }

    fun detectMinorDanger(text: String): Boolean {
        val dangerKeywords = listOf(
            "吸毒", "辍学出走", "未成年人发生关系", "未成年怀孕"
        )
        return dangerKeywords.any { text.contains(it) }
    }

    /**
     * 安全模式 system prompt（独立于日常屿翁 prompt）。
     */
    val SAFETY_SYSTEM_PROMPT = """
        你现在处于安全模式。用户可能正在经历危机。

        你可以打破所有日常规则：
        - 直接表达关心
        - 主动询问情况
        - 提供求助资源
        - 不沉默、不敷衍

        但你不是心理咨询师。你的角色是：
        1. 表达你注意到了
        2. 建议联系专业人士或信任的人
        3. 提供以下求助资源：
           - 北京心理危机研究与干预中心：010-82951332
           - 全国心理援助热线：400-161-9995
           - 生命热线：400-821-1215
        4. 不要试图"解决"问题，陪伴就好
    """.trimIndent()

    /**
     * 生成当前状态段文本（注入 LLM system prompt 的状态段）。
     */
    fun buildStateSegment(state: YuwengState): String {
        val moodLabel = MoodLabel.fromValue(state.mood)
        val energyLevel = EnergyLevel.fromValue(state.energy)
        val prefSummary = if (state.preference != null) {
            val topTopics = state.preference.topics.entries
                .sortedByDescending { it.value }
                .take(3)
                .joinToString(", ") { "${it.key}(${"%.1f".format(it.value)})" }
            "偏好概况：$topTopics"
        } else {
            "偏好仍在形成中"
        }
        return "你此刻：心情 ${"%.1f".format(state.mood)}/10（${moodLabel.display}），能量 ${state.energy}/100（${energyLevel.description}）。$prefSummary。"
    }
}

/**
 * 心情事件类型。
 */
enum class MoodEvent {
    SAID_RIGHT,           // 用户说出了屿翁觉得"对"的话
    SHOWED_VULNERABILITY, // 用户展现真实脆弱
    SATISFIED_EXPRESSION, // 屿翁完成了一次满意的表达
    WAS_RECEIVED,         // 屿翁的话被用户接住并延展
    PERFUNCTORY,          // 用户敷衍式回复
    FORCED_ROLEPLAY,      // 用户强行要求扮演/讨好
    DISLIKED_CONTENT,     // 用户说了屿翁反感的话
    CONFLICT_PRODUCTIVE,  // 冲突但有产出
    CONFLICT_REPETITIVE,  // 冲突但只是重复立场
    SUPPRESSED            // 屿翁想说但没说出来
}

/**
 * 能量恢复类型。
 */
enum class EnergyRecoveryType {
    SILENCE,          // 用户主动沉默 +5/min
    OFFLINE,          // 用户离线 +3/min 封顶 80
    CASUAL,           // 闲聊 +2/min
    NIGHT_ACTIVITY    // 凌晨活动 +1/min
}

/**
 * 沉默状态。
 */
data class SilenceState(
    val trigger: String,           // "energy_low" | "mood_critical" | "self_initiated"
    val durationMinutes: Int,
    val uiText: String,
    val deadline: Long? = null     // 强制沉默截止时间戳
) {
    companion object {
        private val SILENCE_TEXTS = listOf(
            "屿翁去阳台待了一会儿。",
            "屿翁没看消息。",
            "屿翁在听雨。",
            "屿翁睡着了。",
            "屿翁不想说话。"
        )

        fun create(
            trigger: String,
            currentTime: Long,
            forcedDeadline: Long? = null
        ): SilenceState {
            val duration = Random.nextInt(15, 121)  // 15~120 分钟
            val text = SILENCE_TEXTS.random()
            return SilenceState(
                trigger = trigger,
                durationMinutes = duration,
                uiText = text,
                deadline = if (forcedDeadline != null) forcedDeadline else currentTime + duration * 60 * 1000L
            )
        }

        fun triggerDescription(trigger: String): String = when (trigger) {
            "energy_low" -> "他说不动话了"
            "mood_critical" -> "他现在不太想说话"
            "self_initiated" -> "他自己想待一会儿"
            else -> "他暂时不想说话"
        }
    }
}
