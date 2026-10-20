package com.xinyu.yuweng.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.xinyu.yuweng.data.local.dao.ForgettingLogDao
import com.xinyu.yuweng.data.local.dao.MessageDao
import com.xinyu.yuweng.data.local.dao.MessageSummaryDao
import com.xinyu.yuweng.data.local.dao.MemoryCardDao
import com.xinyu.yuweng.data.local.dao.PendingMessageDao
import com.xinyu.yuweng.data.local.dao.UserStateDao
import com.xinyu.yuweng.data.local.database.AppDatabase
import com.xinyu.yuweng.data.remote.MockLlmClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// DataStore extension property
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yuweng_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ========== Database ==========

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun providePendingMessageDao(db: AppDatabase): PendingMessageDao = db.pendingMessageDao()

    @Provides
    fun provideMessageSummaryDao(db: AppDatabase): MessageSummaryDao = db.messageSummaryDao()

    @Provides
    fun provideMemoryCardDao(db: AppDatabase): MemoryCardDao = db.memoryCardDao()

    @Provides
    fun provideForgettingLogDao(db: AppDatabase): ForgettingLogDao = db.forgettingLogDao()

    @Provides
    fun provideUserStateDao(db: AppDatabase): UserStateDao = db.userStateDao()

    // ========== DataStore ==========

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    // ========== LLM ==========

    @Provides
    @Singleton
    fun provideMockLlmClient(): MockLlmClient = MockLlmClient()
}
