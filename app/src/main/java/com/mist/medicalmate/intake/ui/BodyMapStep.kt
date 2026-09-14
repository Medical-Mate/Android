package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
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
 * 전신·피부 칩과 목록 전환은 판 **위** 한 줄에 함께 둔다. 판이 505dp라 아래에 두면 첫
 * 화면에서 보이지 않는데, 목록은 인체도를 쓸 수 없는 사람을 위한 길이라 발견되지 않으면
 * 없는 것과 같다. 둘을 한 줄로 합쳐서 위로 올리는 값이 48dp에서 그친다.
 */
@Composable
internal fun BodyPartStep(state: IntakeUiState, callbacks: IntakeCallbacks, modifier: Modifier = Modifier) {
    val bodyMap = state.bodyMap
    StepContent(step = state.step, scrollKey = bodyMap.screen, modifier = modifier) {
        when (bodyMap.screen) {
            BodyMapScreen.LIST -> BodyMapPartList(state = bodyMap, callbacks = callbacks)
            BodyMapScreen.MAP_3D -> BodyMap3dStep(state = bodyMap, callbacks = callbacks)
            else -> ImagePicker(state = bodyMap, callbacks = callbacks)
        }
    }
}

/**
 * 인체도로 고르는 길. 앵커 화면과 확대 화면이 같은 자리를 쓴다.
 *
 * 제목 아래 조작 자리에는 화면마다 다른 것이 들어가지만 높이가 같다. 앵커에서는 앞뒤
 * 토글, 확대에서는 되돌아가는 버튼이다. 하나가 사라지고 다른 것이 나타나면 확대
 * 애니메이션 중에 판이 위아래로 튄다.
 */
@Composable
private fun ImagePicker(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    val zooming = state.screen == BodyMapScreen.ZONE
    Text(
        text = pickerQuestion(state),
        style = MedicalMateTheme.typography.headingL,
        color = MedicalMateTheme.colors.fgDefault,
    )
    // 앵커 화면에는 설명을 두지 않는다. 인체도와 점이 보이는 상태에서 "가장 불편한 곳
    // 하나를 짚어주세요"는 화면이 이미 말하는 것이고, 두 줄을 쓰면 그만큼 판이 내려가
    // 다리 점이 하단 버튼 뒤로 들어간다. 확대 화면은 판이 짧아 여유가 있어 남긴다.
    if (zooming) {
        Text(
            text = stringResource(R.string.body_map_zone_description),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
    if (zooming) {
        MedicalMateButton(
            onClick = callbacks.onBodyFocusClear,
            label = stringResource(R.string.body_map_other_anchor),
            type = MedicalMateButtonType.OUTLINE,
            size = MedicalMateButtonSize.M,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        MedicalMateSegmentedControl(
            options = bodyMapViewOptions(),
            selectedIndex = state.view.ordinal,
            onSelect = { callbacks.onBodyViewChange(BodyMapView.entries[it]) },
        )
        SideAnchorRow(state = state, callbacks = callbacks) {
            MedicalMateButton(
                onClick = callbacks.onBodyListModeToggle,
                label = stringResource(R.string.body_map_use_list),
                type = MedicalMateButtonType.GHOST,
                size = MedicalMateButtonSize.S,
            )
        }
    }
    AnimatedCard(state = state, callbacks = callbacks)
}

/** 화면 제목. 확대 중이면 앵커 이름을 부른다. */
@Composable
private fun pickerQuestion(state: BodyMapUiState): String {
    val focus = state.focus
    return if (state.screen == BodyMapScreen.ZONE && focus != null) {
        stringResource(R.string.body_map_zone_question, focus.title())
    } else {
        stringResource(R.string.intake_body_part_question)
    }
}

/**
 * 확대 애니메이션이 걸린 판.
 *
 * 축을 짚은 점에 두려면 판의 실제 폭을 알아야 해서 [BoxWithConstraints]로 감싼다. 판은
 * 폭을 채우고 높이만 좌표에서 계산한 값이다.
 */
@Composable
private fun AnimatedCard(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    val bodyHeight = bodyMapBodyHeight()
    // 확대되는 이미지가 판 밖으로 나가지 않게 판 모양으로 자른다. 자르지 않으면 커지는
    // 전신이 위아래 버튼을 덮고 지나간다.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().clip(MedicalMateRadius.md)) {
        val cardWidth = maxWidth
        BodyMapCardTransition(
            screen = state.screen,
            zoomOrigin =
            bodyMapZoomOrigin(
                point = state.focusPoint,
                imageWidth = (bodyHeight * state.bodyImage.aspectRatio).value,
                cardWidth = cardWidth.value,
            ),
        ) { screen ->
            if (screen == BodyMapScreen.ZONE) {
                ZoneCard(state = state, callbacks = callbacks)
            } else {
                AnchorCard(state = state, callbacks = callbacks)
            }
        }
    }
}

/** 전신에서 확대할 부위를 짚는 판. */
@Composable
private fun AnchorCard(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    val image = state.bodyImage
    val dots = bodyMapAnchorDots(state.view, state.selection)
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
}

/**
 * 확대해 구역을 고르는 판. 1l-2다.
 *
 * 고른 뒤에도 이 화면에 머문다. 고른 점이 브랜드색으로 바뀌고 아래 알약에 이름이 나오는데,
 * 곧바로 앵커 화면으로 돌아가면 그 둘을 볼 수 없다.
 */
@Composable
private fun ZoneCard(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    val anchor = state.anchor
    val detail = anchor?.detail
    val focus = state.focus
    if (anchor == null || detail == null || focus == null) return
    val dots = bodyMapZoneDots(anchor, focus.side, state.selection)
    BodyMapCard(
        height = bodyMapCardHeight(dots, detail),
        orientationLabels = detail.view == BodyMapView.FRONT && !detail.mirrored,
        caption = state.selection?.takeIf { it.belongsTo(focus.anchorId, focus.side) }?.label(),
    ) {
        BodyMapCanvas(
            image = detail,
            dots = dots,
            onDotClick = { callbacks.onBodyDotClick(it) },
            mirrored = detail.mirrored && focus.side == BodyMapSide.LEFT,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 전신 · 피부 칩과 목록 전환을 담은 줄.
 *
 * 칩 둘은 인체도에 점으로 찍을 수 없는 앵커다. 몸 전체에 걸리는 증상(열·피로)과 어디든
 * 생기는 증상(발진)이라 한 점으로 찍을 수 없다. 구역 단계가 없어서 누르면 바로 정해진다.
 *
 * 길을 바꾸는 버튼은 같은 줄 오른쪽에 붙인다([trailing]). 따로 줄을 두면 판이 그만큼 더
 * 밀려 내려간다. 3D 화면도 같은 줄을 쓰는데 거기서는 버튼이 하나라 슬롯으로 받는다.
 *
 * **그 버튼에 `weight`를 주지 않는다.** 남은 폭을 전부 먹으면 라벨이 그 안에서 가운데
 * 정렬돼 칩과의 간격이 들쭉날쭉해 보인다. 빈 자리를 [Spacer]가 밀고 버튼은 제 폭으로
 * 오른쪽 끝에 선다.
 */
@Composable
internal fun SideAnchorRow(
    state: BodyMapUiState,
    callbacks: IntakeCallbacks,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bodyMapSideAnchors.forEach { anchor ->
            MedicalMateChip(
                label = bodyMapLabelOf(anchor.id),
                selected = state.selection == BodyMapSelection(anchor.id),
                onClick = { callbacks.onBodySideAnchorClick(anchor.id) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
internal fun bodyMapViewOptions(): List<String> = listOf(
    stringResource(R.string.body_map_view_front),
    stringResource(R.string.body_map_view_back),
)
