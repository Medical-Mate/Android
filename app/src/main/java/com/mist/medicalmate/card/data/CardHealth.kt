package com.mist.medicalmate.card.data

import com.mist.medicalmate.card.ui.BriefCardItem

/**
 * 카드에 실린 건강 정보를 화면 값으로.
 *
 * `CardMapping`에서 갈라 나왔다. 그 파일이 detekt의 함수 수 한도에 닿아서이고, 갈래가 다른
 * 값이라 나눠 두는 편이 읽기도 낫다. 서버 계약이 바뀌면 이 파일과 `CardMapping` 둘을 본다.
 */

/**
 * 카드 안에 줄로 서는 건강 정보.
 *
 * 복용약과 기저질환만이다. 알러지는 카드 밖 경고로 따로 간다 — 처방을 바꾸는 값이라 다른
 * 정보와 같은 무게로 두지 않는 것이 시안의 판단이다.
 *
 * **적은 것이 없으면 줄을 만들지 않는다.** "없어요"와 "잘 모르겠어요"를 카드에 적지 않는다.
 * 진료실에서 읽는 사람에게 필요한 것은 무엇을 먹고 있느냐이고, 비어 있다는 사실은 그 자리에
 * 아무것도 없는 것으로 충분히 전해진다.
 */
internal fun PatientResponse?.toHealthRows(): List<BriefCardItem> = listOfNotNull(
    this?.medications.toRow("복용약"),
    this?.conditions.toRow("기저질환"),
)

/**
 * 알러지 한 줄을 칩으로 나눈다.
 *
 * 저장할 때 쉼표로 이어 보낸 그대로 온다. `profile`도 같은 규칙으로 나누는데 그 함수를 같이
 * 쓰지 않는다 — 한 도메인이 다른 도메인을 직접 참조하지 않고, 두 응답이 지금 같아 보여도
 * 같이 움직인다는 보장이 없다.
 */
internal fun CardTextFieldResponse?.toAllergies(): List<String> {
    val items = this?.text.orEmpty().split(",").map(String::trim).filter(String::isNotEmpty)
    return if (known(this?.status, items.isNotEmpty())) items else emptyList()
}

private fun CardListFieldResponse?.toRow(key: String): BriefCardItem? {
    val items = this?.items.orEmpty()
    if (!known(this?.status, items.isNotEmpty()) || items.isEmpty()) return null
    return BriefCardItem(key = key, value = items.joinToString(" · "))
}

/**
 * 적어 둔 것이 있다고 볼지.
 *
 * **서버가 상태를 빼고 보낼 때가 있다.** 기기 로그에서 `"medications":{"items":[]}`처럼
 * `status` 없이 오는 것을 봤다. 그때 상태만 보고 자르면 값이 있는데도 줄이 사라진다. 값이
 * 있으면 있는 것으로 본다 — `profile`이 같은 자리에서 쓰는 규칙이다.
 */
private fun known(status: String?, hasValue: Boolean): Boolean =
    if (status == null) hasValue else status == STATUS_KNOWN

/** 값이 있다고 답한 것. `NONE`은 없다는 답이고 `UNKNOWN`은 모른다는 답이라 카드에 적지 않는다. */
private const val STATUS_KNOWN = "KNOWN"
