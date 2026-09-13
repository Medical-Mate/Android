package com.mist.medicalmate.card.data

import com.mist.medicalmate.card.ui.BriefCard
import com.mist.medicalmate.card.ui.BriefCardHospital
import com.mist.medicalmate.card.ui.BriefCardItem
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
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
    // 강도는 KV 줄이 아니라 눈금이다. 시안이 칩·낱말·NRS 등가를 한 줄로 그리고, 그 모양은
    // 줄 하나로 낼 수 없다. 그래서 축에서 빼 따로 든다.
    severity = axes[AXIS_SEVERITY]?.toSeverity(),
    // 건강 정보는 `CardHealth.kt`가 옮긴다. 이 파일이 detekt의 함수 수 한도에 닿았다.
    health = patient.toHealthRows(),
    allergies = patient?.allergies.toAllergies(),
    questions = questions,
    // 응답의 병원 셋 중 `clinic`이 "진료받을 병원"이다(Backend#101). `appointment`는 일정의
    // 병원이고 `visit`은 진료를 받은 병원이라 셋이 다 다를 수 있다.
    hospital = clinic?.toHospital(),
)

/**
 * 진료받을 병원.
 *
 * 이름이 없으면 없는 것으로 본다. 안 골랐을 때 서버가 빈 객체를 줄 수 있고, 이름 없는
 * 병원 블록은 그릴 것이 없다.
 */
private fun ClinicResponse.toHospital(): BriefCardHospital? {
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    return BriefCardHospital(name = name, address = address?.takeIf { it.isNotBlank() })
}

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
    )

/** 강도 축. KV 줄이 아니라 눈금으로 그려서 [AXIS_ORDER]에 없다. */
private const val AXIS_SEVERITY = "severity"

/**
 * 강도 축을 눈금으로.
 *
 * 값이 "3 (꽤 아파요)"처럼 숫자와 낱말이 함께 온다. 앞의 숫자만 읽는다. 낱말은 화면이
 * 단계에서 가져오고, 문자열을 맞춰 보면 서버가 표현을 바꿀 때마다 갈린다.
 *
 * 1~5 밖이거나 숫자가 없으면 그리지 않는다. 눈금은 다섯 단계뿐이고, 모르는 값을 억지로
 * 한 단계에 끼우면 환자가 고른 것과 다른 색이 나온다.
 */
private fun AxisResponse.toSeverity(): MedicalMateSeverity? {
    if (status != STATUS_FILLED) return null
    val level = SEVERITY_LEVEL.find(value.orEmpty())?.value?.toIntOrNull()
    return MedicalMateSeverity.entries.firstOrNull { it.level == level }
}

private val SEVERITY_LEVEL = Regex("\\d+")

private const val STATUS_FILLED = "FILLED"

private const val STATUS_UNKNOWN = "UNKNOWN"

private const val STATUS_AMBIGUOUS = "AMBIGUOUS"

private const val STATUS_CONFIRMED = "CONFIRMED"

private const val UNKNOWN_LABEL = "잘 모르겠어요"

private const val AMBIGUOUS_SUFFIX = "(확실하지 않아요)"

private val WRITTEN_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)
