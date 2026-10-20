package com.xinyu.yuweng.feature.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val selectedView: String = "card"
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val repository: YuwengRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MemoryUiState())
    val state: StateFlow<MemoryUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cards = repository.getActiveCards()
                val forgottenCount = repository.getForgottenCount()

                _state.value = _state.value.copy(
                    activeCards = cards,
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
}
