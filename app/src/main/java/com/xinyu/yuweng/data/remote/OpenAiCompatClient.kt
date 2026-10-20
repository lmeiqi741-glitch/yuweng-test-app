package com.xinyu.yuweng.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.xinyu.yuweng.data.remote.dto.ChatApi
import com.xinyu.yuweng.data.remote.dto.ChatMessage
import com.xinyu.yuweng.data.remote.dto.ChatRequest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * OpenAI chat completions 兼容客户端。
 * Base URL / API Key / 模型名由用户配置驱动，运行时动态构造。
 * 配置变更时由调用方重建实例，不读 BuildConfig。
 */
class OpenAiCompatClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val modelName: String,
    private val temperature: Double = 0.8,
    private val maxTokens: Int = 800
) : LlmClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val api: ChatApi = Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/') + "/")
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ChatApi::class.java)

    override suspend fun chat(systemPrompt: String, messages: List<Pair<String, String>>): String {
        val allMessages = buildList {
            add(ChatMessage("system", systemPrompt))
            messages.forEach { (role, content) ->
                add(ChatMessage(role, content))
            }
        }
        val request = ChatRequest(
            model = modelName,
            messages = allMessages,
            temperature = temperature,
            maxTokens = maxTokens
        )
        val response = api.chatCompletions(request)
        return response.choices.firstOrNull()?.message?.content
            ?: throw LlmException("Empty response from API")
    }

    override suspend fun chatStructured(systemPrompt: String, prompt: String): String {
        val messages = listOf(
            ChatMessage("system", systemPrompt),
            ChatMessage("user", prompt)
        )
        val request = ChatRequest(
            model = modelName,
            messages = messages,
            temperature = 0.3,  // 结构化输出用低温度
            maxTokens = maxTokens
        )
        val response = api.chatCompletions(request)
        return response.choices.firstOrNull()?.message?.content
            ?: throw LlmException("Empty response from API")
    }
}

class LlmException(message: String) : Exception(message)
