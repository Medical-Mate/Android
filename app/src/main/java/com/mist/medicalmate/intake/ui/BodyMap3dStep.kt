package com.mist.medicalmate.intake.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateSegmentedControl
import kotlinx.coroutines.launch

/**
 * 3D 인체도로 부위를 짚는 화면. 테스트용이다(#211).
 *
 * **2D 인체도와 같은 두 단계다.** 전신에서 큰 부위를 짚으면 카메라가 그 부위로 미끄러지고,
 * 거기서 세부 구역을 고른다. 단계를 나누는 값도 2D와 같은 [BodyMapUiState.focus]라, 3D에서
 * 들어간 부위는 2D로 돌아가도 그대로 열려 있다.
 *
 * **시안에 없는 화면이다.** 디자인 시스템의 토큰과 컴포넌트만 쓰고 새 규격을 만들지 않는다.
 * 제목·앞뒤 토글·판·알약까지 2D와 배치를 맞췄다. 두 길을 견주는 것이 목적이라 화면 구성이
 * 다르면 무엇 때문에 낫고 못한지 가려지지 않는다.
 *
 * 고르는 것은 한 곳이고 값은 2D와 같은 [BodyMapSelection]이다. 그래서 여기서 골라도 다음
 * 단계와 브리핑 카드가 달라지지 않는다.
 */
@Composable
internal fun BodyMap3dStep(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    val camera = rememberBodyMap3dCamera()
    var notice by remember { mutableStateOf<Int?>(null) }
    val focus = state.focus

    // 부위가 바뀌면 카메라가 그 자리로 미끄러진다. 순간이동시키면 어디로 들어갔는지 알 수 없다.
    LaunchedEffect(focus) {
        notice = null
        val frame = focus?.let { bodyMap3dFrameOf(it.anchorId) }
        camera.glideTo(
            if (frame != null && focus != null) {
                frame.camera(focus.side, camera.value)
            } else {
                camera.value.facing(camera.value.view())
            },
        )
    }

    Heading(focus)
    if (focus != null) {
        ZoneActions(focus = focus, callbacks = callbacks)
    } else {
        AnchorActions(state = state, camera = camera, callbacks = callbacks)
    }
    BodyMapCard(
        height = bodyMapBodyHeight(),
        orientationLabels = false,
        caption = state.selection?.label(),
    ) {
        BodyMap3dBoard(
            camera = camera,
            focus = focus,
            selection = state.selection,
            onPick = { pick ->
                notice = noticeOf(pick)
                if (pick != null && pick.accepted) {
                    if (focus == null) {
                        callbacks.onBodyAnchorFocus(pick.point.toFocus())
                    } else {
                        callbacks.onBodyPartSelect(pick.point.toSelection())
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
    val message = notice
    if (message != null) {
        Text(
            text = stringResource(message),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/** 제목과 안내. 확대 중이면 그 부위를 부르고, 2D 인체도와 같은 문구를 쓴다. */
@Composable
private fun Heading(focus: BodyMapSelection?) {
    Text(
        text = if (focus != null) {
            stringResource(R.string.body_map_zone_question, focus.title())
        } else {
            stringResource(R.string.intake_body_part_question)
        },
        style = MedicalMateTheme.typography.headingL,
        color = MedicalMateTheme.colors.fgDefault,
    )
    Text(
        text = stringResource(
            if (focus != null) R.string.body_map_zone_description else R.string.body_map_3d_description,
        ),
        style = MedicalMateTheme.typography.bodyM,
        color = MedicalMateTheme.colors.fgSubtle,
    )
}

/**
 * 전신에서 부위를 짚는 동안의 조작.
 *
 * 앞뒤 토글은 카메라를 돌린다. 손가락으로 돌려 둔 상태에서도 지금 보고 있는 면이 켜져
 * 있도록 선택 위치를 카메라에서 읽는다.
 */
@Composable
private fun AnchorActions(state: BodyMapUiState, camera: BodyMap3dCameraState, callbacks: IntakeCallbacks) {
    val scope = rememberCoroutineScope()
    MedicalMateSegmentedControl(
        options = bodyMapViewOptions(),
        selectedIndex = camera.value.view().ordinal,
        onSelect = { index -> scope.launch { camera.glideTo(camera.value.facing(BodyMapView.entries[index])) } },
    )
    SideAnchorRow(state = state, callbacks = callbacks) {
        MedicalMateButton(
            onClick = callbacks.onBodyMap3dToggle,
            label = stringResource(R.string.body_map_use_image),
            type = MedicalMateButtonType.GHOST,
            size = MedicalMateButtonSize.S,
        )
    }
}

/**
 * 부위를 확대한 동안의 조작.
 *
 * 팔·다리는 화면에 한쪽만 담기므로 반대쪽으로 넘어가는 버튼을 함께 둔다. 돌려서 갈 수도
 * 있지만 몸 반대편으로 돌려야 해서 길다.
 */
@Composable
private fun ZoneActions(focus: BodyMapSelection, callbacks: IntakeCallbacks) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
    ) {
        MedicalMateButton(
            onClick = callbacks.onBodyFocusClear,
            label = stringResource(R.string.body_map_other_anchor),
            type = MedicalMateButtonType.OUTLINE,
            size = MedicalMateButtonSize.M,
            modifier = Modifier.weight(1f),
        )
        if (bodyMap3dFrameOf(focus.anchorId)?.oneSide == true) {
            MedicalMateButton(
                onClick = { callbacks.onBodyAnchorFocus(focus.copy(side = focus.side.opposite())) },
                label = stringResource(R.string.body_map_3d_flip_side),
                type = MedicalMateButtonType.OUTLINE,
                size = MedicalMateButtonSize.M,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 좌우를 뒤집는다. 좌우가 없는 자리에서 불릴 일은 없지만 왼쪽으로 둔다. */
private fun BodyMapSide.opposite(): BodyMapSide = if (this == BodyMapSide.LEFT) BodyMapSide.RIGHT else BodyMapSide.LEFT

/**
 * 짚었는데 부위가 정해지지 않았을 때 할 말. 정해졌으면 null이다.
 *
 * 몸 바깥과 "너무 멀다"를 가른다. 둘 다 "다시 짚어주세요"로 묶으면 무엇을 고쳐야 하는지
 * 알 수 없다. 앞은 몸을 짚으면 되고 뒤는 같은 몸에서 조금 옮기면 된다.
 */
@StringRes
private fun noticeOf(pick: BodyMap3dPick?): Int? = when {
    pick == null -> R.string.body_map_3d_off_body
    !pick.accepted -> R.string.body_map_3d_too_far
    else -> null
}
