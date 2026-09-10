package com.mist.medicalmate.card.ui

/**
 * 기록 상세의 상태. Figma 1j-3 `735:3829`.
 *
 * 한 진료의 전체 흐름을 시간순으로 보여준다. 증상을 정리하고, 카드를 만들어 진료실에서
 * 보여주고, 진료 후 들은 것을 적고, 재방문이 잡힌다. 그 네 가지가 한 화면에 이어져 있어야
 * 무엇이 어떻게 흘러왔는지 읽힌다.
 *
 * 재방문 뒤에 만든 카드와 기록도 같은 타임라인에 이어 붙는다. 그래서 [steps]가 목록이고
 * 단계 종류가 고정되어 있지 않다.
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
 * 병원이 없을 수 있어 화면이 아니라 데이터가 문장을 만든다.
 */
data class RecordDetail(
    val id: String,
    val title: String,
    val status: RecordItem.Status,
    val clinicLine: String,
    val steps: List<RecordStep>,
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

    /** 내용이 있는 단계. 카드 하나로 그려진다. */
    data class Block(
        override val at: String,
        val title: String,
        val items: List<RecordDetailItem>,
        val quote: RecordQuote? = null,
        val action: RecordStepAction? = null,
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
 * 단계를 여는 조작.
 *
 * [target]이 어디로 가는지다. 목적지가 아직 없는 단계에는 이 값을 주지 않는다. 눌러도
 * 아무 일이 없는 버튼을 두지 않기 위해서다(#79).
 */
data class RecordStepAction(val label: String, val target: Target) {
    enum class Target { BRIEF_CARD }
}
