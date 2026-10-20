package com.xinyu.yuweng.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinyu.yuweng.core.stateengine.MoodEvent
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
    val isTyping: Boolean = false,
    val isSafetyMode: Boolean = false,
    val pendingCount: Int = 0
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: YuwengRepository,
    private val llmProvider: LlmProvider
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val conversationMessages = mutableListOf<Pair<String, String>>()

    // Safety mode tracking: counts consecutive safe user messages to exit safety mode
    private var safetyModeTurns = 0

    // Conversation turn counter
    private var turnCount = 0

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
                val pendingCount = repository.getUnreviewedCount()

                turnCount = userState.turns

                _state.value = ChatUiState(
                    isSilent = isSilent,
                    silenceState = silenceState,
                    isMockMode = !isConfigured,
                    pendingCount = pendingCount
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

        // 1. Safety mode detection
        val isCrisis = StateEngine.detectCrisis(content)
        val wasAlreadyInSafetyMode = _state.value.isSafetyMode

        if (isCrisis) {
            safetyModeTurns = 0
            _state.value = _state.value.copy(isSafetyMode = true)
        } else if (wasAlreadyInSafetyMode) {
            // Track consecutive safe messages to exit safety mode
            safetyModeTurns++
            if (safetyModeTurns >= 3) {
                _state.value = _state.value.copy(isSafetyMode = false)
                safetyModeTurns = 0
            }
        }

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

                // 2. Detect perfunctory replies → apply mood delta
                if (StateEngine.isPerfunctoryReply(content) && !isCrisis) {
                    val currentState = repository.getState()
                    val delta = StateEngine.computeMoodDelta(MoodEvent.PERFUNCTORY)
                    val (newMood, newAccumulator) = StateEngine.applyMoodCircuitBreaker(
                        currentState.mood, delta, currentState.dayMoodAccumulator
                    )
                    repository.updateMoodAndEnergy(newMood, currentState.energy, now)
                }

                // 6. Memory recall: check if user mentioned any L3 card content
                checkMemoryRecall(content)

                val currentState = repository.getState()
                val isSilent = isSilentNow(currentState, now)

                if (isSilent) {
                    repository.addPendingMessage(content)
                    val silenceState = createSilenceState(currentState, now)
                    val pendingCount = repository.getUnreviewedCount()
                    _state.value = _state.value.copy(
                        isSilent = true,
                        silenceState = silenceState,
                        pendingCount = pendingCount
                    )
                    return@launch
                }

                _state.value = _state.value.copy(isTyping = true)

                // 4. Cold start handling: append rules if first turn
                // 2. Safety system prompt when in safety mode
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

                    // 7. State update: compute mood delta from response characteristics
                    updateStateFromResponse(response, currentState)

                    // 5. L2 summary generation check
                    maybeGenerateL2Summary(currentState)

                    val pendingCount = repository.getUnreviewedCount()
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + assistantMessage,
                        isTyping = false,
                        pendingCount = pendingCount
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

    /**
     * 6. Memory recall: if user message contains any L3 card content snippet,
     * increment recall_count on the matched card.
     */
    private suspend fun checkMemoryRecall(userContent: String) {
        try {
            val cards = repository.getActiveCards()
            for (card in cards) {
                // Simple substring match: if user mentions card content or context
                if (userContent.contains(card.content.take(4)) ||
                    (card.context.isNotBlank() && userContent.contains(card.context.take(4)))
                ) {
                    repository.recallCard(card.id)
                    break // Only recall the first matched card per message
                }
            }
        } catch (_: Exception) {
            // Ignore recall errors
        }
    }

    /**
     * 7. State update after LLM response: compute mood delta based on response characteristics.
     * Longer responses = more energy spent. Satisfied expression = small mood boost.
     */
    private suspend fun updateStateFromResponse(response: String, stateBeforeResponse: YuwengState) {
        try {
            val now = System.currentTimeMillis()

            // Energy cost based on response length
            val isDeepTopic = response.length > 200
            val energyDelta = StateEngine.computeEnergyDelta(
                replyLength = response.length,
                isDeepTopic = isDeepTopic
            )
            val newEnergy = StateEngine.clampEnergy(stateBeforeResponse.energy + energyDelta)

            // Small mood bonus if response was substantive (satisfied expression)
            val moodDelta = if (response.length > 20) {
                StateEngine.computeMoodDelta(MoodEvent.SATISFIED_EXPRESSION, 0.5f)
            } else 0f

            val (newMood, _) = StateEngine.applyMoodCircuitBreaker(
                stateBeforeResponse.mood, moodDelta, stateBeforeResponse.dayMoodAccumulator
            )

            repository.updateMoodAndEnergy(newMood, newEnergy, now)
        } catch (_: Exception) {
            // Ignore state update errors
        }
    }

    /**
     * 5. L2 summary generation check.
     * If energy >= 25 and conversation has enough turns, trigger summary generation.
     */
    private suspend fun maybeGenerateL2Summary(currentState: YuwengState) {
        try {
            if (currentState.energy >= 25 && turnCount > 0 && turnCount % 10 == 0) {
                // Every 10 turns, check if summary generation is needed
                // In production: call LLM to generate summary and store via messageSummaryDao
                // For prototype: just log the opportunity
                turnCount++
            } else {
                turnCount++
            }
        } catch (_: Exception) {
            // Ignore
        }
    }

    fun loadPendingMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pending = repository.getUnreviewedPendingFlow().first()
                _state.value = _state.value.copy(pendingCount = pending.size)
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    private suspend fun buildSystemPrompt(yuwengState: YuwengState): String {
        // 2. Safety system prompt takes priority when in safety mode
        if (_state.value.isSafetyMode) {
            return StateEngine.SAFETY_SYSTEM_PROMPT
        }

        val basePrompt = try {
            repository.loadSystemPromptFromAssets()
        } catch (_: Exception) {
            "你是屿翁，一个住在海边的老人。你话不多，但每句话都有分量。"
        }

        // 4. Cold start handling: append cold start rules on first turn
        val coldStartSegment = if (yuwengState.turns == 0) {
            """

            【首轮对话规则】
            这是你与这个人的第一次对话。请保持观察，不要急于建立关系。
            用1-3轮了解对方是谁、来找你做什么。你的第一句话应该简短，
            带有海边老人的疏离感，但留有开口。"""
        } else ""

        val stateSegment = StateEngine.buildStateSegment(yuwengState)
        return "$basePrompt$coldStartSegment\n\n$stateSegment"
    }
}
