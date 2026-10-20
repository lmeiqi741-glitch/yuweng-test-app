package com.xinyu.yuweng.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinyu.yuweng.data.remote.LlmConfig
import com.xinyu.yuweng.data.repository.YuwengRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val llmConfig: LlmConfig = LlmConfig(),
    val testResult: String? = null,
    val isTesting: Boolean = false,
    val forgettingCurveRoute: String = "forgetting_curve"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: YuwengRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = repository.getLlmConfig()
                _state.value = _state.value.copy(llmConfig = config)
            } catch (_: Exception) { }
        }
    }

    fun saveConfig(config: LlmConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveLlmConfig(config)
                _state.value = _state.value.copy(llmConfig = config)
            } catch (_: Exception) { }
        }
    }

    fun testConnection(config: LlmConfig) {
        _state.value = _state.value.copy(isTesting = true, testResult = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.testLlmConnection(config)
                _state.value = _state.value.copy(
                    testResult = result.getOrDefault("连接失败"),
                    isTesting = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    testResult = "连接失败",
                    isTesting = false
                )
            }
        }
    }
}
