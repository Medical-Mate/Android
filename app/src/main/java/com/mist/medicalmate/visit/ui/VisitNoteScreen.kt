package com.mist.medicalmate.visit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateCardEmphasis
import com.mist.medicalmate.core.designsystem.component.MedicalMateChip
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateFab
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle
import com.mist.medicalmate.core.designsystem.component.MedicalMateTextArea
import com.mist.medicalmate.core.designsystem.component.MedicalMateTooltip
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceInput
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 와이어프레임 1p. Figma `1185:12667`.
 *
 * 진료실에서 들은 말을 정리하지 않고 그대로 받는다. 형식을 물으면 기억이 먼저 흐려진다.
 * 나누는 일은 다음 화면에서 AI가 한다.
 *
 * "AI로 정리하기"는 지금 넘어가는 조작이 아니라 적은 것을 다듬는 조작이다. 옆의 툴팁이
 * 무엇을 하는지 알린다. 저장하기를 누르면 분류 결과(1q-1)로 간다.
 *
 * 마이크는 본문 흐름이 아니라 우하단에 뜬다. 적는 도중 어디까지 썼든 손이 닿는 자리에
 * 있어야 하는 조작인데, 본문 끝에 두면 스크롤을 내려야 나온다. 캘린더의 일정 추가와 같은
 * 배치다.
 */
@Composable
fun VisitNoteScreen(state: VisitNoteUiState, callbacks: VisitNoteCallbacks, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.visit_note_title),
            onLeadingClick = callbacks.onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        Box(modifier = Modifier.weight(1f)) {
            NoteContent(state = state, callbacks = callbacks)
            // 듣는 중에는 마이크를 패널이 들고 있다. 같은 조작이 화면에 둘 있으면 어느 것을
            // 눌러야 멈추는지 알 수 없다.
            if (state.voice == null) {
                MedicalMateFab(
                    onClick = callbacks.onVoiceClick,
                    icon = MedicalMateIcons.Mic,
                    contentDescription = stringResource(R.string.visit_note_voice),
                    modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(MedicalMateSize.gutter),
                )
            }
        }
        MedicalMateBottomCtaBar {
            MedicalMateButton(
                label = stringResource(R.string.visit_note_save),
                onClick = callbacks.onSaveClick,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 메모 화면에서 나가는 길과 화면 안의 조작. */
data class VisitNoteCallbacks(
    val onBackClick: () -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onOrganizeClick: () -> Unit = {},
    val onVoiceClick: () -> Unit = {},
    val onTypeInsteadClick: () -> Unit = {},
    val onSaveClick: () -> Unit = {},
)

@Composable
private fun NoteContent(state: VisitNoteUiState, callbacks: VisitNoteCallbacks) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s16),
    ) {
        Heading()
        VisitCard(state.visit)
        MedicalMateTextArea(
            value = state.note,
            onValueChange = callbacks.onNoteChange,
            placeholder = stringResource(R.string.visit_note_placeholder),
            enabled = !state.organizing,
            maxLength = NOTE_MAX_LENGTH,
        )
        OrganizeRow(state = state, onOrganizeClick = callbacks.onOrganizeClick)
        // 음성 패널은 적던 글 아래에 선다. 증상 문답(1c-3·1c-4)과 같은 컴포넌트다. 그쪽은
        // 입력 자리를 통째로 갈아끼우지만 여기는 적어 둔 글이 그대로 남아야 한다.
        state.voice?.let { voice ->
            MedicalMateVoiceInput(
                state = voice,
                onMicClick = callbacks.onVoiceClick,
                onTypeInsteadClick = callbacks.onTypeInsteadClick,
            )
        }
    }
}

@Composable
private fun Heading() {
    Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6)) {
        Text(
            text = stringResource(R.string.visit_note_heading),
            style = MedicalMateTheme.typography.headingL,
            color = MedicalMateTheme.colors.fgDefault,
        )
        Text(
            text = stringResource(R.string.visit_note_description),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * 어떤 진료를 적는 것인지.
 *
 * 아이콘 상자 · 병원 이름 · 무엇으로 받은 진료인지 · 구분선 · 언제인지 순이다. 1e-1의
 * "진료받을 병원" 블록과 뼈대가 같다. 같은 흐름에서 두 번 나오는 카드라 모양이 같아야 한다.
 *
 * `Hospital Card` 컴포넌트를 쓰지 않는다. 그쪽은 흰 면에 그림자이고 아래가 칩 pill인데,
 * 여기는 조용한 면에 평평한 글줄이다. 시안에서도 이 블록은 인스턴스가 아니라 프레임이다.
 *
 * 병원을 고르지 않았으면 그리지 않는다. 진료 후 기록은 병원을 반드시 고르고 들어온다.
 */
@Composable
private fun VisitCard(visit: VisitHeadline) {
    val clinic = visit.clinic ?: return
    val colors = MedicalMateTheme.colors
    MedicalMateCard(emphasis = MedicalMateCardEmphasis.QUIET) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                Modifier
                    .size(MedicalMateSize.controlMd)
                    .background(color = colors.bgSurface, shape = IconBoxShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(MedicalMateIcons.Hospital),
                    contentDescription = null,
                    tint = colors.fgPrimary,
                    modifier = Modifier.size(MedicalMateSize.iconLg),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2)) {
                Text(text = clinic, style = MedicalMateTheme.typography.headingS, color = colors.fgDefault)
                visit.cardTitle?.let { title ->
                    Text(
                        text = stringResource(R.string.visit_note_headline_card, title),
                        style = MedicalMateTheme.typography.bodyS,
                        color = colors.fgSubtle,
                    )
                }
            }
        }
        MedicalMateDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4)) {
            Text(
                text =
                stringResource(
                    if (visit.today) {
                        R.string.visit_note_headline_label
                    } else {
                        R.string.visit_note_headline_label_past
                    },
                ),
                style = MedicalMateTheme.typography.labelM,
                color = colors.fgPrimary,
            )
            Text(
                text = stringResource(R.string.visit_note_headline_date, visit.visitedOn.format(HeadlineDate)),
                style = MedicalMateTheme.typography.bodyS,
                color = colors.fgSubtle,
            )
        }
    }
}

/** 아이콘 상자의 반경. `Hospital Card` 마스터와 같은 14다. */
private val IconBoxShape = RoundedCornerShape(14.dp)

/** 헤드라인의 날짜. 시안이 "9월 12일"로 적는다. */
private val HeadlineDate: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

/**
 * AI로 정리하기.
 *
 * 칩으로 둔다. 화면의 주 행동은 저장하기이고, 이것은 적은 글을 다듬는 보조 조작이다.
 * 눌러도 화면이 바뀌지 않으므로 선택 상태가 아니라 누르는 조작으로 쓴다.
 */
@Composable
private fun OrganizeRow(state: VisitNoteUiState, onOrganizeClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedicalMateChip(
            label = stringResource(R.string.visit_note_organize),
            selected = false,
            onClick = onOrganizeClick,
            enabled = state.note.isNotBlank() && !state.organizing,
        )
        // 툴팁 트리거는 블록 우측 끝에 둔다(Figma 툴팁 배치 규칙).
        MedicalMateTooltip(
            text = stringResource(R.string.visit_note_organize_tooltip),
            contentDescription = stringResource(R.string.visit_note_organize_tooltip_label),
        )
    }
}

/** 시안의 카운터가 `71 / 300`이다. */
private const val NOTE_MAX_LENGTH = 300

@MedicalMateScreenPreviews
@Composable
private fun VisitNoteScreenPreview() {
    MedicalMateTheme {
        VisitNoteScreen(
            state = VisitNoteUiState(visit = previewVisitHeadline, note = PREVIEW_VISIT_NOTE),
            callbacks = VisitNoteCallbacks(),
        )
    }
}
