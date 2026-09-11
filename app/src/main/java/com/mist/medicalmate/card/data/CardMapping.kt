package com.mist.medicalmate.card.data

import com.mist.medicalmate.card.ui.BriefCard
import com.mist.medicalmate.card.ui.BriefCardItem
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 응답을 화면 값으로 옮긴다.
 *
 * **이 파일만 갈아 끼우면 되게 한 곳에 모았다.** 서버가 카드 본문을 고정 필드에서 가변
 * 목록으로 바꾸는 중이다. 지금 계약은 `onset`·`pattern`·`site`·`medications`가 각각 필드로
 * 있고, 화면은 이미 `List<BriefCardItem>`이라 여기서 펴서 넘긴다. 가변으로 바뀌면 이 파일의
 * [items]만 응답 목록을 그대로 옮기는 것으로 줄어들고 다른 곳은 건드릴 것이 없다.
 */
internal fun CardResponse.toBriefCard(): BriefCard = BriefCard(
    id = cardId.toString(),
    title = title.orEmpty(),
    status = if (status == STATUS_CONFIRMED) BriefCard.Status.CONFIRMED else BriefCard.Status.BEFORE_VISIT,
    patientLine = patientLine(),
    items = items(),
    // 서버 카드에 통증 강도 자리가 없다. 문답 3단계가 받는 값인데 실을 곳이 없어 비워 둔다(#139).
    severity = null,
    allergies = allergies.toLines(),
    questions = questions,
    // `CardResponse`에 병원이 없다. 목록 응답에만 `clinicName`이 있다(#139).
    hospital = null,
)

/**
 * 전달 화면의 응답. 카드 조회와 본문이 같고 내부 추적값만 빠져 있다.
 *
 * id가 응답에 없어서 부른 쪽이 넘긴다. 상태는 확정이다. 확정한 카드만 열리는 경로다.
 */
internal fun HandoffResponse.toBriefCard(cardId: Long): BriefCard = CardResponse(
    cardId = cardId,
    status = STATUS_CONFIRMED,
    patient = patient,
    title = title,
    onset = onset,
    pattern = pattern,
    site = site,
    medications = medications,
    allergies = allergies,
    questions = questions,
    suggestedDepartment = suggestedDepartment,
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
 * 고정 필드를 KV 줄로 편다.
 *
 * `status`가 `KNOWN`이 아닌 필드는 줄을 만들되 글이 다르다. `NONE`("없어요")과
 * `UNKNOWN`("잘 모르겠어요")을 같게 그리면 의사용 카드에 "본인 확인 못 함"을 찍을 수 없다.
 * 값이 아예 안 온 필드는 줄도 만들지 않는다.
 *
 * 강조는 한 줄만 붙일 수 있는데 서버가 어느 것인지 주지 않는다. 아무 데도 붙이지 않는다.
 * 임의로 고르면 그 진료에서 중요한 것과 어긋난다.
 */
private fun CardResponse.items(): List<BriefCardItem> = listOfNotNull(
    site?.let { BriefCardItem(key = "부위", value = it.status.textOr(it.text)) },
    onset?.let { BriefCardItem(key = "기간", value = it.status.textOr(it.text)) },
    pattern?.let { BriefCardItem(key = "양상", value = it.status.textOr(it.text)) },
    medications?.let { meds ->
        BriefCardItem(
            key = "복용약",
            value = meds.status.textOr(
                meds.items.joinToString(" · ") {
                    listOfNotNull(it.name, it.note).joinToString(" ")
                },
            ),
        )
    },
)

/**
 * 알러지는 경고 면에 얹히는 목록이다.
 *
 * `NONE`이면 줄 자체를 두지 않는다. 없다는 것은 경고할 일이 아니다. `UNKNOWN`은 남긴다.
 * 확인하지 못했다는 사실이 의사에게는 정보다.
 */
private fun TextFieldResponse?.toLines(): List<String> = when (this?.status) {
    STATUS_KNOWN -> text?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    STATUS_UNKNOWN -> listOf(UNKNOWN_LABEL)
    else -> emptyList()
}

private fun String?.textOr(text: String?): String = when (this) {
    STATUS_KNOWN -> text.orEmpty()
    STATUS_NONE -> NONE_LABEL
    else -> UNKNOWN_LABEL
}

private const val STATUS_KNOWN = "KNOWN"

private const val STATUS_NONE = "NONE"

private const val STATUS_UNKNOWN = "UNKNOWN"

private const val STATUS_CONFIRMED = "CONFIRMED"

private const val NONE_LABEL = "없음"

private const val UNKNOWN_LABEL = "잘 모르겠어요"

private val WRITTEN_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)
