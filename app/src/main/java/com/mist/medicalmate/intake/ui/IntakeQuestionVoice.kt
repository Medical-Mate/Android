package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.voice.VoiceWords

/*
 * 증상 정리의 마지막 단계(1i)와 정리 완료(1c-5)에서 한 말의 풀이(#284). 부위·강도 풀이는
 * IntakeVoice.kt에 있다.
 */

/** 4단계(1i)에서 한 말. */
internal sealed interface QuestionVoice {
    data class Add(val text: String) : QuestionVoice

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
        editVerb != null && heard.contains(QUESTION) ->
            QuestionVoice.Edit(ordinal, trimmed.substring(editVerb.range.last + 1).clean())

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

/** 묻는 말의 끝. "되나요", "할까요", "괜찮은지", "뭔가요". */
private val ASKING = Regex("""(?:나요|까요|가요|는지|은지|인지|래요|죠|니까)\??$|\?$""")

/** 앞뒤의 공백과 문장부호를 뗀다. */
private fun String.clean(): String = trim { it.isWhitespace() || it in TRIM }

private const val TRIM = ".,!?~"
