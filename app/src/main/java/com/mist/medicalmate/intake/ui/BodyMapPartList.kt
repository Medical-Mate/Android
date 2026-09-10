package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateListRow
import com.mist.medicalmate.core.designsystem.component.MedicalMateRadio
import com.mist.medicalmate.core.designsystem.component.MedicalMateSectionHeader

/**
 * 부위를 목록에서 고르는 길.
 *
 * 인체도는 이미지 위의 좌표를 짚는 조작이라 스크린 리더로는 쓸 수 없다. 확대해도 손이
 * 떨리면 짚기 어렵고, 그림이 벗은 몸이라 사람 앞에서 열기 부담스러울 수도 있다. 어느
 * 이유든 같은 부위를 고를 수 있어야 한다.
 *
 * 목록도 인체도와 같은 두 단계다. 25개 구역을 좌우까지 펼치면 44줄이 되고, 그 안에서
 * 찾는 것이 그림에서 짚는 것보다 어렵다.
 *
 * **고르는 줄은 [MedicalMateRadio]다.** 한 곳만 고르는 목록이고, 라디오가 역할과 선택
 * 상태를 시맨틱에 실어서 스크린 리더가 "선택됨"을 읽는다. `List Row`로 두면 무엇이
 * 골라져 있는지 화면으로도 리더로도 알 수 없다. 앵커 줄은 고르는 것이 아니라 하위
 * 목록으로 들어가는 이동이라 chevron이 있는 `List Row`를 그대로 쓴다.
 */
@Composable
internal fun BodyMapPartList(state: BodyMapUiState, callbacks: IntakeCallbacks, modifier: Modifier = Modifier) {
    val anchor = state.anchor
    val focus = state.focus
    Text(
        text = stringResource(R.string.body_map_list_question),
        style = MedicalMateTheme.typography.headingL,
        color = MedicalMateTheme.colors.fgDefault,
        modifier = modifier,
    )
    MedicalMateButton(
        onClick = callbacks.onBodyListModeToggle,
        label = stringResource(R.string.body_map_use_image),
        type = MedicalMateButtonType.OUTLINE,
        size = MedicalMateButtonSize.M,
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.pickingZone && anchor != null && focus != null) {
        ZoneRows(anchor = anchor, focus = focus, selection = state.selection, callbacks = callbacks)
    } else {
        AnchorRows(selection = state.selection, callbacks = callbacks)
    }
}

/**
 * 앵커 9개. 팔·다리는 좌우가 갈려서 두 줄이다.
 *
 * 구역이 있는 앵커는 눌러도 골라지지 않고 그 앵커의 구역 목록으로 들어간다. 그 안에서
 * 고른 부위 이름을 줄의 보조 텍스트에 적는다. 들어가지 않고도 무엇이 골라져 있는지
 * 보여야 하고, 앵커 이름만으로는 "다리"까지만 알 수 있다.
 *
 * 전신·피부는 구역이 없어서 그 줄이 곧 선택이다. 라디오로 둔다.
 */
@Composable
private fun AnchorRows(selection: BodyMapSelection?, callbacks: IntakeCallbacks) {
    bodyMapAnchors.forEach { anchor ->
        if (anchor.zones.isEmpty()) {
            val choice = BodyMapSelection(anchor.id)
            MedicalMateRadio(
                selected = selection == choice,
                onSelect = { callbacks.onBodySideAnchorClick(anchor.id) },
                label = choice.title(),
            )
        } else {
            anchor.points.forEach { point ->
                AnchorRow(
                    entry = BodyMapSelection(anchor.id, side = point.side),
                    selection = selection,
                    onClick = callbacks.onBodyAnchorFocus,
                )
            }
        }
    }
}

/** 하위 목록으로 들어가는 앵커 한 줄. 그 안에서 고른 부위가 있으면 이름을 함께 적는다. */
@Composable
private fun AnchorRow(entry: BodyMapSelection, selection: BodyMapSelection?, onClick: (BodyMapSelection) -> Unit) {
    val picked = selection?.takeIf { it.belongsTo(entry.anchorId, entry.side) }
    MedicalMateListRow(
        title = entry.title(),
        meta = picked?.let { stringResource(R.string.body_map_list_picked, it.label()) },
        onClick = { onClick(entry) },
    )
}

/**
 * 고른 앵커의 구역들. 좌우가 갈리는 구역은 두 줄이다.
 *
 * 소제목에 앵커 이름을 둔다. 화면 제목이 "아픈 부위를 골라주세요"로 같아서 지금 무엇의
 * 하위 목록인지가 드러나지 않는다.
 */
@Composable
private fun ZoneRows(
    anchor: BodyMapAnchorGeometry,
    focus: BodyMapSelection,
    selection: BodyMapSelection?,
    callbacks: IntakeCallbacks,
) {
    MedicalMateSectionHeader(title = focus.title())
    bodyMapZoneChoices(anchor, focus.side).forEach { choice ->
        MedicalMateRadio(
            selected = selection == choice,
            onSelect = { callbacks.onBodyPartSelect(choice) },
            label = choice.title(),
        )
    }
    MedicalMateButton(
        onClick = callbacks.onBodyFocusClear,
        label = stringResource(R.string.body_map_other_anchor),
        type = MedicalMateButtonType.OUTLINE,
        size = MedicalMateButtonSize.M,
        modifier = Modifier.fillMaxWidth(),
    )
}
