package com.mist.medicalmate.core.speech

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 주변 소음 크기 아래의 소리를 줄여서 받아쓰기에 넘긴다(#328).
 *
 * 0.1초씩 받는다. 받은 조각을 바로 내보내지 않고 [LOOKAHEAD_FRAMES]만큼 늦게 내보낸다 — 말이 시작되는 첫
 * 소리는 아직 작아서, 그 조각만 보고 판정하면 말머리가 잘린다. 늦게 내보내는 동안 뒤에 큰 소리가 오면 앞 조각도
 * 연다. 받아쓰기는 실시간 속도로만 받으므로(문서) 모아 두었다 한꺼번에 보내지 않고 늘 같은 간격으로 하나씩 낸다.
 *
 * **문턱은 주변 소음에서 정한다.** 고정값으로 두면 조용한 방에서는 작은 말이 잘리고 시끄러운 곳에서는 소음이 다
 * 지나간다. 지금까지 들은 가장 조용한 크기를 소음 바닥으로 삼고, 소음이 커지면 바닥도 천천히 따라 올라간다.
 * 그 [OPEN_RATIO]배를 넘어야 말로 본다.
 *
 * **닫힌 소리는 지우지 않고 줄인다.** 완전한 무음은 인식기가 끊긴 입력으로 읽을 수 있어 [CLOSED_GAIN]만큼만
 * 남긴다. 말이 끝나도 [HOLD_FRAMES] 동안은 열어 둔다 — 말끝과 낱말 사이 쉼이 잘리지 않게 한다.
 *
 * Android에 기대지 않아 JVM에서 시험한다.
 */
internal class NoiseGate {
    private val pending = ArrayDeque<ShortArray>()
    private val loud = ArrayDeque<Boolean>()
    private var floor = INITIAL_FLOOR
    private var hold = 0

    /** 지금 소음 바닥. debug 로그에 남긴다. */
    val noiseFloor: Double get() = floor

    /** 지금까지 내보낸 조각과 그중 연 조각의 수. debug 로그에 남긴다. */
    var emitted = 0
        private set
    var opened = 0
        private set

    /**
     * 0.1초 조각 하나를 받아, 늦게 내보낼 조각을 돌려준다. 처음 [LOOKAHEAD_FRAMES]번은 모으기만 해서 null이다.
     */
    fun push(frame: ShortArray): ShortArray? {
        val level = rms(frame)
        val isLoud = level >= threshold()
        trackFloor(level)
        pending.addLast(frame)
        loud.addLast(isLoud)
        if (pending.size <= LOOKAHEAD_FRAMES) return null

        val out = pending.removeFirst()
        // 내보낼 조각부터 앞으로 올 조각까지 하나라도 크면 연다. 말머리를 살리는 자리다.
        val speech = loud.removeFirst() || loud.any { it }
        hold = if (speech) HOLD_FRAMES else max(hold - 1, 0)
        val open = speech || hold > 0
        emitted++
        if (open) opened++
        return if (open) out else out.scaled(CLOSED_GAIN)
    }

    private fun threshold(): Double = max(floor * OPEN_RATIO, MIN_THRESHOLD)

    /** 더 조용하면 바로 내려가고, 더 크면 아주 조금씩 올라간다. 말 한마디에 바닥이 들리지 않는다. */
    private fun trackFloor(level: Double) {
        floor = if (level < floor) level.coerceAtLeast(MIN_FLOOR) else min(floor * FLOOR_RISE, level)
    }
}

/** 조각의 크기(RMS). 16비트 PCM 값 그대로의 단위다. */
internal fun rms(frame: ShortArray): Double {
    if (frame.isEmpty()) return 0.0
    var sum = 0.0
    frame.forEach { sum += it.toDouble() * it }
    return sqrt(sum / frame.size)
}

private fun ShortArray.scaled(gain: Double): ShortArray = ShortArray(size) { (this[it] * gain).roundToInt().toShort() }

/** 0.2초. 말머리를 살리려고 늦게 내보내는 양이다. 이만큼 받아쓰기가 늦어진다. */
internal const val LOOKAHEAD_FRAMES = 2

/** 0.4초. 말이 끝난 뒤에도 열어 두는 양이다. 낱말 사이 쉼이 잘리지 않을 만큼이다. */
internal const val HOLD_FRAMES = 4

/** 소음 바닥의 몇 배를 넘어야 말로 보는지. 약 8dB이다. */
internal const val OPEN_RATIO = 2.5

/** 닫힌 소리를 남기는 비율. 약 -26dB이다. */
internal const val CLOSED_GAIN = 0.05

/** 소음 바닥이 이보다 낮아지지 않는다. 아주 조용한 방에서 문턱이 0에 붙어 숨소리까지 여는 것을 막는다. */
private const val MIN_FLOOR = 30.0

/** 문턱이 이보다 낮아지지 않는다. 약 -50dBFS다. */
private const val MIN_THRESHOLD = 100.0

/** 처음 소음 바닥. 첫 조각부터 말을 해도 문턱이 너무 높지 않게 낮게 잡고 조용한 순간을 만나면 내려간다. */
private const val INITIAL_FLOOR = 150.0

/** 더 큰 소리가 이어질 때 소음 바닥이 0.1초마다 오르는 비율. 4초 남짓에 두 배가 된다. */
private const val FLOOR_RISE = 1.02
