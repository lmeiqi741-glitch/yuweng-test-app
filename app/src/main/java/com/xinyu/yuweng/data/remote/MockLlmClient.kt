package com.xinyu.yuweng.data.remote

import android.util.Log
import com.xinyu.yuweng.core.stateengine.YuwengState
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Mock LLM 客户端。
 * 无 API Key 时全功能可用：延迟、沉默、记忆引用、状态联动全部走通。
 * 基于状态机的规则回复，从模板池按 mood/energy 阈值选回复。
 *
 * 四档回复策略：
 * - 沉默档：energy<25 或 mood<-7(60%概率) → 返回空（模拟沉默）
 * - 短回复档：energy 25-50 或 mood -7~-3 → 简短回复（≤20字）
 * - 正常档：energy 50-80, mood -3~+3 → 正常回复
 * - 延展档：energy>80 → 可长回复
 */
class MockLlmClient : LlmClient {

    companion object {
        private const val TAG = "MockLlmClient"
    }

    override suspend fun chat(systemPrompt: String, messages: List<Pair<String, String>>): String {
        // 延迟 random(3,15) 秒
        val delaySeconds = Random.nextInt(3, 16)
        delay(delaySeconds * 1000L)

        // 从 system prompt 解析状态段（Mock 模式自己维护状态，实际从外部注入）
        // 原型期：Mock 客户端不持有状态，由调用方在组装 prompt 时注入状态段
        // 这里通过解析 system prompt 中的状态段来读取当前状态
        val state = parseStateFromPrompt(systemPrompt)

        // 沉默判定
        if (shouldSilence(state)) {
            Log.d(TAG, "Mock silence triggered: energy=${state.energy}, mood=${state.mood}")
            return ""  // 空字符串 = 沉默
        }

        // 根据状态选回复档位
        return selectResponse(state, messages)
    }

    override suspend fun chatStructured(systemPrompt: String, prompt: String): String {
        delay(2000L)  // 结构化也延迟
        // 原型期返回 JSON 占位，真实期由 LLM 生成
        return """{"event":"neutral","mood_delta":0,"energy_delta":0}"""
    }

    private fun parseStateFromPrompt(prompt: String): YuwengState {
        // 从 "你此刻：心情 X/10（...），能量 Y/100（...）" 解析
        val moodRegex = Regex("心情\\s*(-?\\d+\\.?\\d*)/10")
        val energyRegex = Regex("能量\\s*(\\d+)/100")
        val mood = moodRegex.find(prompt)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val energy = energyRegex.find(prompt)?.groupValues?.get(1)?.toIntOrNull() ?: 70
        return YuwengState(mood = mood, energy = energy)
    }

    private fun shouldSilence(state: YuwengState): Boolean {
        if (state.energy < 25) return true
        if (state.mood < -7 && Random.nextFloat() < 0.6f) return true
        return false
    }

    private fun selectResponse(state: YuwengState, messages: List<Pair<String, String>>): String {
        val lastUserMsg = messages.lastOrNull { it.first == "user" }?.second ?: ""
        val isShortRequired = state.energy < 25 || state.mood < -7

        return when {
            state.energy < 50 || state.mood < -3 -> shortReplies.random()
            state.energy > 80 -> longReplies.random()
            else -> normalReplies.random()
        }
    }

    // 短回复池（≤20字，低落时使用）
    private val shortReplies = listOf(
        "嗯。",
        "听到了。",
        "这样啊。",
        "不说话了。",
        "有点累。",
        "先放着。",
        "哦。",
        "知道了。",
        "嗯，今天就这样。",
        "不想说了。"
    )

    // 正常回复池
    private val normalReplies = listOf(
        "这个挺有意思。你说的那个，我想了一下，确实有另一种可能。",
        "我有时候也在想类似的事。不是每次都找到答案，但想的过程本身就不错。",
        "你说的我不完全同意。不过能说出来就挺好的。",
        "这问题没有标准答案。我见过两种人，一种选了 A 一种选了 B，都过得还行。",
        "我不太喜欢这个词。它把一件事说得太简单了。",
        "你继续说，我听着。",
        "嗯，这个角度我没想过。",
        "有道理。但我还是觉得应该再想想。",
        "每个人对这件事的感受不一样。你的感受是真实的。",
        "我没法替你决定，但我可以说说我是怎么看这件事的。"
    )

    // 延展回复池（长回复，高能量时使用）
    private val longReplies = listOf(
        "这件事我想过很久。起初觉得很简单，后来发现里面有很多层。第一层是……第二层其实跟表面完全相反。我现在也还在想。不过有一点是确定的：急不得。",
        "你说到了一个我很在意的东西。我想多说几句。我见过一些人处理类似的事，有的选择了回避，有的选择了面对。两种都有代价。关键不在于选哪个，在于你选了之后能不能接受那个代价。我说不清楚更多了，但这确实是值得想的事。",
        "我最近一直在想一个问题，跟你说的有点关系。如果一件事注定会消失，那值不值得认真对待？我的回答是值得。不是因为结果，是因为你认真对待这件事的那个瞬间，它是真的。"
    )
}
