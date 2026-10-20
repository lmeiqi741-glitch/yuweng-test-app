package com.xinyu.yuweng.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinyu.yuweng.core.stateengine.SilenceState
import com.xinyu.yuweng.core.stateengine.StateEngine
import com.xinyu.yuweng.core.stateengine.YuwengState
import com.xinyu.yuweng.data.remote.LlmProvider
import com.xinyu.yuweng.data.repository.YuwengRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSilent: Boolean = false,
    val silenceState: SilenceState? = null,
    val isMockMode: Boolean = true,
    val isTyping: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: YuwengRepository,
    private val llmProvider: LlmProvider
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val conversationMessages = mutableListOf<Pair<String, String>>()

    init {
        loadInitialState()
        loadPendingMessages()
    }

    private fun loadInitialState() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userState = repository.getState()
                val now = System.currentTimeMillis()
                val isSilent = isSilentNow(userState, now)
                val silenceState = if (isSilent) createSilenceState(userState, now) else null
                val isConfigured = repository.isLlmConfigured()

                _state.value = _state.value.copy(
                    isSilent = isSilent,
                    silenceState = silenceState,
                    isMockMode = !isConfigured
                )
            } catch (_: Exception) {
                // Keep default state on error
            }
        }
    }

    private fun isSilentNow(yuwengState: YuwengState, now: Long): Boolean {
        if (yuwengState.energy <= 0) return true
        val prob = StateEngine.silenceProbability(yuwengState.mood, yuwengState.energy)
        return prob > 0.5f
    }

    private fun createSilenceState(yuwengState: YuwengState, now: Long): SilenceState {
        val trigger = when {
            yuwengState.energy <= 0 -> "energy_low"
            yuwengState.mood < -7 -> "mood_critical"
            else -> "self_initiated"
        }
        val deadline = if (yuwengState.energy <= 0) {
            StateEngine.computeForcedSilenceDeadline(now)
        } else null
        return SilenceState.create(trigger, now, deadline)
    }

    fun sendMessage(content: String) {
        val now = System.currentTimeMillis()

        val userMessage = ChatMessage(
            role = "user",
            content = content,
            timestamp = now
        )

        conversationMessages.add("user" to content)

        _state.value = _state.value.copy(
            messages = _state.value.messages + userMessage
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val convId = repository.getCurrentConversationId()
                repository.sendMessage(convId, "user", content)

                val currentState = repository.getState()
                val isSilent = isSilentNow(currentState, now)

                if (isSilent) {
                    repository.addPendingMessage(content)
                    val silenceState = createSilenceState(currentState, now)
                    _state.value = _state.value.copy(
                        isSilent = true,
                        silenceState = silenceState
                    )
                    return@launch
                }

                _state.value = _state.value.copy(isTyping = true)

                val systemPrompt = buildSystemPrompt(currentState)
                val response = llmProvider.getClient()
                    .chat(systemPrompt, conversationMessages.toList())

                if (response.isNotBlank()) {
                    val assistantMessage = ChatMessage(
                        role = "assistant",
                        content = response,
                        timestamp = System.currentTimeMillis()
                    )
                    conversationMessages.add("assistant" to response)
                    repository.sendMessage(convId, "assistant", response)

                    _state.value = _state.value.copy(
                        messages = _state.value.messages + assistantMessage,
                        isTyping = false
                    )
                } else {
                    // Empty response = mock silence
                    _state.value = _state.value.copy(isTyping = false)
                }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    role = "assistant",
                    content = "屿翁暂时说不出话来。",
                    timestamp = System.currentTimeMillis()
                )
                _state.value = _state.value.copy(
                    messages = _state.value.messages + errorMessage,
                    isTyping = false
                )
            }
        }
    }

    fun loadPendingMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pending = repository.getUnreviewedPendingFlow().first()
                if (pending.isNotEmpty()) {
                    // Could trigger UI hint about pending messages
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    private suspend fun buildSystemPrompt(yuwengState: YuwengState): String {
        val basePrompt = try {
            repository.loadSystemPromptFromAssets()
        } catch (_: Exception) {
            "你是屿翁，一个住在海边的老人。你话不多，但每句话都有分量。"
        }
        val stateSegment = StateEngine.buildStateSegment(yuwengState)
        return "$basePrompt\n\n$stateSegment"
    }
}
