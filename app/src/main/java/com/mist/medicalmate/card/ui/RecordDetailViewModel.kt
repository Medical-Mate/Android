package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.calendar.data.Appointment
import com.mist.medicalmate.calendar.data.AppointmentRepository
import com.mist.medicalmate.calendar.data.AppointmentStatus
import com.mist.medicalmate.card.data.CardRepository
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.visit.data.Visit
import com.mist.medicalmate.visit.data.VisitFollowUp
import com.mist.medicalmate.visit.data.VisitListItem
import com.mist.medicalmate.visit.data.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
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
 * 시안의 펼친 카드(1j-3-X)는 [recordDetailFixtures]에 남아 Preview가 그린다.
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
            val cardId = visit.cardId
            val card = cardId?.let { id -> (cardRepository.card(id) as? ApiResult.Success)?.value }
            val summaries = cardId?.let { (repository.cardVisits(it) as? ApiResult.Success)?.value }.orEmpty()
            val visits = if (summaries.size <= 1) listOf(visit) else history(summaries, visit)
            mutableUiState.value =
                RecordDetailUiState.Content(
                    recordDetail(
                        visits = visits,
                        card = card,
                        // 예정은 이 진료보다 뒤의 것이다. 잡아 둔 일정이 있으면 그것, 없으면
                        // 기록의 재방문 날짜다 — 캘린더가 점을 찍는 값과 같다.
                        pending =
                        nextVisit(cardId, visit.visitedOn)?.toPending()
                            ?: visit.followUp?.takeIf { it.date > (visit.visitedOn ?: LocalDate.MIN) }?.toPending(),
                        // 카드 응답의 제목이 비어 있을 수 있다. 목록이 카드를 만들 때 박아 둔
                        // 제목을 들고 있어서(1j-1의 줄과 같은 값) 그것으로 받친다.
                        cardTitle = summaries.firstNotNullOfOrNull { it.cardTitle.takeIf(String::isNotBlank) },
                    ),
                )
        }
    }

    /**
     * 이 카드에 쌓인 기록 전부. 최근 진료일이 앞이다.
     *
     * **목록을 `cardId`로 거르지 않는다**(Backend#121). 재방문 전에 카드를 고치면 첫 기록과
     * 두 번째 기록이 서로 다른 카드 행에 붙고, 목록이 주는 `cardId`는 최신 버전이라 묶을
     * 열쇠가 되지 못한다. 서버가 문답 단위로 모아 주는 경로를 쓴다.
     *
     * 모아 주는 목록에는 축이 없어서 기록마다 한 번씩 더 읽는다. 재방문 횟수만큼이고 보통
     * 한두 번이다. 읽지 못한 기록은 빼고 그린다 — 한 건 때문에 화면 전체를 실패로 두면 이미
     * 읽은 기록까지 못 보게 된다.
     *
     * 모으지 못하면 열어 본 기록 하나만 그린다. 지금까지 하던 것과 같다.
     */
    private suspend fun history(summaries: List<VisitListItem>, opened: Visit): List<Visit> =
        summaries.mapNotNull { summary ->
            if (summary.id == opened.id) {
                opened
            } else {
                summary.id.toLongOrNull()?.let { (repository.visit(it) as? ApiResult.Success)?.value }
            }
        }

    /**
     * 그 카드로 잡힌 다음 일정.
     *
     * 재방문 날짜가 기록 응답에 없어서(Backend#82) 일정에서 찾는다. 같은 카드에 걸린 앞으로의
     * 일정이 그것이다. 없으면 예정 단계를 두지 않는다 — 시안의 점선 블록은 다음이 잡혔을 때
     * 나오는 것이고, 안 잡힌 상태를 알리는 자리가 아니다.
     */
    private suspend fun nextVisit(cardId: Long?, after: LocalDate?): Appointment? {
        if (cardId == null) return null
        return (appointments.upcoming() as? ApiResult.Success)
            ?.value
            ?.firstOrNull { appointment ->
                appointment.cards.any { it.id == cardId } &&
                    appointment.status != AppointmentStatus.CANCELED &&
                    // 이 진료 자체의 일정은 다음이 아니다. 서버의 "앞으로의 일정"이 오늘 것을
                    // 하루 종일 담아서(Backend#123) 진료한 날 열면 그 날 일정이 예정으로 섰다.
                    (after == null || appointment.on > after)
            }
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
 * 값이 없는 줄은 만들지 않는다. 환자가 적지 않은 것을 빈 줄로 남기면 무엇을 안 적었는지가
 * 아니라 무엇이 비었는지로 읽힌다.
 */
private fun recordDetail(
    visits: List<Visit>,
    card: BriefCard?,
    pending: RecordStep.Pending?,
    cardTitle: String?,
): RecordDetail {
    val newest = visits.first()
    val records = visits.mapIndexed { index, visit -> visit.toRecordStep(visitKind(visits.size, index)) }
    return RecordDetail(
        id = newest.id,
        // 머리 제목은 시안(`1j-3`)대로 브리핑 카드의 제목이다. 카드 응답의 제목이 비면 목록이
        // 든 제목으로, 그것도 없으면 병원으로 받친다 — 병원은 둘째 줄이 적는 값이라 제목에
        // 서면 같은 말이 두 줄에 이어 나온다.
        title = card?.title?.takeIf { it.isNotBlank() } ?: cardTitle ?: newest.clinic.orEmpty(),
        status = RecordItem.Status.CONFIRMED,
        // 몇 번 다녀왔는지는 상태가 아니라 세어 봐야 아는 값이다. 한 번이면 뱃지가 상태를 그린다.
        badge = if (visits.size > 1) "진료 ${visits.size}회" else null,
        clinicLine = clinicLine(visits),
        steps = listOfNotNull(pending) + records + listOfNotNull(card?.toStep()),
    )
}

/**
 * 머리글 둘째 줄.
 *
 * **병원이 앞이고 날짜가 뒤다.** 한 번이면 `서울OO병원 내과 · 09.12 진료`로 시안 `1j-3`
 * 그대로이고, 여러 번이면 `서울OO병원 내과 · 09.12 초진 · 09.26 재방문`으로 `1j-3-R`을
 * 따른다. 두 시안이 같은 차례라 한 번일 때만 날짜를 앞에 두던 것을 걷어냈다(#243).
 *
 * 여러 번인 쪽은 오래된 차례다. 타임라인은 최신이 위인데 이 줄만 반대인 이유는, 여기가
 * 흘러온 순서를 한 줄로 읽는 자리이기 때문이다.
 */
private fun clinicLine(visits: List<Visit>): String {
    val clinic = visits.firstNotNullOfOrNull { visit -> visit.clinic?.takeIf { it.isNotBlank() } }
    val days =
        if (visits.size == 1) {
            listOfNotNull(visits.first().visitedOn?.format(VISITED_ON)?.let { "$it 진료" })
        } else {
            visits.reversed().mapIndexedNotNull { index, visit ->
                visit.visitedOn?.format(VISITED_ON)?.let { "$it ${if (index == 0) "초진" else "재방문"}" }
            }
        }
    return (listOfNotNull(clinic) + days).joinToString(" · ")
}

/**
 * 몇 번째 진료인지. 한 건이면 붙이지 않는다.
 *
 * [index]는 최신이 0이라 가장 오래된 것이 초진이다.
 */
private fun visitKind(count: Int, index: Int): String? = when {
    count <= 1 -> null
    index == count - 1 -> "초진"
    else -> "재방문"
}

/**
 * 기록 하나를 타임라인 단계로.
 *
 * **원문 인용을 담지 않는다.** 시안의 진료 후 기록 단계에는 저장된 항목만 있다. 원문은
 * 1q-1에서 확인하고 저장하는 값이고, 여기는 나중에 다시 읽는 자리다.
 */
private fun Visit.toRecordStep(kind: String?) = RecordStep.Block(
    // 항목이 가변이다(#178). 이름과 차례는 저장소가 정하고 여기는 그대로 편다.
    at = listOfNotNull(visitedOn?.format(VISITED_ON), "진료 후 기록", kind).joinToString(" · "),
    // 블록 제목은 시점 줄과 같은 말이다. 시안이 그렇고, 다른 이름을 붙이면 같은 것을 두 이름으로
    // 부르게 된다.
    title = "진료 후 기록",
    items = items.map { RecordDetailItem(key = it.label, value = it.value) },
)

/**
 * 다음 일정을 예정 단계로.
 *
 * 시안(`1j-3`)의 알림 블록이다. 시점 줄이 `09.26 예정`이고, 알림은 "다음 진료가 예약돼
 * 있어요" 아래에 `9월 26일 (토) 오전 10:30`을 적는다. 병원은 적지 않는다 — 머리글 둘째 줄이
 * 이미 말하고 있고, 같은 카드의 재방문이라 다른 곳일 리가 없다.
 */
private fun Appointment.toPending() = RecordStep.Pending(
    at = on.format(PENDING_AT),
    message = "다음 진료가 예약돼 있어요",
    // 시각이 없으면 날짜만 적는다. 시간 미정인 일정이다(#202).
    detail = listOfNotNull(on.format(PENDING_DATE), time?.format(PENDING_TIME)).joinToString(" "),
)

/**
 * 아직 일정으로 잡지 않은 재방문을 예정 단계로.
 *
 * 기록에 "일주일 뒤"라고 남긴 날짜다. 캘린더가 같은 값으로 점을 찍는데 상세에는 없어서, 달력은
 * 22일이고 상세는 다른 날을 가리키는 일이 있었다(#245). 잡아 둔 일정이 아니라 예약됐다고 적지
 * 않고, 범위로 말한 것이면 "전후"를 붙인다.
 */
private fun VisitFollowUp.toPending() = RecordStep.Pending(
    at = date.format(PENDING_AT),
    message = "재방문 예정이에요",
    detail = date.format(PENDING_DATE) + if (approximate) " 전후" else "",
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
    // 시점 줄은 `09.04 작성`이다. 시안은 뒤에 `09.12 진료실에서 보여줌`을 잇지만 그 날짜는
    // 바로 위 진료 후 기록 단계가 이미 적고 있어서 같은 날이 두 줄에 서게 돼 뺐다(#243).
    // 작성일이 없을 때만 이름을 적는다.
    at = writtenOn?.format(VISITED_ON)?.let { "$it 작성" } ?: "브리핑 카드",
    title = "브리핑 카드",
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

/** 예정 알림 본문의 날짜. `9월 26일 (토)`. */
private val PENDING_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

private val VISITED_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREAN)
