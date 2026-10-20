package com.xinyu.yuweng.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.xinyu.yuweng.core.stateengine.EnergyRecoveryType
import com.xinyu.yuweng.core.stateengine.StateEngine
import com.xinyu.yuweng.core.stateengine.YuwengState
import com.xinyu.yuweng.data.repository.YuwengRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class MemoryDecayWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: YuwengRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "MemoryDecayWorker"
        const val WORK_NAME = "yuweng_memory_decay"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MemoryDecayWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val state = repository.getState()
            val now = System.currentTimeMillis()
            val hoursElapsed = if (state.lastEnergyUpdateAt > 0) {
                ((now - state.lastEnergyUpdateAt) / (1000 * 60 * 60)).toFloat()
            } else 0f

            // Mood decay toward baseline
            val decayedMood = StateEngine.decayMood(state.mood, hoursElapsed.coerceAtMost(24f))

            // Energy recovery (offline recovery)
            val recoveredEnergy = StateEngine.recoverEnergy(
                state.energy, hoursElapsed.toLong().coerceAtMost(1440), EnergyRecoveryType.OFFLINE
            )

            // Reset day accumulator at midnight
            val dayAccumulator = state.dayMoodAccumulator

            repository.updateMoodAndEnergy(decayedMood, recoveredEnergy, now)
            Log.d(TAG, "Mood: ${state.mood}->$decayedMood, Energy: ${state.energy}->$recoveredEnergy")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Decay failed", e)
            Result.retry()
        }
    }
}

@HiltWorker
class AutonomousBehaviorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: YuwengRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AutonomousBehaviorWorker"
        const val WORK_NAME = "yuweng_autonomous_behavior"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutonomousBehaviorWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val state = repository.getState()
            val behaviors = listOf(
                "我最近一直在想一个问题。",
                "你有没有觉得，有时候沉默比说话更有力量？",
                "今天天气不错。我在海边坐着。",
                "我想起一件很久以前的事。",
                "你最近怎么样？不用认真回答。",
                "我昨天做了个梦，梦到你种了一棵树。"
            )
            val message = behaviors.random()

            val convId = repository.getCurrentConversationId()
            repository.sendMessage(convId, "assistant", message)

            // Energy bonus for completing autonomous behavior
            val newEnergy = StateEngine.autonomousBehaviorBonus(state.energy)
            repository.updateMoodAndEnergy(state.mood, newEnergy, System.currentTimeMillis())

            Log.d(TAG, "Autonomous behavior: $message")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Autonomous behavior failed", e)
            Result.retry()
        }
    }
}
