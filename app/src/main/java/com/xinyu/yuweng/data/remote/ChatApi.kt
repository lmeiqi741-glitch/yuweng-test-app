package com.xinyu.yuweng.data.remote

import com.xinyu.yuweng.data.remote.dto.ChatRequest
import com.xinyu.yuweng.data.remote.dto.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit 接口：OpenAI chat completions 兼容格式。
 * Base URL 由用户在设置页配置。
 */
interface ChatApi {
    @POST("chat/completions")
    suspend fun chatCompletions(@Body request: ChatRequest): ChatResponse
}
