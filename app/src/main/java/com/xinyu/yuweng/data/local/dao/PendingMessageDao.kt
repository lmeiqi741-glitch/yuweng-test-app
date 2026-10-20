package com.xinyu.yuweng.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xinyu.yuweng.data.local.entity.PendingMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMessageDao {
    @Insert
    suspend fun insert(message: PendingMessageEntity): Long

    @Query("SELECT * FROM pending_messages WHERE isReviewed = 0 ORDER BY createdAt ASC")
    suspend fun getUnreviewed(): List<PendingMessageEntity>

    @Query("SELECT * FROM pending_messages WHERE isReviewed = 0 ORDER BY createdAt ASC")
    fun getUnreviewedFlow(): Flow<List<PendingMessageEntity>>

    @Query("SELECT COUNT(*) FROM pending_messages WHERE isReviewed = 0")
    suspend fun getUnreviewedCount(): Int

    @Query("SELECT COUNT(*) FROM pending_messages WHERE isReviewed = 0")
    fun getUnreviewedCountFlow(): Flow<Int>

    @Update
    suspend fun update(message: PendingMessageEntity)

    @Query("UPDATE pending_messages SET isReviewed = 1 WHERE id = :id")
    suspend fun markReviewed(id: Long)
}
