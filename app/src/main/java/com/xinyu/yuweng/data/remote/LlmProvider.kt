package com.xinyu.yuweng.data.remote

import android.util.Log
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * LLM 客户端工厂/提供者。
 * 根据用户配置决定使用 Mock 还是真实 OpenAI 兼容客户端。
 * 未配置 API 时默认走 Mock；已配置则走真实请求。
 * 配置变更时由调用方重建 OpenAiCompatClient 实例。
 */
@Singleton
class LlmProvider @Inject constructor(
    private val configStore: LlmConfigStore,
    private val mockClient: Provider<MockLlmClient>
) {
    private var cachedClient: LlmClient? = null
    private var cachedConfig: LlmConfig? = null

    /**
     * 获取当前 LLM 客户端。
     * 如果配置变更则重建真实客户端，否则返回缓存。
     */
    suspend fun getClient(): LlmClient {
        val config = configStore.getConfig()
        if (!config.isConfigured) {
            Log.d("LlmProvider", "Using Mock client (no API configured)")
            return mockClient.get()
        }

        // 检查配置是否变更
        if (cachedConfig != null && !configChanged(cachedConfig!!, config)) {
            return cachedClient!!
        }

        Log.d("LlmProvider", "Creating OpenAI client for ${config.baseUrl}")
        val newClient = OpenAiCompatClient(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            modelName = config.modelName,
            temperature = config.temperature,
            maxTokens = config.maxTokens
        )
        cachedClient = newClient
        cachedConfig = config
        return newClient
    }

    /**
     * 强制刷新（如测试连接时）。
     */
    suspend fun testConnection(config: LlmConfig): Result<String> {
        return try {
            val client = OpenAiCompatClient(
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                modelName = config.modelName,
                temperature = 0.1,
                maxTokens = 10
            )
            val response = client.chatStructured("回复OK", "回复OK")
            Result.success(response)
        } catch (e: Exception) {
            // 不泄露完整 Key
            val safeMessage = e.message?.replace(config.apiKey, "***") ?: "连接失败"
            Result.failure(Exception(safeMessage))
        }
    }

    private fun configChanged(old: LlmConfig, new: LlmConfig): Boolean =
        old.baseUrl != new.baseUrl ||
                old.apiKey != new.apiKey ||
                old.modelName != new.modelName ||
                old.temperature != new.temperature ||
                old.maxTokens != new.maxTokens

    fun invalidate() {
        cachedClient = null
        cachedConfig = null
    }
}
