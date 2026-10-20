package com.xinyu.yuweng.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * L2: 情景记忆摘要。
 * 字段与规格文档 02 文档 2.2 messages_summary 表严格一致。
 * topics / key_quotes 以 JSON 字符串存储。
 */
@Entity(tableName = "messages_summary")
data class MessageSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val userId: Long = 1,           // 纯本地单用户，固定为 1
    val startedAt: Long,
    val endedAt: Long,
    val turnCount: Int,
    val summary: String,            // LLM 生成，屿翁视角
    val emotionalArc: String,       // "从低落走向平静"
    val topics: String,             // JSON 数组字符串
    val keyQuotes: String,          // JSON 数组字符串
    val moodAfter: Float,
    val energyDelta: Int,
    val createdAt: Long,
    val expiresAt: Long             // created_at + 7 天
)
