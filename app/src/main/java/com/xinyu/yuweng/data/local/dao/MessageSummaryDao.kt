package com.xinyu.yuweng.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xinyu.yuweng.data.local.entity.MessageSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageSummaryDao {
    @Insert
    suspend fun insert(summary: MessageSummaryEntity): Long

    @Query("SELECT * FROM messages_summary WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(userId: Long, limit: Int): List<MessageSummaryEntity>

    @Query("SELECT * FROM messages_summary WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentFlow(userId: Long, limit: Int): Flow<List<MessageSummaryEntity>>

    @Query("SELECT * FROM messages_summary WHERE expiresAt < :now")
    suspend fun getExpired(now: Long): List<MessageSummaryEntity>

    @Query("DELETE FROM messages_summary WHERE id = :id")
    suspend fun delete(id: Long)
}
