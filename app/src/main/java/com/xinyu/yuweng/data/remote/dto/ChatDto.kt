package com.xinyu.yuweng.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.8,
    @SerialName("max_tokens")
    val maxTokens: Int = 800
)

@Serializable
data class ChatMessage(
    val role: String,     // "system" | "user" | "assistant"
    val content: String
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    val message: ChatMessage
)

@Serializable
data class ErrorResponse(
    val error: ErrorDetail? = null
)

@Serializable
data class ErrorDetail(
    val message: String? = null,
    val type: String? = null
)
