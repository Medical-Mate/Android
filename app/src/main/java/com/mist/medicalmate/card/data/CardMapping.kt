package com.mist.medicalmate.card.data

import com.mist.medicalmate.card.ui.BriefCard
import com.mist.medicalmate.card.ui.BriefCardItem
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 응답을 화면 값으로 옮긴다.
 *
 * **이 파일만 갈아 끼우면 되게 한 곳에 모았다.** 실제로 2026-09-11에 서버가 카드 본문을
 * 고정 필드(`onset`·`pattern`·`site`·`medications`)에서 `axes` 맵으로 바꿨고, 고친 곳은
 * 여기뿐이다. 화면과 ViewModel은 그대로다.
 */
internal fun CardResponse.toBriefCard(): BriefCard = BriefCard(
    id = cardId.toString(),
    // 서버가 `title`을 아직 내려주지 않는다. 환자가 말한 것이 제목 자리를 대신한다.
    title = title ?: chiefComplaint.orEmpty(),
    status = if (status == STATUS_CONFIRMED) BriefCard.Status.CONFIRMED else BriefCard.Status.BEFORE_VISIT,
    patientLine = patientLine(),
    items = axes.toItems(),
    // 강도가 축 하나가 됐다. 따로 받지 않고 [items]에 줄로 들어간다.
    severity = null,
    // 새 스키마에 알러지가 없다. 신상정보에서 오던 값인데 카드가 더 이상 싣지 않는다.
    allergies = emptyList(),
    questions = questions,
    // `CardResponse`에 병원이 없다. 목록 응답에만 `clinicName`이 있다.
    hospital = null,
)

/**
 * 전달 화면의 응답.
 *
 * id가 응답에 없어서 부른 쪽이 넘긴다. 상태는 확정이다. 확정한 카드만 열리는 경로다.
 */
internal fun HandoffResponse.toBriefCard(cardId: Long): BriefCard = CardResponse(
    cardId = cardId,
    status = STATUS_CONFIRMED,
    patient = patient,
    title = title,
    chiefComplaint = chiefComplaint,
    axes = axes,
    redFlags = redFlags,
    patientNotes = patientNotes,
    questions = questions,
    departmentGuidance = departmentGuidance,
    createdAt = confirmedAt,
).toBriefCard()

/** 시안의 "김OO · 32세 여 · 2026.09.04 작성". */
private fun CardResponse.patientLine(): String = listOfNotNull(
    patient?.name,
    patient?.age?.let { "${it}세 ${sexLabel(patient.sex)}".trim() },
    createdAt?.let { "${OffsetDateTime.parse(it).format(WRITTEN_ON)} 작성" },
).joinToString(" · ")

private fun sexLabel(sex: String?): String = when (sex) {
    "FEMALE" -> "여"
    "MALE" -> "남"
    else -> ""
}

/**
 * 축을 KV 줄로 편다.
 *
 * **아직 묻지 않은 축은 줄을 만들지 않는다.** 8축이 늘 자리를 차지하고 1턴째는 대부분
 * `NOT_ASKED`라, 그대로 그리면 빈 줄 여덟 개가 먼저 보인다. 건너뛴 축도 같다. 환자가
 * 넘어가기로 한 것을 카드에 남길 이유가 없다.
 *
 * `UNKNOWN`("잘 모르겠다")은 남긴다. 확인하지 못했다는 것이 의사에게는 정보다. `AMBIGUOUS`는
 * 값이 있어도 확실하지 않다는 뜻이라 그 사실을 함께 적는다.
 *
 * 차례는 서버가 준 순서가 아니라 [AXIS_ORDER]다. 맵이라 순서가 보장되지 않고, 카드에서
 * 부위가 먼저 오는 것은 읽는 차례의 문제다.
 */
private fun Map<String, AxisResponse>.toItems(): List<BriefCardItem> = AXIS_ORDER.mapNotNull { axis ->
    val field = this[axis] ?: return@mapNotNull null
    val value =
        when (field.status) {
            STATUS_FILLED -> field.value.orEmpty()
            STATUS_UNKNOWN -> UNKNOWN_LABEL
            STATUS_AMBIGUOUS -> field.value?.let { "$it $AMBIGUOUS_SUFFIX" } ?: UNKNOWN_LABEL
            else -> return@mapNotNull null
        }
    BriefCardItem(key = axisLabel(axis), value = value, axis = axis)
}

/**
 * 축 이름.
 *
 * 문자열 리소스가 아니라 여기 둔다. `data` 계층이라 `Context`가 없고, 축 이름은 서버 계약에
 * 묶인 값이라 화면 카피와 성격이 다르다. 번역이 필요해지면 화면으로 올린다.
 */
private fun axisLabel(axis: String): String = when (axis) {
    "site" -> "부위"
    "onset" -> "시작"
    "character" -> "양상"
    "radiation" -> "뻗치는 곳"
    "associated" -> "같이 있는 증상"
    "time_course" -> "경과"
    "exacerbating_relieving" -> "심해질 때·나아질 때"
    "severity" -> "강도"
    else -> axis
}

/** 카드에서 읽는 차례. SOCRATES 8축이다. */
private val AXIS_ORDER =
    listOf(
        "site",
        "onset",
        "character",
        "radiation",
        "associated",
        "time_course",
        "exacerbating_relieving",
        "severity",
    )

private const val STATUS_FILLED = "FILLED"

private const val STATUS_UNKNOWN = "UNKNOWN"

private const val STATUS_AMBIGUOUS = "AMBIGUOUS"

private const val STATUS_CONFIRMED = "CONFIRMED"

private const val UNKNOWN_LABEL = "잘 모르겠어요"

private const val AMBIGUOUS_SUFFIX = "(확실하지 않아요)"

private val WRITTEN_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)
