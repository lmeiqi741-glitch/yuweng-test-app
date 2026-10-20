package com.xinyu.yuweng.data.repository

import android.content.Context
import android.util.Log
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
import com.xinyu.yuweng.data.remote.LlmConfigStore
import com.xinyu.yuweng.data.remote.LlmProvider
import com.xinyu.yuweng.data.remote.LlmConfig
import com.xinyu.yuweng.core.stateengine.SilenceState
import com.xinyu.yuweng.core.stateengine.StateEngine
import com.xinyu.yuweng.core.stateengine.YuwengState
import com.xinyu.yuweng.core.stateengine.PreferenceVector
import com.xinyu.yuweng.core.stateengine.MoodEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 屿翁数据仓库。
 * 协调 Room 数据、状态机、LLM 客户端、Asset 读取。
 */
@Singleton
class YuwengRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val pendingMessageDao: PendingMessageDao,
    private val messageSummaryDao: MessageSummaryDao,
    private val memoryCardDao: MemoryCardDao,
    private val forgettingLogDao: ForgettingLogDao,
    private val userStateDao: UserStateDao,
    private val llmProvider: LlmProvider,
    private val llmConfigStore: LlmConfigStore,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "YuwengRepository"
        private const val USER_ID = 1L
        private const val SESSION_GAP_MINUTES = 30L
    }

    // ========== 状态管理 ==========

    suspend fun ensureUserState() {
        if (userStateDao.getState() == null) {
            userStateDao.insert(UserStateEntity())
        }
    }

    fun getStateFlow(): Flow<UserStateEntity?> = userStateDao.getStateFlow()

    suspend fun getState(): YuwengState {
        val entity = userStateDao.getState() ?: UserStateEntity().also { userStateDao.insert(it) }
        return entity.toState()
    }

    suspend fun saveState(state: YuwengState) {
        userStateDao.update(UserStateEntity.fromState(state))
    }

    suspend fun updateMoodAndEnergy(mood: Float, energy: Int, now: Long) {
        userStateDao.updateMoodAndEnergy(mood, energy, now)
    }

    // ========== 对话管理 ==========

    fun getConversationMessages(convId: String): Flow<List<MessageEntity>> =
        messageDao.getConversationMessages(convId)

    suspend fun getCurrentConversationId(): String {
        val lastMsg = messageDao.getLastMessage()
        if (lastMsg != null) {
            val now = System.currentTimeMillis()
            val gap = (now - lastMsg.createdAt) / (60 * 1000)
            if (gap <= SESSION_GAP_MINUTES) {
                return lastMsg.conversationId
            }
        }
        // 新开会话
        return "conv_${System.currentTimeMillis()}"
    }

    suspend fun sendMessage(conversationId: String, role: String, content: String) {
        messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markConversationRead(convId: String) {
        messageDao.markConversationRead(convId)
    }

    // ========== Pending Messages ==========

    suspend fun addPendingMessage(content: String) {
        pendingMessageDao.insert(
            PendingMessageEntity(
                content = content,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    fun getUnreviewedPendingFlow(): Flow<List<PendingMessageEntity>> =
        pendingMessageDao.getUnreviewedFlow()

    suspend fun getUnreviewedCount(): Int = pendingMessageDao.getUnreviewedCount()

    fun getUnreviewedCountFlow(): Flow<Int> = pendingMessageDao.getUnreviewedCountFlow()

    suspend fun reviewPendingMessages(mood: Float) {
        val pending = pendingMessageDao.getUnreviewed()
        if (pending.isEmpty()) return

        // 屿翁按心情选择性读
        val toRead = when {
            mood >= 0f -> pending
            mood >= -7f -> pending.takeLast(3)
            else -> pending.takeLast(1)
        }

        toRead.forEach { pendingMessageDao.markReviewed(it.id) }
        Log.d(TAG, "Reviewed ${toRead.size}/${pending.size} pending messages (mood=$mood)")
    }

    // ========== 记忆卡片 ==========

    fun getActiveCardsFlow(): Flow<List<MemoryCardEntity>> =
        memoryCardDao.getActiveCardsFlow(USER_ID)

    suspend fun getActiveCards(): List<MemoryCardEntity> =
        memoryCardDao.getActiveCards(USER_ID)

    suspend fun getCardById(id: Long): MemoryCardEntity? =
        memoryCardDao.getById(id)

    suspend fun getAllCards(): List<MemoryCardEntity> =
        memoryCardDao.getAllCards(USER_ID)

    suspend fun saveMemoryCard(card: MemoryCardEntity) {
        memoryCardDao.insert(card)
    }

    suspend fun recallCard(id: Long) {
        memoryCardDao.incrementRecall(id, System.currentTimeMillis())
    }

    suspend fun redeemCard(cardId: Long) {
        // 从遗忘日志赎回
        val logEntry = forgettingLogDao.getByCardId(cardId)
        if (logEntry != null) {
            forgettingLogDao.markRedeemed(logEntry.id)
        }
        // 卡片复活为 fresh
        memoryCardDao.updateStatus(cardId, "fresh", null)
    }

    // ========== 遗忘日志 ==========

    fun getForgettingLogFlow(): Flow<List<ForgettingLogEntity>> =
        forgettingLogDao.getAllFlow(USER_ID)

    suspend fun getForgottenCount(): Int =
        forgettingLogDao.getForgottenCount(USER_ID)

    // ========== LLM ==========

    suspend fun isLlmConfigured(): Boolean = llmConfigStore.getConfig().isConfigured

    suspend fun getLlmConfig(): LlmConfig = llmConfigStore.getConfig()

    suspend fun saveLlmConfig(config: LlmConfig) = llmConfigStore.saveConfig(config)

    suspend fun testLlmConnection(config: LlmConfig): Result<String> =
        llmProvider.testConnection(config)

    // ========== Asset 读取 ==========

    fun loadSystemPromptFromAssets(): String {
        return context.assets.open("specs/01-屿翁system-prompt.txt")
            .bufferedReader()
            .use { it.readText() }
    }

    // ========== 工具 ==========

    private fun UserStateEntity.toState(): YuwengState {
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
