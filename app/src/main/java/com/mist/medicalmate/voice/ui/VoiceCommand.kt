package com.mist.medicalmate.voice.ui

/**
 * "헤이 메이트" 다음에 말할 수 있는 것(#273).
 *
 * 지금은 화면을 옮기는 것뿐이다. 저장·삭제처럼 되돌릴 수 없는 동작은 넣지 않았다 — 잘못
 * 알아들으면 사고가 난다. 넣게 되면 실행 전에 확인을 거친다.
 *
 * 어디로 옮길지는 그래프(`navigation`)가 정한다. 여기는 무엇을 원하는지까지만 안다.
 */
enum class VoiceCommand {
    HOME,
    CALENDAR,
    RECORD,
    PROFILE,
    BACK,
    INTAKE,

    /** 진료 후 기록을 남긴다(#288). 말한 날, 없으면 오늘의 일자 화면에서 시작한다. */
    VISIT_RECORD,
    BRIEF_CARDS,
    SCHEDULE_ADD,

    /** 하던 증상 정리를 이어 한다(#320). 홈의 "이어서 하기"와 같다. 홈이 그 정리를 안다. */
    RESUME,

    /** 다음 진료가 언제인지 묻는다(#320). 홈 배너가 "다음 진료가 언제인지 물을 수 있어요"라고 적는다. */
    NEXT_VISIT,
}

/**
 * 받아쓴 말을 명령으로. 모르는 말이면 null이다.
 *
 * **띄어쓰기를 지우고 낱말을 찾는다.** 받아쓰기가 "일정 추가"와 "일정추가"를 그때그때 다르게
 * 적는다. 낱말이 서로 겹치는 것이 있어 차례가 곧 우선순위다. "증상 기록"은 기록 탭이 아니라
 * 증상 정리이고, "일정 추가"·"예약 잡아 줘"는 캘린더가 아니라 일정 추가다. 카드는 증상보다
 * 먼저다. "복부 증상 브리핑 카드 보여 줘"는 증상 정리가 아니라 카드다.
 *
 * 한 뜻에 여러 말을 받는다(#282). 사람은 정해 둔 말로 말하지 않는다.
 *
 * **카드를 새로 만들자는 말은 증상 정리다**(#284). "브리핑 카드 만들어 줘", "카드 만들자"가 카드
 * 목록으로 가면 만들 길이 없다. 카드는 증상 정리를 마쳐야 생긴다.
 */
fun parseVoiceCommand(text: String): VoiceCommand? {
    val heard = text.filterNot { it.isWhitespace() || it in PUNCTUATION }
    val keyword = KEYWORDS.firstOrNull { (_, words) -> words.any { it in heard } }?.first
    return when {
        heard.isEmpty() -> null
        // 알림 설정은 내 정보에 있다(#320). "진료 후 기록 알림 꺼 줘"가 "진료후기록"으로 기록을 시작했다.
        heard.contains("알림") -> VoiceCommand.PROFILE
        keyword != VoiceCommand.SCHEDULE_ADD && CREATE_CARD.containsMatchIn(heard) -> VoiceCommand.INTAKE
        else -> keyword
    }
}

/** 카드를 새로 만들자는 말. "브리핑 카드를 만들자", "카드 작성할래", "새 카드". */
private val CREATE_CARD = Regex("""(?:카드|브리핑).{0,3}(?:만들|작성|쓰자|쓸래|써줘|새로)|새(?:로운)?(?:브리핑)?카드""")

private const val PUNCTUATION = ".,?!~"

/*
 * 차례가 곧 우선순위다. "뒤로"는 맨 끝이다(#320) — "이전 기록 보여 줘"가 "이전"으로 뒤로 갔고, "홈으로 돌아가"가
 * "돌아가"로 뒤로 갔다. 이어 하기는 증상보다 먼저다 — "증상 정리 이어서 할래"가 새 정리를 열었다. 일정 추가는 그보다 먼저다 — "다음 진료
 * 일정 잡아 줘"는 묻는 말이 아니다.
 */
private val KEYWORDS: List<Pair<VoiceCommand, List<String>>> =
    listOf(
        VoiceCommand.SCHEDULE_ADD to
            listOf(
                "일정추가", "일정등록", "일정잡", "일정만들", "일정넣", "예약추가", "예약등록", "예약잡", "예약넣",
                "스케줄추가", "스케줄잡", "진료잡", "진료예약잡",
            ),
        VoiceCommand.RESUME to listOf("이어서", "이어하", "이어할", "이어서할", "하던거", "하던정리", "하다만"),
        VoiceCommand.NEXT_VISIT to
            listOf("다음진료", "다음예약", "다음일정언제", "진료언제", "진료가언제", "병원언제", "언제가야", "진료날언제"),
        VoiceCommand.BRIEF_CARDS to listOf("브리핑", "카드"),
        VoiceCommand.INTAKE to
            listOf("증상", "아파", "아픈", "아프", "아퍼", "문진", "불편해", "결려", "결리", "저려", "저리", "쑤셔", "쑤시"),
        VoiceCommand.VISIT_RECORD to
            listOf(
                "진료후기록", "기록할", "기록하기", "기록남", "기록쓸", "기록써", "기록적", "기록해", "진료끝", "진료다녀",
                "진료받고왔", "진료받았", "진료봤", "병원다녀왔", "들은말", "들은거",
            ),
        VoiceCommand.PROFILE to
            listOf("내정보", "나의정보", "제정보", "설정", "프로필", "마이페이지", "내계정", "건강정보", "먹는약", "복용약"),
        VoiceCommand.CALENDAR to listOf("캘린더", "달력", "일정", "예약", "스케줄"),
        VoiceCommand.RECORD to listOf("기록", "히스토리", "진료내역", "다녀온"),
        VoiceCommand.HOME to listOf("홈", "처음", "메인", "첫화면"),
        VoiceCommand.BACK to listOf("뒤로", "이전", "돌아가", "전화면", "앞화면", "되돌아"),
    )
