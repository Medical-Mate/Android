package com.mist.medicalmate.card.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity

/**
 * 브리핑 카드 한 장.
 *
 * **내용은 AI가 정한다.** 문답에서 주고받은 말을 AI가 카드 모양으로 정리해서 넘겨주고
 * 화면은 그것을 그린다. 그래서 [items]가 목록이다. 부위·기간·양상처럼 항목을 고정으로
 * 박아 두면 AI가 다른 항목을 보내거나 어떤 항목을 빼는 순간 화면이 못 그린다.
 *
 * [severity]와 [questions], [allergies]는 자리와 모양이 정해진 것들이라 따로 받는다.
 * 강도는 색과 NRS 등가가 붙고, 질문은 번호 pill이 붙고, 알러지는 경고 면에 얹힌다.
 * 이것들을 [items]에 섞으면 그 모양을 잃는다.
 *
 * **[allergies]는 AI가 만든 것이 아니다.** 신상정보 등록(1b-3)에서 환자가 고른 값이
 * 그대로 올라온다. 문답에서 말한 것이 아니라 프로필에 있는 사실이라 AI가 다시 정리할
 * 대상이 아니고, 카드 맨 위에 항상 표시된다.
 */
data class BriefCard(
    val id: String,
    val title: String,
    val status: Status,
    val patientLine: String,
    val items: List<BriefCardItem>,
    val severity: MedicalMateSeverity?,
    val allergies: List<String>,
    val questions: List<String>,
) {
    /** 백엔드 카드 상태 `draft` / `confirmed`에 대응한다. */
    enum class Status { BEFORE_VISIT, CONFIRMED }
}

/**
 * 카드의 항목 한 줄. Figma `KV Row`.
 *
 * [emphasized]는 그 진료에서 가장 중요한 한 항목이다. 문서 8.3이 카드마다 하나를 넘기지
 * 말라고 한다. 전부 강조하면 아무것도 강조되지 않는다. 어느 항목을 세울지는 AI가 정한다.
 */
data class BriefCardItem(val key: String, val value: String, val emphasized: Boolean = false)

/**
 * 브리핑 카드 화면의 상태.
 *
 * 읽기와 수정을 한 화면의 두 모드로 둔다. Figma 1e-1과 1e-1-E가 같은 카드의 두 모습이다.
 */
sealed interface BriefCardUiState {
    data object Loading : BriefCardUiState

    data object Failed : BriefCardUiState

    /**
     * [drafts]는 수정 중인 값이다. 항목 순번을 열쇠로 쓴다.
     *
     * 원본을 그대로 고치지 않는 이유는 취소가 있기 때문이다. 수정 중에 취소를 누르면
     * [card]가 그대로 남아 있어야 한다.
     *
     * **항목별로 수정하지 않고 카드 전체를 한 번에 연다.** 수정 아이콘을 누르면 카드 안의
     * 모든 값이 입력 상태가 된다. Figma 수정사항이 항목별 수정을 버리고 일괄 수정으로
     * 정리했고, 그래야 "내가 직접 고칠 수 있다"가 전달된다.
     */
    data class Content(val card: BriefCard, val editing: Boolean = false, val drafts: List<String> = emptyList()) :
        BriefCardUiState {
        fun draftAt(index: Int): String = drafts.getOrElse(index) { card.items[index].value }
    }
}
