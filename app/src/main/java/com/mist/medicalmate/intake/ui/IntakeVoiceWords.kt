package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.voice.VoiceWords

/*
 * 증상 정리에서 단계를 넘나드는 짧은 말(#284). "다음", "뒤로", 다시 고르기, 모르겠다는 말이다.
 * 문답의 답에 섞여도 걸리지 않게 대부분 짧은 말일 때만 본다.
 */

/**
 * "다음"으로 들리는 말.
 *
 * **말 전체가 넘어가자는 말일 때만이다**(#320). 전에는 "다음"·"계속"·"없어"로 시작하는 짧은 말이면 다 받아서
 * "계속 먹어도 되나요?", "다음에 와야 하나요", "없어도 되나요" 같은 질문이 정리를 끝내 버렸다. 이제 넘어가자는
 * 말("다음", "다음으로 가 줘", "넘어가자", "다 됐어요", "없어요", "계속할게")에 붙는 끝말만 받는다.
 */
internal fun isNextVoice(text: String): Boolean = !VoiceWords.isAsking(text) && NEXT.matches(VoiceWords.compact(text))

/**
 * "뒤로"로 들리는 말. 말 전체가 돌아가자는 말일 때만이다(#320). "이전에 그랬어", "뒤로 숙이면 아파"는 답이다.
 */
internal fun isBackVoice(text: String): Boolean = BACK.matches(VoiceWords.compact(text))

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

/** 반대쪽으로 옮기라는 말. "반대", "반대편", "다른 쪽". */
internal fun isOppositeVoice(text: String): Boolean {
    val heard = VoiceWords.compact(text)
    return heard.length <= OPPOSITE_MAX && OPPOSITE.any { heard.contains(it) }
}

private val OPPOSITE = listOf("반대", "다른쪽", "다른편", "건너편", "맞은편")

private const val OPPOSITE_MAX = 14

private val UNSURE = listOf("모르겠", "잘몰라", "몰라요", "모르겠어", "뭐가있", "어디가있", "뭐있", "다시말해", "다시알려")

private val NEXT =
    Regex(
        "(?:다음|다음으로|다음단계|다음단계로|다음거|넘어가|넘겨|넘어가자|다됐|다했|이제없|없|계속|계속진행|계속해)" +
            "(?:요|이요|어|어요|습니다|자|기|줘|가|가자|가줘|가주세요|가요|넘어가|넘어가줘|으로가줘|해|해줘|해주세요|할게|할게요|할래|진행|진행해줘)?",
    )

private val BACK =
    Regex(
        "(?:뒤로|이전|이전으로|이전단계|이전단계로|전으로|전단계|전단계로|돌아가)" +
            "(?:요|가|가자|가줘|가주세요|가기|갈래|갈게|돌아가|돌아가줘|돌아갈래|줘|자)?",
    )

private val RESET = listOf("다시고를", "다시선택", "처음부터", "다른부위", "다른데", "취소")

private const val RESET_MAX = 12
