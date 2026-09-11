package com.mist.medicalmate.card.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity

/**
 * 기록 상세의 상태. Figma 1j-3 `735:3829`.
 *
 * 한 진료의 전체 흐름을 보여준다. 카드를 만들어 진료실에서 보여주고, 진료 후 들은 것을
 * 적고, 재방문이 잡힌다. 그것이 한 화면에 이어져 있어야 무엇이 어떻게 흘러왔는지 읽힌다.
 *
 * **최신 날짜가 위다.** 재방문 뒤에 만든 카드와 기록이 같은 타임라인에 계속 쌓이므로,
 * 오래된 것이 위에 있으면 방금 있었던 일을 보려고 매번 끝까지 내려야 한다.
 *
 * 증상 정리 단계는 넣지 않는다. 문답에서 답한 내용은 브리핑 카드에 담기므로 카드 위에 같은
 * 값을 한 번 더 보여주는 자리가 된다.
 *
 * 재방문 뒤에 만든 카드와 기록도 같은 타임라인에 이어 붙는다. 그래서 [RecordDetail.steps]가
 * 목록이고 단계 종류가 고정되어 있지 않다.
 */
sealed interface RecordDetailUiState {
    data object Loading : RecordDetailUiState

    data object Failed : RecordDetailUiState

    data class Content(val detail: RecordDetail) : RecordDetailUiState
}

/**
 * 한 진료 묶음.
 *
 * [clinicLine]은 "서울OO병원 내과 · 09.12 진료"처럼 어디서 언제 받았는지다. 진료 전이면
 * 병원이 없을 수 있어 화면이 아니라 데이터가 문장을 만든다. 재방문이 쌓이면
 * "09.12 초진 · 09.26 재방문"처럼 날짜가 늘어난다(1j-3-R `1039:2799`).
 *
 * [badge]는 상태 배지를 덮는다. 재방문이 쌓인 기록은 "진료 완료"가 아니라 "진료 2회"를
 * 단다. 몇 번 갔는지는 상태가 아니라 세어 봐야 아는 값이라 데이터가 문장으로 준다.
 *
 * [steps]는 **최신순으로 온다.** 화면이 받은 순서대로 그린다. `at`이 "09.04 작성 · 09.12
 * 진료실에서 보여줌"처럼 기간을 담는 표시 문자열이라 화면에서 날짜로 정렬할 수 없다.
 */
data class RecordDetail(
    val id: String,
    val title: String,
    val status: RecordItem.Status,
    val clinicLine: String,
    val steps: List<RecordStep>,
    val badge: String? = null,
)

/**
 * 타임라인의 한 단계.
 *
 * [at]은 점 옆에 붙는 때다. "09.04 · 증상 정리"처럼 날짜와 무엇인지를 함께 담는다. 날짜와
 * 종류를 따로 받아 화면이 이어 붙이지 않는 이유는, 단계마다 문장이 다르기 때문이다.
 * 카드는 "09.04 작성 · 09.12 진료실에서 보여줌"처럼 두 날짜를 갖는다.
 */
sealed interface RecordStep {
    val at: String

    /**
     * 내용이 있는 단계. 카드 하나로 그려진다.
     *
     * [card]가 있으면 접었다 펼 수 있다. 브리핑 카드 단계가 그렇다. 접혀 있을 때는
     * [items]의 앞 몇 줄만 보이고, 펴면 나머지 줄과 통증 강도·알러지·질문까지 나온다.
     */
    data class Block(
        override val at: String,
        val title: String,
        val items: List<RecordDetailItem>,
        val quote: RecordQuote? = null,
        val card: RecordStepCard? = null,
    ) : RecordStep

    /** 아직 오지 않은 단계. 점선 블록으로 그려진다. */
    /**
     * 아직 오지 않은 단계.
     *
     * [detail]은 있을 때만 둘째 줄로 나온다. 재방문이 잡힌 경우에는 날짜와 시간이 있고,
     * 병원이 정해지지 않은 경우에는 알릴 값이 없다.
     */
    data class Pending(override val at: String, val message: String, val detail: String? = null) : RecordStep
}

/**
 * 단계 안의 한 줄.
 *
 * [tone]으로 값의 색이 갈린다. 알러지는 경고색, 재방문 날짜는 브랜드색이다. 어느 값을
 * 세울지는 화면이 아니라 데이터가 정한다.
 */
data class RecordDetailItem(val key: String, val value: String, val tone: Tone = Tone.DEFAULT) {
    enum class Tone { DEFAULT, WARNING, LINK }
}

/** 환자가 말한 원문. 정리된 항목 아래에 그대로 남긴다. */
data class RecordQuote(val label: String, val text: String)

/**
 * 펼칠 수 있는 브리핑 카드 단계. Figma 1j-3-X `1038:2768`.
 *
 * 전에는 이 자리가 브리핑 카드 화면(1e-1)으로 건너가는 줄이었다. 시안이 그 자리에서 펴
 * 보는 것으로 바꿨다. 기록을 훑다가 카드를 확인하는 일이라 화면을 옮기면 돌아와서 보던
 * 자리를 다시 찾아야 한다.
 *
 * [collapsedItemCount]는 접혀 있을 때 보이는 줄 수다. 시안이 부위·기간·양상 셋을 남긴다.
 * 어느 줄을 남길지가 아니라 몇 줄을 남길지로 받는 이유는, 카드의 줄 순서를 데이터가
 * 정하고 앞쪽이 늘 더 중요한 값이기 때문이다.
 */
data class RecordStepCard(
    val collapsedItemCount: Int,
    val severity: MedicalMateSeverity? = null,
    val allergies: List<String> = emptyList(),
    val questions: List<String> = emptyList(),
)
