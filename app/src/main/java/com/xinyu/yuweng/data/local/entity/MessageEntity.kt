package com.xinyu.yuweng.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * L1: 当前对话消息。
 * 字段与规格文档 02 的 messages 表严格一致。
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val role: String,           // "user" | "assistant"
    val content: String,
    val createdAt: Long,
    val isReadByYuweng: Boolean = false
)
