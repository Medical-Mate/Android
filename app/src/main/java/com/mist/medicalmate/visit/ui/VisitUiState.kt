package com.mist.medicalmate.visit.ui

import androidx.annotation.Keep
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.visit.data.VisitFollowUp
import java.time.LocalDate

/**
 * 진료 후 기록 플로우의 상태. Figma 1m·1p·1q-1.
 *
 * 세 화면이 한 흐름이지만 목적지가 각각이라 상태도 화면 단위로 둔다. 서버 연동에서는
 * 병원 id와 방문 id가 화면 사이를 라우트로 건너간다.
 */

/**
 * 1m 병원 찾기. Figma `489:5447`.
 *
 * [query]로 좁힌 [results]에서 하나를 고른다. 검색을 서버가 하면 [results]가 응답이 되고
 * 화면은 그대로다.
 */
/**
 * 병원을 고르는 목적. 같은 화면이 두 자리에서 쓰인다.
 *
 * [AFTER_VISIT]은 1m이다. 진료를 받고 나서 어디서 받았는지 고른다. 반드시 골라야 다음으로
 * 간다.
 *
 * [BEFORE_VISIT]은 1m-B다. 증상 정리를 마치고 진료받을 병원을 미리 찾는다. 건너뛸 수 있다.
 * 병원은 진료 후에도 등록할 수 있어서 여기서 반드시 정해야 하는 값이 아니다.
 *
 * [SCHEDULE]은 일정 추가(1r-4)의 병원 필드에서 온 것이다. 문구와 CTA는 [BEFORE_VISIT]과
 * 같고 돌아가는 자리만 다르다. 고른 병원이 카드가 아니라 만들던 일정으로 간다.
 *
 * 화면을 두 개로 만들지 않는다. 검색과 목록과 선택이 같고 문구와 CTA만 다르다.
 */
@Keep
enum class HospitalPickPurpose {
    AFTER_VISIT,
    BEFORE_VISIT,
    SCHEDULE,
}

data class HospitalPickUiState(
    val query: String = "",
    val results: List<Hospital> = emptyList(),
    val selected: Hospital? = null,
    val purpose: HospitalPickPurpose = HospitalPickPurpose.AFTER_VISIT,
    val searching: Boolean = false,
    /**
     * 조건에 맞는 전체 건수. [results]보다 클 수 있다.
     *
     * 부분 일치라 "서울"이면 4천 건이 넘는다. 그때 필요한 것은 더 받는 것이 아니라 검색어를
     * 좁히는 것이고, 그 사실을 알려야 목록 끝까지 훑다 포기하지 않는다.
     */
    val total: Int = 0,
    /** 서버에 닿지 못했는지. 못 찾은 것과 다르다. */
    val failed: Boolean = false,
) {
    /**
     * 하단 CTA를 누를 수 있는지.
     *
     * 진료 후(1m)에는 병원을 골라야 한다. 어느 진료의 기록인지가 정해지지 않으면 다음
     * 화면이 무엇을 적는지 모른다.
     *
     * 진료 전(1m-B)에는 고르지 않아도 넘어간다. 시안이 아무것도 고르지 않은 상태에서도 CTA를
     * 살려 뒀고, 옆의 `건너뛰기`와 같은 곳으로 간다. 병원은 진료 후에도 등록할 수 있다.
     */
    val canSubmit: Boolean = selected != null || purpose != HospitalPickPurpose.AFTER_VISIT

    /**
     * 하단 CTA 바를 그리는지.
     *
     * 시안은 결과가 없는 동안 바를 비활성으로 두지 않고 아예 없앤다. `1m-B`의 입력 전
     * 프레임(`1092:3858`)에 `Footer`가 없고 그 자리를 `Empty State`가 496으로 늘어 채운다.
     * 결과가 있는 `1m-B`(`1041:3687`)와 `1m`(`489:5447`)에는 있다.
     *
     * [results]가 비면 [selected]도 항상 비어 있다. 검색어가 바뀔 때 결과에서 빠진 선택을
     * 지우기 때문이다. 그래서 고른 것이 있는지 따로 보지 않는다.
     *
     * 진료 전에는 이 바가 사라지면 앞으로 갈 길이 상단의 `건너뛰기`뿐이다. 시안은 그
     * 문구까지 지웠는데 대신 갈 자리가 없어서 남겨 뒀다. #119에 올린 확인 대기 항목이다.
     */
    val showSubmit: Boolean = results.isNotEmpty()

    /** 받은 것보다 더 있는지. */
    val truncated: Boolean = total > results.size
}

/**
 * 아직 진료를 받지 않은 병원을 찾는 자리인지.
 *
 * 1m-B와 일정 추가가 여기 해당한다. 둘은 문구가 같고 시작이 빈 목록이다. 1m은 진료를
 * 받고 온 것이라 목록을 먼저 보여준다.
 */
internal val HospitalPickPurpose.beforeVisit: Boolean
    get() = this != HospitalPickPurpose.AFTER_VISIT

/**
 * 검색 결과 한 곳.
 *
 * **id가 없다.** 서버가 심평원에서 가져오면서 id를 매기지 않는다. 이름이 곧 식별자이고,
 * 일정 등록의 `clinicName`에 그 값을 그대로 넣는다.
 *
 * [address]는 우리가 요청해서 받은 값이다(Backend#80). 같은 이름의 다른 지점을 구별할 수 있는
 * 유일한 값이라 결과 줄에 함께 적는다. 심평원에 없는 곳은 비어 있고 그때는 줄을 그리지 않는다.
 */
data class Hospital(val name: String, val address: String? = null)

/**
 * 1p 진료 후 메모. Figma `405:1926`.
 *
 * [visit]은 무엇을 받은 진료인지다. 브리핑 카드로 진료를 받았으면 그 카드를 가리킨다.
 * [note]는 환자가 적은 원문이고 뒤 화면까지 그대로 따라간다. AI가 나눈 결과가 틀렸을 때
 * 대조할 것이 원문뿐이다.
 */
data class VisitNoteUiState(
    val visit: VisitHeadline,
    val note: String = "",
    /**
     * 음성으로 적는 중인지. 꺼져 있으면 [MedicalMateVoiceState.IDLE]이 아니라 null이다.
     *
     * 증상 문답은 글과 음성이 입력 자리를 번갈아 차지하지만(1c-2 대 1c-3) 여기는 적던 글이
     * 그대로 남아 있고 음성 패널이 그 아래 선다. 그래서 "지금 음성인가"를 따로 든다.
     */
    val voice: MedicalMateVoiceState? = null,
) {
    val canSave: Boolean = note.isNotBlank()
}

/**
 * 어떤 진료를 적는 것인지. "오늘 진료 / 서울OO병원 내과 · 9월 12일 / 복부 통증 · 3주 브리핑
 * 카드로 진료받았어요"의 재료다.
 *
 * 완성된 문장을 담지 않는다. 앞말과 뒷말은 문자열 리소스에 있고 조립은 화면이 한다.
 *
 * [clinic]은 1m에서 고른 병원, [cardTitle]은 이 기록이 붙을 카드의 제목이다. 둘 다 캘린더
 * 일자에서 라우트를 타고 따라온다. 없으면 그 줄을 그리지 않는다.
 */
/**
 * 1p 머리말.
 *
 * [today]는 진료가 오늘이었는지다. 시안의 "오늘 진료"는 그 날 바로 적는 경우를 그린 것이고,
 * 어제 진료를 오늘 적으면 거짓이 된다. 화면이 그 낱말을 이 값으로 고른다.
 */
data class VisitHeadline(
    val visitedOn: LocalDate,
    val clinic: String? = null,
    val cardTitle: String? = null,
    val today: Boolean = true,
)

/**
 * 1q-1 자동 분류 결과와 1q-1-E 전체 수정. Figma `405:2193`, `636:3675`.
 *
 * 한 화면의 두 모드다. 편집이 켜지면 값이 그 자리에서 입력으로 바뀌고 하단이 삭제로 바뀐다.
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
 * 빠질 수 있기 때문이다. [classifiedCount]가 AI가 몇 가지로 나눴는지다. AI를 거치지 않았거나
 * 나눈 것이 없으면 null이고, 그때 화면은 그 줄을 그리지 않는다. 문구가 아니라 수로 드는 것은
 * 카피가 문자열 리소스에 있어야 하기 때문이다.
 *
 * [clinic]은 병원 이름만이고 [clinicLine]은 화면에 그리는 "서울OO병원 · 2026.09.12"다. 저장할
 * 때 이름만 필요해서 날짜가 붙기 전 값을 따로 든다.
 */
data class VisitRecord(
    val id: String,
    val clinic: String?,
    val clinicLine: String,
    val items: List<VisitRecordItem>,
    val memo: String,
    val classifiedCount: Int? = null,
    /** 어느 항목에도 들어가지 않은 문장. 저장 요청에 그대로 실린다. */
    val patientNotes: List<String> = emptyList(),
    /** AI가 뽑은 재방문 날짜. 저장 요청에 실린다. 일정은 이 값으로 생기지 않는다. */
    val followUp: VisitFollowUp? = null,
)

/**
 * 분류 결과의 한 줄.
 *
 * [key]는 화면에 적는 이름이고 [axis]는 서버 축 id다. 둘을 나눠 두는 이유는 항목 이름이 닫힌
 * 목록이 아니기 때문이다(#178) — AI가 축을 늘리면 이름을 모르는 줄이 생기고, 그때 표시는
 * 축 id로 하더라도 저장은 그 축으로 나가야 한다.
 *
 * [tone]이 값의 색을 정한다. 재방문 날짜는 브랜드색으로 세운다.
 */
data class VisitRecordItem(val key: String, val value: String, val tone: Tone = Tone.DEFAULT, val axis: String = "") {
    enum class Tone { DEFAULT, LINK }
}
