package com.xinyu.yuweng.feature.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinyu.yuweng.core.stateengine.StateEngine
import com.xinyu.yuweng.data.local.entity.MemoryCardEntity
import com.xinyu.yuweng.data.repository.YuwengRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgettingLogEntry(
    val id: Long,
    val contentSnapshot: String,
    val forgottenAt: Long,
    val whyForgotten: String,
    val redeemed: Boolean
)

data class MemoryUiState(
    val activeCards: List<MemoryCardEntity> = emptyList(),
    val forgettingLog: List<ForgettingLogEntry> = emptyList(),
    val forgottenCount: Int = 0,
    val selectedView: String = "card",
    val searchQuery: String = "",
    val searchResults: List<MemoryCardEntity> = emptyList()
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val repository: YuwengRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MemoryUiState())
    val state: StateFlow<MemoryUiState> = _state.asStateFlow()

    // All cards (cached for search)
    private var allCards: List<MemoryCardEntity> = emptyList()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cards = repository.getActiveCards()
                val forgottenCount = repository.getForgottenCount()
                val logEntities = repository.getForgottenLog()

                allCards = cards

                // Map entities to UI forgetting log entries
                val forgettingLog = logEntities.map { entity ->
                    ForgettingLogEntry(
                        id = entity.id,
                        contentSnapshot = entity.contentSnapshot,
                        forgottenAt = entity.forgottenAt,
                        whyForgotten = entity.whyForgotten,
                        redeemed = entity.redeemed
                    )
                }

                _state.value = _state.value.copy(
                    activeCards = cards,
                    forgettingLog = forgettingLog,
                    forgottenCount = forgottenCount
                )
            } catch (_: Exception) { }
        }
    }

    fun selectView(view: String) {
        _state.value = _state.value.copy(selectedView = view)
    }

    fun redeemCard(cardId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.redeemCard(cardId)
                loadData()
            } catch (_: Exception) { }
        }
    }

    /**
     * Calculate per-card forgetting rate using StateEngine.computeCardForgettingRate().
     *
     * Card age days = (now - createdAt) / (1000 * 60 * 60 * 24).
     * Fading days = if status == "fading", (now - lastRecalledAt) / (1000*60*60*24).
     */
    fun getCardForgettingRate(card: MemoryCardEntity): Float {
        val now = System.currentTimeMillis()
        val cardAgeDays = ((now - card.createdAt) / (1000 * 60 * 60 * 24)).toInt()
        val fadingDays = if (card.status == "fading" && card.lastRecalledAt != null) {
            ((now - card.lastRecalledAt) / (1000 * 60 * 60 * 24)).toInt()
        } else 0

        return StateEngine.computeCardForgettingRate(
            status = card.status,
            cardAgeDays = cardAgeDays,
            fadingDays = fadingDays,
            recallCount = card.recallCount
        )
    }

    /**
     * Get the lifecycle description for a card in Yuweng's voice.
     */
    fun getCardLifecycleDescription(card: MemoryCardEntity): String {
        val now = System.currentTimeMillis()
        val cardAgeDays = ((now - card.createdAt) / (1000 * 60 * 60 * 24)).toInt()
        val fadingDays = if (card.status == "fading" && card.lastRecalledAt != null) {
            ((now - card.lastRecalledAt) / (1000 * 60 * 60 * 24)).toInt()
        } else 0

        return StateEngine.getCardLifecycleDescription(
            status = card.status,
            cardAgeDays = cardAgeDays,
            fadingDays = fadingDays
        )
    }

    /**
     * Search cards by content and context (topics).
     * Filters cards whose content or context contains the query string.
     */
    fun searchCards(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(
                searchQuery = "",
                searchResults = emptyList()
            )
            return
        }

        val lowerQuery = query.lowercase()
        val results = allCards.filter { card ->
            card.content.lowercase().contains(lowerQuery) ||
                card.context.lowercase().contains(lowerQuery)
        }

        _state.value = _state.value.copy(
            searchQuery = query,
            searchResults = results
        )
    }
}
