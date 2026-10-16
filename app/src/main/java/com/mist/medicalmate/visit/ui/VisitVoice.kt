package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.core.voice.VoiceWords

/*
 * 진료 후 기록을 말로 할 때의 말 풀이(#288). 병원 확인(1m-12)과 메모(1p)다. 기록 확인(1q-1)은
 * VisitRecordVoice.kt에 있다.
 */

/** 병원 확인(1m-12)의 답. 맞으면 true, 다른 병원이면 false, 둘 다 아니면 null이다. */
internal fun parseClinicConfirm(text: String): Boolean? {
    val heard = VoiceWords.compact(text)
    return when {
        // 그만두자는 말은 다른 병원이 아니다(#320). 전에는 "취소"가 "아니요"로 읽혀 다른 병원을 찾았다.
        CLINIC_CANCEL.any { heard.contains(it) } -> null
        OTHER_CLINIC.any { heard.contains(it) } -> false
        heard.contains("맞") || VoiceWords.isYes(text) -> true
        VoiceWords.isNo(text) -> false
        else -> null
    }
}

/** 메모(1p)에서 받는 명령. 그 밖의 말은 메모에 적는다. */
internal enum class NoteVoice {
    /** 다 적었다. 1q-1로 넘어가 AI가 나눈다. */
    DONE,

    /** 방금 적은 것을 지운다. */
    UNDO,

    /** 적은 것을 다 지운다. 되돌릴 수 없어 묻는다. */
    CLEAR,

    /** 앞 화면으로. 전역 "뒤로"에 넘긴다. */
    BACK,
}

/**
 * 메모에서 한 말이 명령인지. **짧은 말일 때만이다.** 메모에는 무엇이든 적힌다. "다음 주에 다시 오래요"가
 * "다음"으로 넘어가거나 "약은 빼고 드세요"가 지우기가 되면 안 된다.
 */
internal fun parseNoteVoice(text: String): NoteVoice? {
    val heard = VoiceWords.compact(text)
    if (heard.length > NOTE_COMMAND_MAX) return null
    return when {
        NOTE_CLEAR.any { heard.contains(it) } -> NoteVoice.CLEAR
        NOTE_UNDO.any { heard.contains(it) } -> NoteVoice.UNDO
        NOTE_DONE.any { heard.startsWith(it) } || heard in NOTE_NEXT -> NoteVoice.DONE
        NOTE_BACK.any { heard.startsWith(it) } -> NoteVoice.BACK
        else -> null
    }
}

private val OTHER_CLINIC = listOf("다른병원", "다른곳", "다른데", "아니에요", "틀려", "아닌데")

private const val NOTE_COMMAND_MAX = 10

/*
 * 다 적었다는 말. "다음"·"넘어가"는 그 말뿐일 때만이다(#320). 앞머리로 보면 "다음 주에 오래요"가 넘어가기가 됐다.
 * "끝났"도 "끝났어"까지 본다 — "끝났대요"는 메모다.
 */
private val NOTE_DONE = listOf("다했", "다적었", "다말했", "정리해", "끝났어", "끝이야", "저장", "다끝났")

private val NOTE_NEXT = setOf("다음", "다음으로", "다음으로가줘", "다음으로넘어가", "넘어가", "넘어가줘", "넘어갈래", "넘어가자")

private val CLINIC_CANCEL = listOf("취소", "잠깐", "나갈", "그만")

private val NOTE_UNDO = listOf("방금거지워", "방금말지워", "방금지워", "마지막거지워", "마지막말지워", "그거지워", "방금거빼")

private val NOTE_CLEAR = listOf("다지워", "전부지워", "모두지워", "처음부터다시", "다시쓸래", "처음부터쓸래")

private val NOTE_BACK = listOf("뒤로", "이전")
