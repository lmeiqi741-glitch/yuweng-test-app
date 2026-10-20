package com.xinyu.yuweng.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xinyu.yuweng.core.stateengine.PreferenceVector
import com.xinyu.yuweng.core.stateengine.YuwengState
import org.json.JSONObject

/**
 * 屿翁状态（users 表）。
 * 字段与规格文档 02 严格一致。
 */
@Entity(tableName = "users")
data class UserStateEntity(
    @PrimaryKey val id: Long = 1,
    val mood: Float = 0f,
    val energy: Int = 70,
    val preferenceVector: String? = null,
    val lastEnergyUpdate: Long = 0L,
    val moodLog: String = "[]",
    val dayMoodAccumulator: Float = 0f
) {
    companion object {
        fun fromState(state: YuwengState): UserStateEntity {
            val prefJson = state.preference?.let { pref ->
                JSONObject().apply {
                    put("topics", JSONObject(pref.topics as Map<*, *>))
                    put("tone", JSONObject(pref.tone as Map<*, *>))
                    put("rhythm", JSONObject(pref.rhythm as Map<*, *>))
                    put("values", JSONObject(pref.values as Map<*, *>))
                }.toString()
            }
            return UserStateEntity(
                mood = state.mood,
                energy = state.energy,
                preferenceVector = prefJson,
                lastEnergyUpdate = state.lastEnergyUpdateAt,
                moodLog = "[]",  // TODO: 实际 mood_log 序列化
                dayMoodAccumulator = state.dayMoodAccumulator
            )
        }
    }

    fun toState(): YuwengState {
        val pref = if (preferenceVector != null) {
            try {
                val json = JSONObject(preferenceVector)
                PreferenceVector(
                    topics = json.optJSONObject("topics")?.let { obj ->
                        obj.keys().asSequence().associateWith { obj.getDouble(it).toFloat() }
                    } ?: emptyMap(),
                    tone = json.optJSONObject("tone")?.let { obj ->
                        obj.keys().asSequence().associateWith { obj.getDouble(it).toFloat() }
                    } ?: emptyMap(),
                    rhythm = json.optJSONObject("rhythm")?.let { obj ->
                        obj.keys().asSequence().associateWith { obj.getDouble(it).toFloat() }
                    } ?: emptyMap(),
                    values = json.optJSONObject("values")?.let { obj ->
                        obj.keys().asSequence().associateWith { obj.getDouble(it).toFloat() }
                    } ?: emptyMap()
                )
            } catch (_: Exception) { null }
        } else null

        return YuwengState(
            mood = mood,
            energy = energy,
            preference = pref,
            lastEnergyUpdateAt = lastEnergyUpdate,
            dayMoodAccumulator = dayMoodAccumulator
        )
    }
}
