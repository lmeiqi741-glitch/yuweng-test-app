package com.xinyu.yuweng.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xinyu.yuweng.core.designsystem.theme.DeepGray
import com.xinyu.yuweng.core.designsystem.theme.Divider
import com.xinyu.yuweng.core.designsystem.theme.FogGray
import com.xinyu.yuweng.core.designsystem.theme.InkColor
import com.xinyu.yuweng.core.designsystem.theme.MistBlue
import com.xinyu.yuweng.core.designsystem.theme.MoonWhite
import com.xinyu.yuweng.core.designsystem.theme.MossGreen
import com.xinyu.yuweng.core.designsystem.theme.DaiGray
import com.xinyu.yuweng.core.designsystem.theme.SecondaryBg
import com.xinyu.yuweng.core.designsystem.theme.WarmGold

// ─────────────────────────────────────────────────────────────────────────────
// 1. XinyuButton — 5 variants: Primary, Secondary, Text, Icon, Pill
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Button variants for the Xinyu design system.
 * Each variant embodies the design principles: 慢(Slow), 轻(Light), 真(Real), 柔(Soft).
 */
enum class XinyuButtonVariant { Primary, Secondary, Text, Icon, Pill }

/**
 * XinyuButton with press scale animation via InteractionSource.
 * Material3 Surface onClick doesn't expose interactionSource, so we wrap
 * in a Box with clickable modifier for press detection.
 */
@Composable
fun XinyuButton(
    text: String,
    variant: XinyuButtonVariant,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "button_scale"
    )

    val shape = when (variant) {
        XinyuButtonVariant.Primary, XinyuButtonVariant.Secondary, XinyuButtonVariant.Text ->
            RoundedCornerShape(24.dp)
        XinyuButtonVariant.Icon -> CircleShape
        XinyuButtonVariant.Pill -> RoundedCornerShape(999.dp)
    }

    val bgColor = when (variant) {
        XinyuButtonVariant.Primary, XinyuButtonVariant.Pill -> WarmGold
        XinyuButtonVariant.Secondary, XinyuButtonVariant.Icon -> MoonWhite
        XinyuButtonVariant.Text -> Color.Transparent
    }

    val textColor = InkColor
    val buttonHeight = when (variant) {
        XinyuButtonVariant.Primary, XinyuButtonVariant.Secondary -> 56.dp
        XinyuButtonVariant.Pill -> 40.dp
        XinyuButtonVariant.Icon -> 48.dp
        XinyuButtonVariant.Text -> 0.dp // wraps content
    }
    val fontSize = if (variant == XinyuButtonVariant.Pill) 14.sp else 16.sp

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Surface(
            modifier = when (variant) {
                XinyuButtonVariant.Icon -> Modifier.size(48.dp)
                XinyuButtonVariant.Text -> Modifier
                else -> Modifier.fillMaxWidth()
            },
            shape = shape,
            color = bgColor,
            shadowElevation = when (variant) {
                XinyuButtonVariant.Primary -> if (isPressed) 2.dp else 6.dp
                XinyuButtonVariant.Icon -> 2.dp
                XinyuButtonVariant.Pill -> 2.dp
                else -> 0.dp
            },
            border = if (variant == XinyuButtonVariant.Secondary)
                BorderStroke(1.dp, MistBlue) else null
        ) {
            Box(
                modifier = when {
                    variant == XinyuButtonVariant.Icon -> Modifier
                    variant == XinyuButtonVariant.Text -> Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    else -> Modifier
                        .then(if (buttonHeight > 0.dp) Modifier.height(buttonHeight) else Modifier)
                        .then(
                            if (variant == XinyuButtonVariant.Pill)
                                Modifier.padding(horizontal = 20.dp)
                            else Modifier
                        )
                },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview(name = "XinyuButton Variants")
@Composable
fun XinyuButtonPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6F0))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        XinyuButton("发送消息", XinyuButtonVariant.Primary, modifier = Modifier.fillMaxWidth()) {}
        XinyuButton("取消", XinyuButtonVariant.Secondary, modifier = Modifier.fillMaxWidth()) {}
        XinyuButton("跳过", XinyuButtonVariant.Text) {}
        XinyuButton("", XinyuButtonVariant.Icon) {}
        XinyuButton("确认", XinyuButtonVariant.Pill) {}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. XinyuCard — 3 types: Base, Island, Memory
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Card surface variants for the Xinyu design system.
 * Base: Standard white card with soft shadow.
 * Island: Warm gold tinted floating card.
 * Memory: Secondary background with left colored stripe.
 */
enum class XinyuCardType { Base, Island, Memory }

@Composable
fun XinyuCard(
    type: XinyuCardType,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (type) {
        XinyuCardType.Base -> {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = MoonWhite
            ) {
                content()
            }
        }

        XinyuCardType.Island -> {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = WarmGold.copy(alpha = 0.15f)
            ) {
                content()
            }
        }

        XinyuCardType.Memory -> {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SecondaryBg
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(80.dp)
                            .background(
                                WarmGold,
                                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        content()
                    }
                }
            }
        }
    }
}

@Preview(name = "XinyuCard Variants")
@Composable
fun XinyuCardPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6F0))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        XinyuCard(XinyuCardType.Base) {
            Text(
                text = "基础卡片\n这是一段正文内容",
                modifier = Modifier.padding(20.dp),
                fontSize = 16.sp,
                color = InkColor
            )
        }
        XinyuCard(XinyuCardType.Island) {
            Text(
                text = "岛屿卡片\n温暖如光的容器",
                modifier = Modifier.padding(20.dp),
                fontSize = 16.sp,
                color = InkColor
            )
        }
        XinyuCard(XinyuCardType.Memory) {
            Text(
                text = "记忆卡片\n带着时间痕迹的卡片",
                modifier = Modifier.padding(20.dp),
                fontSize = 16.sp,
                color = InkColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. XinyuInput — 3 states: default, focus, letter-writing
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Input field states for the Xinyu design system.
 * Default: Standard input with divider border.
 * Focus: Warm gold 2dp border for active state.
 * LetterWriting: Paper-like secondary background for letter composition.
 */
enum class XinyuInputState { Default, Focus, LetterWriting }

@Composable
fun XinyuInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    state: XinyuInputState = XinyuInputState.Default
) {
    val bgColor = when (state) {
        XinyuInputState.Default, XinyuInputState.Focus -> MoonWhite
        XinyuInputState.LetterWriting -> SecondaryBg
    }

    val borderColor = when (state) {
        XinyuInputState.Default -> Divider
        XinyuInputState.Focus -> WarmGold
        XinyuInputState.LetterWriting -> Divider
    }

    val borderWidth = when (state) {
        XinyuInputState.Default -> 1.dp
        XinyuInputState.Focus -> 2.dp
        XinyuInputState.LetterWriting -> 1.dp
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            textStyle = TextStyle(
                color = InkColor,
                fontSize = 16.sp
            ),
            cursorBrush = SolidColor(WarmGold),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = FogGray,
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

@Preview(name = "XinyuInput States")
@Composable
fun XinyuInputPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6F0))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        XinyuInput(
            value = "",
            onValueChange = {},
            placeholder = "说点什么…",
            state = XinyuInputState.Default
        )
        XinyuInput(
            value = "你好",
            onValueChange = {},
            placeholder = "说点什么…",
            state = XinyuInputState.Focus
        )
        XinyuInput(
            value = "亲爱的你",
            onValueChange = {},
            placeholder = "写信给…",
            state = XinyuInputState.LetterWriting
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. XinyuToast — bottom placement, slide-up fade, DeepGray bg
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Toast notification that appears in the bottom third of the screen.
 * Uses fade animation for gentle appearance (no jarring pop-in).
 * Auto-dismiss after 2.5s when used with state management.
 */
@Composable
fun XinyuToast(
    message: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "toast_alpha"
    )

    if (visible) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .graphicsLayer { this.alpha = alpha }
                .background(
                    DeepGray.copy(alpha = 0.85f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = MoonWhite,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(name = "XinyuToast")
@Composable
fun XinyuToastPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6F0)),
        contentAlignment = Alignment.BottomCenter
    ) {
        XinyuToast(
            message = "消息已送达",
            visible = true,
            modifier = Modifier.padding(bottom = 40.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. XinyuDialog — centered, radius 24dp, MoonWhite bg, emotional text
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dialog for secondary confirmations with emotional, gentle language.
 * Uses Dialog composable (floating window overlay).
 * MoonWhite background with 24dp rounded corners.
 */
@Composable
fun XinyuDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (visible) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(24.dp),
                color = MoonWhite,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = DaiGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    XinyuButton(
                        text = confirmText,
                        variant = XinyuButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onConfirm
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    XinyuButton(
                        text = "再想想",
                        variant = XinyuButtonVariant.Text,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Preview(name = "XinyuDialog")
@Composable
fun XinyuDialogPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6F0))
    ) {
        XinyuDialog(
            visible = true,
            title = "确定要离开吗？",
            message = "屿翁会在这里等你回来。\n每一次告别，都是为了更好的重逢。",
            confirmText = "我知道了",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. XinyuEmptyState — icon + warm message + optional action
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Empty state placeholder with centered layout and generous whitespace.
 * Shows a nature emoji, warm message, and optional CTA button.
 */
@Composable
fun XinyuEmptyState(
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌿",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = DaiGray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            XinyuButton(
                text = actionText,
                variant = XinyuButtonVariant.Pill,
                onClick = onAction
            )
        }
    }
}

@Preview(name = "XinyuEmptyState")
@Composable
fun XinyuEmptyStatePreview() {
    XinyuEmptyState(
        message = "这里还空空的，\n种下第一颗心情吧。",
        actionText = "开始记录",
        onAction = {}
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. MoodChip — mood label + progress bar, expandable numeric value
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Compact mood indicator chip showing qualitative mood label and progress bar.
 * Tap to expand and reveal the numeric mood value (-10 to +10).
 * Color shifts based on mood range.
 */
@Composable
fun MoodChip(
    moodValue: Int,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val progress = ((moodValue + 10) / 20f).coerceIn(0f, 1f)

    val label = when {
        moodValue <= -7 -> "低落"
        moodValue <= -3 -> "忧伤"
        moodValue <= 0 -> "平静"
        moodValue <= 3 -> "温和"
        moodValue <= 7 -> "愉悦"
        else -> "欣喜"
    }

    val chipColor = when {
        moodValue <= -5 -> MistBlue
        moodValue <= 0 -> FogGray
        moodValue <= 5 -> MossGreen
        else -> WarmGold
    }

    val progressAnim by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "mood_progress"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(999.dp),
        color = chipColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = chipColor,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Divider)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(chipColor)
                )
            }
            if (expanded) {
                Text(
                    text = "$moodValue",
                    fontSize = 13.sp,
                    color = chipColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(name = "MoodChip Variants")
@Composable
fun MoodChipPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6F0))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MoodChip(moodValue = -8)
        MoodChip(moodValue = -2)
        MoodChip(moodValue = 3)
        MoodChip(moodValue = 9)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. SegmentedToggle — two-segment selector with sliding indicator
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Two-segment toggle (e.g., Card view | Tree view).
 * Features a smooth sliding indicator animation.
 */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = SecondaryBg
    ) {
        BoxWithConstraints {
            val segmentWidth = maxWidth / options.size
            val density = LocalDensity.current

            val offsetX by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = tween(durationMillis = 400),
                label = "segment_offset"
            )

            // Sliding indicator background
            Box(
                modifier = Modifier
                    .size(width = segmentWidth, height = (44 - 4).dp)
                    .offset {
                        IntOffset(
                            x = (offsetX * density.run { segmentWidth.toPx() }).toInt(),
                            y = with(density) { 2.dp.toPx().toInt() }
                        )
                    }
                    .background(MoonWhite, RoundedCornerShape(999.dp))
                    .shadow(2.dp, RoundedCornerShape(999.dp))
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (index == selectedIndex) InkColor else FogGray
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "SegmentedToggle")
@Composable
fun SegmentedTogglePreview() {
    var selected by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6F0))
            .padding(24.dp)
    ) {
        SegmentedToggle(
            options = listOf("卡片", "树状"),
            selectedIndex = selected,
            onSelect = { selected = it }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. SilenceCard — silence state with grayed avatar, reason, pending count
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Card displayed when Yuweng is in silent mode.
 * Shows grayed-out avatar, silence text, trigger reason, and pending message count.
 */
@Composable
fun SilenceCard(
    silenceText: String,
    triggerReason: String,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    XinyuCard(
        type = XinyuCardType.Base,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Grayed-out avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(FogGray.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌫",
                    fontSize = 32.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = silenceText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = FogGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = triggerReason,
                fontSize = 13.sp,
                color = DaiGray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            if (pendingCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = WarmGold.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "有 $pendingCount 条消息在等待",
                        fontSize = 12.sp,
                        color = DaiGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "SilenceCard")
@Composable
fun SilenceCardPreview() {
    SilenceCard(
        silenceText = "屿翁正在安静中…",
        triggerReason = "检测到你在忙碌，\n不打扰是最好的陪伴。",
        pendingCount = 3,
        modifier = Modifier.padding(24.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: BorderStroke factory
// ─────────────────────────────────────────────────────────────────────────────

private fun BorderStroke(width: Dp, color: Color): androidx.compose.foundation.BorderStroke =
    androidx.compose.foundation.BorderStroke(width, color)
