package com.xinyu.yuweng

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xinyu.yuweng.data.work.AutonomousBehaviorWorker
import com.xinyu.yuweng.data.work.MemoryDecayWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class YuwengApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        initWorkManager()
        Log.d("YuwengApp", "屿翁 初始化完成")
    }

    private fun initWorkManager() {
        val decayRequest = PeriodicWorkRequestBuilder<MemoryDecayWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "yuweng_memory_decay",
            ExistingPeriodicWorkPolicy.KEEP,
            decayRequest
        )

        val behaviorRequest = PeriodicWorkRequestBuilder<AutonomousBehaviorWorker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "yuweng_autonomous_behavior",
            ExistingPeriodicWorkPolicy.KEEP,
            behaviorRequest
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
