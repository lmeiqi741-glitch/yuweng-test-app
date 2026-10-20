package com.xinyu.yuweng.feature.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xinyu.yuweng.core.stateengine.SilenceState
import com.xinyu.yuweng.core.stateengine.StateEngine
import com.xinyu.yuweng.core.stateengine.YuwengState
import com.xinyu.yuweng.data.repository.YuwengRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatusUiState(
    val yuwengState: YuwengState = YuwengState(),
    val isSilent: Boolean = false,
    val silenceState: SilenceState? = null,
    val pendingCount: Int = 0,
    val moodHistory: List<Float> = emptyList()
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val repository: YuwengRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatusUiState())
    val state: StateFlow<StatusUiState> = _state.asStateFlow()

    init {
        loadState()
    }

    fun loadState() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userState = repository.getState()
                val now = System.currentTimeMillis()
                val pendingCount = repository.getUnreviewedCount()
                val isSilent = userState.energy <= 0 ||
                    StateEngine.silenceProbability(userState.mood, userState.energy) > 0.5f
                val silenceState = if (isSilent) createSilenceState(userState, now) else null

                _state.value = StatusUiState(
                    yuwengState = userState,
                    isSilent = isSilent,
                    silenceState = silenceState,
                    pendingCount = pendingCount,
                    moodHistory = emptyList()
                )
            } catch (_: Exception) { }
        }
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
}
