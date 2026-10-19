package com.mist.medicalmate.core.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NoiseGateTest {
    @Test
    fun `처음 두 조각은 모으기만 한다`() {
        // 말머리를 살리려고 0.2초 늦게 내보낸다.
        val gate = NoiseGate()

        assertNull(gate.push(frame(50)))
        assertNull(gate.push(frame(50)))
    }

    @Test
    fun `주변 소음 크기의 소리는 줄인다`() {
        val gate = NoiseGate()
        val out = List(20) { gate.push(frame(80)) }.filterNotNull()

        assertTrue(out.last().all { abs(it.toInt()) <= 4 })
    }

    @Test
    fun `소음보다 큰 말은 그대로 넘긴다`() {
        val gate = NoiseGate()
        repeat(10) { gate.push(frame(80)) }
        val out = List(5) { gate.push(frame(2_000)) }.filterNotNull()

        assertEquals(2_000, out.last().first().toInt())
    }

    @Test
    fun `말이 시작되기 전 조각도 함께 연다`() {
        // 늦게 내보내는 동안 뒤에 큰 소리가 오면 앞 조각을 연다. 말머리의 작은 소리가 잘리지 않는다.
        val gate = NoiseGate()
        repeat(10) { gate.push(frame(80)) }
        gate.push(frame(80))
        // 큰 소리가 들어온 순간 내보내는 것은 그보다 0.2초 앞의 조용한 조각이다.
        val beforeSpeech = gate.push(frame(2_000))

        assertEquals(80, requireNotNull(beforeSpeech).first().toInt())
    }

    @Test
    fun `말이 끝나도 잠깐은 열어 둔다`() {
        // 말끝과 낱말 사이 쉼이 잘리지 않게 한다.
        val gate = NoiseGate()
        repeat(10) { gate.push(frame(80)) }
        repeat(3) { gate.push(frame(2_000)) }
        val afterSpeech = List(LOOKAHEAD_FRAMES + 1) { gate.push(frame(80)) }.last()

        assertEquals(80, requireNotNull(afterSpeech).first().toInt())
    }

    @Test
    fun `소음이 커지면 문턱도 따라 올라간다`() {
        // 시끄러운 곳에 오래 있으면 그 소음은 말로 보지 않는다.
        val gate = NoiseGate()
        repeat(10) { gate.push(frame(80)) }
        repeat(200) { gate.push(frame(600)) }
        val out = gate.push(frame(600))

        assertTrue(requireNotNull(out).all { abs(it.toInt()) <= 30 })
    }

    @Test
    fun `크기는 RMS다`() {
        assertEquals(100.0, rms(frame(100)), 0.001)
        assertEquals(0.0, rms(ShortArray(0)), 0.001)
    }

    /** 같은 값으로 채운 0.1초 조각. */
    private fun frame(value: Int) = ShortArray(1_600) { value.toShort() }
}
