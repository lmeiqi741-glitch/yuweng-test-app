package com.xinyu.yuweng.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xinyu.yuweng.data.local.entity.MemoryCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryCardDao {
    @Insert
    suspend fun insert(card: MemoryCardEntity): Long

    @Update
    suspend fun update(card: MemoryCardEntity)

    @Query("SELECT * FROM memory_cards WHERE userId = :userId AND status IN ('fresh','stable','fading') ORDER BY createdAt DESC")
    fun getActiveCardsFlow(userId: Long): Flow<List<MemoryCardEntity>>

    @Query("SELECT * FROM memory_cards WHERE userId = :userId AND status IN ('fresh','stable','fading') ORDER BY createdAt DESC")
    suspend fun getActiveCards(userId: Long): List<MemoryCardEntity>

    @Query("SELECT * FROM memory_cards WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllCards(userId: Long): List<MemoryCardEntity>

    @Query("SELECT * FROM memory_cards WHERE id = :id")
    suspend fun getById(id: Long): MemoryCardEntity?

    @Query("SELECT * FROM memory_cards WHERE userId = :userId AND status = :status")
    suspend fun getByStatus(userId: Long, status: String): List<MemoryCardEntity>

    @Query("SELECT COUNT(*) FROM memory_cards WHERE userId = :userId AND status IN ('fresh','stable','fading')")
    suspend fun getActiveCount(userId: Long): Int

    @Query("UPDATE memory_cards SET recallCount = recallCount + 1, lastRecalledAt = :now WHERE id = :id")
    suspend fun incrementRecall(id: Long, now: Long)

    @Query("UPDATE memory_cards SET status = :status, forgottenAt = :forgottenAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, forgottenAt: Long?)
}
