package com.mist.medicalmate.core.speech

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 음성 인식 대역.
 *
 * 기본은 "쓸 수 없는 기기"다. 대부분의 시험이 음성과 상관없고, 기본을 켜 두면 그 시험들이
 * 받아쓰기까지 도는 것을 모르고 지나간다.
 *
 * [keepOpen]이면 [chunks]를 흘린 뒤 끝나지 않고 멈춰 있는다. 실제 인식이 그렇다 — 멈추라고
 * 하기 전까지 듣는다. 끝나 버리면 "듣는 중"을 볼 수 없다.
 */
internal class FakeSpeechToText(
    private val usable: Boolean = false,
    private val chunks: List<SpeechChunk> = emptyList(),
    private val keepOpen: Boolean = false,
) : SpeechToText {
    var stopped = false

    override suspend fun available(): Boolean = usable

    override fun listen(): Flow<SpeechChunk> = flow {
        chunks.forEach { emit(it) }
        if (keepOpen) awaitCancellation()
    }

    override suspend fun stop() {
        stopped = true
    }
}
