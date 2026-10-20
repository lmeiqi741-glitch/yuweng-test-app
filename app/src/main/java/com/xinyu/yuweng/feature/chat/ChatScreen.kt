package com.xinyu.yuweng.feature.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xinyu.yuweng.core.designsystem.XinyuFontSize
import com.xinyu.yuweng.core.designsystem.XinyuRadius
import com.xinyu.yuweng.core.designsystem.XinyuSpacing
import com.xinyu.yuweng.core.designsystem.XinyuTouchTarget
import com.xinyu.yuweng.core.designsystem.theme.*
import com.xinyu.yuweng.core.stateengine.SilenceState

/**
 * 屿翁对话页（ViewModel 装配入口）。
 */
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChatScreenContent(
        messages = state.messages,
        isSilent = state.isSilent,
        silenceState = state.silenceState,
        isMockMode = state.isMockMode,
        isTyping = state.isTyping,
        onSendMessage = { viewModel.sendMessage(it) }
    )
}

@Composable
fun ChatScreenContent(
    messages: List<ChatMessage> = emptyList(),
    isSilent: Boolean = false,
    silenceState: SilenceState? = null,
    isMockMode: Boolean = true,
    isTyping: Boolean = false,
    onSendMessage: (String) -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonWhite)
            .padding(horizontal = XinyuSpacing.xl)
    ) {
        Spacer(modifier = Modifier.height(XinyuSpacing.lg))

        // 顶部角色区
        YuwengCharacterArea(
            isSilent = isSilent,
            silenceState = silenceState,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(XinyuSpacing.xl))

        // Mock mode hint
        if (isMockMode) {
            MockModeHint(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(XinyuSpacing.md))
        }

        // 消息流
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(XinyuSpacing.lg),
            contentPadding = PaddingValues(vertical = XinyuSpacing.lg)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }

            // 沉默状态条
            if (isSilent && silenceState != null) {
                item {
                    SilenceStateBar(silenceState)
                }
            }

            // 屿翁正在思考
            if (isTyping) {
                item {
                    TypingIndicator()
                }
            }
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.md))

        // 输入区
        ChatInputArea(
            value = inputText,
            onValueChange = { inputText = it },
            enabled = !isSilent,
            isSilent = isSilent,
            onSend = {
                if (inputText.isNotBlank()) {
                    onSendMessage(inputText.trim())
                    inputText = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(XinyuSpacing.xl))
    }
}

@Composable
fun TypingIndicator() {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "typingAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(WarmGold.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text("屿", fontSize = 14.sp, color = DeepGray)
        }
        Spacer(modifier = Modifier.width(XinyuSpacing.sm))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(XinyuRadius.md))
                .background(Color(0xFFC19A6B).copy(alpha = 0.1f))
                .padding(XinyuSpacing.md)
        ) {
            Text(
                "正在为你编织…",
                fontSize = XinyuFontSize.caption,
                color = FogGray,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.alpha(0.5f + alpha * 0.5f)
            )
        }
    }
}

@Composable
fun YuwengCharacterArea(
    isSilent: Boolean,
    silenceState: SilenceState?,
    modifier: Modifier = Modifier
) {
    val activityText = if (isSilent && silenceState != null) {
        val location = silenceState.uiText
            .replace("屿翁", "")
            .replace("去", "")
            .replace("了", "")
            .trim()
        "屿翁 在 $location"
    } else {
        "屿翁 在 海滩垂钓"
    }

    val alpha by animateFloatAsState(
        targetValue = if (isSilent) 0.4f else 0.85f,
        animationSpec = tween(durationMillis = 800),
        label = "avatarAlpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(
                    if (isSilent) FogGray.copy(alpha = 0.3f)
                    else WarmGold.copy(alpha = 0.6f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "屿",
                fontSize = 28.sp,
                color = if (isSilent) FogGray else DeepGray
            )
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.sm))

        Text(
            text = activityText,
            fontSize = XinyuFontSize.caption,
            color = FogGray,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isYuweng = message.role == "assistant"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isYuweng) Arrangement.Start else Arrangement.End
    ) {
        if (isYuweng) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(WarmGold.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text("屿", fontSize = 14.sp, color = DeepGray)
            }
            Spacer(modifier = Modifier.width(XinyuSpacing.sm))
        }

        val bubbleColor = if (isYuweng) {
            Color(0xFFC19A6B).copy(alpha = 0.15f)
        } else {
            MistBlue.copy(alpha = 0.2f)
        }
        val textColor = if (isYuweng) DeepGray else InkColor

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(XinyuRadius.md))
                .background(bubbleColor)
                .padding(XinyuSpacing.lg)
        ) {
            Text(
                text = message.content,
                fontSize = XinyuFontSize.body,
                color = textColor
            )
        }

        if (!isYuweng) {
            Spacer(modifier = Modifier.width(XinyuSpacing.sm))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MistBlue.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text("你", fontSize = 14.sp, color = DeepGray)
            }
        }
    }
}

@Composable
fun SilenceStateBar(silenceState: SilenceState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(XinyuRadius.sm))
            .background(FogGray.copy(alpha = 0.1f))
            .padding(XinyuSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = silenceState.uiText,
            fontSize = XinyuFontSize.caption,
            color = FogGray,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
fun MockModeHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(XinyuRadius.sm))
            .background(WarmGold.copy(alpha = 0.2f))
            .padding(XinyuSpacing.lg)
    ) {
        Text(
            text = "屿翁还在用模拟模式和说话。去设置里填入 API，他会更像他。",
            fontSize = XinyuFontSize.caption,
            color = DaiGray
        )
    }
}

@Composable
fun ChatInputArea(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isSilent: Boolean,
    onSend: () -> Unit
) {
    val placeholder = if (isSilent) "屿翁还没看消息" else "说点什么…"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = {
                Text(placeholder, color = FogGray, fontSize = XinyuFontSize.body)
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(XinyuRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WarmGold,
                unfocusedBorderColor = Divider,
                disabledBorderColor = Divider.copy(alpha = 0.5f),
                focusedContainerColor = MoonWhite,
                unfocusedContainerColor = MoonWhite,
                disabledContainerColor = MoonWhite.copy(alpha = 0.7f)
            ),
            textStyle = XinyuFontSize.body
        )

        Spacer(modifier = Modifier.width(XinyuSpacing.sm))

        Button(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = WarmGold,
                contentColor = InkColor,
                disabledContainerColor = WarmGold.copy(alpha = 0.3f)
            ),
            modifier = Modifier.size(XinyuTouchTarget.min)
        ) {
            Text("→", fontSize = 18.sp)
        }
    }
}

data class ChatMessage(
    val role: String,     // "user" | "assistant"
    val content: String,
    val timestamp: Long = 0L
)
