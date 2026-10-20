package com.xinyu.yuweng.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xinyu.yuweng.data.local.entity.ForgettingLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ForgettingLogDao {
    @Insert
    suspend fun insert(entry: ForgettingLogEntity): Long

    @Update
    suspend fun update(entry: ForgettingLogEntity)

    @Query("SELECT * FROM forgetting_log WHERE userId = :userId ORDER BY forgottenAt DESC")
    fun getAllFlow(userId: Long): Flow<List<ForgettingLogEntity>>

    @Query("SELECT * FROM forgetting_log WHERE userId = :userId ORDER BY forgottenAt DESC")
    suspend fun getAll(userId: Long): List<ForgettingLogEntity>

    @Query("SELECT COUNT(*) FROM forgetting_log WHERE userId = :userId AND redeemed = 0")
    suspend fun getForgottenCount(userId: Long): Int

    @Query("SELECT * FROM forgetting_log WHERE memoryCardId = :cardId LIMIT 1")
    suspend fun getByCardId(cardId: Long): ForgettingLogEntity?

    @Query("UPDATE forgetting_log SET redeemed = 1 WHERE id = :id")
    suspend fun markRedeemed(id: Long)
}
