package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md의 `Avatar`의 `Type` variant. 환자는 브랜드 tint, 의사는 중립이다. */
enum class MedicalMateAvatarType {
    PATIENT,
    DOCTOR,
}

/**
 * DESIGN.md의 `Avatar`. 44 원형이다.
 *
 * [initial]은 이름의 첫 글자를 받는다. 사진이 들어오면 여기에 이미지 슬롯을 추가한다.
 *
 * [contentDescription]을 주지 않으면 접근성 트리에서 지운다. 이름이 옆에 함께 나오는
 * 자리에서 같은 이름을 두 번 읽으면 소리만 길어진다. 아바타만 단독으로 두는 자리라면
 * 이름을 넘긴다.
 *
 * [size]는 기본이 문서의 44다. Figma 홈 헤더 인스턴스가 36으로 축소되어 있어
 * 파라미터로 열었다. 44도 48보다 작으므로 터치 목표는 호출자가 바깥에서 확보한다.
 * 아바타 자체가 hit area를 넓히면 헤더 배치가 어긋난다.
 */
@Composable
fun MedicalMateAvatar(
    initial: String,
    modifier: Modifier = Modifier,
    type: MedicalMateAvatarType = MedicalMateAvatarType.PATIENT,
    contentDescription: String? = null,
    size: Dp = DefaultAvatarSize,
    onClick: (() -> Unit)? = null,
) {
    val colors = MedicalMateTheme.colors
    val container =
        when (type) {
            MedicalMateAvatarType.PATIENT -> colors.bgPrimarySubtle
            MedicalMateAvatarType.DOCTOR -> colors.bgSubtle
        }
    val content =
        when (type) {
            MedicalMateAvatarType.PATIENT -> colors.fgPrimary
            MedicalMateAvatarType.DOCTOR -> colors.fgSubtle
        }
    val describe =
        if (contentDescription == null) {
            Modifier.clearAndSetSemantics { }
        } else {
            Modifier.semantics { this.contentDescription = contentDescription }
        }

    val body: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial.take(1),
                style = MedicalMateTheme.typography.bodyLStrong,
            )
        }
    }
    val shared = modifier.size(size).then(describe)

    if (onClick == null) {
        Surface(
            shape = MedicalMateRadius.full,
            color = container,
            contentColor = content,
            modifier = shared,
            content = body,
        )
    } else {
        Surface(
            onClick = onClick,
            shape = MedicalMateRadius.full,
            color = container,
            contentColor = content,
            modifier = shared,
            content = body,
        )
    }
}

/** 문서의 컴포넌트 규격이 지정한 지름. Scale에 없는 값이다. */
private val DefaultAvatarSize = 44.dp
