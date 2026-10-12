package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 병원 찾기(1m · 1m-B)에서 알아듣는 말(#282).
 *
 * 목록에서 고르는 것과 다시 찾는 것 둘이다. **모든 말을 검색어로 받지 않는다.** 그러면 "뒤로"도
 * 병원 이름으로 찾게 되어 전역 명령이 막힌다. 찾으라는 말이 있거나 병원 이름처럼 끝나는 말만
 * 검색어로 받는다.
 */
internal sealed interface HospitalPickVoice {
    /** 몇 번째 줄. 0부터 센다. [VoiceWords.LAST]면 마지막 줄이다. */
    data class Pick(val ordinal: Int) : HospitalPickVoice

    /** 골라 둔 병원으로 마친다. "이걸로", "완료". */
    data object Submit : HospitalPickVoice

    data class Search(val query: String) : HospitalPickVoice
}

internal fun parseHospitalPickVoice(text: String): HospitalPickVoice? {
    val ordinal = VoiceWords.ordinal(text)
    val compact = VoiceWords.compact(text)
    val asked = SEARCH_WORDS.containsMatchIn(text)
    val query = text.replace(SEARCH_WORDS, "").trim { it in TRIM }
    return when {
        ordinal != null -> HospitalPickVoice.Pick(ordinal)
        SUBMIT.any { it in compact } -> HospitalPickVoice.Submit
        query.isEmpty() -> null
        asked || HOSPITAL_ENDING.containsMatchIn(query) -> HospitalPickVoice.Search(query)
        else -> null
    }
}

private const val TRIM = " .,!?"

private val SUBMIT = listOf("이걸로", "이거로", "완료", "선택", "골라")

/** 찾으라는 말과 그 앞의 조사. "고려튼튼을 찾아 줘" → "고려튼튼". */
private val SEARCH_WORDS = Regex("""\s*(?:을|를|으로|로)?\s*(?:찾아\s*(?:줘|주세요|봐)?|검색\s*(?:해\s*(?:줘|주세요)?)?)\s*""")

/** 병원 이름의 끝. 예약 문자에서 병원을 찾는 규칙(#270)의 일부다. */
private val HOSPITAL_ENDING = Regex("""(?:병원|의원|치과|한의원|클리닉|센터|의학과|내과|외과|안과|피부과|이비인후과|소아과|산부인과)\s*$""")
