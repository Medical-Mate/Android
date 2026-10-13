package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceText
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

/**
 * 차례가 곧 우선순위다. 몇 번째가 먼저다. 다음이 다시 찾으라는 말이다. "이거 말고 서울대병원"은
 * "이거"가 있어도 고르는 말이 아니라 다시 찾는 말이다. 그다음이 골라 둔 것으로 마치라는 말이고,
 * 마지막이 병원 이름처럼 끝나는 말이다.
 */
internal fun parseHospitalPickVoice(text: String): HospitalPickVoice? {
    val ordinal = VoiceWords.ordinal(text)
    val compact = VoiceWords.compact(text)
    val asked = SEARCH_WORDS.containsMatchIn(text) || compact.contains("말고")
    val query = text.substringAfter("말고").replace(SEARCH_WORDS, " ").trim { it in TRIM }
    return when {
        ordinal != null -> HospitalPickVoice.Pick(ordinal)
        asked -> query.takeIf { it.isNotEmpty() }?.let(HospitalPickVoice::Search)
        SUBMIT.any { compact.startsWith(it) || it in compact } -> HospitalPickVoice.Submit
        query.isNotEmpty() && HOSPITAL_ENDING.containsMatchIn(query) -> HospitalPickVoice.Search(query)
        else -> null
    }
}

private const val TRIM = " .,!?~"

private val SUBMIT =
    listOf("이걸로", "이거로", "이거요", "이거", "이곳", "여기로", "여기요", "거기로", "그걸로", "그거로", "이병원", "완료", "선택", "골라", "맞아")

/** 찾으라는 말과 그 앞의 조사. "고려튼튼을 찾아 줘" → "고려튼튼". */
private val SEARCH_WORDS =
    Regex(
        """\s*(?:을|를|으로|로|은|는)?\s*(?:다시\s*)?(?:찾아\s*(?:줘|주세요|봐|볼래)?|찾을래|찾고\s*싶어|""" +
            """검색\s*(?:해\s*(?:줘|주세요|볼래)?)?|알아봐\s*(?:줘)?|보여\s*(?:줘|주세요))\s*""",
    )

/** 병원 이름의 끝. 예약 문자에서 병원을 찾는 규칙(#270)의 일부다. */
private val HOSPITAL_ENDING =
    Regex("""(?:병원|의원|치과|한의원|클리닉|센터|의학과|내과|외과|안과|피부과|이비인후과|소아과|소아청소년과|산부인과|비뇨기과|정신과)\s*$""")

/**
 * 말로 찾은 병원을 어떻게 할지(#282). 결과가 오면 한 번 정한다.
 *
 * 사람은 "고려튼튼통증의학과"라고 말하고 심평원에는 "고려튼튼통증의학과의원"으로 올라 있다. 그래서
 * 이름이 같다는 것은 띄어쓰기와 끝의 "의원"·"병원"을 빼고 같은 것이다([sameHospitalName]).
 *
 * 서버가 부분 일치로 찾아서 "서울병원"이면 "서울OO병원"까지 수십 곳이 온다. 결과 수만 보면 늘
 * 여럿이 된다. 그래서 차례가 이렇다.
 * - 이름이 같은 곳이 하나면 그곳이다
 * - 이름이 같은 곳이 여럿이면 그곳들만 놓고 몇 번째인지나 동네를 묻는다
 * - 이름이 같은 곳이 없는데 결과가 하나면 그곳이다
 * - 없으면 이름을 다시 묻고, 여럿이면 몇 번째인지나 이름을 더 묻는다
 */
sealed interface HospitalVoiceDecision {
    data class Pick(val hospital: Hospital) : HospitalVoiceDecision

    /** @param candidates 이어서 동네나 지점 이름으로 좁힐 곳들. */
    data class Ask(val reason: HospitalAskReason, val candidates: List<Hospital>) : HospitalVoiceDecision
}

enum class HospitalAskReason {
    /** 이름이 같은 곳이 여럿이다. */
    SAME_NAME,

    /** 이름이 같은 곳은 없고 비슷한 곳이 여럿이다. */
    MANY,

    /** 하나도 없다. */
    NONE,
}

internal fun decideVoicePick(spoken: String, results: List<Hospital>): HospitalVoiceDecision {
    val same = results.filter { sameHospitalName(spoken, it.name) }
    return when {
        same.size == 1 -> HospitalVoiceDecision.Pick(same.single())
        same.size > 1 -> HospitalVoiceDecision.Ask(HospitalAskReason.SAME_NAME, same)
        results.size == 1 -> HospitalVoiceDecision.Pick(results.single())
        results.isEmpty() -> HospitalVoiceDecision.Ask(HospitalAskReason.NONE, emptyList())
        else -> HospitalVoiceDecision.Ask(HospitalAskReason.MANY, results)
    }
}

/** 되물을 말. 무엇을 찾았는지가 윗줄, 무엇을 말하면 되는지가 아랫줄이다. */
internal fun HospitalVoiceDecision.Ask.toPrompt(): VoicePrompt = when (reason) {
    HospitalAskReason.SAME_NAME ->
        VoicePrompt(
            message = VoiceText(R.string.voice_hospital_found_same, listOf(candidates.size.toString())),
            question = VoiceText(R.string.voice_hospital_ask_which),
        )

    HospitalAskReason.MANY ->
        VoicePrompt(
            message = VoiceText(R.string.voice_hospital_found_many, listOf(candidates.size.toString())),
            question = VoiceText(R.string.voice_hospital_ask_which_or_name),
        )

    HospitalAskReason.NONE ->
        VoicePrompt(
            message = VoiceText(R.string.voice_hospital_not_found),
            question = VoiceText(R.string.voice_hospital_ask_name),
        )
}

/** 띄어쓰기와 끝의 "의원"·"병원"을 빼고 같은 이름인지. "고려튼튼 통증의학과" = "고려튼튼통증의학과의원". */
internal fun sameHospitalName(spoken: String, name: String): Boolean {
    val a = VoiceWords.compact(spoken).removeSuffix("의원")
    val b = VoiceWords.compact(name).removeSuffix("의원")
    return a.isNotEmpty() && (a == b || a.removeSuffix("병원") == b.removeSuffix("병원"))
}

/**
 * 동네나 지점 이름으로 좁힌다. "강남에 있는 거", "역삼동", "강남점", "서초구 쪽".
 *
 * 말의 낱말을 주소와 이름에서 찾는다. **모든 후보에 다 있는 낱말은 버린다.** 같은 이름의 지점을 가리는
 * 중이라 이름 낱말은 셋 다 맞아 가려 주지 못한다. 가장 많이 맞은 곳들을 돌려준다. 하나면 고르고,
 * 여럿이면 다시 좁혀 묻는다.
 */
internal fun List<Hospital>.byArea(text: String): List<Hospital> {
    val words =
        text.split(' ', ',', '.')
            .map { it.replace(AREA_TAIL, "") }
            .map(VoiceWords::compact)
            .filter { it.length >= 2 && it !in AREA_STOP }
    val places = associateWith { VoiceWords.compact(it.name + (it.address ?: "")) }
    val telling = words.filter { word -> places.values.count { word in it } in 1 until size }
    if (telling.isEmpty()) return emptyList()
    val scored = map { hospital -> hospital to telling.count { it in places.getValue(hospital) } }
    val best = scored.maxOf { (_, hits) -> hits }
    return if (best == 0) emptyList() else scored.filter { (_, hits) -> hits == best }.map { it.first }
}

/** 동네를 말하는 것 같은지. 못 찾았을 때 "그 동네엔 없어요"라고 할지, 전역 명령으로 넘길지 가른다. */
internal fun looksLikeArea(text: String): Boolean = AREA_HINT.containsMatchIn(text)

/** 낱말 끝의 조사와 군말. "강남에" → "강남", "역삼동쪽" → "역삼동". */
private val AREA_TAIL = Regex("""(?:에\s*있는|에서|에|쪽|근처|있는|거|것|꺼|의|으로|로)+$""")

private val AREA_STOP = setOf("여기", "거기", "저기", "병원", "의원", "있는", "그거", "이거")

private val AREA_HINT = Regex("""(?:구|동|시|군|읍|면|역|점)(?:\s|$|에|쪽|근처)|쪽|근처|에\s*있는""")
