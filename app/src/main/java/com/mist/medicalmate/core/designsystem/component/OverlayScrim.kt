package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Overlay Scrim`.
 *
 * modal과 시트 뒤에 깐다. [MedicalMateBottomSheet]는 자체 스크림을 갖고 있으므로 직접
 * 만든 오버레이에만 쓴다.
 *
 * [onDismiss]가 없으면 눌러도 닫히지 않는다. 되돌릴 수 없는 선택을 받는 중에는 바깥
 * 누름으로 빠져나갈 수 없어야 한다(문서의 컴포넌트 규격). 그 경우에도 터치는 삼켜서 뒤에 있는
 * 화면이 눌리지 않게 한다. 스크림이 시각적으로만 덮고 터치를 흘려보내면 사용자는 보이지
 * 않는 버튼을 누르게 된다.
 *
 * 눌림 표시(ripple)를 주지 않는다. 스크림은 누르는 대상이 아니라 닫는 자리다.
 */
@Composable
fun MedicalMateOverlayScrim(modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val touch =
        if (onDismiss == null) {
            Modifier.pointerInput(Unit) { detectTapGestures { } }
        } else {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDismiss,
            )
        }

    Box(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgScrim.copy(alpha = SCRIM_ALPHA))
            .then(touch),
    )
}

/** 문서의 컴포넌트 규격이 지정한 불투명도 50%. */
internal const val SCRIM_ALPHA = 0.5f
