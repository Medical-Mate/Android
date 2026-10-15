package com.mist.medicalmate.core.voice

/**
 * 여러 화면이 함께 쓰는 말 풀이(#282).
 *
 * 받아쓰기가 같은 말을 그때그때 다르게 띄어 적고 끝에 마침표를 붙인다. 비교하기 전에 띄어쓰기와
 * 문장부호를 지운다([compact]).
 *
 * **사람은 정해 둔 말로 말하지 않는다.** "네" 대신 "응 해 줘"·"그렇게 해", "첫 번째" 대신 "첫째"·
 * "맨 위에 거"라고 한다. 한 뜻에 여러 말을 받는다. 다만 일상 말에 흔한 낱말("어", "하나")은 받지
 * 않는다. 엉뚱한 말이 확인이나 선택으로 읽히는 쪽이 못 알아듣는 쪽보다 나쁘다.
 */
object VoiceWords {
    /** 띄어쓰기와 문장부호를 지운다. "일정 추가." → "일정추가" */
    fun compact(text: String): String = text.filterNot { it.isWhitespace() || it in PUNCTUATION }

    /** 확인에 "네"라고 했는지. "네, 저장해 줘", "응 그렇게 해"도 네다. */
    fun isYes(text: String): Boolean {
        val heard = compact(text)
        return !isNo(text) && YES.any { heard.startsWith(it) }
    }

    /** 확인에 "아니요"라고 했는지. "아니 하지 마", "잠깐만"도 아니요다. */
    fun isNo(text: String): Boolean {
        val heard = compact(text)
        return NO.any { heard.startsWith(it) } || NO_ANYWHERE.any { it in heard }
    }

    /**
     * 막대를 닫으라는 짧은 말. "그만", "닫아 줘", "됐어". 긴 문장 속의 "끝"은 아니다.
     *
     * **말 전체가 닫자는 말일 때만이다**(#320). 전에는 "그만"·"끝"으로 시작하는 짧은 말이면 닫아서, 의사에게 물을
     * "그만 먹어도 되나요?", 메모에 적을 "끝났대요", 굴리는 말 "끝까지"가 막대를 닫았다.
     */
    fun isStop(text: String): Boolean = !isAsking(text) && STOP.matches(compact(text))

    /** 묻는 말인지. 물음표로 끝나거나 "…나요", "…까요", "…되죠"처럼 끝난다. */
    fun isAsking(text: String): Boolean = text.trim().endsWith("?") || ASKING_END.containsMatchIn(compact(text))

    /**
     * 막대에 띄운 것을 다시 보여 달라는 말(#284). "다시 보여 줘", "뭐였지", "못 봤어". 목록을 미처 못
     * 보고 말이 먼저 나간 사람이 다시 볼 길이다.
     */
    fun isRepeat(text: String): Boolean {
        val heard = compact(text)
        return heard.length <= REPEAT_MAX_LENGTH && REPEAT.any { it in heard }
    }

    /** 저장하라는 말. "저장해 줘", "등록해", "이대로 완료", "일정 잡아 줘", "다 됐어". */
    fun isSave(text: String): Boolean {
        val heard = compact(text)
        return SAVE.any { it in heard }
    }

    /**
     * 목록에서 몇 번째인지. 0부터 센다. 없으면 null이다.
     *
     * "첫 번째", "두번째 거", "3번", "첫째", "둘째", "맨 위에 거"가 앞에서부터이고, "마지막", "맨 아래",
     * "끝에 거"는 [LAST]다. 받아쓰기가 숫자를 글자로도 아라비아 숫자로도 적는다. 둘 다 받는다.
     */
    fun ordinal(text: String): Int? {
        val heard = compact(text)
        val word = ORDINAL_WORDS.indexOfFirst { heard.contains("${it}번째") }
        val short = SHORT_ORDINALS.indexOfFirst { heard.contains(it) }
        val digit = DIGIT_ORDINAL.find(heard)?.groupValues?.get(1)?.toInt()
        return when {
            word >= 0 -> word
            short >= 0 -> short
            digit != null -> digit - 1
            FIRST.any { it in heard } -> 0
            LAST_WORDS.any { it in heard } -> LAST
            else -> null
        }
    }

    /** 몇 번째를 가리키는 말을 지운다. 남은 말에서 무엇을 하라는지 읽을 때 쓴다. */
    fun stripOrdinal(text: String): String = text.replace(ORDINAL_PHRASE, " ")

    /** [ordinal]이 "마지막"을 이 값으로 돌려준다. 쓰는 쪽이 목록 끝으로 바꾼다. */
    const val LAST = -1

    private const val PUNCTUATION = ".,?!~"

    private val ASKING_END = Regex("""(?:나요|까요|가요|는지|은지|인지|니까|되죠|하죠|있죠)$""")

    private const val REPEAT_MAX_LENGTH = 14

    private val REPEAT =
        listOf("다시보여", "다시알려", "다시말해", "다시읽어", "한번더보여", "한번더알려", "뭐였", "못봤", "목록보여", "안보여")

    /** "어"는 받지 않는다. "어제", "어디"가 확인으로 읽힌다. */
    private val YES =
        listOf(
            "네", "예", "응", "웅", "넵", "그래", "그렇게", "좋아", "좋습니다", "맞아", "맞습니다", "저장",
            "오케이", "ok", "yes", "확인", "부탁", "해줘", "해주세요", "고고", "콜",
        )

    private val NO = listOf("아니", "아뇨", "노", "취소", "싫어", "잠깐", "잠시만", "안해", "하지마", "별로", "다시")

    /** 문장 어디에 있어도 아니요인 말. "음 하지 마", "그건 안 할래". */
    private val NO_ANYWHERE = listOf("하지마", "안할래", "안해도", "하지말")

    private val STOP =
        Regex(
            "(?:이제)?(?:그만|닫아|닫기|끝|끝내|종료|됐어|됐다|됐어요|됐습니다|꺼줘|꺼|멈춰|멈춰줘)" +
                "(?:요|해|해요|할래|할게|하자|해줘|줘|주세요|닫아|닫아줘|이야|이에요)?",
        )

    private val SAVE =
        listOf(
            "저장", "등록해", "등록할래", "완료", "이대로", "마무리", "잡아줘", "잡아주세요", "예약해", "확정",
            "다됐", "다했", "끝내", "이걸로할게", "이렇게할게", "추가해줘",
        )

    private val ORDINAL_WORDS = listOf("첫", "두", "세", "네", "다섯", "여섯", "일곱", "여덟", "아홉", "열")

    private val SHORT_ORDINALS = listOf("첫째", "둘째", "셋째", "넷째", "다섯째", "여섯째", "일곱째", "여덟째", "아홉째", "열째")

    private val FIRST = listOf("맨위", "맨처음", "제일위", "제일처음", "처음거", "첫거", "위에거", "첫번")

    private val LAST_WORDS = listOf("마지막", "맨아래", "맨밑", "제일아래", "제일밑", "끝에거", "아래거", "밑에거")

    private val DIGIT_ORDINAL = Regex("""(\d{1,2})(?:번째|번)""")

    private val ORDINAL_PHRASE =
        Regex(
            """(?:첫|두|세|네|다섯|여섯|일곱|여덟|아홉|열)\s*번\s*째|\d{1,2}\s*번\s*째?|""" +
                """(?:첫|둘|셋|넷|다섯|여섯|일곱|여덟|아홉|열)째|""" +
                """(?:맨|제일)\s*(?:위|아래|밑|처음|끝)(?:에)?(?:\s*(?:거|것|꺼))?|마지막(?:\s*(?:거|것|꺼))?""",
        )
}
