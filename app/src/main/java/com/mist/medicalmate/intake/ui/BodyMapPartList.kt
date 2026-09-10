package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadgeTone
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
 *
 * 고르고 빼는 것도 인체도와 같은 길로 보낸다([IntakeCallbacks.onBodyDotClick]). 두 길이
 * 각자 상태를 고치면 한쪽만 다중선택이 되는 일이 생긴다.
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
        AnchorRows(selected = state.selected, callbacks = callbacks)
    }
    BodyMapSelectedParts(selected = state.selected, callbacks = callbacks)
    BodyMapImageModeButton(callbacks = callbacks)
}

/**
 * 앵커 9개. 팔·다리는 좌우가 갈려서 두 줄이다.
 *
 * 구역이 있는 앵커는 눌러도 골라지지 않고 그 앵커의 구역 목록으로 들어간다. 몇 곳을
 * 골랐는지 배지로 보여주는 이유는, 들어가지 않고도 어디에 골라둔 것이 있는지 알아야
 * 하기 때문이다.
 */
@Composable
private fun AnchorRows(selected: List<BodyMapSelection>, callbacks: IntakeCallbacks) {
    bodyMapAnchors.forEach { anchor ->
        // 전신·피부는 인체도에 점이 없어서 좌표도 없다. 좌우 없는 한 줄로 낸다.
        val sides = anchor.points.map { it.side }.ifEmpty { listOf(BodyMapSide.CENTER) }
        sides.forEach { side ->
            val selection = BodyMapSelection(anchor.id, side = side)
            MedicalMateListRow(
                title = selection.title(),
                meta = selection.departments().joinToString(" · ").ifEmpty { null },
                badge =
                pickedBadge(
                    hasZones = anchor.zones.isNotEmpty(),
                    picked = selected.count { it.belongsTo(anchor.id, side) },
                ),
                badgeTone = MedicalMateBadgeTone.BRAND,
                onClick = { callbacks.onBodyDotClick(bodyMapDotId(anchor.id, side)) },
            )
        }
    }
}

/**
 * 앵커 줄의 배지.
 *
 * 구역이 있는 앵커는 그 안에서 몇 곳을 골랐는지, 없는 앵커(전신·피부)는 골랐는지를
 * 보여준다. 둘의 의미가 달라서 문구도 다르다.
 */
@Composable
private fun pickedBadge(hasZones: Boolean, picked: Int): String? = when {
    picked == 0 -> null
    hasZones -> stringResource(R.string.body_map_selected_count, picked)
    else -> stringResource(R.string.body_map_list_picked_one)
}

/** 고른 앵커의 구역들. 좌우가 갈리는 구역은 두 줄이고, 여러 줄을 함께 고를 수 있다. */
@Composable
private fun ZoneRows(anchor: BodyMapAnchorGeometry, state: BodyMapUiState, callbacks: IntakeCallbacks) {
    anchor.zones.forEach { zone ->
        zone.points.forEach { point ->
            val side = if (point.side == BodyMapSide.BASE) state.focus?.side ?: point.side else point.side
            val selection = BodyMapSelection(anchor.id, zone.id, side)
            MedicalMateListRow(
                title = selection.title(),
                meta = selection.departments().joinToString(" · ").ifEmpty { null },
                badge = if (selection in state.selected) stringResource(R.string.body_map_list_picked_one) else null,
                badgeTone = MedicalMateBadgeTone.BRAND,
                onClick = { callbacks.onBodyPartToggle(selection) },
            )
        }
    }
    MedicalMateButton(
        onClick = callbacks.onBodyFocusClear,
        label = stringResource(R.string.body_map_other_anchor),
        type = MedicalMateButtonType.OUTLINE,
        size = MedicalMateButtonSize.M,
        modifier = Modifier.fillMaxWidth(),
    )
}
