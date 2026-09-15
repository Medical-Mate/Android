package com.mist.medicalmate.visit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.core.model.FollowUpScheduler
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.AXIS_FOLLOW_UP
import com.mist.medicalmate.visit.data.NewVisit
import com.mist.medicalmate.visit.data.NewVisitItem
import com.mist.medicalmate.visit.data.VisitClassification
import com.mist.medicalmate.visit.data.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 자동 분류 결과 상태 보유자.
 *
 * **분류가 아직 없다.** 메모를 소견·검사·약·재방문으로 나누는 것은 폰 안 모델이 할 일이고
 * 엔진이 붙지 않았다(#142). 그래서 [load]는 네 줄의 자리만 만들고 값을 비워 둔다. 환자가
 * 편집에서 채우거나, 엔진이 붙으면 그 자리가 채워진 채로 온다. 픽스처 문장을 넣어 두지
 * 않는 이유는 저장이 실제 서버로 나가기 때문이다. 듣지 않은 소견이 기록에 남으면 안 된다.
 *
 * 편집은 [VisitRecordDraft]에 담는다. 원본을 바로 고치면 취소했을 때 되돌릴 것이 없다.
 * 확인하면 사본을 원본으로 옮긴다. 사본을 고치는 조작은 [editActions]에 있다.
 */
@HiltViewModel
class VisitRecordViewModel
@Inject
internal constructor(
    private val repository: VisitRepository,
    private val followUpScheduler: FollowUpScheduler,
    private val clock: Clock,
) : ViewModel() {
    /** 저장 요청이 나가 있는 동안. 저장하기를 두 번 누르면 기록이 두 개 생긴다. */
    private var saving = false

    /**
     * 진료를 받은 날.
     *
     * 오늘이 아니다. 어제 진료를 오늘 적을 수 있고, 그때 기록이 오늘 날짜로 남으면 그 일자
     * 화면에는 영영 나오지 않는다. 흐름이 시작된 캘린더 일자가 라우트를 타고 온다.
     */
    private var visitedOn: LocalDate = LocalDate.now(clock)

    /**
     * 직전에 받은 분류.
     *
     * 다시 나눌 때 그대로 돌려보낸다. **안 보내면 AI 모델 호출이 다시 나가고 그 비용이 서버
     * 크레딧과 같은 주머니에서 빠진다**고 백엔드가 적었다.
     */
    private var labels: Map<String, String>? = null

    private val mutableUiState = MutableStateFlow<VisitRecordUiState>(VisitRecordUiState.Loading)
    val uiState: StateFlow<VisitRecordUiState> = mutableUiState.asStateFlow()

    /** 편집 모드가 아니면 아무것도 하지 않는다. 사본이 없는데 고칠 수는 없다. */
    val editActions =
        VisitRecordEditActions { change ->
            update { content ->
                val draft = content.draft ?: return@update content
                content.copy(draft = change(draft))
            }
        }

    /**
     * 메모를 항목으로 나눠 화면을 채운다.
     *
     * **나누지 못해도 화면은 연다.** 환자는 방금 메모를 적었고 그것이 이 흐름에서 잃으면 안
     * 되는 값이다. AI가 답하지 않았다고 화면 전체를 실패로 두면 손으로 적어 저장할 길까지
     * 막힌다. 그때는 빈 네 줄이 열리고 캡션이 붙지 않는다.
     *
     * 직전 결과의 [labels]를 함께 보낸다. 다시 부를 때 AI 모델을 부르지 않고 재조립만 한다.
     */
    fun load(clinic: String?, note: String, visitedOn: LocalDate?) {
        // 흐름이 시작된 캘린더 일자다. 없으면 오늘로 둔다.
        this.visitedOn = visitedOn ?: LocalDate.now(clock)
        mutableUiState.value = VisitRecordUiState.Loading
        viewModelScope.launch {
            val classified =
                if (note.isBlank()) {
                    null
                } else {
                    (
                        repository.classify(
                            note,
                            this@VisitRecordViewModel.visitedOn,
                            clinic,
                            labels,
                        ) as? ApiResult.Success
                        )
                        ?.value
                }
            labels = classified?.labels ?: labels
            mutableUiState.value =
                VisitRecordUiState.Content(
                    record = newRecord(clinic, note, this@VisitRecordViewModel.visitedOn, classified),
                )
        }
    }

    /** Nav 우측 `편집`. 카드 안의 모든 값을 한 번에 연다. */
    fun onEditClick() {
        update { it.copy(draft = VisitRecordDraft.of(it.record)) }
    }

    /**
     * Nav 우측 `확인`. 사본을 원본으로 옮기고 편집 모드를 닫는다.
     *
     * 서버 저장은 아직 없다. AI 분류 결과를 저장하는 API를 붙이면 여기서 호출한다.
     */
    fun onEditDoneClick() {
        update { content ->
            val draft = content.draft ?: return@update content
            content.copy(record = content.record.copy(items = draft.items), draft = null)
        }
    }

    /** Nav 우측 `취소`. 사본을 버리고 원래 값으로 돌아간다. */
    fun onCancelClick() {
        update { it.copy(draft = null) }
    }

    /**
     * 읽는 중의 저장하기. `POST /api/cards/{cardId}/visit`.
     *
     * 편집 중에는 하단에 이 버튼이 없다. 그 자리가 삭제이고 사본을 옮기는 것은 `확인`이 한다.
     *
     * [cardId]가 없으면 붙일 곳이 없어 아무것도 하지 않는다. 서버가 카드에 매달린 기록만
     * 받는다. 캘린더 일자에 카드가 걸린 일정이 없으면 여기까지 온 것 자체가 길을 잘못 든
     * 것이고, 그 자리를 막는 것은 캘린더 쪽 일이다.
     *
     * 실패하면 화면에 남는다. 일정 추가(1r-4)와 같다. 나가 버리면 적은 것이 사라지고 다시
     * 누를 수도 없다.
     *
     * **저장이 되면 재방문 날짜로 일정을 만든다**(#245). 이 화면의 재방문 줄에 날짜가 보이고
     * 고칠 수 있어서 저장이 곧 확인이다. 서버가 기록에서 일정을 만들지 않으므로 앱이 만들고,
     * 못 만들어도 저장은 성공이다 — 기록이 본체고 일정은 덧붙이는 것이다. 만들어진 일정은
     * 돌아가는 일자 화면의 "다음 일정"에 바로 선다.
     */
    fun onSaveClick(cardId: String?, onSaved: () -> Unit) {
        val content = mutableUiState.value as? VisitRecordUiState.Content
        val card = cardId?.toLongOrNull()
        if (content == null || card == null || saving) return

        saving = true
        update { it.copy(saveFailure = null) }
        viewModelScope.launch {
            val result = repository.create(card, content.record.toNewVisit(visitedOn))
            if (result is ApiResult.Success) scheduleFollowUp(content.record, card)
            saving = false
            when (result) {
                is ApiResult.Success -> onSaved()
                is ApiResult.NetworkUnavailable -> update { it.copy(saveFailure = VisitSaveFailure.RETRYABLE) }
                is ApiResult.Rejected ->
                    update {
                        it.copy(
                            saveFailure =
                            if (result.retryable) VisitSaveFailure.RETRYABLE else VisitSaveFailure.REJECTED,
                        )
                    }
            }
        }
    }

    /** 재방문 날짜가 진료일보다 뒤일 때만 일정을 만든다. 지난 날짜는 예정이 아니다. */
    private suspend fun scheduleFollowUp(record: VisitRecord, cardId: Long) {
        val on = record.followUp?.date ?: return
        if (!on.isAfter(visitedOn)) return
        followUpScheduler.schedule(record.clinic, on, cardId)
    }

    /**
     * 하단 `진료 후 기록 삭제`. 대화상자를 띄우는 것까지만 한다.
     *
     * 개체를 통째로 지우는 것이라 문서가 확인을 필수로 둔다. 원문 메모까지 사라지는 자리다.
     */
    fun onDeleteClick() {
        update { it.copy(deleteRequested = true) }
    }

    fun onDeleteDismiss() {
        update { it.copy(deleteRequested = false) }
    }

    /**
     * 대화상자의 `삭제`. 화면을 떠나는 것은 호출자가 한다.
     *
     * 서버 삭제는 아직 없다. 지금은 대화상자를 닫고 편집 모드를 내린다.
     */
    fun onDeleteConfirm() {
        update { it.copy(deleteRequested = false, draft = null) }
    }

    private fun update(transform: (VisitRecordUiState.Content) -> VisitRecordUiState.Content) {
        val content = mutableUiState.value as? VisitRecordUiState.Content ?: return
        mutableUiState.value = transform(content)
    }
}

/**
 * 빈 분류 결과 한 장.
 *
 * 네 줄의 이름을 여기 둔다. 편집에서 줄을 새로 만들 수 없어서(×로 지우기만 한다) 자리는
 * 미리 있어야 한다. 그리고 [VisitRecordItem.key]가 저장할 때 어느 서버 필드인지를 가리키는
 * 이름이기도 하다. 위치로 찾으면 한 줄을 지운 뒤에 어긋난다.
 *
 * 재방문 줄도 서버로 간다. `follow_up` 축이 생겼다(#178).
 *
 * **AI가 나눈 것만 줄이 된다**(#183). 항목이 고정이 아니다 — AI가 축을 늘릴 수도 있고 못 찾은
 * 항목은 줄이 없다.
 */
private fun newRecord(clinic: String?, note: String, today: LocalDate, classified: VisitClassification?) = VisitRecord(
    id = "",
    clinic = clinic,
    clinicLine = listOfNotNull(clinic, today.format(VISITED_ON)).joinToString(" · "),
    items = classifiedItems(classified),
    memo = note,
    classifiedCount = classified?.items?.size?.takeIf { it > 0 },
    patientNotes = classified?.patientNotes.orEmpty(),
    followUp = classified?.followUp,
)

/**
 * 그릴 줄.
 *
 * **항목이 고정이 아니다.** AI가 찾은 축만 그 차례대로 선다. 못 찾은 항목은 빈 줄이 아니라
 * 없는 줄이다 — 서버도 키 자체를 보내지 않는다. 빈 자리를 네 개 깔아 두면 AI가 무엇을 찾았고
 * 무엇을 못 찾았는지가 화면에서 지워진다.
 *
 * **하나도 못 나눴으면 줄이 없다.** 전에는 네 자리를 빈 채로 열었는데, 화면이 네 항목을
 * 묻고는 아무 답도 못 내놓는 모양이 됐다. 적은 말이 사라진 것도 아니다 — 서버가 분류되지 않은
 * 문장을 `patientNotes`로 돌려주고 원문은 `rawNote`로 저장된다. 그때는 카드 아래의 원문만
 * 남기는 편이 본 것을 그대로 말한다.
 *
 * 대신 그 기록은 손으로 채울 자리가 없다. 편집은 있는 줄을 고치고 ×로 지우기만 하고 줄을
 * 새로 만들지 못한다. 그 자리는 #184다.
 */
private fun classifiedItems(classified: VisitClassification?): List<VisitRecordItem> {
    val found = classified?.items.orEmpty()
    return found.map { item ->
        VisitRecordItem(
            key = item.label,
            value = item.value,
            tone = if (item.axis == AXIS_FOLLOW_UP) VisitRecordItem.Tone.LINK else VisitRecordItem.Tone.DEFAULT,
            axis = item.axis,
        )
    }
}

/**
 * 화면의 줄을 서버 축으로.
 *
 * **지운 줄과 비운 줄은 보내지 않는다.** 안 적은 것과 빈 문자열은 다르다. 값이 있는 줄만
 * 담고, 축이 없는 줄(있으면 안 되지만)도 뺀다.
 *
 * `status`와 `source`는 보내지 않는다. 서버가 정한다.
 */
private fun VisitRecord.toNewVisit(today: LocalDate) = NewVisit(
    clinicName = clinic,
    visitedOn = today,
    items =
    items.mapNotNull { item ->
        if (item.axis.isBlank() || item.value.isBlank()) return@mapNotNull null
        NewVisitItem(axis = item.axis, value = item.value)
    },
    followUp = followUp,
    patientNotes = patientNotes,
    rawNote = memo,
)

private val VISITED_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)
