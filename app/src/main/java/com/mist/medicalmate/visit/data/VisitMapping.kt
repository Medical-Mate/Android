package com.mist.medicalmate.visit.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 응답을 화면 값으로 옮긴다.
 *
 * 카드의 `CardMapping`과 같은 자리다. 서버가 계약을 바꾸면 고칠 곳이 여기 하나가 되도록
 * 모아 둔다 — 실제로 2026-09-13에 고정 세 필드가 축 맵으로 바뀌었고(#178) 그때 화면과
 * ViewModel은 손대지 않았다.
 */
internal fun VisitResponse.toVisit() = Visit(
    id = visitId.toString(),
    cardId = cardId,
    clinic = clinicName,
    visitedOn = visitedOn?.let(LocalDate::parse),
    items = axes.toItems().withRevisitDate(followUp?.toFollowUp()),
    followUp = followUp?.toFollowUp(),
    patientNotes = patientNotes,
    rawNote = rawNote,
)

internal fun ClassifyMemoResponse.toClassification() = VisitClassification(
    items = axes.toItems().withRevisitDate(followUp?.toFollowUp()),
    followUp = followUp?.toFollowUp(),
    patientNotes = patientNotes,
    labels = labels,
)

internal fun VisitSummaryResponse.toListItem() = VisitListItem(
    id = visitId.toString(),
    cardId = cardId,
    cardTitle = cardTitle.orEmpty(),
    clinic = clinicName,
    visitedOn = LocalDate.parse(visitedOn),
    followUpDate = followUpDate?.let(LocalDate::parse),
)

/**
 * 축을 줄로 편다.
 *
 * **값이 있는 축만 남긴다.** 서버가 못 찾은 항목은 빈 값이 아니라 키가 없고, 값이 비었다는
 * 것은 환자가 지웠다는 뜻이다. 어느 쪽이든 그릴 줄이 아니다.
 *
 * 차례는 [AXIS_ORDER]다. 맵이라 순서가 보장되지 않는다. **모르는 축은 뒤에 그 순서대로
 * 붙인다** — 항목 이름이 닫힌 목록이 아니라 AI가 늘릴 수 있고, 아는 것만 그리면 환자가 적은
 * 줄이 사라진다.
 */
private fun Map<String, VisitAxisResponse>.toItems(): List<VisitItem> {
    val known = AXIS_ORDER.filter { containsKey(it) }
    val rest = keys.filterNot { it in AXIS_ORDER }
    return (known + rest).mapNotNull { axis ->
        val value = this[axis]?.value?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        VisitItem(axis = axis, label = axisLabel(axis), value = value)
    }
}

/**
 * 재방문 줄에 날짜를 덧붙인다.
 *
 * 축의 값은 환자가 말한 그대로다("2주 뒤"). 그것만으로는 달력의 어느 날인지 알 수 없고, 날짜만
 * 적으면 환자가 한 말이 사라진다. 둘을 함께 적어 시안의 `2주 뒤 (9월 27일 전후)`가 된다.
 *
 * **[VisitFollowUp.approximate]면 "전후"를 붙인다.** "2주 뒤"는 날짜가 아니라 범위라서, 정확한
 * 날짜처럼 그리면 그날이 아니면 안 되는 것으로 읽힌다.
 *
 * 축 이름과 같은 이유로 여기 둔다 — 화면 둘(1q-1·1j-3)이 같은 줄을 그리고, 조립 규칙이 두
 * 곳에 있으면 갈린다.
 */
private fun List<VisitItem>.withRevisitDate(followUp: VisitFollowUp?): List<VisitItem> {
    val date = followUp?.date ?: return this
    val note = date.format(REVISIT_DATE) + if (followUp.approximate) " 전후" else ""
    return map { item ->
        if (item.axis != AXIS_FOLLOW_UP) item else item.copy(value = "${item.value} ($note)")
    }
}

private fun FollowUpResponse.toFollowUp(): VisitFollowUp? {
    val day = date?.let(LocalDate::parse) ?: return null
    return VisitFollowUp(date = day, text = text, approximate = approximate)
}

/**
 * 축 이름.
 *
 * 문자열 리소스가 아니라 여기 둔다. `data` 계층이라 `Context`가 없고, 축 이름은 서버 계약에
 * 묶인 값이라 화면 카피와 성격이 다르다. 카드 쪽과 같은 규칙이다.
 *
 * **모르는 축은 축 id를 그대로 쓴다.** 빈 이름으로 두면 값만 떠 있는 줄이 되고, 그 줄이 무엇을
 * 적은 것인지 알 수 없다.
 */
private fun axisLabel(axis: String): String = when (axis) {
    AXIS_FINDINGS -> "소견"
    AXIS_TESTS -> "검사"
    AXIS_MEDICATION -> "약"
    AXIS_FOLLOW_UP -> "재방문"
    else -> axis
}

/** 시안 1q-1이 그리는 차례. */
private val AXIS_ORDER = listOf(AXIS_FINDINGS, AXIS_TESTS, AXIS_MEDICATION, AXIS_FOLLOW_UP)

internal const val AXIS_FINDINGS = "findings"

internal const val AXIS_TESTS = "tests"

internal const val AXIS_MEDICATION = "medication_instructions"

internal const val AXIS_FOLLOW_UP = "follow_up"

/** 시안의 "9월 27일". */
private val REVISIT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)
