package com.xinyu.yuweng.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 沉默期间用户发送的暂存消息。
 * 字段与规格文档 02 的 pending_messages 表严格一致。
 */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val createdAt: Long,
    val isReviewed: Boolean = false
)
