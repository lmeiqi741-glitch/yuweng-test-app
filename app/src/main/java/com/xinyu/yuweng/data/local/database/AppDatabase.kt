package com.xinyu.yuweng.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xinyu.yuweng.data.local.dao.ForgettingLogDao
import com.xinyu.yuweng.data.local.dao.MessageDao
import com.xinyu.yuweng.data.local.dao.MessageSummaryDao
import com.xinyu.yuweng.data.local.dao.MemoryCardDao
import com.xinyu.yuweng.data.local.dao.PendingMessageDao
import com.xinyu.yuweng.data.local.dao.UserStateDao
import com.xinyu.yuweng.data.local.entity.ForgettingLogEntity
import com.xinyu.yuweng.data.local.entity.MessageEntity
import com.xinyu.yuweng.data.local.entity.MessageSummaryEntity
import com.xinyu.yuweng.data.local.entity.MemoryCardEntity
import com.xinyu.yuweng.data.local.entity.PendingMessageEntity
import com.xinyu.yuweng.data.local.entity.UserStateEntity

@Database(
    entities = [
        MessageEntity::class,
        PendingMessageEntity::class,
        MessageSummaryEntity::class,
        MemoryCardEntity::class,
        ForgettingLogEntity::class,
        UserStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun pendingMessageDao(): PendingMessageDao
    abstract fun messageSummaryDao(): MessageSummaryDao
    abstract fun memoryCardDao(): MemoryCardDao
    abstract fun forgettingLogDao(): ForgettingLogDao
    abstract fun userStateDao(): UserStateDao

    companion object {
        const val DATABASE_NAME = "yuweng_database"
    }
}
