package com.mist.medicalmate.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.R
import com.mist.medicalmate.card.data.CardListItem
import com.mist.medicalmate.card.data.CardRepository
import com.mist.medicalmate.card.data.OpenIntake
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceText
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 브리핑 카드 전체 상태 보유자.
 *
 * `GET /api/me/cards`가 최근 작성 순으로 준다. 월별로 묶는 것은 화면이 할 일이라 여기서
 * 한다. 서버가 묶어 주면 그 기준을 바꿀 때마다 배포해야 한다.
 *
 * 편집 조작이 [RecordViewModel]과 같다. 두 화면이 같은 CRUD 규칙을 쓰기 때문이다. 그래도
 * 상태 타입이 달라 합치지 않았다. 각자 다른 엔드포인트를 부른다.
 *
 * **카드를 아직 만들지 않은 문답도 한 줄로 선다**(#338, 시안 1j-4의 "무릎 통증"). 홈의 이어서 하기와 같은 문답이고
 * 서버가 늘 최근 하나만 남긴다. 칩은 증상 정리 중이면 작성 중, 질문 고르기까지 마쳤으면 임시 저장이다. 누르면 이어서 하기로
 * 가고, 편집에서 카드와 함께 골라 지운다. 그 줄의 id는 카드 id와 겹치지 않게 [INTAKE_ROW_PREFIX]를 붙인다.
 * 문답을 못 읽어도 카드 목록은 그린다 — 줄 하나 때문에 목록 전체가 실패할 일은 아니다.
 */
@HiltViewModel
class BriefCardListViewModel
@Inject
internal constructor(
    private val repository: CardRepository,
    private val clock: Clock,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<BriefCardListUiState>(BriefCardListUiState.Loading)
    val uiState: StateFlow<BriefCardListUiState> = mutableUiState.asStateFlow()

    /** 받아 온 카드. 말로 찾을 때 작성일과 병원이 필요한데 화면 줄에는 글로만 남는다(#286). */
    private var cards: List<CardListItem> = emptyList()

    /** 여럿이 걸려 되물은 후보. 다음 말이 "두 번째"면 이 안에서 센다. */
    private var candidates: List<CardListItem> = emptyList()

    /** 카드 없는 문답. 없거나 못 읽었으면 null이다. */
    private var intake: OpenIntake? = null

    /**
     * 목록을 읽는다. 화면에 돌아올 때마다 부른다 — 이어서 하기로 문답을 마치고 오면 그 줄이 카드가 돼 있다.
     *
     * 이미 그린 목록이 있으면 읽는 동안 그대로 둔다. 고르는 중(편집)이면 다시 읽지 않는다 — 고른 것이 사라진다.
     */
    fun load() {
        val current = mutableUiState.value
        if (current is BriefCardListUiState.Content && current.editing) return
        if (current !is BriefCardListUiState.Content) mutableUiState.value = BriefCardListUiState.Loading
        viewModelScope.launch {
            val result = repository.cards()
            intake = (repository.openIntake() as? ApiResult.Success)?.value
            mutableUiState.value =
                when (result) {
                    is ApiResult.Success -> {
                        cards = result.value
                        BriefCardListUiState.Content(groups = result.value.toGroups(intake, LocalDate.now(clock)))
                    }

                    is ApiResult.Rejected, is ApiResult.NetworkUnavailable -> BriefCardListUiState.Failed
                }
        }
    }

    fun onEditStart() {
        updateContent { it.copy(selectedIds = emptySet()) }
    }

    fun onEditCancel() {
        updateContent { it.copy(selectedIds = null, deleteRequested = false) }
    }

    fun onSelectChange(id: String, selected: Boolean) {
        updateContent { content ->
            val ids = content.selectedIds ?: return@updateContent content
            content.copy(selectedIds = if (selected) ids + id else ids - id)
        }
    }

    fun onDeleteClick() {
        updateContent { if (it.selectedCount == 0) it else it.copy(deleteRequested = true) }
    }

    fun onDeleteDismiss() {
        updateContent { it.copy(deleteRequested = false) }
    }

    /**
     * 고른 카드를 지운다.
     *
     * 한 장씩 `DELETE /api/cards/{id}`로 나간다(#157). 문답 줄은 `DELETE /api/sessions/{id}`다(#338). 안 지워진
     * 것은 목록에 남긴다. 빈 묶음은 함께 사라지고 편집에서 빠져나온다.
     */
    fun onDeleteConfirm() {
        val ids = (mutableUiState.value as? BriefCardListUiState.Content)?.selectedIds.orEmpty()
        if (ids.isEmpty()) return
        updateContent { it.copy(deleteRequested = false) }
        viewModelScope.launch {
            val (intakeIds, cardIds) = ids.partition { intakeSessionOf(it) != null }
            val gone = repository.deleteAll(cardIds.toSet()) +
                intakeIds.filter { id ->
                    val session = intakeSessionOf(id)
                    session != null && repository.deleteIntake(session) is ApiResult.Success
                }
            if (intakeIds.any { it in gone }) intake = null
            updateContent { content -> content.without(gone).copy(selectedIds = null) }
        }
    }

    /**
     * 말로 카드를 찾아 연다(#286). 하나면 열고, 여럿이면 후보를 적어 몇 번째인지 묻고, 없으면 알린다.
     *
     * 화면을 연 말이 넘어오면 목록을 받기 전일 수 있어 받을 때까지 기다린다. 고르는 중(편집)이면 받지 않는다.
     */
    /**
     * 카드가 하나도 없는지. 다 읽을 때까지 기다린다. 못 읽었으면 없다고 하지 않는다. 문답 줄은 세지 않는다 — 말로 열
     * 카드가 없다는 뜻이다.
     */
    internal suspend fun awaitEmpty(): Boolean {
        val state = withTimeoutOrNull(VOICE_WAIT_MS) { uiState.first { it !is BriefCardListUiState.Loading } }
        return state is BriefCardListUiState.Content && cards.isEmpty()
    }

    internal suspend fun onVoice(text: String, today: LocalDate, open: (String) -> Unit): VoiceReply {
        val state = withTimeoutOrNull(VOICE_WAIT_MS) { uiState.first { it !is BriefCardListUiState.Loading } }
        val pool = candidates.ifEmpty { cards }
        val found = pool.findByVoice(text, today)?.ifEmpty { cards.findByVoice(text, today).orEmpty() }
        val card = found?.singleOrNull()
        return when {
            state !is BriefCardListUiState.Content || state.editing -> VoiceReply.NotMine
            isCardDeleteRequest(text) -> VoiceReply.Done(R.string.voice_by_hand)
            found == null -> VoiceReply.NotMine
            card != null -> {
                candidates = emptyList()
                open(card.id)
                VoiceReply.Done(R.string.voice_card_opened, listOf(card.title))
            }

            found.isEmpty() -> VoiceReply.Done(R.string.voice_card_none, question = VoiceText(R.string.voice_card_ask))
            else -> {
                candidates = found
                VoiceReply.Done(
                    R.string.voice_card_which,
                    listOf(found.size.toString()),
                    question = VoiceText(
                        R.string.voice_card_ask_which,
                        listOf(
                            found.joinToString("\n") {
                                it.voiceLine()
                            },
                        ),
                    ),
                )
            }
        }
    }

    private fun updateContent(change: (BriefCardListUiState.Content) -> BriefCardListUiState.Content) {
        val content = mutableUiState.value as? BriefCardListUiState.Content ?: return
        mutableUiState.value = change(content)
    }
}

/**
 * 응답을 월별 묶음으로.
 *
 * 서버가 최근 작성 순으로 주므로 순서를 다시 세우지 않는다. 같은 달끼리 이어 붙이기만 한다.
 */
private fun List<CardListItem>.toGroups(intake: OpenIntake?, today: LocalDate): List<RecordGroup> =
    withIntake(intake, today)
        .groupBy { (on, _) -> on.format(MONTH_LABEL) }
        .map { (label, rows) -> RecordGroup(monthLabel = label, items = rows.map { it.second }) }

/**
 * 카드 줄 사이에 문답 줄을 날짜순으로 끼운다. 카드는 서버가 최근 작성 순으로 준다.
 *
 * 같은 날이면 문답이 위다 — 진행 중인 쪽이 더 최근이다. 시작 시각이 없으면 맨 위 묶음의 맨 위에 서고 날짜는
 * 적지 않는다.
 */
private fun List<CardListItem>.withIntake(intake: OpenIntake?, today: LocalDate): List<Pair<LocalDate, RecordItem>> {
    val rows = map { it.writtenOn to it.toRow() }
    if (intake == null) return rows
    val on = intake.startedOn ?: rows.firstOrNull()?.first ?: today
    val at = rows.indexOfFirst { (written, _) -> !written.isAfter(on) }.takeIf { it >= 0 } ?: rows.size
    return rows.toMutableList().apply { add(at, on to intake.toRow(today)) }
}

/**
 * 문답 줄. 시안 1j-4의 "무릎 통증 · 오늘 · n단계 중 n단계 · 이어서 정리하기"다. 제목은 홈의 이어서 하기처럼 부위다.
 *
 * 단계는 홈의 이어서 하기처럼 끝낸 단계를 센다(사용자 결정). 세션은 강도를 고른 뒤에 생기므로(#310) 문답 중이면
 * 두 단계를 끝냈고, 질문 고르기까지 마친 임시 저장이면 세 단계를 다 끝냈다.
 */
private fun OpenIntake.toRow(today: LocalDate) = RecordItem(
    id = INTAKE_ROW_PREFIX + sessionId,
    title = site ?: INTAKE_TITLE,
    status = if (tempSaved) RecordItem.Status.TEMP_SAVED else RecordItem.Status.IN_PROGRESS,
    meta = listOfNotNull(
        startedOn?.let { if (it == today) TODAY else it.format(WRITTEN_ON) },
        "${IntakeStep.total}단계 중 ${if (tempSaved) IntakeStep.total else IntakeStep.SYMPTOM_CHAT.number - 1}단계",
    ).joinToString(" · "),
    resumeLabel = RESUME_LABEL,
)

/** 문답 줄의 세션. 그 줄이 아니면 null이다. 누르면 화면이 이어서 하기로 보낸다. */
internal fun intakeSessionOf(id: String): Long? = id.removePrefix(INTAKE_ROW_PREFIX)
    .takeIf { id.startsWith(INTAKE_ROW_PREFIX) }
    ?.toLongOrNull()

/** 문답 줄 id의 머리. 카드 id는 숫자라 겹치지 않는다. */
internal const val INTAKE_ROW_PREFIX = "intake:"

private const val INTAKE_TITLE = "증상 정리"

private const val TODAY = "오늘"

private const val RESUME_LABEL = "이어서 정리하기"

/**
 * 목록의 한 줄.
 *
 * 진료를 마쳤으면 진료 완료, 아니면 진료 전이다. 문서가 "진료 완료" 뱃지는 `visited`로 판단하라고 적었다.
 * 카드는 만들 때부터 확정돼 있다(Backend#181).
 * 병원명은 선택 입력이라 진료를 마쳤어도 비어 있을 수 있다.
 *
 * 보조 줄([RecordItem.detail])은 비워 둔다. 목록 응답에 본문이 담기지 않아서 카드가 무엇을
 * 담았는지 알 수 없다. 상세를 열어야 나온다.
 */
private fun CardListItem.toRow() = RecordItem(
    id = id,
    title = title,
    status = if (visited) RecordItem.Status.CONFIRMED else RecordItem.Status.BEFORE_VISIT,
    clinic = clinic,
    // 병원을 안 정한 카드는 "병원 미정"이다. 서버 문서가 그렇게 찍으라고 적었다(Backend#101).
    meta = listOf(writtenOn.format(WRITTEN_ON) + " 작성", clinic ?: "병원 미정").joinToString(" · "),
)

private val MONTH_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)

private val WRITTEN_ON: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREAN)

/**
 * 지워진 것만 목록에서 뺀다.
 *
 * 여러 장을 한 번에 지울 때 일부만 실패할 수 있다. 실패한 것을 함께 빼면 지워지지 않은
 * 카드가 지워진 것처럼 보이고, 다시 열었을 때 되살아난 것으로 읽힌다.
 */
private fun BriefCardListUiState.Content.without(ids: Set<String>): BriefCardListUiState.Content = copy(
    groups =
    groups
        .map { group -> group.copy(items = group.items.filterNot { it.id in ids }) }
        .filter { it.items.isNotEmpty() },
)

/**
 * Figma 1j-4(`1122:4830`)의 목록. Preview와 픽스처가 함께 쓴다.
 *
 * 1j-1과 같은 카드인데 보조 줄이 다르다. 기록은 진료에서 들은 것을 적고 여기는 카드가 담은
 * 항목을 적는다. 서버 연동 시 삭제한다.
 */
internal val previewBriefCardGroups =
    listOf(
        RecordGroup(
            monthLabel = "2026년 9월",
            items =
            listOf(
                RecordItem(
                    id = "card-1",
                    title = "복부 통증 · 3주",
                    status = RecordItem.Status.CONFIRMED,
                    meta = "09.04 작성 · 서울OO병원 내과",
                    detail = "부위 · 기간 · 양상 · 복용약 · 기저질환",
                ),
                RecordItem(
                    id = "card-3",
                    title = "무릎 통증",
                    status = RecordItem.Status.IN_PROGRESS,
                    meta = "오늘 · 4단계 중 2단계",
                    resumeLabel = "이어서 정리하기",
                ),
            ),
        ),
        RecordGroup(
            monthLabel = "2026년 8월",
            items =
            listOf(
                RecordItem(
                    id = "card-2",
                    title = "두통 · 잦은 어지러움",
                    status = RecordItem.Status.BEFORE_VISIT,
                    meta = "08.21 작성 · 병원 미정",
                    detail = "부위 · 기간 · 양상 · 질문 3개",
                ),
            ),
        ),
    )

/** 화면을 연 말이 넘어왔을 때 목록을 받기를 기다리는 한도. */
private const val VOICE_WAIT_MS = 5_000L
