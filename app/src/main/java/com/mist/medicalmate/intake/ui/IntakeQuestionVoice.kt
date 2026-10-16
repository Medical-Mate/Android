package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.voice.VoiceWords

/*
 * 증상 정리의 강도(1d)·추천 질문 고르기(1c-D)와 정리 완료(1c-5)에서 한 말의 풀이(#284). 부위 풀이는
 * BodyVoicePick.kt에 있다.
 */

/**
 * 말로 고른 아픈 정도(1d). 숫자와 낱말을 모두 받는다.
 *
 * - "7점", "10점 중에 7점" → 0~10점(NRS)을 다섯 단계로. 카드가 NRS로도 적는 값이라 사람이 점수로
 *   말할 수 있다.
 * - "4단계" → 그 단계.
 * - "많이 아파요", "견디기 힘들어요", "조금 불편해요" → 화면의 다섯 낱말과 그 비슷한 말.
 *
 * 낱말은 센 것부터 본다. "많이"가 "조금 많이"에도 들어 있다.
 */
internal fun parseSeverityVoice(text: String): MedicalMateSeverity? {
    // 받아쓰기가 수를 우리말로 적기도 한다("칠 점", "다섯 단계", "세 번째"). 숫자로 바꿔 읽는다(#320).
    val heard = koreanNumbers(VoiceWords.compact(text))
    val score = NRS.findAll(heard).lastOrNull()?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..NRS_MAX }
    // 막대가 읽어 준 다섯 줄을 "세 번째", "마지막"으로 고른다. 위에서부터 1단계다.
    val ordinal = VoiceWords.ordinal(text)?.let { if (it == VoiceWords.LAST) LEVELS else it + 1 }
    val step = STEP.find(heard)?.groupValues?.get(1)?.toIntOrNull() ?: ordinal?.takeIf { it in 1..LEVELS }
    return when {
        // 0점은 단계 밖이라 가장 낮은 단계로 둔다.
        score != null ->
            MedicalMateSeverity.entries.firstOrNull { score in it.nrsFirst..it.nrsLast } ?: MedicalMateSeverity.LEVEL_1

        step != null -> MedicalMateSeverity.entries.firstOrNull { it.level == step }
        else -> SEVERITY_WORDS.firstOrNull { (_, words) -> words.any { heard.contains(it) } }?.first
    }
}

/** 추천 질문 고르기(1c-D)에서 한 말. */
internal sealed interface QuestionVoice {
    data class Add(val text: String) : QuestionVoice

    /** "첫 번째 질문 담아 줘". 그 줄을 체크한다. @param ordinal 0부터 센다. [VoiceWords.LAST]면 마지막이다. */
    data class Pick(val ordinal: Int) : QuestionVoice

    /** "다 담아 줘". 위에서부터 상한까지 체크한다. */
    data object PickAll : QuestionVoice

    /** @param ordinal 0부터 센다. [VoiceWords.LAST]면 마지막이다. null이면 [words]로 찾는다. */
    data class Remove(val ordinal: Int?, val words: String) : QuestionVoice

    data class Edit(val ordinal: Int?, val text: String) : QuestionVoice
}

/**
 * 질문 단계의 말을 읽는다.
 *
 * 지우기·고치기는 그 말이 있을 때만이다. 더하기는 "질문 추가, …"처럼 말할 때와, 묻는 말
 * ("약 계속 먹어도 되나요")일 때다. 사람은 질문을 더하라고 하기보다 그냥 질문을 말한다. 그 밖의
 * 말은 null이고 전역 명령으로 넘긴다.
 */
internal fun parseQuestionVoice(text: String): QuestionVoice? {
    val trimmed = text.clean()
    val heard = VoiceWords.compact(trimmed)
    val ordinal = VoiceWords.ordinal(trimmed)
    val addVerb = ADD_PREFIX.find(trimmed)
    val editVerb = EDIT.find(trimmed)
    return when {
        addVerb != null -> QuestionVoice.Add(trimmed.substring(addVerb.range.last + 1).clean())
        editVerb != null && heard.contains(QUESTION) -> QuestionVoice.Edit(ordinal, editedText(trimmed, editVerb))
        PICK_ALL.containsMatchIn(heard) -> QuestionVoice.PickAll
        // "두 번째 체크 풀어", "세 번째 빼 줘". 몇 번째를 말했으면 "질문"이라는 말이 없어도 그 줄이다(#320). 체크를
        // 푸는 말이 담는 말("체크")보다 먼저다 — 전에는 "체크 풀어"가 담기로 읽혔다.
        ordinal != null && UNPICK.containsMatchIn(trimmed) -> QuestionVoice.Remove(ordinal, "")
        ordinal != null && PICK.containsMatchIn(trimmed) -> QuestionVoice.Pick(ordinal)

        REMOVE.containsMatchIn(trimmed) && heard.contains(QUESTION) ->
            QuestionVoice.Remove(
                ordinal,
                VoiceWords.stripOrdinal(trimmed).replace(REMOVE, " ").replace(QUESTION_NOUN, " ").trim(),
            )

        ASKING.containsMatchIn(trimmed) -> QuestionVoice.Add(trimmed)
        else -> null
    }
}

/** 지울 질문. 몇 번째로 말했으면 그 줄, 아니면 낱말이 들어 있는 줄이다. 여럿이면 고르지 않는다. */
internal fun List<String>.questionIndexByVoice(ordinal: Int?, words: String): List<Int> {
    val keys = words.split(' ').map(VoiceWords::compact).filter { it.length >= 2 }
    return when {
        ordinal != null -> listOfNotNull(
            (
                if (ordinal ==
                    VoiceWords.LAST
                ) {
                    lastIndex
                } else {
                    ordinal
                }
                ).takeIf { it in indices },
        )
        keys.isEmpty() -> emptyList()
        else -> indices.filter { index -> keys.all { VoiceWords.compact(this[index]).contains(it) } }
    }
}

/**
 * 고칠 글. 동사 뒤("두 번째 질문 고쳐, 공복으로 가나요")가 먼저이고, 비었으면 동사 앞("두 번째 질문을 공복으로
 * 가나요로 바꿔 줘")에서 몇 번째와 "질문을"을 떼고 읽는다(#320). 일정 추가의 할 일 고치기와 같은 두 갈래다.
 */
private fun editedText(trimmed: String, verb: MatchResult): String {
    val after = trimmed.substring(verb.range.last + 1).clean()
    if (after.isNotEmpty()) return after
    val before = VoiceWords.stripOrdinal(trimmed.substring(0, verb.range.first))
    return before.replace(QUESTION_NOUN, " ").trim().replace(EDIT_TARGET_END, "").clean()
}

/** 1c-5의 갈림길. */
internal enum class IntakeDoneChoice { CARD, HOSPITAL }

/**
 * 1c-5에서 한 말. 카드가 먼저다. "카드 만들고 병원 찾을래"는 카드를 먼저 만든다 — 카드 화면에서
 * 병원을 고를 수 있다.
 */
internal fun parseDoneVoice(text: String): IntakeDoneChoice? {
    val heard = VoiceWords.compact(text)
    return when {
        DONE_CARD.any { heard.contains(it) } -> IntakeDoneChoice.CARD
        heard.contains("병원") -> IntakeDoneChoice.HOSPITAL
        else -> null
    }
}

private val DONE_CARD = listOf("카드", "브리핑", "만들", "작성", "정리해")

private const val QUESTION = "질문"

private val QUESTION_NOUN = Regex("""질문(?:을|은|는|이|가)?""")

private val ADD_PREFIX = Regex("""^(?:질문\s*(?:하나\s*)?(?:추가|더|넣어)(?:해\s*줘|해\s*주세요|할게|할래)?|물어볼\s*거)[\s,]*""")

private val EDIT = Regex("""(?:고쳐|바꿔|수정)(?:\s*줘|\s*주세요|할래|할게)?[\s,]*""")

private val REMOVE = Regex("""지워|빼\s*줘|빼|없애|삭제""")

/** 추천 줄을 담는 말. 몇 번째와 함께 올 때만 본다 — "넣어"만으로는 무엇을 넣으라는지 모른다. */
private val PICK = Regex("""담아|담을|담고|넣어|넣을|골라|고를|체크|선택|할게|할래""")

/** 담은 줄을 푸는 말. 몇 번째와 함께 올 때만 본다. */
private val UNPICK = Regex("""풀어|풀래|해제|빼|지워|없애|삭제|취소|안\s*담|안\s*할""")

/** 추천을 다 담으라는 말. "다 담아 줘", "전부 넣어", "모두 체크해". */
private val PICK_ALL = Regex("""^(?:추천(?:질문)?)?(?:다|전부|모두|싹|몽땅)(?:담|넣|체크|골라|선택)""")

/** 고칠 글 끝의 "로"·"으로". "…하나요로 바꿔 줘". */
private val EDIT_TARGET_END = Regex("""\s*(?:으로|로)$""")

/** 묻는 말의 끝. "되나요", "할까요", "괜찮은지", "뭔가요". */
private val ASKING = Regex("""(?:나요|까요|가요|는지|은지|인지|래요|죠|니까)\??$|\?$""")

/** 앞뒤의 공백과 문장부호를 뗀다. */
private fun String.clean(): String = trim { it.isWhitespace() || it in TRIM }

private const val TRIM = ".,!?~"

private val NRS = Regex("""(\d{1,2})점""")

private val STEP = Regex("""([1-5])(?:단계|번)""")

private const val NRS_MAX = 10

private const val LEVELS = 5

/** "칠점" → "7점", "다섯단계" → "5단계". 점·단계 바로 앞의 우리말 수만 바꾼다. */
private fun koreanNumbers(heard: String): String = KOREAN_NUMBER.replace(heard) { match ->
    val number = KOREAN_DIGITS[match.groupValues[1]] ?: return@replace match.value
    "$number${match.groupValues[2]}"
}

private val KOREAN_DIGITS =
    mapOf(
        "영" to 0, "빵" to 0, "일" to 1, "하나" to 1, "한" to 1, "이" to 2, "둘" to 2, "두" to 2, "삼" to 3,
        "셋" to 3, "세" to 3, "사" to 4, "넷" to 4, "네" to 4, "오" to 5, "다섯" to 5, "육" to 6, "여섯" to 6,
        "칠" to 7, "일곱" to 7, "팔" to 8, "여덟" to 8, "구" to 9, "아홉" to 9, "십" to 10, "열" to 10,
    )

private val KOREAN_NUMBER = Regex("""(다섯|여섯|일곱|여덟|아홉|하나|영|빵|일|한|이|둘|두|삼|셋|세|사|넷|네|오|육|칠|팔|구|십|열)(점(?!점)|단계)""")

private val SEVERITY_WORDS =
    listOf(
        // 화면의 다섯 줄에 적힌 설명도 받는다(#320). 사람은 읽은 문장으로 답한다 — "잠도 못 자요", "하던 일은 할 수 있어요".
        MedicalMateSeverity.LEVEL_5 to
            listOf(
                "견디기힘들", "참을수없", "못참", "죽겠", "너무아파", "엄청아파", "극심", "최악", "제일아파",
                "잠도못", "잠을못", "못자", "아무것도못",
            ),
        MedicalMateSeverity.LEVEL_4 to
            listOf("많이아파", "많이", "심해", "심하게", "힘들어", "참기힘들", "일상생활이어렵", "생활이힘들"),
        MedicalMateSeverity.LEVEL_3 to listOf("꽤", "보통", "중간", "적당히", "그럭저럭", "멈추게", "멈출때"),
        MedicalMateSeverity.LEVEL_2 to listOf("은근", "좀아파", "자꾸", "계속아파", "집중이안", "집중이잘안", "생각나"),
        MedicalMateSeverity.LEVEL_1 to listOf("조금", "살짝", "약간", "불편", "신경쓰", "할수있", "계속할수"),
    )
