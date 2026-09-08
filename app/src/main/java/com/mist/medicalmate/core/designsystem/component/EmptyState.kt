package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md 8.4 `Empty State`의 `Type` variant. */
enum class MedicalMateEmptyStateType {
    NO_RECORD,
    NO_RESULT,
    OFFLINE,
    MIC_DENIED,
}

/**
 * DESIGN.md 8.4 `Empty State`.
 *
 * 사과보다 다음 행동을 제시한다. "죄송합니다"로 시작하면 사용자는 무엇을 해야 할지
 * 모른 채 화면을 떠난다.
 *
 * [note]는 제목 위에 먼저 나온다. [MedicalMateEmptyStateType.OFFLINE]에서는 작성 내용이
 * 남아 있다는 사실을 가장 먼저 알려야 한다(문서 8.4). 연결이 끊겼다는 말만 보이면
 * 환자는 방금 적은 증상이 날아갔다고 생각한다.
 *
 * 행동 버튼은 Tonal이다. 브랜드 채움은 실제 주 행동에만 쓴다(문서 8.1).
 */
@Composable
fun MedicalMateEmptyState(
    type: MedicalMateEmptyStateType,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val colors = MedicalMateTheme.colors

    @DrawableRes val icon =
        when (type) {
            MedicalMateEmptyStateType.NO_RECORD -> MedicalMateIcons.EmptyBox
            MedicalMateEmptyStateType.NO_RESULT -> MedicalMateIcons.SearchOff
            MedicalMateEmptyStateType.OFFLINE -> MedicalMateIcons.WifiOff
            MedicalMateEmptyStateType.MIC_DENIED -> MedicalMateIcons.MicOff
        }

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(vertical = MedicalMateSpace.s32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.fgMuted,
            modifier = Modifier.size(EmptyStateIconSize),
        )
        note?.let {
            Text(
                text = it,
                style = MedicalMateTheme.typography.bodyMStrong,
                color = colors.fgPrimary,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = title,
            style = MedicalMateTheme.typography.headingS,
            color = colors.fgDefault,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            style = MedicalMateTheme.typography.bodyM,
            color = colors.fgSubtle,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onActionClick != null) {
            MedicalMateButton(
                onClick = onActionClick,
                label = actionLabel,
                type = MedicalMateButtonType.TONAL,
                size = MedicalMateButtonSize.M,
            )
        }
    }
}

/** 빈 상태 그림은 기본 아이콘보다 크게 둔다. 화면 가운데를 채우는 유일한 요소다. */
private val EmptyStateIconSize = 48.dp
