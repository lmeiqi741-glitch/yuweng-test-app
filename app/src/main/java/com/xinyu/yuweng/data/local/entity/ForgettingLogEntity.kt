package com.xinyu.yuweng.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 遗忘日志。
 * 字段与规格文档 02 文档 2.5 forgetting_log 表严格一致。
 * 含 redeemed 字段支持赎回。
 */
@Entity(tableName = "forgetting_log")
data class ForgettingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 1,
    val memoryCardId: Long,
    val contentSnapshot: String,
    val forgottenAt: Long,
    val whyForgotten: String,
    val redeemed: Boolean = false
)
