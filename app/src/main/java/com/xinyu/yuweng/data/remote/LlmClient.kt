package com.xinyu.yuweng.data.remote

/**
 * LLM 客户端接口。
 * 原型期支持 chat（自由文本）和 chatStructured（结构化输出）两种模式。
 */
interface LlmClient {
    /**
     * 自由对话
     * @param systemPrompt system 消息原文（已从 assets 读取，不修改）
     * @param messages 对话历史（已不含 system）
     * @return 屿翁回复文本
     */
    suspend fun chat(systemPrompt: String, messages: List<Pair<String, String>>): String

    /**
     * 结构化调用（用于 L2/L3 生成、事件判定等）
     * @param systemPrompt system 消息
     * @param prompt 单次 user 消息
     * @return 结构化文本输出
     */
    suspend fun chatStructured(systemPrompt: String, prompt: String): String
}
