package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.voice.VoiceWords

/*
 * 증상 정리에서 단계를 넘나드는 짧은 말(#284). "다음", "뒤로", 다시 고르기, 모르겠다는 말이다.
 * 문답의 답에 섞여도 걸리지 않게 대부분 짧은 말일 때만 본다.
 */

/** "다음"으로 들리는 말. 짧을 때만 본다. "다음 주에 다시 올게요" 같은 답이 걸리면 안 된다. */
internal fun isNextVoice(text: String): Boolean {
    val heard = VoiceWords.compact(text)
    return heard.length <= NEXT_MAX && NEXT.any { heard.startsWith(it) }
}

/** "뒤로"로 들리는 말. 짧을 때만 본다. 문답의 답에 "뒤로"가 섞일 수 있다. */
internal fun isBackVoice(text: String): Boolean {
    val heard = VoiceWords.compact(text)
    return heard.length <= NEXT_MAX && BACK.any { heard.startsWith(it) }
}

/** 부위를 다시 고르겠다는 말. */
internal fun isResetVoice(text: String): Boolean {
    val heard = VoiceWords.compact(text)
    return heard.length <= RESET_MAX && RESET.any { heard.contains(it) }
}

/** 어디인지 모르겠다는 말. 고를 수 있는 곳을 다시 읽어 준다. */
internal fun isUnsureVoice(text: String): Boolean {
    val heard = VoiceWords.compact(text)
    return UNSURE.any { heard.contains(it) }
}

private val UNSURE = listOf("모르겠", "잘몰라", "몰라요", "모르겠어", "뭐가있", "어디가있", "뭐있", "다시말해", "다시알려")

private val NEXT = listOf("다음", "넘어가", "넘겨", "다됐", "다했", "없어", "없습니다", "됐어요", "계속")

private val BACK = listOf("뒤로", "이전", "전으로", "돌아가")

private val RESET = listOf("다시고를", "다시선택", "처음부터", "다른부위", "다른데", "취소")

private const val NEXT_MAX = 8

private const val RESET_MAX = 12
