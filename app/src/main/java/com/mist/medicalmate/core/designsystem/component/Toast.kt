package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.Amber100
import com.mist.medicalmate.core.designsystem.Green100
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.Primary300
import com.mist.medicalmate.core.designsystem.Red100

/** DESIGN.md 8.4 `Toast`의 `Tone` variant. */
enum class MedicalMateToastTone {
    NORMAL,
    POSITIVE,
    CAUTIONARY,
    NEGATIVE,
}

/**
 * DESIGN.md 8.4 `Toast`.
 *
 * 행동 직후 사라지는 피드백이다. 화면에 남아야 하는 안내는 [MedicalMateNotice]다.
 *
 * 이 컴포넌트는 모양만 그린다. 띄우고 없애는 것은 호출자가 정한다. 문서 8.4의 위치
 * 우선순위는 Bottom CTA Bar 바로 위, 없으면 Tab Bar 위 20, 둘 다 없으면 safe area 위
 * 20이다.
 *
 * [actionLabel]은 되돌릴 수 있는 파괴 동작에만 붙인다. 되돌릴 수 없는 동작의 확인은
 * 사라지기 전에 [MedicalMateDialog]가 받아야 한다. 사라지는 Toast에 "실행 취소"를 걸면
 * 놓친 사용자에게 되돌릴 방법이 남지 않는다.
 *
 * 최대 2줄이다. 두 줄에 담기지 않는 내용은 Toast가 아니라 화면에 남을 안내다.
 *
 * 아이콘 색은 원시 팔레트에서 가져온다. inverse 면 위에서 쓸 상태색이 시맨틱 40개에
 * 없다. 문서 2.2가 이 경우 각 계열의 100 단계나 `primary/300`을 쓰라고 지정했고, 시맨틱
 * 컬렉션을 Figma와 어긋나게 늘리는 대신 문서가 지목한 팔레트 값을 직접 쓴다.
 */
@Composable
fun MedicalMateToast(
    message: String,
    modifier: Modifier = Modifier,
    tone: MedicalMateToastTone = MedicalMateToastTone.NORMAL,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val accent: Color

    @DrawableRes val icon: Int
    when (tone) {
        MedicalMateToastTone.NORMAL -> {
            accent = Primary300
            icon = MedicalMateIcons.Info
        }

        MedicalMateToastTone.POSITIVE -> {
            accent = Green100
            icon = MedicalMateIcons.CheckCircle
        }

        MedicalMateToastTone.CAUTIONARY -> {
            accent = Amber100
            icon = MedicalMateIcons.AlertTriangle
        }

        MedicalMateToastTone.NEGATIVE -> {
            accent = Red100
            icon = MedicalMateIcons.AlertCircle
        }
    }

    Surface(
        shape = ToastShape,
        color = MedicalMateTheme.colors.bgInverse,
        contentColor = MedicalMateTheme.colors.fgOnInverse,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
            Modifier
                .heightIn(min = ToastMinHeight)
                .padding(start = MedicalMateSpace.s16, end = MedicalMateSpace.s8),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(MedicalMateSize.iconMd),
            )
            Text(
                text = message,
                style = MedicalMateTheme.typography.bodyM,
                maxLines = TOAST_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onActionClick != null) {
                ToastAction(label = actionLabel, accent = accent, onClick = onActionClick)
            }
        }
    }
}

/**
 * Toast 안의 행동.
 *
 * [MedicalMateButton]의 Ghost를 쓰지 않는다. Ghost의 글자색이 `fg/primary`라서 inverse
 * 면 위에서 읽히지 않는다. 대신 아이콘과 같은 강조색을 쓴다.
 *
 * 터치 목표 48을 지킨다. Toast 높이가 56이라 세로 여유는 있고 가로만 확보하면 된다.
 */
@Composable
private fun ToastAction(label: String, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MedicalMateRadius.sm,
        color = Color.Transparent,
        contentColor = accent,
    ) {
        Box(
            modifier =
            Modifier
                .defaultMinSize(
                    minWidth = MedicalMateSize.touchMin,
                    minHeight = MedicalMateSize.touchMin,
                )
                .padding(horizontal = MedicalMateSpace.s8),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = MedicalMateTheme.typography.labelM)
        }
    }
}

/** 문서 8.4가 지정한 radius 18. Scale에 없는 값이다. */
private val ToastShape = RoundedCornerShape(18.dp)

private val ToastMinHeight = 56.dp

private const val TOAST_MAX_LINES = 2
