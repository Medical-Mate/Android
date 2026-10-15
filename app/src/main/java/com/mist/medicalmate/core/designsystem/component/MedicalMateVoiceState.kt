package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.StringRes
import com.mist.medicalmate.R

/**
 * 받아쓰기의 상태(#318). `Voice Input · Mate`([MedicalMateVoiceInputMate])가 제목과 설명을 고른다.
 *
 * 예전 `Voice Input`(마이크 88 원)은 Archive로 옮겨져 걷어냈다. 상태와 문구만 남는다. 마스터 `Voice Input · Mate`는
 * `Listening` 하나라 나머지 셋의 문구는 예전 마스터(`313:993`)에서 읽은 것이다.
 */
enum class MedicalMateVoiceState(@StringRes val titleRes: Int, @StringRes val descriptionRes: Int) {
    IDLE(R.string.voice_idle_title, R.string.voice_idle_description),
    LISTENING(R.string.voice_listening_title, R.string.voice_listening_description),
    PROCESSING(R.string.voice_processing_title, R.string.voice_processing_description),
    DENIED(R.string.voice_denied_title, R.string.voice_denied_description),
}
