package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateChip
import com.mist.medicalmate.core.designsystem.component.MedicalMateSegmentedControl

/**
 * 1단계 아픈 부위. 와이어프레임 1l-1(앞면) · 1l-3(뒷면) · 1l-2(세부 구역).
 *
 * 세 화면이 한 단계에 들어 있다. 앞면과 뒷면은 토글이고, 구역은 앵커를 짚으면
 * 이어진다. 목적지를 나누면 진행 표시가 `증상 문답 1 / 4`에서 어긋난다.
 *
 * 이미지 위의 좌표를 짚는 조작이라 스크린 리더로 쓸 수 없다. 그래서 같은 부위를
 * 목록에서 고르는 길을 함께 둔다([BodyMapPartList]). 음성 입력에 글 대안을 두는 것과
 * 같은 성격이다(DESIGN.md 9절).
 */
@Composable
internal fun BodyPartStep(state: IntakeUiState, callbacks: IntakeCallbacks, modifier: Modifier = Modifier) {
    val bodyMap = state.bodyMap
    StepContent(step = state.step, modifier = modifier) {
        if (bodyMap.byList) {
            BodyMapPartList(state = bodyMap, callbacks = callbacks)
        } else if (bodyMap.pickingZone) {
            ZonePicker(state = bodyMap, callbacks = callbacks)
        } else {
            AnchorPicker(state = bodyMap, callbacks = callbacks)
        }
    }
}

/** 앵커 단계. 전신에서 큰 부위 하나를 짚는다. */
@Composable
private fun AnchorPicker(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    val image = state.bodyImage
    val dots = bodyMapAnchorDots(state.view, state.selection)
    Text(
        text = stringResource(R.string.intake_body_part_question),
        style = MedicalMateTheme.typography.headingL,
        color = MedicalMateTheme.colors.fgDefault,
    )
    Text(
        text = stringResource(R.string.intake_body_part_description),
        style = MedicalMateTheme.typography.bodyM,
        color = MedicalMateTheme.colors.fgSubtle,
    )
    MedicalMateSegmentedControl(
        options = bodyMapViewOptions(),
        selectedIndex = state.view.ordinal,
        onSelect = { callbacks.onBodyViewChange(BodyMapView.entries[it]) },
    )
    BodyMapCard(
        height = bodyMapBodyHeight(),
        orientationLabels = image.view == BodyMapView.FRONT,
        caption = state.selection?.label(),
    ) {
        BodyMapCanvas(
            image = image,
            dots = dots,
            onDotClick = { callbacks.onBodyDotClick(it) },
            modifier = Modifier.fillMaxSize(),
        )
    }
    SideAnchorChips(state = state, callbacks = callbacks)
    ListModeButton(byList = false, callbacks = callbacks)
}

/** 구역 단계. 짚은 앵커를 확대해 세부 구역을 고른다. 1l-2다. */
@Composable
private fun ZonePicker(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    val anchor = state.anchor
    val detail = anchor?.detail
    val selection = state.selection
    if (anchor == null || detail == null || selection == null) return
    val dots = bodyMapZoneDots(anchor, selection.side, selection)
    Text(
        text = stringResource(R.string.body_map_zone_question, selection.title()),
        style = MedicalMateTheme.typography.headingL,
        color = MedicalMateTheme.colors.fgDefault,
    )
    Text(
        text = stringResource(R.string.body_map_zone_description),
        style = MedicalMateTheme.typography.bodyM,
        color = MedicalMateTheme.colors.fgSubtle,
    )
    BodyMapCard(
        height = bodyMapCardHeight(dots, detail),
        orientationLabels = detail.view == BodyMapView.FRONT && !detail.mirrored,
        caption = selection.zoneId?.let { selection.label() },
    ) {
        BodyMapCanvas(
            image = detail,
            dots = dots,
            onDotClick = { callbacks.onBodyDotClick(it) },
            mirrored = detail.mirrored && selection.side == BodyMapSide.LEFT,
            modifier = Modifier.fillMaxSize(),
        )
    }
    MedicalMateButton(
        onClick = callbacks.onBodyAnchorReset,
        label = stringResource(R.string.body_map_reset_anchor),
        type = MedicalMateButtonType.OUTLINE,
        size = MedicalMateButtonSize.M,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * 전신 · 피부 칩.
 *
 * 인체도에 점으로 찍을 수 없는 앵커 둘이다. 고르면 구역 단계가 없어서 바로 정해진다.
 */
@Composable
private fun SideAnchorChips(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8)) {
        bodyMapSideAnchors.forEach { anchor ->
            MedicalMateChip(
                label = bodyMapLabelOf(anchor.id),
                selected = state.selection?.anchorId == anchor.id,
                onClick = { callbacks.onBodySideAnchorClick(anchor.id) },
            )
        }
    }
}

/** 인체도와 목록을 오가는 버튼. */
@Composable
private fun ListModeButton(byList: Boolean, callbacks: IntakeCallbacks) {
    MedicalMateButton(
        onClick = callbacks.onBodyListModeToggle,
        label = stringResource(if (byList) R.string.body_map_use_image else R.string.body_map_use_list),
        type = MedicalMateButtonType.GHOST,
        size = MedicalMateButtonSize.M,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 목록 화면에서도 인체도로 돌아갈 수 있어야 한다. */
@Composable
internal fun BodyMapImageModeButton(callbacks: IntakeCallbacks) {
    ListModeButton(byList = true, callbacks = callbacks)
}

@Composable
private fun bodyMapViewOptions(): List<String> = listOf(
    stringResource(R.string.body_map_view_front),
    stringResource(R.string.body_map_view_back),
)
