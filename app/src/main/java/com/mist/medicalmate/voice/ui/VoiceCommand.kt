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
    BRIEF_CARDS,
    SCHEDULE_ADD,
}

/**
 * 받아쓴 말을 명령으로. 모르는 말이면 null이다.
 *
 * **띄어쓰기를 지우고 낱말을 찾는다.** 받아쓰기가 "일정 추가"와 "일정추가"를 그때그때 다르게
 * 적는다. 낱말이 서로 겹치는 것이 있어 차례가 곧 우선순위다 — "증상 기록"은 기록 탭이 아니라
 * 증상 정리이고, "일정 추가"는 캘린더가 아니라 일정 추가다.
 */
fun parseVoiceCommand(text: String): VoiceCommand? {
    val heard = text.filterNot { it.isWhitespace() || it in PUNCTUATION }
    if (heard.isEmpty()) return null
    return KEYWORDS.firstOrNull { (_, words) -> words.any { it in heard } }?.first
}

private const val PUNCTUATION = ".,?!~"

private val KEYWORDS: List<Pair<VoiceCommand, List<String>>> =
    listOf(
        VoiceCommand.SCHEDULE_ADD to listOf("일정추가", "일정등록", "예약추가", "예약등록"),
        VoiceCommand.INTAKE to listOf("증상", "아파", "아픈"),
        VoiceCommand.BRIEF_CARDS to listOf("브리핑", "카드"),
        VoiceCommand.PROFILE to listOf("내정보", "설정", "프로필"),
        VoiceCommand.BACK to listOf("뒤로", "이전", "돌아가"),
        VoiceCommand.CALENDAR to listOf("캘린더", "달력", "일정", "예약"),
        VoiceCommand.RECORD to listOf("기록"),
        VoiceCommand.HOME to listOf("홈", "처음"),
    )
