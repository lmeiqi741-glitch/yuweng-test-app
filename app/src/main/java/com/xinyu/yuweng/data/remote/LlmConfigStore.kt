package com.xinyu.yuweng.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LLM 配置数据类。
 */
data class LlmConfig(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val modelName: String = "gpt-4o-mini",
    val temperature: Double = 0.8,
    val maxTokens: Int = 800,
    val embeddingModel: String = "",
    val isConfigured: Boolean = false
)

/**
 * 预设快填配置。
 */
data class LlmPreset(
    val name: String,
    val baseUrl: String,
    val recommendedModel: String
)

object LlmPresets {
    val OPENAI = LlmPreset("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini")
    val DEEPSEEK = LlmPreset("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat")
    val TONGYI = LlmPreset("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-turbo")
    val KIMI = LlmPreset("Kimi", "https://api.moonshot.cn/v1", "moonshot-v1-8k")
    val ZHIPU = LlmPreset("智谱", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash")
    val OLLAMA = LlmPreset("本地 Ollama", "http://localhost:11434/v1", "llama3")

    val ALL = listOf(OPENAI, DEEPSEEK, TONGYI, KIMI, ZHIPU, OLLAMA)
}

/**
 * DataStore 持久化的 LLM 配置存储。
 * API Key 加密存储（EncryptedDataStore），原型期使用 DataStore + 后续可换加密方案。
 */
@Singleton
class LlmConfigStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("llm_base_url")
        private val KEY_API_KEY = stringPreferencesKey("llm_api_key")      // TODO: 加密存储
        private val KEY_MODEL = stringPreferencesKey("llm_model")
        private val KEY_TEMPERATURE = doublePreferencesKey("llm_temperature")
        private val KEY_MAX_TOKENS = stringPreferencesKey("llm_max_tokens")
        private val KEY_EMBEDDING_MODEL = stringPreferencesKey("llm_embedding_model")
        private val KEY_IS_CONFIGURED = stringPreferencesKey("llm_is_configured")
    }

    val configFlow: Flow<LlmConfig> = dataStore.data.map { prefs ->
        val isConfigured = prefs[KEY_IS_CONFIGURED]?.toBoolean() ?: false
        val apiKey = prefs[KEY_API_KEY] ?: ""
        LlmConfig(
            baseUrl = prefs[KEY_BASE_URL] ?: "https://api.openai.com/v1",
            apiKey = apiKey,
            modelName = prefs[KEY_MODEL] ?: "gpt-4o-mini",
            temperature = prefs[KEY_TEMPERATURE] ?: 0.8,
            maxTokens = (prefs[KEY_MAX_TOKENS] ?: "800").toIntOrNull() ?: 800,
            embeddingModel = prefs[KEY_EMBEDDING_MODEL] ?: "",
            isConfigured = isConfigured && apiKey.isNotBlank()
        )
    }

    suspend fun getConfig(): LlmConfig = configFlow.first()

    suspend fun saveConfig(config: LlmConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = config.baseUrl
            prefs[KEY_API_KEY] = config.apiKey
            prefs[KEY_MODEL] = config.modelName
            prefs[KEY_TEMPERATURE] = config.temperature
            prefs[KEY_MAX_TOKENS] = config.maxTokens.toString()
            prefs[KEY_EMBEDDING_MODEL] = config.embeddingModel
            prefs[KEY_IS_CONFIGURED] = (config.apiKey.isNotBlank()).toString()
        }
    }

    suspend fun clearConfig() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_API_KEY)
            prefs[KEY_IS_CONFIGURED] = "false"
        }
    }
}
