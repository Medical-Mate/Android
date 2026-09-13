package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.card.data.CardRepository
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.Visit
import com.mist.medicalmate.visit.data.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 기록 상세 상태 보유자.
 *
 * `GET /api/visits/{visitId}`를 단계 목록으로 바꾼다. 그 일이 화면이 아니라 여기 있어야
 * JVM에서 확인할 수 있다.
 *
 * 숫자가 아닌 id는 부르기 전에 [RecordDetailUiState.Failed]다. 목록에서 들어오는 경로만
 * 있어서 지금은 나지 않지만, 지워진 기록의 링크로 들어오는 경우가 이 상태가 된다.
 *
 * 시안의 여러 단계 타임라인(1j-3-X·1j-3-R)은 [recordDetailFixtures]에 남아 Preview가 그린다.
 */
@HiltViewModel
class RecordDetailViewModel
@Inject
internal constructor(
    private val repository: VisitRepository,
    private val cardRepository: CardRepository,
    private val appointments: AppointmentRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<RecordDetailUiState>(RecordDetailUiState.Loading)
    val uiState: StateFlow<RecordDetailUiState> = mutableUiState.asStateFlow()

    private val mutableExpanded = MutableStateFlow<Set<Int>>(emptySet())

    /**
     * 펼친 단계의 자리 번호.
     *
     * 화면 상태라 ViewModel이 든다. 단계 안에 두면 목록이 다시 만들어질 때마다 접힌다.
     * 단계에 id가 없어서 자리 번호로 가리킨다. 목록이 서버에서 바뀌면 번호가 어긋날 수
     * 있는데, 그때는 화면을 다시 여는 것과 같아서 접힌 채로 시작하는 편이 맞는다.
     */
    val expandedSteps: StateFlow<Set<Int>> = mutableExpanded.asStateFlow()

    /**
     * 기록 하나를 읽는다.
     *
     * **세 곳에서 읽는다.** 기록은 진료에서 들은 것뿐이고, 그 진료를 준비한 브리핑 카드와
     * 다음에 갈 일정은 다른 응답에 있다. 시안의 타임라인이 그 셋을 한 줄로 잇는다.
     *
     * 기록을 못 읽으면 실패다. 카드와 일정은 못 읽어도 그 단계만 빠진다. 기록 상세를 여는
     * 사람이 보려는 것은 진료에서 들은 말이고, 준비물이 없다고 그것까지 감출 이유가 없다.
     */
    fun load(recordId: String) {
        val visitId = recordId.toLongOrNull()
        if (visitId == null) {
            mutableUiState.value = RecordDetailUiState.Failed
            return
        }
        mutableUiState.value = RecordDetailUiState.Loading
        mutableExpanded.value = emptySet()
        viewModelScope.launch {
            val visit = (repository.visit(visitId) as? ApiResult.Success)?.value
            if (visit == null) {
                mutableUiState.value = RecordDetailUiState.Failed
                return@launch
            }
            val card = visit.cardId?.let { id -> (cardRepository.card(id) as? ApiResult.Success)?.value }
            mutableUiState.value =
                RecordDetailUiState.Content(visit.toDetail(card = card, next = nextVisit(visit.cardId)))
        }
    }

    /**
     * 그 카드로 잡힌 다음 일정.
     *
     * 재방문 날짜가 기록 응답에 없어서(Backend#82) 일정에서 찾는다. 같은 카드에 걸린 앞으로의
     * 일정이 그것이다. 없으면 예정 단계를 두지 않는다 — 시안의 점선 블록은 다음이 잡혔을 때
     * 나오는 것이고, 안 잡힌 상태를 알리는 자리가 아니다.
     */
    private suspend fun nextVisit(cardId: Long?): Appointment? {
        if (cardId == null) return null
        return (appointments.upcoming() as? ApiResult.Success)
            ?.value
            ?.firstOrNull { it.cardId == cardId && it.status != AppointmentStatus.CANCELED }
    }

    fun onExpandToggle(index: Int) {
        mutableExpanded.value =
            mutableExpanded.value.let { if (index in it) it - index else it + index }
    }
}

/**
 * 기록을 상세 타임라인으로.
 *
 * **최신이 위다.** 예정 · 진료 후 기록 · 브리핑 카드 순이다. 시안 `1j-3`은 카드가 위이고
 * `1j-3-R`(재방문 누적)은 최신이 위인데, 디자인 피드백이 "최신 기록이 맨 위로 가는게 멘탈
 * 모델"이라고 적어 둔 쪽을 따랐다.
 *
 * **원문 인용을 담지 않는다.** 시안의 진료 후 기록 단계에는 저장된 항목만 있다. 원문은
 * 1q-1에서 확인하고 저장하는 값이고, 여기는 나중에 다시 읽는 자리다.
 *
 * 값이 없는 줄은 만들지 않는다. 환자가 적지 않은 것을 빈 줄로 남기면 무엇을 안 적었는지가
 * 아니라 무엇이 비었는지로 읽힌다.
 */
private fun Visit.toDetail(card: BriefCard?, next: Appointment?): RecordDetail {
    // 항목이 가변이다(#178). 이름과 차례는 저장소가 정하고 여기는 그대로 편다.
    val items = items.map { RecordDetailItem(key = it.label, value = it.value) }
    val day = visitedOn?.format(VISITED_ON).orEmpty()
    return RecordDetail(
        id = id,
        title = card?.title?.takeIf { it.isNotBlank() } ?: clinic.orEmpty(),
        status = RecordItem.Status.CONFIRMED,
        clinicLine = listOfNotNull(visitedOn?.format(CLINIC_LINE), clinic).joinToString(" · "),
        steps =
        listOfNotNull(
            next?.toPending(),
            RecordStep.Block(at = "$day · 진료 후 기록", title = "진료에서 들은 것", items = items),
            card?.toStep(),
        ),
    )
}

/**
 * 다음 일정을 예정 단계로.
 *
 * 시안의 점선 블록이다. 날짜는 칩 자리에, 병원과 시각은 둘째 줄에 온다.
 */
private fun Appointment.toPending() = RecordStep.Pending(
    at = at.toLocalDate().format(PENDING_AT),
    message = "재방문 예약됨",
    detail = listOf(title, at.toLocalTime().format(PENDING_TIME)).joinToString(" · "),
)

/**
 * 브리핑 카드를 타임라인 단계로.
 *
 * 접으면 앞 세 줄, 펴면 나머지 줄과 강도 · 알러지 · 질문까지 나온다(1j-3-X). 세 줄인 이유는
 * 시안이 부위 · 기간 · 양상을 접힌 상태로 보여주기 때문이다.
 *
 * **건강 정보가 카드에서 온다**(#181). 전에는 카드 응답에 없어 비어 있었다. 브리핑 카드
 * 화면이 프로필에서 읽어 얹고 있었는데, 여기는 지난 진료를 다시 읽는 자리라 오늘의 프로필을
 * 얹으면 그때 먹던 약이 아니게 된다. 이제 서버가 카드에 박아 준다.
 */
private fun BriefCard.toStep() = RecordStep.Block(
    at = "브리핑 카드",
    title = "진료 전에 정리한 것",
    items = (items + health).map { RecordDetailItem(key = it.key, value = it.value) },
    card =
    RecordStepCard(
        collapsedItemCount = COLLAPSED_ITEMS,
        severity = severity,
        allergies = allergies,
        questions = questions,
    ),
)

/** 접힌 카드에 보이는 줄 수. 시안이 부위 · 기간 · 양상 셋을 보여준다. */
private const val COLLAPSED_ITEMS = 3

private val PENDING_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd 예정", Locale.KOREAN)

private val PENDING_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

private val VISITED_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREAN)

private val CLINIC_LINE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)
