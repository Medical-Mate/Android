package com.mist.medicalmate.visit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateCard
import com.mist.medicalmate.core.designsystem.component.MedicalMateCardEmphasis
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateFab
import com.mist.medicalmate.core.designsystem.component.MedicalMateFooter
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle
import com.mist.medicalmate.core.designsystem.component.MedicalMateTextArea
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceInputMate
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.voice.rememberVoiceScrollState
import com.mist.medicalmate.core.voice.voicePanelSpace
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 와이어프레임 1p. Figma `1185:12667`.
 *
 * 진료실에서 들은 말을 정리하지 않고 그대로 받는다. 형식을 물으면 기억이 먼저 흐려진다.
 * 나누는 일은 다음 화면에서 AI가 한다.
 *
 * 저장하기를 누르면 분류 결과(1q-1)로 간다. 적는 칸 아래의 "AI로 정리하기" 칩과 툴팁은 10-15 시안(1p · 1p-T)에서
 * 빠져 걷어냈다(#318). 둘 다 저장하기와 같은 곳으로 가던 진입점이었다.
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
            // 눌러야 멈추는지 알 수 없다. 음성을 쓸 수 없는 기기에서는 아예 그리지 않는다.
            if (state.voice == null && state.voiceAvailable) {
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
        MedicalMateFooter {
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
    val onVoiceClick: () -> Unit = {},
    val onTypeInsteadClick: () -> Unit = {},
    val onSaveClick: () -> Unit = {},
)

/**
 * 적는 칸이 글에 따라 자란다.
 *
 * **자란 끝을 따라간다**(#183). 키보드가 올라오면 보이는 높이가 줄고, 칸이 아래로 자라면
 * 방금 친 글자가 키보드 뒤로 들어간다. 칸 바로 아래 줄을 시야로 불러 칸의 아랫변이 늘 보이게
 * 한다 — 칸 자체를 부르면 칸이 화면보다 길어졌을 때 윗변에 맞춰져 커서가 도로 가린다.
 */
@Composable
private fun NoteContent(state: VisitNoteUiState, callbacks: VisitNoteCallbacks) {
    val bottomOfBox = remember { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    LaunchedEffect(state.note, imeBottom) {
        if (imeBottom > 0) bottomOfBox.bringIntoView()
    }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberVoiceScrollState()).voicePanelSpace()
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s16),
    ) {
        Heading()
        VisitCard(state.visit)
        // 칸의 아랫변 바로 아래를 시야로 부르는 표지. 칸 사이 간격을 더하지 않도록 칸과 한 묶음에 둔다.
        Column {
            NoteArea(state = state, callbacks = callbacks)
            Spacer(modifier = Modifier.bringIntoViewRequester(bottomOfBox))
        }
        // 받아쓰는 중에는 적던 글 아래에 `Voice Input · Mate`가 선다(1p-T, #318). 글은 시안대로 "선생님이 뭐라고
        // 하셨나요?" · "듣고 있어요"로 고정이고 눌러도 아무 일이 없다(사용자 결정). 마이크 FAB는 그동안 감춘다.
        state.voice?.let { voice ->
            val waiting = voice == MedicalMateVoiceState.IDLE || voice == MedicalMateVoiceState.LISTENING
            MedicalMateVoiceInputMate(
                state = voice,
                onClick = null,
                title = stringResource(R.string.visit_note_voice_title).takeIf { waiting },
                description = stringResource(R.string.voice_listening_title).takeIf { waiting },
                speaking = state.note,
            )
        }
    }
}

@Composable
private fun NoteArea(state: VisitNoteUiState, callbacks: VisitNoteCallbacks) {
    MedicalMateTextArea(
        value = state.note,
        onValueChange = callbacks.onNoteChange,
        // 듣는 중에 적는 칸을 누르면 받아쓰기를 끝내고 손으로 적는다. 직접 입력 대안이 같은 자리에 남는다.
        modifier = Modifier.onFocusChanged {
            if (it.isFocused &&
                state.voice != null
            ) {
                callbacks.onTypeInsteadClick()
            }
        },
        placeholder = stringResource(R.string.visit_note_placeholder),
        maxLength = VISIT_NOTE_MAX_LENGTH,
    )
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
                Text(text = clinic, style = MedicalMateTheme.typography.bodyLStrong, color = colors.fgDefault)
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
                style = MedicalMateTheme.typography.bodySStrong,
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

/** 시안의 카운터가 `71 / 300`이다. */
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
