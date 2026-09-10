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

/**
 * 부위를 목록에서 고르는 길.
 *
 * 인체도는 이미지 위의 좌표를 짚는 조작이라 스크린 리더로는 쓸 수 없다. 확대해도 손이
 * 떨리면 짚기 어렵고, 그림이 벗은 몸이라 사람 앞에서 열기 부담스러울 수도 있다. 어느
 * 이유든 같은 부위를 고를 수 있어야 한다.
 *
 * 목록도 인체도와 같은 두 단계다. 25개 구역을 좌우까지 펼치면 44줄이 되고, 그 안에서
 * 찾는 것이 그림에서 짚는 것보다 어렵다.
 */
@Composable
internal fun BodyMapPartList(state: BodyMapUiState, callbacks: IntakeCallbacks, modifier: Modifier = Modifier) {
    val anchor = state.anchor
    Text(
        text = stringResource(R.string.body_map_list_question),
        style = MedicalMateTheme.typography.headingL,
        color = MedicalMateTheme.colors.fgDefault,
        modifier = modifier,
    )
    if (state.pickingZone && anchor != null) {
        ZoneRows(anchor = anchor, state = state, callbacks = callbacks)
    } else {
        AnchorRows(callbacks = callbacks)
    }
    BodyMapImageModeButton(callbacks = callbacks)
}

/** 앵커 9개. 팔·다리는 좌우가 갈려서 두 줄이다. */
@Composable
private fun AnchorRows(callbacks: IntakeCallbacks) {
    bodyMapAnchors.forEach { anchor ->
        if (anchor.view == null) {
            PartRow(BodyMapSelection(anchor.id), callbacks)
        } else {
            anchor.points.forEach { point ->
                PartRow(BodyMapSelection(anchor.id, side = point.side), callbacks)
            }
        }
    }
}

/** 고른 앵커의 구역들. 좌우가 갈리는 구역은 두 줄이다. */
@Composable
private fun ZoneRows(anchor: BodyMapAnchorGeometry, state: BodyMapUiState, callbacks: IntakeCallbacks) {
    anchor.zones.forEach { zone ->
        zone.points.forEach { point ->
            val side = if (point.side == BodyMapSide.BASE) state.selection?.side ?: point.side else point.side
            PartRow(BodyMapSelection(anchor.id, zone.id, side), callbacks)
        }
    }
    MedicalMateButton(
        onClick = callbacks.onBodyAnchorReset,
        label = stringResource(R.string.body_map_reset_anchor),
        type = MedicalMateButtonType.OUTLINE,
        size = MedicalMateButtonSize.M,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 부위 한 줄. */
@Composable
private fun PartRow(selection: BodyMapSelection, callbacks: IntakeCallbacks) {
    MedicalMateListRow(
        title = selection.title(),
        onClick = { callbacks.onBodyPartSelect(selection) },
    )
}
