package com.mist.medicalmate.intake.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonSize
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateSegmentedControl
import kotlinx.coroutines.launch

/**
 * 3D 인체도로 부위를 짚는 화면. 테스트용이다(#211).
 *
 * **시안에 없는 화면이다.** 디자인 시스템의 토큰과 컴포넌트만 쓰고 새 규격을 만들지
 * 않는다. 2D와 같은 자리에 같은 크기의 판을 두고 제목·앞뒤 토글·판·알약까지 배치를
 * 맞췄다. 두 길을 견주는 것이 목적이라 화면 구성이 다르면 무엇 때문에 낫고 못한지
 * 가려지지 않는다.
 *
 * 고르는 것은 한 곳이고 값은 2D와 같은 [BodyMapSelection]이다. 그래서 여기서 골라도
 * 다음 단계와 브리핑 카드가 달라지지 않는다.
 */
@Composable
internal fun BodyMap3dStep(state: BodyMapUiState, callbacks: IntakeCallbacks) {
    val camera = rememberBodyMap3dCamera()
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<Int?>(null) }

    Text(
        text = stringResource(R.string.intake_body_part_question),
        style = MedicalMateTheme.typography.headingL,
        color = MedicalMateTheme.colors.fgDefault,
    )
    Text(
        text = stringResource(R.string.body_map_3d_description),
        style = MedicalMateTheme.typography.bodyM,
        color = MedicalMateTheme.colors.fgSubtle,
    )
    MedicalMateSegmentedControl(
        options = bodyMapViewOptions(),
        selectedIndex = camera.value.view().ordinal,
        onSelect = { index ->
            scope.launch {
                camera.animateTo(
                    targetValue = camera.value.facing(BodyMapView.entries[index]),
                    animationSpec = tween(durationMillis = GLIDE_MILLIS, easing = FastOutSlowInEasing),
                )
            }
        },
    )
    SideAnchorRow(state = state, callbacks = callbacks) {
        MedicalMateButton(
            onClick = callbacks.onBodyMap3dToggle,
            label = stringResource(R.string.body_map_use_image),
            type = MedicalMateButtonType.GHOST,
            size = MedicalMateButtonSize.S,
        )
    }
    BodyMapCard(
        height = bodyMapBodyHeight(),
        orientationLabels = false,
        caption = state.selection?.label(),
    ) {
        BodyMap3dBoard(
            camera = camera,
            selection = state.selection,
            onPick = { pick ->
                notice = noticeOf(pick)
                if (pick != null && pick.accepted) callbacks.onBodyPartSelect(pick.point.toSelection())
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

/** 자료가 적어 둔 카메라 전환 시간. */
private const val GLIDE_MILLIS = 460
