package com.xinyu.yuweng.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * L3: 长期记忆卡片 "屿翁记得"。
 * 字段与规格文档 02 文档 2.3 memory_cards 表严格一致。
 * card_type: moment / quote / question / feeling / song / self
 * status: fresh / stable / fading / forgotten
 */
@Entity(tableName = "memory_cards")
data class MemoryCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 1,
    val cardType: String,           // moment/quote/question/feeling/song/self
    val content: String,            // 屿翁第一人称
    val context: String,            // 为什么记住
    val emotionalWeight: Float,     // 0~1
    val status: String = "fresh",   // fresh/stable/fading/forgotten
    val recallCount: Int = 0,
    val lastRecalledAt: Long? = null,
    val createdAt: Long,
    val forgottenAt: Long? = null
)
