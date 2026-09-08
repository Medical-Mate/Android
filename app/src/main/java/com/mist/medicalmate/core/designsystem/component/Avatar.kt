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
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md 8.3 `Avatar`의 `Type` variant. 환자는 브랜드 tint, 의사는 중립이다. */
enum class MedicalMateAvatarType {
    PATIENT,
    DOCTOR,
}

/**
 * DESIGN.md 8.3 `Avatar`. 44 원형이다.
 *
 * [initial]은 이름의 첫 글자를 받는다. 사진이 들어오면 여기에 이미지 슬롯을 추가한다.
 *
 * [contentDescription]을 주지 않으면 접근성 트리에서 지운다. 이름이 옆에 함께 나오는
 * 자리에서 같은 이름을 두 번 읽으면 소리만 길어진다. 아바타만 단독으로 두는 자리라면
 * 이름을 넘긴다.
 */
@Composable
fun MedicalMateAvatar(
    initial: String,
    modifier: Modifier = Modifier,
    type: MedicalMateAvatarType = MedicalMateAvatarType.PATIENT,
    contentDescription: String? = null,
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

    Surface(
        shape = MedicalMateRadius.full,
        color = container,
        contentColor = content,
        modifier =
        modifier
            .size(AvatarSize)
            .then(describe),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial.take(1),
                style = MedicalMateTheme.typography.bodyLStrong,
            )
        }
    }
}

/** 문서 8.3이 지정한 지름. Scale에 없는 값이다. */
private val AvatarSize = 44.dp
