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
import com.mist.medicalmate.core.designsystem.component.MedicalMateNotice
import com.mist.medicalmate.core.designsystem.component.MedicalMateSegmentedControl

/**
 * 1단계 아픈 부위. 와이어프레임 1l-1(앞면) · 1l-3(뒷면) · 1l-2(세부 구역).
 *
 * 세 화면이 한 단계에 들어 있다. 앞면과 뒷면은 토글이고, 구역은 앵커를 짚으면
 * 이어진다. 목적지를 나누면 진행 표시가 `증상 문답 1 / 4`에서 어긋난다.
 *
 * **세부 구역은 여러 곳을 고를 수 있다.** 아픈 곳이 한 군데로 끝나지 않는 경우가 많고,
 * 문답이 그 여러 곳을 전제로 시작해야 한다. 앵커는 고르는 대상이 아니라 확대해 들어가는
 * 입구라 하나만 열린다. 다른 앵커의 구역을 고르려면 앵커 화면으로 돌아가 다시 짚는다.
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

/** 앵커 단계. 전신에서 확대할 부위를 짚는다. */
@Composable
private fun AnchorPicker(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    val image = state.bodyImage
    val dots = bodyMapAnchorDots(state.view, state.selected)
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
        caption = selectedCountCaption(state.selected.size),
    ) {
        BodyMapCanvas(
            image = image,
            dots = dots,
            onDotClick = callbacks.onBodyDotClick,
            modifier = Modifier.fillMaxSize(),
        )
    }
    SideAnchorChips(state = state, callbacks = callbacks)
    BodyMapSelectedParts(selected = state.selected, callbacks = callbacks)
    DepartmentNotice(selected = state.selected)
    ListModeButton(byList = false, callbacks = callbacks)
}

/** 구역 단계. 짚은 앵커를 확대해 세부 구역을 고른다. 1l-2다. */
@Composable
private fun ZonePicker(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    val anchor = state.anchor
    val detail = anchor?.detail
    val focus = state.focus
    if (anchor == null || detail == null || focus == null) return
    val dots = bodyMapZoneDots(anchor, focus.side, state.selected)
    Text(
        text = stringResource(R.string.body_map_zone_question, focus.title()),
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
        caption = state.selectedInFocus.takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.label() },
    ) {
        BodyMapCanvas(
            image = detail,
            dots = dots,
            onDotClick = callbacks.onBodyDotClick,
            mirrored = detail.mirrored && focus.side == BodyMapSide.LEFT,
            modifier = Modifier.fillMaxSize(),
        )
    }
    BodyMapSelectedParts(selected = state.selected, callbacks = callbacks)
    DepartmentNotice(selected = state.selected)
    MedicalMateButton(
        onClick = callbacks.onBodyFocusClear,
        label = stringResource(R.string.body_map_other_anchor),
        type = MedicalMateButtonType.OUTLINE,
        size = MedicalMateButtonSize.M,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * 전신 · 피부 칩.
 *
 * 인체도에 점으로 찍을 수 없는 앵커 둘이다. 구역이 없어서 확대할 것도 없고, 누르면 바로
 * 골라진다. 다시 누르면 빠진다.
 */
@Composable
private fun SideAnchorChips(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8)) {
        bodyMapSideAnchors.forEach { anchor ->
            val selection = BodyMapSelection(anchor.id)
            MedicalMateChip(
                label = bodyMapLabelOf(anchor.id),
                selected = selection in state.selected,
                onClick = { callbacks.onBodyPartToggle(selection) },
            )
        }
    }
}

/**
 * 진료과 안내.
 *
 * 온톨로지가 부위마다 진료과를 들고 있다. 어느 과에 가야 하는지가 환자의 첫 질문이라
 * 부위를 고른 자리에서 바로 알려준다. 값은 서버가 주고 규칙은
 * [BodyMapSelection.departments]에 있다.
 *
 * 여러 곳을 고르면 과가 늘어난다. 부위별로 어느 과인지는 [BodyMapSelectedParts]의 줄에
 * 있고, 여기는 갈 수 있는 과 전체를 겹치지 않게 모아 보여준다.
 */
@Composable
private fun DepartmentNotice(selected: List<BodyMapSelection>) {
    val departments = selected.departments()
    if (departments.isEmpty()) return
    MedicalMateNotice(
        title = stringResource(R.string.body_map_department_title),
        body = stringResource(R.string.body_map_department_body, departments.joinToString(" · ")),
    )
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

/**
 * 앵커 화면 판 안의 알약.
 *
 * 이름을 다 잇지 않고 개수만 넣는다. 여러 곳을 고르면 이름이 길어져 알약이 인체도를
 * 덮는다. 이름은 판 아래 목록에 있다.
 */
@Composable
private fun selectedCountCaption(count: Int): String? =
    if (count == 0) null else stringResource(R.string.body_map_selected_caption, count)

@Composable
private fun bodyMapViewOptions(): List<String> = listOf(
    stringResource(R.string.body_map_view_front),
    stringResource(R.string.body_map_view_back),
)
