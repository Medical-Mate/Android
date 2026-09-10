package com.mist.medicalmate.visit.ui

/**
 * 진료 후 기록 플로우의 상태. Figma 1m·1p·1q-1·1k.
 *
 * 네 화면이 한 흐름이지만 목적지가 각각이라 상태도 화면 단위로 둔다. 서버 연동에서는
 * 병원 id와 방문 id가 화면 사이를 라우트로 건너간다.
 */

/**
 * 1m 병원 찾기. Figma `489:5447`.
 *
 * [query]로 좁힌 [results]에서 하나를 고른다. 검색을 서버가 하면 [results]가 응답이 되고
 * 화면은 그대로다.
 */
data class HospitalPickUiState(
    val query: String = "",
    val results: List<Hospital> = emptyList(),
    val selectedId: String? = null,
) {
    val canSubmit: Boolean = selectedId != null
}

/** 검색 결과 한 곳. 이름으로 찾으면 주소가 함께 등록된다. */
data class Hospital(val id: String, val name: String, val address: String)

/**
 * 1p 진료 후 메모. Figma `405:1926`.
 *
 * [visit]은 무엇을 받은 진료인지다. 브리핑 카드로 진료를 받았으면 그 카드를 가리킨다.
 * [note]는 환자가 적은 원문이고 뒤 화면까지 그대로 따라간다. AI가 나눈 결과가 틀렸을 때
 * 대조할 것이 원문뿐이다.
 */
data class VisitNoteUiState(val visit: VisitHeadline, val note: String = "", val organizing: Boolean = false) {
    val canSave: Boolean = note.isNotBlank() && !organizing
}

/** "오늘 진료 / 서울OO병원 내과 · 9월 12일 / 복부 통증 · 3주 브리핑 카드로 진료받았어요" */
data class VisitHeadline(val label: String, val title: String, val detail: String)

/**
 * 1q-1 자동 분류 결과와 1q-1-E 전체 수정. Figma `405:2193`, `636:3675`.
 *
 * 한 화면의 두 모드다. 편집이 켜지면 값이 그 자리에서 입력으로 바뀌고, 일정 등록
 * 체크박스가 숨는다(시안이 `hidden`으로 표시해 둔 부분이다).
 *
 * [VisitRecordDraft]를 따로 두는 이유는 취소가 있기 때문이다. 원본을 바로 고치면 되돌릴
 * 것이 없다.
 */
sealed interface VisitRecordUiState {
    data object Loading : VisitRecordUiState

    data object Failed : VisitRecordUiState

    /**
     * [draft]가 있으면 편집 모드다. 모드를 따로 두면 "편집 중이라면서 사본이 없는" 상태를
     * 만들 수 있다.
     *
     * [deleteRequested]는 삭제 확인 대화상자(1q-1-DC)가 떠 있는지다. 삭제를 취소하면 편집
     * 모드는 그대로 남아야 해서 편집 상태와 분리한다.
     */
    data class Content(
        val record: VisitRecord,
        val draft: VisitRecordDraft? = null,
        val scheduleRevisit: Boolean = false,
        val deleteRequested: Boolean = false,
    ) : VisitRecordUiState {
        val editing: Boolean get() = draft != null

        /** 화면에 그릴 항목. 편집 중이면 사본, 아니면 원본이다. */
        val items: List<VisitRecordItem> get() = draft?.items ?: record.items

        /**
         * 편집 중에 무엇이든 바뀌었는지. Nav 우측이 `취소`에서 `확인`으로 바뀌는 기준이다.
         *
         * 아무것도 안 건드렸는데 `확인`이 떠 있으면 뭘 확인하라는 건지 알 수 없다.
         */
        val changed: Boolean get() = draft != null && draft.items != record.items
    }
}

/**
 * 편집 중인 사본.
 *
 * 원문 메모는 담지 않는다. 문서가 지울 수 없는 것 목록에 그 메모를 넣었다. AI 정리는 고치되
 * 환자가 적은 말은 남는다는 P2다. 그래서 편집 모드에서도 메모 블록은 ×도 입력도 없다.
 */
data class VisitRecordDraft(val items: List<VisitRecordItem>) {
    internal companion object {
        fun of(record: VisitRecord) = VisitRecordDraft(items = record.items)
    }
}

/**
 * AI가 메모를 나눈 결과.
 *
 * [items]는 소견·검사·약·재방문 넷이다. 개수를 고정하지 않는 이유는 AI가 찾지 못한 항목이
 * 빠질 수 있기 때문이다. [caption]이 몇 가지로 나눴는지 알린다.
 */
data class VisitRecord(
    val id: String,
    val clinicLine: String,
    val items: List<VisitRecordItem>,
    val memo: String,
    val caption: String,
)

/** [tone]이 값의 색을 정한다. 재방문 날짜는 브랜드색으로 세운다. */
data class VisitRecordItem(val key: String, val value: String, val tone: Tone = Tone.DEFAULT) {
    enum class Tone { DEFAULT, LINK }
}

/**
 * 1k 이번 진료 정리. Figma `405:2322`.
 *
 * [previous]가 없으면 비교 영역을 그리지 않는다. 첫 진료에는 견줄 것이 없다.
 */
data class VisitSummaryUiState(
    val current: VisitCompareCard,
    val hospital: HospitalSummary,
    val previous: VisitCompareCard? = null,
)

/** 비교 카드 한 장. "지난 진료 · 8.21 / 두통 · 어지러움 / 진통제 처방 · 경과 관찰" */
data class VisitCompareCard(val label: String, val title: String, val detail: String)

/** [visitLine]은 "2026.09.12 진료 · 다음 방문 09.26"처럼 언제 받고 언제 다시 가는지다. */
data class HospitalSummary(val name: String, val address: String, val visitLine: String)
