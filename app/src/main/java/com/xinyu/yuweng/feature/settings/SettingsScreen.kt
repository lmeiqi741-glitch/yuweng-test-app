package com.xinyu.yuweng.feature.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xinyu.yuweng.core.designsystem.XinyuFontSize
import com.xinyu.yuweng.core.designsystem.XinyuRadius
import com.xinyu.yuweng.core.designsystem.XinyuSpacing
import com.xinyu.yuweng.core.designsystem.theme.*
import com.xinyu.yuweng.data.remote.LlmConfig
import com.xinyu.yuweng.data.remote.LlmPreset
import com.xinyu.yuweng.data.remote.LlmPresets

/**
 * 设置页（任务一：模型 API 配置 + 遗忘曲线图入口）。
 */
@Composable
fun SettingsScreen(
    onNavigateToForgettingCurve: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreenContent(
        llmConfig = state.llmConfig,
        testResult = state.testResult,
        isTesting = state.isTesting,
        onSaveLlmConfig = { viewModel.saveConfig(it) },
        onTestConnection = { viewModel.testConnection(it) },
        onNavigateToForgettingCurve = onNavigateToForgettingCurve
    )
}

@Composable
fun SettingsScreenContent(
    llmConfig: LlmConfig = LlmConfig(),
    testResult: String? = null,
    isTesting: Boolean = false,
    onSaveLlmConfig: (LlmConfig) -> Unit = {},
    onTestConnection: (LlmConfig) -> Unit = {},
    onNavigateToForgettingCurve: () -> Unit = {}
) {
    var baseUrl by remember { mutableStateOf(llmConfig.baseUrl) }
    var apiKey by remember { mutableStateOf(llmConfig.apiKey) }
    var modelName by remember { mutableStateOf(llmConfig.modelName) }
    var temperature by remember { mutableStateOf(llmConfig.temperature.toString()) }
    var maxTokens by remember { mutableStateOf(llmConfig.maxTokens.toString()) }
    var embeddingModel by remember { mutableStateOf(llmConfig.embeddingModel) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonWhite)
            .verticalScroll(scrollState)
            .padding(horizontal = XinyuSpacing.xl)
    ) {
        Spacer(modifier = Modifier.height(XinyuSpacing.xl))

        Text(
            text = "设置",
            fontSize = XinyuFontSize.h1,
            color = InkColor
        )

        Spacer(modifier = Modifier.height(XinyuSpacing.xl))

        // 模型 API 配置
        Text(
            text = "模型 API 配置",
            fontSize = XinyuFontSize.h3,
            color = DaiGray
        )
        Spacer(modifier = Modifier.height(XinyuSpacing.md))

        Text(
            text = "屿翁通过你配置的 API 说话。所有配置仅存本机。",
            fontSize = XinyuFontSize.caption,
            color = FogGray
        )

        Spacer(modifier = Modifier.height(XinyuSpacing.lg))

        // 预设快填
        Text(
            text = "预设",
            fontSize = XinyuFontSize.caption,
            color = FogGray
        )
        Spacer(modifier = Modifier.height(XinyuSpacing.sm))

        // Preset chips in a flow-like layout
        val presets = LlmPresets.ALL
        var selectedPreset by remember { mutableStateOf<String?>(null) }

        // Row of preset buttons (simplified - 2 columns)
        presets.chunked(2).forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(XinyuSpacing.sm)
            ) {
                rowPresets.forEach { preset ->
                    val isSelected = selectedPreset == preset.name
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedPreset = preset.name
                            baseUrl = preset.baseUrl
                            modelName = preset.recommendedModel
                        },
                        label = { Text(preset.name, fontSize = XinyuFontSize.caption) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmGold.copy(alpha = 0.3f),
                            selectedLabelColor = InkColor
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(XinyuSpacing.sm))
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.lg))

        // Base URL
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(XinyuRadius.md),
            colors = xinyuTextFieldColors(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(XinyuSpacing.md))

        // API Key (masked)
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(XinyuRadius.md),
            colors = xinyuTextFieldColors(),
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                        tint = FogGray
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(XinyuSpacing.md))

        // Model name
        OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it },
            label = { Text("模型名称") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(XinyuRadius.md),
            colors = xinyuTextFieldColors(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(XinyuSpacing.lg))

        // 测试连接按钮
        Button(
            onClick = {
                val config = LlmConfig(
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    modelName = modelName,
                    temperature = temperature.toDoubleOrNull() ?: 0.8,
                    maxTokens = maxTokens.toIntOrNull() ?: 800,
                    embeddingModel = embeddingModel,
                    isConfigured = apiKey.isNotBlank()
                )
                onTestConnection(config)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(XinyuRadius.md),
            colors = ButtonDefaults.buttonColors(
                containerColor = WarmGold,
                contentColor = InkColor
            ),
            enabled = !isTesting
        ) {
            if (isTesting) {
                Text("正在测试…")
            } else {
                Text("测试连接")
            }
        }

        // 测试结果
        if (testResult != null) {
            Spacer(modifier = Modifier.height(XinyuSpacing.sm))
            Text(
                text = testResult ?: "",
                fontSize = XinyuFontSize.caption,
                color = if (testResult == "OK" || testResult?.contains("success") == true) MossGreen else FogGray
            )
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.md))

        // 保存配置按钮
        Button(
            onClick = {
                val config = LlmConfig(
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    modelName = modelName,
                    temperature = temperature.toDoubleOrNull() ?: 0.8,
                    maxTokens = maxTokens.toIntOrNull() ?: 800,
                    embeddingModel = embeddingModel,
                    isConfigured = apiKey.isNotBlank()
                )
                onSaveLlmConfig(config)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(XinyuRadius.md),
            colors = ButtonDefaults.buttonColors(
                containerColor = SecondaryBg,
                contentColor = InkColor
            )
        ) {
            Text("保存配置")
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.md))

        // 高级选项（可折叠）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvanced = !showAdvanced }
                .padding(vertical = XinyuSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "高级选项",
                fontSize = XinyuFontSize.caption,
                color = FogGray
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (showAdvanced) "▲" else "▼",
                fontSize = 12.sp,
                color = FogGray
            )
        }

        if (showAdvanced) {
            // Temperature
            OutlinedTextField(
                value = temperature,
                onValueChange = { temperature = it },
                label = { Text("Temperature (0~2)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(XinyuRadius.md),
                colors = xinyuTextFieldColors(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(XinyuSpacing.md))

            // Max Tokens
            OutlinedTextField(
                value = maxTokens,
                onValueChange = { maxTokens = it },
                label = { Text("Max Tokens") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(XinyuRadius.md),
                colors = xinyuTextFieldColors(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(XinyuSpacing.md))

            // Embedding model
            OutlinedTextField(
                value = embeddingModel,
                onValueChange = { embeddingModel = it },
                label = { Text("Embedding 模型（可空）") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(XinyuRadius.md),
                colors = xinyuTextFieldColors(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.xl))

        // 遗忘曲线图入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(XinyuRadius.md))
                .background(SecondaryBg)
                .clickable { onNavigateToForgettingCurve() }
                .padding(XinyuSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(XinyuSpacing.md))
            Column {
                Text("屿翁的遗忘曲线", fontSize = XinyuFontSize.body, color = InkColor)
                Text(
                    "纯展示 · 遗忘不可控",
                    fontSize = XinyuFontSize.micro,
                    color = FogGray
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("→", color = FogGray)
        }

        Spacer(modifier = Modifier.height(XinyuSpacing.xxl))
    }
}

@Composable
fun xinyuTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = WarmGold,
    unfocusedBorderColor = Divider,
    focusedLabelColor = WarmGold,
    unfocusedLabelColor = FogGray,
    focusedContainerColor = MoonWhite,
    unfocusedContainerColor = MoonWhite,
    cursorColor = WarmGold
)

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) WarmGold.copy(alpha = 0.3f) else Color.Transparent
    val borderColor = if (selected) WarmGold.copy(alpha = 0.3f) else Divider

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(XinyuRadius.pill))
            .clickable { onClick() },
        shape = RoundedCornerShape(XinyuRadius.pill),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = XinyuSpacing.md, vertical = XinyuSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            label()
        }
    }
}
