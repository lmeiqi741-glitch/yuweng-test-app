package com.xinyu.yuweng.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xinyu.yuweng.data.local.entity.UserStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: UserStateEntity)

    @Update
    suspend fun update(state: UserStateEntity)

    @Query("SELECT * FROM users WHERE id = 1")
    suspend fun getState(): UserStateEntity?

    @Query("SELECT * FROM users WHERE id = 1")
    fun getStateFlow(): Flow<UserStateEntity?>

    @Query("UPDATE users SET mood = :mood, energy = :energy, lastEnergyUpdate = :now WHERE id = 1")
    suspend fun updateMoodAndEnergy(mood: Float, energy: Int, now: Long)

    @Query("UPDATE users SET energy = :energy, lastEnergyUpdate = :now WHERE id = 1")
    suspend fun updateEnergy(energy: Int, now: Long)

    @Query("UPDATE users SET preferenceVector = :pref WHERE id = 1")
    suspend fun updatePreference(pref: String?)

    @Query("UPDATE users SET moodLog = :log WHERE id = 1")
    suspend fun updateMoodLog(log: String)

    @Query("UPDATE users SET dayMoodAccumulator = :value WHERE id = 1")
    suspend fun updateDayAccumulator(value: Float)

    @Query("UPDATE users SET dayMoodAccumulator = 0 WHERE id = 1")
    suspend fun resetDayAccumulator()
}
