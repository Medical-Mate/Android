package com.mist.medicalmate.intake.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.BuildConfig
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateChip
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButtonStyle
import com.mist.medicalmate.core.designsystem.component.MedicalMateSegmentedControl
import com.mist.medicalmate.core.voice.VoiceFocus
import com.mist.medicalmate.core.voice.VoiceFocusFrame
import com.mist.medicalmate.core.voice.rememberVoiceFocus
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
internal fun BodyMap3dStep(
    state: BodyMapUiState,
    callbacks: IntakeCallbacks,
    voiceFocus: VoiceFocus<IntakeFocusTarget>? = null,
) {
    // 말로 부위를 고르거나 옮기면 판으로 화면이 옮겨 가고 잠깐 테를 두른다(#284).
    val shown = rememberVoiceFocus(voiceFocus)
    val camera = rememberBodyMap3dCamera()
    var notice by remember { mutableStateOf<Int?>(null) }
    var resets by remember { mutableIntStateOf(0) }
    val focus = state.focus

    // 부위가 바뀌면 카메라가 그 자리로 미끄러진다. 순간이동시키면 어디로 들어갔는지 알 수 없다.
    // 초기화도 이 자리에서 함께 처리한다. 버튼에서 따로 카메라를 옮기면, 지운 것 때문에 다시
    // 도는 이 효과와 겹쳐 둘이 서로를 끊는다.
    LaunchedEffect(focus, resets) {
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

    // 말로 앞면·뒷면을 바꿨다(#336). 세그먼트를 누른 것과 같이 그 면으로 돈다.
    LaunchedEffect(state.viewAsked) {
        if (state.viewAsked > 0) camera.glideTo(camera.value.facing(state.view))
    }

    Heading(focus)
    if (focus != null) {
        ZoneActions(focus = focus, callbacks = callbacks)
    } else {
        AnchorActions(state = state, camera = camera, callbacks = callbacks)
    }
    Board(
        state = state,
        voiceFocus = shown,
        camera = camera,
        onPick = { pick ->
            notice = noticeOf(pick)
            if (pick != null && pick.accepted) {
                if (focus == null) {
                    callbacks.onBodyAnchorFocus(pick.point.toFocus())
                } else {
                    callbacks.onBodyPartToggle(pick.point.toSelection())
                }
            }
        },
        onReset = {
            callbacks.onBodyReset()
            resets++
        },
    )
    val message = notice
    if (message != null) {
        Text(
            text = stringResource(message),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * 인체도 판.
 *
 * 고른 것이 있거나 큰 부위를 짚어 확대했으면 판 오른쪽 위에 지우는 버튼이 선다(#331). 3D는 돌리고
 * 확대하다 보면 처음 자리에서 멀어지는데, 고른 것을 물린 채로는 어디서부터 다시 짚어야 할지가 흐려진다.
 * 전신에서 아무것도 고르지 않았으면 지울 것이 없어서 두지 않는다.
 *
 * 판 안에 얹는 것은 좌우 안내와 고른 부위 알약이 그렇듯 판의 언어다. 판 밖에 두면 조작
 * 줄이 한 줄 더 생겨 판이 그만큼 내려간다.
 */
@Composable
private fun Board(
    state: BodyMapUiState,
    voiceFocus: VoiceFocus<IntakeFocusTarget>?,
    camera: BodyMap3dCameraState,
    onPick: (BodyMap3dPick?) -> Unit,
    onReset: () -> Unit,
) {
    VoiceFocusFrame(voiceFocus, IntakeFocusTarget.Board, MedicalMateRadius.md) {
        BodyMapCard(
            height = bodyMapBodyHeight(),
            orientationLabels = false,
            caption = state.selection?.label(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                BodyMap3dBoard(
                    camera = camera,
                    focus = state.focus,
                    selection = state.selection,
                    onPick = onPick,
                    modifier = Modifier.fillMaxSize(),
                )
                // 새 모델과 지금 모델을 번갈아 본다(#330). debug 빌드에서만 있다.
                if (BuildConfig.DEBUG) {
                    MedicalMateChip(
                        label = stringResource(R.string.body_map_3d_model_bx05),
                        selected = BodyMap3dModelChoice.current == BodyMap3dModel.BX05,
                        onClick = {
                            BodyMap3dModelChoice.current =
                                if (BodyMap3dModelChoice.current == BodyMap3dModel.BX05) {
                                    BodyMap3dModel.CURRENT
                                } else {
                                    BodyMap3dModel.BX05
                                }
                        },
                        modifier = Modifier.align(Alignment.TopStart).padding(ResetInset),
                    )
                }
                // 큰 부위를 짚어 확대만 한 때도 선다(#331). 전신으로 돌아갈 길을 찾지 않아도 된다.
                if (state.focus != null || state.selection != null) {
                    MedicalMateIconButton(
                        onClick = onReset,
                        icon = MedicalMateIcons.Close,
                        contentDescription = stringResource(R.string.body_map_3d_reset),
                        style = MedicalMateIconButtonStyle.TONAL,
                        size = MedicalMateIconButtonSize.M,
                        modifier = Modifier.align(Alignment.TopEnd).padding(ResetInset),
                    )
                }
            }
        }
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
        // 인체도를 쓸 수 없는 사람의 길이다. 3D가 기본이 되면서(#235) 이 자리로 옮겼다 —
        // 2D 화면에 있던 것을 그대로 두면 닿을 수가 없다.
        MedicalMateButton(
            onClick = callbacks.onBodyListModeToggle,
            label = stringResource(R.string.body_map_use_list),
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

/**
 * 지우는 버튼을 판 모서리에서 띄우는 만큼.
 *
 * 버튼은 40으로 보이지만 48 터치 영역을 품고 있어서 그 4가 이미 안쪽에 있다. 판이 좌우
 * 안내를 두는 12에 맞추려고 8만 더한다.
 */
private val ResetInset = MedicalMateSpace.s8

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
