package com.xinyu.yuweng.feature.status

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xinyu.yuweng.core.designsystem.XinyuFontSize
import com.xinyu.yuweng.core.designsystem.XinyuRadius
import com.xinyu.yuweng.core.designsystem.XinyuSpacing
import com.xinyu.yuweng.core.designsystem.theme.*
import com.xinyu.yuweng.core.stateengine.MoodLabel
import com.xinyu.yuweng.core.stateengine.PreferenceVector
import com.xinyu.yuweng.core.stateengine.SilenceState
import com.xinyu.yuweng.core.stateengine.YuwengState

/**
 * 屿翁状态页（验收标准：心情 MoodChip + 能量 + 偏好行为隐喻 + 沉默状态卡）。
 */
@Composable
fun StatusScreen(viewModel: StatusViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatusScreenContent(
        state = state.yuwengState,
        isSilent = state.isSilent,
        silenceState = state.silenceState,
        pendingCount = state.pendingCount,
        moodHistory = state.moodHistory,
        onNavigateToChat = {},
        onNavigateToMemory = {},
        onNavigateToForgottenLog = {}
    )
}

@Composable
fun StatusScreenContent(
    state: YuwengState = YuwengState(),
    isSilent: Boolean = false,
    silenceState: SilenceState? = null,
    pendingCount: Int = 0,
    moodHistory: List<Float> = emptyList(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
    onNavigateToForgottenLog: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonWhite)
            .verticalScroll(scrollState)
            .padding(horizontal = XinyuSpacing.xl)
    ) {
        Spacer(modifier = Modifier.height = XinyuSpacing.xl))

        Text(
            text = "屿翁的状态",
            style = XinyuFontSize.h1,
            color = InkColor
        )

        Spacer(modifier = Modifier.height = XinyuSpacing.xl))

        // 沉默状态卡（沉默时显示）
        if (isSilent && silenceState != null) {
            SilenceStatusCard(
                silenceState = silenceState,
                pendingCount = pendingCount,
                onNavigateToChat = onNavigateToChat
            )
            Spacer(modifier = Modifier.height = XinyuSpacing.xl))
        }

        // 心情
        MoodSection(
            mood = state.mood,
            moodHistory = moodHistory,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height = XinyuSpacing.xl))

        // 能量
        EnergySection(energy = state.energy)

        Spacer(modifier = Modifier.height = XinyuSpacing.xl))

        // 偏好（行为隐喻，values 隐藏）
        PreferenceSection(preference = state.preference)

        Spacer(modifier = Modifier.height = XinyuSpacing.xl))

        // 入口
        EntrySection(
            onNavigateToMemory = onNavigateToMemory,
            onNavigateToForgottenLog = onNavigateToForgottenLog
        )

        Spacer(modifier = Modifier.height = XinyuSpacing.xxl))
    }
}

@Composable
fun MoodSection(
    mood: Float,
    moodHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val moodLabel = MoodLabel.fromValue(mood)
    val barProgress = ((mood + 10f) / 20f).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = barProgress,
        animationSpec = tween(durationMillis = 800),
        label = "moodProgress"
    )

    XinyuCardWithTitle(title = "心情", modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = moodLabel.display,
                style = XinyuFontSize.h3,
                color = when {
                    mood < -3 -> MistBlue
                    mood > 3 -> WarmGold
                    else -> DaiGray
                }
            )
            if (expanded) {
                Spacer(modifier = Modifier.width(XinyuSpacing.sm))
                Text(
                    text = "${"%.1f".format(mood)}/10",
                    style = XinyuFontSize.caption,
                    color = FogGray
                )
            }
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.md))

        // 心情条形
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(XinyuRadius.pill))
                .background(Divider)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(XinyuRadius.pill))
                    .background(
                        when {
                            mood < -3 -> MistBlue
                            mood > 3 -> WarmGold
                            else -> MossGreen
                        }
                    )
            )
        }

        // 最近7天迷你曲线
        if (moodHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(XinyuSpacing.lg))
            MiniMoodCurve(moodHistory)
        }
    }
}

@Composable
fun MiniMoodCurve(history: List<Float>) {
    // Simple ASCII-style mood curve
    val normalized = history.takeLast(7).map { ((it + 10f) / 20f * 4).toInt().coerceIn(0, 4) }
    val lines = (0..4).reversed().map { level ->
        normalized.joinToString(" ") { v ->
            if (v >= level) "▓" else " "
        }
    }

    Column {
        lines.forEach { line ->
            Text(
                text = line,
                fontSize = 10.sp,
                color = FogGray,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun EnergySection(energy: Int) {
    val animatedEnergy by animateFloatAsState(
        targetValue = energy.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "energyProgress"
    )

    val energyText = when {
        energy > 80 -> "状态极佳"
        energy in 50..80 -> "正常"
        energy in 25..50 -> "偏低"
        energy in 1..24 -> "很低"
        else -> "不说话了"
    }

    XinyuCardWithTitle(title = "能量") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$energy",
                style = XinyuFontSize.h2,
                color = when {
                    energy > 50 -> MossGreen
                    energy > 25 -> SunsetOrange
                    else -> FogGray
                }
            )
            Spacer(modifier = Modifier.width(XinyuSpacing.sm))
            Text(
                text = "/100 ($energyText)",
                style = XinyuFontSize.caption,
                color = FogGray
            )
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.md))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(XinyuRadius.pill))
                .background(Divider)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedEnergy / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(XinyuRadius.pill))
                    .background(
                        when {
                            energy > 50 -> MossGreen
                            energy > 25 -> SunsetOrange
                            else -> FogGray
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.sm))

        Text(
            text = "你无法给屿翁充电。他只是有时候精神好，有时候不好。",
            style = XinyuFontSize.micro,
            color = FogGray,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
fun PreferenceSection(preference: PreferenceVector?) {
    XinyuCardWithTitle(title = "偏好") {
        if (preference == null) {
            Text(
                text = "屿翁还在认识你。他的偏好正在形成中。",
                style = XinyuFontSize.caption,
                color = FogGray,
                fontStyle = FontStyle.Italic
            )
        } else {
            // topics 行为隐喻
            val topTopics = preference.topics.entries
                .sortedByDescending { kotlin.math.abs(it.value) }
                .take(3)
            if (topTopics.isNotEmpty()) {
                Text(
                    text = "屿翁最近似乎…",
                    style = XinyuFontSize.caption,
                    color = FogGray
                )
                Spacer(modifier = Modifier.height(XinyuSpacing.sm))
                topTopics.forEach { (topic, value) ->
                    val preference = if (value > 0) "喜欢" else "不太喜欢"
                    Text(
                        text = "$preference $topic",
                        style = XinyuFontSize.body,
                        color = DaiGray
                    )
                }
            }
            // rhythm 隐喻
            val topRhythm = preference.rhythm.entries
                .sortedByDescending { kotlin.math.abs(it.value) }
                .firstOrNull()
            if (topRhythm != null) {
                val rhythmText = if (topRhythm.value > 0) "倾向于${topRhythm.key}" else "不太想${topRhythm.key}"
                Text(
                    text = rhythmText,
                    style = XinyuFontSize.body,
                    color = DaiGray
                )
            }

            Spacer(modifier = Modifier.height(XinyuSpacing.md))

            Text(
                text = "这是屿翁的内核，他不展示。",
                style = XinyuFontSize.micro,
                color = FogGray,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
fun EntrySection(
    onNavigateToMemory: () -> Unit,
    onNavigateToForgottenLog: () -> Unit
) {
    Column {
        Text(
            text = "记忆入口",
            style = XinyuFontSize.h3,
            color = InkColor
        )
        Spacer(modifier = Modifier.height(XinyuSpacing.md))

        // 屿翁记得
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(XinyuRadius.sm))
                .background(SecondaryBg)
                .clickable { onNavigateToMemory() }
                .padding(XinyuSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📝", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(XinyuSpacing.md))
            Text("屿翁记得", style = XinyuFontSize.body, color = InkColor)
            Spacer(modifier = Modifier.weight(1f))
            Text("→", color = FogGray)
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.sm))

        // 遗忘日志
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(XinyuRadius.sm))
                .background(SecondaryBg)
                .clickable { onNavigateToForgottenLog() }
                .padding(XinyuSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🍃", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(XinyuSpacing.md))
            Text("遗忘日志", style = XinyuFontSize.body, color = InkColor)
            Spacer(modifier = Modifier.weight(1f))
            Text("→", color = FogGray)
        }
    }
}

@Composable
fun SilenceStatusCard(
    silenceState: SilenceState,
    pendingCount: Int,
    onNavigateToChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(XinyuRadius.lg))
            .background(MistBlue.copy(alpha = 0.1f))
            .padding(XinyuSpacing.xl)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(XinyuRadius.pill))
                        .background(FogGray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("屿", fontSize = 18.sp, color = FogGray)
                }
                Spacer(modifier = Modifier.width(XinyuSpacing.md))
                Column {
                    Text(
                        text = silenceState.uiText,
                        style = XinyuFontSize.body,
                        color = DaiGray
                    )
                    Text(
                        text = SilenceState.triggerDescription(silenceState.trigger),
                        style = XinyuFontSize.caption,
                        color = FogGray
                    )
                }
            }

            if (pendingCount > 0) {
                Spacer(modifier = Modifier.height(XinyuSpacing.md))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(XinyuRadius.sm))
                        .background(WarmGold.copy(alpha = 0.15f))
                        .clickable { onNavigateToChat() }
                        .padding(XinyuSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "还没看的消息 · ${pendingCount} 条",
                        style = XinyuFontSize.caption,
                        color = DaiGray
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text("→", color = FogGray)
                }
            }
        }
    }
}

@Composable
fun XinyuCardWithTitle(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(XinyuRadius.md))
            .background(Color.White)
            .padding(XinyuSpacing.lg)
    ) {
        Text(
            text = title,
            style = XinyuFontSize.h3,
            color = DaiGray
        )
        Spacer(modifier = Modifier.height(XinyuSpacing.lg))
        content()
    }
}


