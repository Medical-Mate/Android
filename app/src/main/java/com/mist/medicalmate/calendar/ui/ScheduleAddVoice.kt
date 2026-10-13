package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 일정 추가(1r-4)에서 알아듣는 말(#282).
 *
 * 화면의 칸과 버튼을 모두 말로 부른다. 날짜·시간·병원은 예약 문자 공유(#270)의 읽기를 그대로
 * 써서, "10월 20일 오후 3시 고려튼튼통증의학과"처럼 한 번에 말해도 칸이 채워진다.
 *
 * 몇 번째인지(`ordinal`)는 0부터 센다. [VoiceWords.LAST]면 마지막이다. 없으면 [ScheduleAddVoice.words]의
 * 낱말로 고른다.
 */
internal sealed interface ScheduleAddVoice {
    /** 저장. 확인을 받고 한다. */
    data object Save : ScheduleAddVoice

    /** 화면을 닫는다. 적은 것이 있으면 확인을 받는다. */
    data object Exit : ScheduleAddVoice

    /** 지금 병원 이름으로 병원 찾기를 연다. "병원 바꿀래". */
    data object OpenHospital : ScheduleAddVoice

    /** 날짜나 시간을 고르는 시트를 연다. "날짜 고를래". */
    data class OpenSheet(val sheet: ScheduleAddSheet) : ScheduleAddVoice

    /** 시간을 비운다. 시간 미정으로 저장된다. */
    data object ClearTime : ScheduleAddVoice

    /** 할 일 한 줄을 더한다. 글이 비면 무엇을 적을지 묻는다. */
    data class TodoAdd(val label: String) : ScheduleAddVoice

    data class TodoCheck(val ordinal: Int?, val words: String, val done: Boolean) : ScheduleAddVoice

    data class TodoDelete(val ordinal: Int?, val words: String) : ScheduleAddVoice

    data class TodoEdit(val ordinal: Int?, val label: String) : ScheduleAddVoice

    /** 가져갈 카드로 고른다. "두 번째 카드", "두통 카드". */
    data class CardPick(val ordinal: Int?, val words: String) : ScheduleAddVoice

    /** 가져갈 카드에서 뺀다. "두통 카드 빼 줘". */
    data class CardDrop(val ordinal: Int?, val words: String) : ScheduleAddVoice

    /** 새 브리핑 카드를 만들러 간다. */
    data object CardNew : ScheduleAddVoice

    /** 날짜·시간·병원일 수 있는 말. 읽어 봐서 없으면 이 화면의 말이 아니다. */
    data class Fill(val text: String) : ScheduleAddVoice
}

/**
 * 받아쓴 말을 일정 추가의 할 일로.
 *
 * 차례가 곧 우선순위다. 할 일과 카드는 그 낱말이 있을 때만이고 먼저 본다("할 일, 10시 전에
 * 금식"의 10시가 진료 시각이 되면 안 된다). 화면 조작(나가기·시트·병원 찾기·시간 비우기)은
 * 그 말로 시작할 때만이다. 날짜나 시각이 들어 있으면 시트를 여는 말보다 채우는 말이 먼저다
 * ("날짜 20일로 바꿔 줘"). 저장도 날짜가 함께 없을 때만이다. 나머지는 날짜·시간·병원으로 읽어 본다.
 *
 * **같은 일을 여러 말로 한다.** 할 일은 "할 일"·"준비물"·"챙길 거"·"체크리스트"로, 카드는
 * "카드"·"브리핑"으로 가리킨다. 각 동작의 말도 여럿이다. 목록은 이 파일 아래에 있다.
 */
internal fun parseScheduleAddVoice(text: String): ScheduleAddVoice? = when {
    text.isBlank() -> null
    TODO_WORD.containsMatchIn(text) -> parseTodo(text)
    CARD_WORD.containsMatchIn(text) -> parseCard(text)
    else -> parseScreen(text)
}

/**
 * 할 일. 더하라는 말("추가"·"넣어")이 있으면 더하기가 먼저다. 그래야 "할 일 추가, 약 빼먹지
 * 않기"가 "빼"로 지우기가 되지 않는다. 그다음이 지우기·고치기·체크이고, 어느 말도 없는데 할 일을
 * 가리키는 말로 시작하면 더하기다("할 일, 10시 전에 금식").
 *
 * 더할 글은 할 일을 가리키는 말과 더하라는 말을 걷어낸 나머지다. "준비물에 신분증 넣어 줘" →
 * "신분증". 글이 비면("할 일 추가") [ScheduleAddVoice.TodoAdd]의 글이 빈 채로 가고, 무엇을 적을지
 * 묻는다. 고치라는 말만 있고 새 글이 없으면 [ScheduleAddVoice.TodoEdit]의 글이 비고, 역시 묻는다.
 */
private fun parseTodo(text: String): ScheduleAddVoice? {
    val ordinal = VoiceWords.ordinal(text)
    val compact = VoiceWords.compact(text)
    val label = text.replace(TODO_WORD_WITH_PARTICLE, " ").replace(ADD_VERB, " ").trim { it in TRIM }
    return when {
        ADD_VERB.containsMatchIn(text) -> ScheduleAddVoice.TodoAdd(label)
        TODO_DELETE.any { it in compact } -> ScheduleAddVoice.TodoDelete(ordinal, text)
        EDIT_VERB.containsMatchIn(text) -> ScheduleAddVoice.TodoEdit(ordinal, editedLabel(text).orEmpty())
        TODO_UNDONE.any { it in compact } -> ScheduleAddVoice.TodoCheck(ordinal, text, done = false)
        TODO_DONE.any { it in compact } -> ScheduleAddVoice.TodoCheck(ordinal, text, done = true)
        TODO_START.containsMatchIn(text) -> ScheduleAddVoice.TodoAdd(label)
        else -> null
    }
}

/**
 * 고칠 할 일의 새 글. 고치라는 말이 없으면 null이다.
 *
 * 새 글이 고치라는 말 뒤에도("고쳐, 약 챙기기"), 앞에도("약 챙기기로 바꿔 줘") 온다. 뒤를 먼저 보고
 * 비었으면 앞을 본다. 앞을 볼 때는 몇 번째와 할 일을 가리키는 말을 걷어낸다.
 */
private fun editedLabel(text: String): String? {
    val verb = EDIT_VERB.find(text) ?: return null
    val after = text.substring(verb.range.last + 1).replace(POLITE_TAIL, "").trim { it in TRIM }
    val before =
        VoiceWords.stripOrdinal(text.substring(0, verb.range.first))
            .replace(TODO_WORD_WITH_PARTICLE, " ")
            .replace(TARGET_PARTICLE, "")
            .trim { it in TRIM }
    return after.ifEmpty { before }.ifEmpty { null }
}

private fun parseCard(text: String): ScheduleAddVoice {
    val ordinal = VoiceWords.ordinal(text)
    val compact = VoiceWords.compact(text)
    return when {
        CARD_NEW.any { it in compact } -> ScheduleAddVoice.CardNew
        CARD_DROP.any { it in compact } -> ScheduleAddVoice.CardDrop(ordinal, text)
        else -> ScheduleAddVoice.CardPick(ordinal, text)
    }
}

/** 화면 조작과 저장. 어느 것도 아니면 날짜·시간·병원으로 읽어 본다. */
private fun parseScreen(text: String): ScheduleAddVoice {
    val dated = DATE_TIME_HINT.containsMatchIn(text)
    return when {
        EXIT.containsMatchIn(text) -> ScheduleAddVoice.Exit
        OPEN_HOSPITAL.containsMatchIn(text) -> ScheduleAddVoice.OpenHospital
        dated -> ScheduleAddVoice.Fill(text)
        CLEAR_TIME.containsMatchIn(text) -> ScheduleAddVoice.ClearTime
        OPEN_DATE.containsMatchIn(text) -> ScheduleAddVoice.OpenSheet(ScheduleAddSheet.DATE)
        OPEN_TIME.containsMatchIn(text) -> ScheduleAddVoice.OpenSheet(ScheduleAddSheet.TIME)
        VoiceWords.isSave(text) -> ScheduleAddVoice.Save
        else -> ScheduleAddVoice.Fill(text)
    }
}

/** 할 일을 가리키는 말. */
private const val TODO_NOUN =
    """할\s*일|해야\s*할\s*(?:것|거|일)|체크\s*리스트|준비물|챙길\s*(?:것|거|꺼)|챙겨야\s*할\s*(?:것|거)|투두"""

private val TODO_WORD = Regex(TODO_NOUN)

private val TODO_START = Regex("""^\s*(?:$TODO_NOUN)""")

/** 할 일을 가리키는 말과 그 뒤의 조사. "할 일에", "준비물로", "할 일을". */
private val TODO_WORD_WITH_PARTICLE = Regex("""(?:$TODO_NOUN)\s*(?:에다가|에다|에|으로|로|을|를|은|는|이|가)?""")

/** 더하라는 말. 뒤에 붙는 "해 줘"·"할래"까지 함께 걷어낸다. */
private val ADD_VERB =
    Regex(
        """(?:추가|넣어|넣을래|넣자|더해|더할래|적어|적을래|등록|써\s*줘|써\s*둬|메모)""" +
            """\s*(?:해|하고|할래|할게|하자)?\s*(?:줘|주세요|줄래|둬|놔|놓아)?""",
    )

/** 고치라는 말. */
private val EDIT_VERB = Regex("""고쳐|고칠래|수정|바꿔|바꿀래|변경|바꾸자""")

/** 고치라는 말 뒤의 "줘"·"해 주세요". 새 글이 아니다. */
private val POLITE_TAIL = Regex("""^\s*(?:해)?\s*(?:줘|주세요|줄래|할래|하자)?""")

/** "약 챙기기로", "8시간 공복으로"의 끝 조사. */
private val TARGET_PARTICLE = Regex("""\s*(?:으로|로)\s*$""")

private val TODO_DELETE = listOf("지워", "지울래", "삭제", "빼", "없애", "제거", "날려")

private val TODO_UNDONE = listOf("완료취소", "체크해제", "체크풀", "해제", "안했", "아직", "되돌려", "취소", "못했")

private val TODO_DONE = listOf("완료", "했어", "했다", "했음", "했습니다", "체크", "끝냈", "끝났", "다했", "챙겼", "마쳤", "처리")

/** 카드를 가리키는 말. "두통 브리핑 가져갈게"처럼 "카드"를 빼고 말하기도 한다. */
private val CARD_WORD = Regex("""카드|브리핑""")

private val CARD_NEW = listOf("새카드", "새로만들", "카드만들", "새로작성", "새로쓸", "새브리핑", "새로정리", "하나더만들")

private val CARD_DROP = listOf("빼", "안가져", "해제", "취소", "제외", "필요없", "안쓸", "안챙겨", "두고갈", "놔두고")

/** 나가기. 그 말로 시작할 때만이다. */
private val EXIT =
    Regex(
        """^\s*(?:그냥\s*)?(?:나가|나갈|화면\s*닫|창\s*닫|안\s*할래|안\s*만들래|그만\s*쓸래|다음에\s*할래|""" +
            """나중에\s*할래|(?:일정\s*(?:추가|등록)?|작성|예약)\s*(?:취소|안\s*할래|그만|안\s*해|접을래|접어))""",
    )

/** 지금 이름으로 병원 찾기를 연다. "병원 바꿀래", "병원 다시 고를래", "다른 병원으로 할래". */
private val OPEN_HOSPITAL =
    Regex(
        """^\s*(?:다른\s*(?:병원|곳|데)|(?:병원)(?:을|은|도)?\s*(?:다시\s*)?""" +
            """(?:찾|바꾸|바꿔|바꿀|고르|골라|고를|선택|변경|검색|정할|정하))""",
    )

/** 날짜 시트를 연다. "날짜 고를래", "언제 갈지 정할래". */
private val OPEN_DATE =
    Regex("""^\s*(?:날짜|일자|언제)(?:\s*(?:를|는|도|갈지|인지))?\s*(?:다시\s*)?(?:고르|골라|고를|선택|열어|바꾸|바꿔|바꿀|변경|정하|정할|보여)""")

/** 시간 시트를 연다. "시간 고를래", "몇 시인지 정할래". */
private val OPEN_TIME =
    Regex("""^\s*(?:시간|시각|몇\s*시)(?:\s*(?:를|는|도|인지))?\s*(?:다시\s*)?(?:고르|골라|고를|선택|열어|바꾸|바꿔|바꿀|변경|정하|정할|보여)""")

/** 시간을 비운다. "시간 미정", "몇 시인지 몰라", "시간은 아직 안 정했어". */
private val CLEAR_TIME =
    Regex("""(?:시간|시각|몇\s*시)(?:은|는|이|가|인지)?\s*(?:아직\s*)?(?:미정|지워|빼|없|몰라|모름|모르|비워|안\s*정|정해지지|나중에|상관없)""")

/** 날짜나 시각이 들어 있는지. 시트를 여는 말보다 채우는 말이 먼저다. */
private val DATE_TIME_HINT =
    Regex("""\d|오늘|내일|모레|글피|다음\s*주|이번\s*주|다음\s*달|[월화수목금토일]요일|오전|오후|아침|점심|저녁|정오""")

private const val TRIM = " .,!?~"
