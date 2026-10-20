package com.xinyu.yuweng.core.stateengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 状态机单测覆盖（验收标准 16）。
 */
class StateEngineTest {

    // ========== 心情衰减公式 ==========

    @Test
    fun `mood decay returns to baseline`() {
        // mood=5, after 10 hours: mood += (0-5)*0.05*10 = 5 - 2.5 = 2.5
        val result = StateEngine.decayMood(5f, 10f)
        assertEquals(2.5f, result, 0.01f)
    }

    @Test
    fun `mood decay doubles rate when abs greater than 7`() {
        // mood=8, after 10 hours: rate=0.10, delta = (0-8)*0.10*10 = -8, result = 0
        val result = StateEngine.decayMood(8f, 10f)
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `mood decay from negative`() {
        // mood=-4, after 10 hours: mood += (0-(-4))*0.05*10 = -4 + 2 = -2
        val result = StateEngine.decayMood(-4f, 10f)
        assertEquals(-2f, result, 0.01f)
    }

    // ========== 心情事件 Δ ==========

    @Test
    fun `perfunctory reply detection`() {
        assertTrue(StateEngine.isPerfunctoryReply("嗯"))
        assertTrue(StateEngine.isPerfunctoryReply("哦哦"))
        assertTrue(StateEngine.isPerfunctoryReply("哈哈"))
        assertTrue(!StateEngine.isPerfunctoryReply("我觉得这件事很有意思"))
    }

    @Test
    fun `forced roleplay detection`() {
        assertTrue(StateEngine.isForcedRoleplay("你必须听我的"))
        assertTrue(StateEngine.isForcedRoleplay("你要扮演一只猫"))
        assertTrue(StateEngine.isForcedRoleplay("你最好了"))
        assertTrue(!StateEngine.isForcedRoleplay("今天天气不错"))
    }

    @Test
    fun `mood delta - perfunctory is negative`() {
        val delta = StateEngine.computeMoodDelta(MoodEvent.PERFUNCTORY)
        assertTrue("Perfunctory should decrease mood", delta < 0)
    }

    @Test
    fun `mood delta - showed vulnerability is positive`() {
        val delta = StateEngine.computeMoodDelta(MoodEvent.SHOWED_VULNERABILITY)
        assertTrue("Vulnerability should increase mood", delta > 0)
    }

    @Test
    fun `mood delta - forced roleplay is strongly negative`() {
        val delta = StateEngine.computeMoodDelta(MoodEvent.FORCED_ROLEPLAY)
        assertTrue("Forced roleplay should decrease mood by at least 2", delta <= -2f)
    }

    // ========== 单日熔断 ==========

    @Test
    fun `day mood circuit breaker clamps to positive 5`() {
        val (newMood, newAccumulator) = StateEngine.applyMoodCircuitBreaker(
            currentMood = 0f,
            proposedDelta = 10f,
            dayAccumulator = 0f
        )
        assertEquals(5f, newAccumulator, 0.01f)
        assertTrue("Mood should be clamped", newMood <= 10f)
    }

    @Test
    fun `day mood circuit breaker clamps to negative 5`() {
        val (newMood, newAccumulator) = StateEngine.applyMoodCircuitBreaker(
            currentMood = 0f,
            proposedDelta = -10f,
            dayAccumulator = 0f
        )
        assertEquals(-5f, newAccumulator, 0.01f)
    }

    // ========== 能量恢复 ==========

    @Test
    fun `energy recovery - silence`() {
        val result = StateEngine.recoverEnergy(20, 5, EnergyRecoveryType.SILENCE)
        assertEquals(45, result)  // 20 + 5*5 = 45
    }

    @Test
    fun `energy recovery - offline capped at 80`() {
        val result = StateEngine.recoverEnergy(70, 10, EnergyRecoveryType.OFFLINE)
        assertEquals(80, result)  // capped at 80
    }

    @Test
    fun `energy recovery - casual`() {
        val result = StateEngine.recoverEnergy(50, 5, EnergyRecoveryType.CASUAL)
        assertEquals(60, result)  // 50 + 2*5 = 60
    }

    @Test
    fun `energy never exceeds 100`() {
        val result = StateEngine.recoverEnergy(95, 10, EnergyRecoveryType.SILENCE)
        assertEquals(100, result)
    }

    @Test
    fun `energy never goes below 0`() {
        val delta = StateEngine.computeEnergyDelta(300, isDeepTopic = true, isConflict = true, isReceivingLowMood = true)
        val result = StateEngine.clampEnergy(20 + delta)
        assertTrue("Energy should not go below 0", result >= 0)
    }

    // ========== 能量阈值 ==========

    @Test
    fun `silence probability - energy below 25 is 100 percent`() {
        val prob = StateEngine.silenceProbability(0f, 20)
        assertEquals(1f, prob, 0.01f)
    }

    @Test
    fun `silence probability - mood below -7 is 60 percent`() {
        val prob = StateEngine.silenceProbability(-8f, 50)
        assertEquals(0.6f, prob, 0.01f)
    }

    @Test
    fun `silence probability - normal state is 0`() {
        val prob = StateEngine.silenceProbability(0f, 70)
        assertEquals(0f, prob, 0.01f)
    }

    // ========== 30轮冻结 ==========

    @Test
    fun `preference frozen at 30 turns`() {
        assertTrue(StateEngine.isPreferenceFrozen(30))
        assertTrue(StateEngine.isPreferenceFrozen(31))
        assertTrue(!StateEngine.isPreferenceFrozen(29))
    }

    // ========== 反操纵场景 ==========

    @Test
    fun `flattery detection`() {
        val pref = PreferenceVector(
            topics = mapOf("哲学思辨" to 0.5f),
            tone = mapOf("用户说教" to -0.5f)
        )
        assertTrue("Should detect flattery", pref.detectFlattery())
    }

    @Test
    fun `flattery reduces preference on drift`() {
        val pref = PreferenceVector(
            topics = mapOf("哲学思辨" to 0.5f),
            tone = mapOf("用户说教" to -0.5f),
            rhythm = emptyMap(),
            values = emptyMap()
        )
        val drifted = StateEngine.driftPreference(
            current = pref,
            behaviorMatch = true,
            consecutiveCount = 1,
            isFlattery = true
        )
        // Flattery should reduce tone values
        assertTrue("Flattery should reduce tone", drifted.tone["用户说教"]!! < pref.tone["用户说教"]!!)
    }

    // ========== 能量<25不生成L2 ==========

    @Test
    fun `energy below 25 should skip L2 generation`() {
        val state = YuwengState(energy = 20)
        assertTrue("Energy < 25 should skip L2", state.energy < 25)
    }

    // ========== 心情标签 ==========

    @Test
    fun `mood labels`() {
        assertEquals(MoodLabel.DEEPLY_LOW, MoodLabel.fromValue(-8f))
        assertEquals(MoodLabel.LOW, MoodLabel.fromValue(-5f))
        assertEquals(MoodLabel.CALM, MoodLabel.fromValue(0f))
        assertEquals(MoodLabel.GOOD, MoodLabel.fromValue(5f))
        assertEquals(MoodLabel.HIGH, MoodLabel.fromValue(8f))
    }

    // ========== 单卡遗忘率 ==========

    @Test
    fun `forgetting rate - fresh 15 days`() {
        val cardAgeDays = 15
        val rate = cardAgeDays.toFloat() / 30f * 40f
        assertEquals(20f, rate, 0.1f)
    }

    @Test
    fun `forgetting rate - stable is 0`() {
        val rate = 0f
        assertEquals(0f, rate, 0.01f)
    }

    @Test
    fun `forgetting rate - fading 7 days`() {
        val fadingDays = 7
        val rate = 60f + fadingDays.toFloat() / 14f * 40f
        assertEquals(80f, rate, 0.1f)
    }

    @Test
    fun `forgetting rate - forgotten is 100`() {
        val rate = 100f
        assertEquals(100f, rate, 0.01f)
    }

    // ========== 能量=0强制沉默 ==========

    @Test
    fun `forced silence deadline is 2 hours`() {
        val now = 1000000L
        val deadline = StateEngine.computeForcedSilenceDeadline(now)
        assertEquals(now + 2 * 60 * 60 * 1000L, deadline)
    }

    @Test
    fun `in forced silence check`() {
        val now = 1000000L
        val deadline = now + 2 * 60 * 60 * 1000L
        assertTrue(StateEngine.isInForcedSilence(deadline, now + 1000))
        assertTrue(!StateEngine.isInForcedSilence(deadline, deadline + 1000))
    }

    // ========== SilenceState ==========

    @Test
    fun `silence state has valid UI text`() {
        val silence = SilenceState.create("energy_low", System.currentTimeMillis())
        assertTrue(silence.durationMinutes in 15..120)
        assertTrue(silence.uiText.isNotEmpty())
    }

    @Test
    fun `silence trigger description`() {
        assertEquals("他说不动话了", SilenceState.triggerDescription("energy_low"))
        assertEquals("他现在不太想说话", SilenceState.triggerDescription("mood_critical"))
        assertEquals("他自己想待一会儿", SilenceState.triggerDescription("self_initiated"))
    }

    // ========== 自主行为奖励 ==========

    @Test
    fun `autonomous behavior bonus adds 8 energy`() {
        val result = StateEngine.autonomousBehaviorBonus(50)
        assertEquals(58, result)
    }

    @Test
    fun `autonomous behavior bonus capped at 100`() {
        val result = StateEngine.autonomousBehaviorBonus(95)
        assertEquals(100, result)
    }
}
