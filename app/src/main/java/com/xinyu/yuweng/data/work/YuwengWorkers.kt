package com.xinyu.yuweng.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.xinyu.yuweng.core.stateengine.EnergyRecoveryType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * 记忆衰减 + 能量恢复每日任务。
 */
@HiltWorker
class MemoryDecayWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "MemoryDecayWorker"
        private const val WORK_NAME = "yuweng_memory_decay"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MemoryDecayWorker>(
                1, TimeUnit.DAYS
            ).setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running daily memory decay")
        // TODO: Apply mood decay, energy recovery, L2/L3 expiration
        return Result.success()
    }
}

/**
 * 每周自主行为任务。
 */
@HiltWorker
class AutonomousBehaviorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(applicationContext, workerParams) {

    companion object {
        private const val TAG = "AutonomousBehaviorWorker"
        private const val WORK_NAME = "yuweng_autonomous_behavior"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutonomousBehaviorWorker>(
                7, TimeUnit.DAYS
            ).setConstraints(
                Constraints.Builder().build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running weekly autonomous behavior")
        // TODO: Generate Yuweng's proactive message
        return Result.success()
    }
}
