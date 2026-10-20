package com.xinyu.yuweng.feature.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xinyu.yuweng.core.designsystem.XinyuFontSize
import com.xinyu.yuweng.core.designsystem.XinyuRadius
import com.xinyu.yuweng.core.designsystem.XinyuSpacing
import com.xinyu.yuweng.core.designsystem.theme.*
import com.xinyu.yuweng.core.stateengine.StateEngine

/**
 * 遗忘曲线图（设置页入口，纯展示，体现"遗忘不可控"）。
 * 验收标准 14。
 *
 * 理论曲线四段：
 * - 0-30天 fresh 线：0% → 40% 缓慢上升
 * - 第30天判定点：recall≥3 → stable 逃逸线（水平0%）；否则进入 fading
 * - 30-44天 fading 线：60% → 100%
 * - 第44天：forgotten（100%）
 */
@Composable
fun ForgettingCurveScreen(
    activeCards: List<CardAgePoint> = emptyList(),
    onBack: () -> Unit = {}
) {
    var animationProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1200),
        label = "curveAnimation"
    )

    LaunchedEffect(Unit) {
        animationProgress = 1f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonWhite)
            .padding(horizontal = XinyuSpacing.xl)
    ) {
        Spacer(modifier = Modifier.height(XinyuSpacing.xl))

        Text(
            text = "屿翁的遗忘曲线",
            fontSize = XinyuFontSize.h1,
            color = InkColor
        )

        Spacer(modifier = Modifier.height(XinyuSpacing.md))

        Text(
            text = "大多数记忆会慢慢淡下去。被反复想起的，会留下来。",
            fontSize = XinyuFontSize.caption,
            color = FogGray,
            fontStyle = FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(XinyuSpacing.xl))

        // 图表
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(XinyuRadius.md))
                .background(Color.White)
                .padding(XinyuSpacing.lg)
        ) {
            ForgettingCurveChart(
                animationProgress = animatedProgress,
                activeCards = activeCards,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.lg))

        // 图注
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(XinyuRadius.sm))
                .background(SecondaryBg)
                .padding(XinyuSpacing.lg)
        ) {
            Text(
                text = "第 30 天，我决定要不要继续记得",
                fontSize = XinyuFontSize.caption,
                color = WarmGold
            )
            Spacer(modifier = Modifier.height(XinyuSpacing.sm))
            Text(
                text = "被想起 ≥3 次 → 留下来",
                fontSize = XinyuFontSize.micro,
                color = MistBlue
            )
            Text(
                text = "忘了的不是不重要，是没一直留在脑子里。",
                fontSize = XinyuFontSize.micro,
                color = FogGray,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

data class CardAgePoint(
    val cardAgeDays: Int,
    val forgettingRate: Float,
    val label: String = ""
)

@Composable
fun ForgettingCurveChart(
    animationProgress: Float,
    activeCards: List<CardAgePoint>,
    modifier: Modifier = Modifier
) {
    val mainColor = DuskPurple
    val decisionColor = WarmGold
    val escapeColor = MistBlue

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 40f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        // Draw curve based on animation progress
        val path = Path()
        val escapePath = Path()
        val fadingPath = Path()

        val totalXPoints = 45
        val progressPoints = (totalXPoints * animationProgress).toInt().coerceAtLeast(1)

        // Fresh line: 0-30 days, 0%→40%
        for (i in 0..progressPoints.coerceAtMost(30)) {
            val x = padding + (i.toFloat() / 45f) * chartWidth
            val y = height - padding - (StateEngine.computeCardForgettingRate("fresh", i) / 100f) * chartHeight
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Decision point at day 30
        val day30X = padding + (30f / 45f) * chartWidth
        val day30Y = height - padding - (40f / 100f) * chartHeight

        // Escape line (stable, recall≥3): horizontal at 0%
        if (animationProgress > 0.5f) {
            val escapeY = height - padding
            escapePath.moveTo(day30X, day30Y)
            escapePath.lineTo(day30X, escapeY)
            escapePath.lineTo(padding + chartWidth, escapeY)
        }

        // Fading line: 30-44 days, 60%→100%
        val fadingProgress = ((animationProgress - 0.3f) / 0.7f).coerceIn(0f, 1f)
        val fadingPoints = ((14f) * fadingProgress).toInt()
        for (i in 0..fadingPoints.coerceAtMost(14)) {
            val x = padding + ((30f + i.toFloat()) / 45f) * chartWidth
            val rate = 60f + (i.toFloat() / 14f) * 40f
            val y = height - padding - (rate / 100f) * chartHeight
            if (i == 0) fadingPath.moveTo(x, day30Y) else fadingPath.lineTo(x, y)
        }

        // Draw fresh line
        drawPath(
            path = path,
            color = mainColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw escape line (dashed effect - draw as line)
        drawPath(
            path = escapePath,
            color = escapeColor.copy(alpha = 0.6f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw fading line
        drawPath(
            path = fadingPath,
            color = mainColor.copy(alpha = 0.5f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Decision point marker
        if (animationProgress > 0.3f) {
            drawCircle(
                color = decisionColor,
                radius = 6.dp.toPx(),
                center = Offset(day30X, day30Y)
            )
        }

        // Draw actual data points (scatter)
        activeCards.forEach { point ->
            val px = padding + (point.cardAgeDays.toFloat() / 45f) * chartWidth
            val py = height - padding - (point.forgettingRate / 100f) * chartHeight
            drawCircle(
                color = mainColor.copy(alpha = 0.7f),
                radius = 4.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}
