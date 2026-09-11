package com.mist.medicalmate.card.ui

import com.mist.medicalmate.card.data.AxisEdit
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
 *
 * [hospital]은 진료받을 병원이다. 증상 정리를 마치고 병원을 먼저 찾은 경우에만 있다.
 * 그래서 null이 될 수 있다.
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
    val hospital: BriefCardHospital? = null,
) {
    /** 백엔드 카드 상태 `draft` / `confirmed`에 대응한다. */
    enum class Status { BEFORE_VISIT, CONFIRMED }
}

/**
 * 카드에 붙은 진료받을 병원. Figma 1e-1의 `진료받을 병원` 섹션이다.
 *
 * 날짜는 들고 있지 않다. 시안의 이 섹션은 `Hospital Card`에서 날짜 칩을 끄고 이름과 주소만
 * 보여준다. 진료 날짜는 캘린더 일정이 들고 있고 카드가 정하는 값이 아니다.
 */
data class BriefCardHospital(val name: String, val address: String)

/**
 * 카드의 항목 한 줄. Figma `KV Row`.
 *
 * [emphasized]는 그 진료에서 가장 중요한 한 항목이다. 문서의 컴포넌트 규격이 카드마다 하나를 넘기지
 * 말라고 한다. 전부 강조하면 아무것도 강조되지 않는다. 어느 항목을 세울지는 AI가 정한다.
 */
data class BriefCardItem(
    val key: String,
    val value: String,
    val emphasized: Boolean = false,
    /**
     * 서버의 축 id(`onset`·`site` 같은 것).
     *
     * 고친 값을 보낼 때 어느 축인지 알아야 한다. 서버가 `{"axes":[{"axis":"onset",
     * "value":"3주 전"}]}` 모양으로 받고, 줄 이름("시작")은 우리가 붙인 표시용이라 그것으로는
     * 못 찾는다.
     *
     * 축에서 오지 않은 줄은 `null`이다. 그런 줄은 고쳐도 보낼 곳이 없다.
     */
    val axis: String? = null,
)

/**
 * 편집 중인 사본.
 *
 * 원본을 그대로 고치지 않는 이유는 취소가 있기 때문이다. 편집 중에 취소를 누르면 카드가
 * 그대로 남아 있어야 한다.
 *
 * 값만 담지 않고 목록째로 담는다. 항목과 질문을 지우고 더할 수 있게 되면서 순번을 열쇠로
 * 쓰는 방식이 깨졌다. 두 번째 항목을 지우면 그 뒤 항목의 순번이 하나씩 밀린다.
 */
data class BriefCardDraft(val items: List<BriefCardItem>, val questions: List<String>) {
    internal companion object {
        fun of(card: BriefCard) = BriefCardDraft(items = card.items, questions = card.questions)
    }
}

/**
 * 브리핑 카드 화면의 상태.
 *
 * 읽기와 편집을 한 화면의 두 모드로 둔다. Figma 1e-1과 1e-1-E가 같은 카드의 두 모습이다.
 */
sealed interface BriefCardUiState {
    data object Loading : BriefCardUiState

    data object Failed : BriefCardUiState

    /**
     * [draft]가 있으면 편집 모드다. 모드를 따로 두지 않는 이유는 둘이 어긋날 수 있기
     * 때문이다. 편집 중이라면서 사본이 없거나 그 반대인 상태를 만들 수 없어야 한다.
     *
     * **항목별로 편집하지 않고 카드 전체를 한 번에 연다.** Nav 우측 `편집`을 누르면 카드
     * 안의 모든 값이 입력 상태가 된다. Figma가 항목별 편집을 버리고 일괄 편집으로 정리했고,
     * 그래야 "내가 직접 고칠 수 있다"가 전달된다.
     *
     * [deleteRequested]는 삭제 확인 대화상자(1e-1-DC)가 떠 있는지다. 편집 상태와 분리한다.
     * 삭제를 취소하면 편집 모드는 그대로 남아 있어야 한다.
     *
     * [saveFailed]는 확인을 눌렀는데 서버가 받지 못한 경우다. 편집 모드를 그대로 두고
     * 알리기만 한다. 닫아 버리면 방금 고친 것이 사라진다.
     */
    data class Content(
        val card: BriefCard,
        val draft: BriefCardDraft? = null,
        val deleteRequested: Boolean = false,
        val saveFailed: Boolean = false,
    ) : BriefCardUiState {
        val editing: Boolean get() = draft != null

        /** 화면에 그릴 항목. 편집 중이면 사본, 아니면 원본이다. */
        val items: List<BriefCardItem> get() = draft?.items ?: card.items

        val questions: List<String> get() = draft?.questions ?: card.questions

        /**
         * 편집 중에 무엇이든 바뀌었는지. Nav 우측이 `취소`에서 `확인`으로 바뀌는 기준이다.
         *
         * 아무것도 안 건드렸는데 `확인`이 떠 있으면 뭘 확인하라는 건지 알 수 없다. 문서의
         * CRUD 규칙이 그래서 바꾼 게 있을 때만 확인을 띄우라고 한다.
         */
        val changed: Boolean get() = draft != null && draft != BriefCardDraft.of(card)

        /**
         * 값이 달라진 축만.
         *
         * 안 바뀐 축을 함께 보내면 서버가 그것도 환자가 고친 값으로 남긴다. 원본과 사본을
         * 축 id로 맞춰 비교한다. 차례가 같더라도 줄이 지워질 수 있어 자리로 맞추지 않는다.
         */
        fun changedAxes(): List<AxisEdit> {
            val before = card.items.mapNotNull { item -> item.axis?.let { it to item.value } }.toMap()
            return draft?.items.orEmpty()
                .mapNotNull { item ->
                    val axis = item.axis ?: return@mapNotNull null
                    AxisEdit(axis = axis, value = item.value).takeIf { before[axis] != item.value }
                }
        }
    }
}
