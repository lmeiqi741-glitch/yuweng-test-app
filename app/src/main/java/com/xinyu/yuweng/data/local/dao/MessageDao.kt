package com.xinyu.yuweng.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xinyu.yuweng.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt ASC")
    fun getConversationMessages(convId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt ASC")
    suspend fun getConversationMessagesOnce(convId: String): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastMessage(): MessageEntity?

    @Query("SELECT DISTINCT conversationId FROM messages ORDER BY createdAt DESC")
    suspend fun getConversationIds(): List<String>

    @Query("UPDATE messages SET isReadByYuweng = 1 WHERE conversationId = :convId AND isReadByYuweng = 0")
    suspend fun markConversationRead(convId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :convId")
    suspend fun getConversationTurnCount(convId: String): Int

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMessages(convId: String, limit: Int): List<MessageEntity>

    @Query("SELECT MAX(createdAt) FROM messages WHERE conversationId = :convId")
    suspend fun getLastMessageTime(convId: String): Long?
}
