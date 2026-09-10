package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 대화상자가 무슨 종류의 결정인지. 상단 아이콘 배지의 색과 아이콘을 정한다.
 *
 * 마스터가 이 배지를 "글을 읽기 전에 성격이 전달되는" 요소로 설명한다. 그래서 아이콘을
 * 호출자가 고르지 않고 결정의 성격만 고른다. 같은 성격의 대화상자가 화면마다 다른 아이콘을
 * 쓰면 그 전달이 깨진다.
 */
enum class MedicalMateDialogTone {
    /** 지우는 결정. 되돌릴 수 없다. */
    DANGER,

    /** 그 밖의 확인. */
    NEUTRAL,
}

/**
 * DESIGN.md의 `Dialog`.
 *
 * 되돌릴 수 없는 동작을 확인받는 자리다. 되돌릴 수 있는 동작은 [MedicalMateToast]의
 * action으로 충분하고, 확인 대화상자를 남발하면 사용자가 내용을 읽지 않고 누른다.
 *
 * 구성은 위에서부터 원형 아이콘 배지, 제목, 본문, 버튼 두 개다. 본문은 "무엇을 잃는지"를
 * 말한다. 이 앱에서 잃는 것은 진료실에서 보여줄 내용이다.
 *
 * 취소를 왼쪽, 실행을 오른쪽에 둔다. 둘은 같은 폭으로 나눠 가진다. `AlertDialog`의 기본
 * 버튼 배치를 쓰지 않는 이유가 이것이다. 그쪽은 버튼을 오른쪽에 글자 폭만큼 붙여 놓아서
 * 마스터의 좌우 균등 배치와 다르고, 누르는 면적도 작다.
 *
 * [onDismissRequest]는 바깥을 눌렀을 때도 불린다. 되돌릴 수 없는 선택 중에는 바깥 누름을
 * 막아야 하므로, 그런 자리에서는 호출자가 빈 람다를 주고 취소 버튼만 남긴다(문서의
 * Overlay Scrim 규칙과 같은 이유다).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalMateDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tone: MedicalMateDialogTone = MedicalMateDialogTone.DANGER,
) {
    val colors = MedicalMateTheme.colors
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier.width(DialogWidth)) {
        Surface(shape = MedicalMateRadius.xl, color = colors.bgSurface, contentColor = colors.fgDefault) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MedicalMateSpace.s20,
                        end = MedicalMateSpace.s20,
                        top = TopPadding,
                        bottom = MedicalMateSpace.s16,
                    ),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconBadge(tone = tone)
                Text(
                    text = title,
                    style = MedicalMateTheme.typography.headingM,
                    color = colors.fgDefault,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = message,
                    style = MedicalMateTheme.typography.bodyS,
                    color = colors.fgSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = MedicalMateSpace.s4),
                    horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
                ) {
                    MedicalMateButton(
                        onClick = onDismissRequest,
                        label = dismissLabel,
                        type = MedicalMateButtonType.TONAL,
                        size = MedicalMateButtonSize.M,
                        modifier = Modifier.weight(1f),
                    )
                    MedicalMateButton(
                        onClick = onConfirm,
                        label = confirmLabel,
                        type = MedicalMateButtonType.DANGER,
                        size = MedicalMateButtonSize.M,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** 상단 원형 배지. 마스터의 48 상자에 24 아이콘이다. */
@Composable
private fun IconBadge(tone: MedicalMateDialogTone) {
    val colors = MedicalMateTheme.colors
    val container: Color
    val icon: Int
    when (tone) {
        MedicalMateDialogTone.DANGER -> {
            container = colors.bgDanger
            icon = MedicalMateIcons.Trash
        }

        MedicalMateDialogTone.NEUTRAL -> {
            container = colors.bgPrimaryFaint
            icon = MedicalMateIcons.AlertCircle
        }
    }
    Box(
        modifier = Modifier.size(MedicalMateSize.controlMd).background(container, MedicalMateRadius.full),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (tone == MedicalMateDialogTone.DANGER) colors.fgDanger else colors.fgPrimary,
            modifier = Modifier.size(MedicalMateSize.iconLg),
        )
    }
}

/** 문서의 컴포넌트 규격이 지정한 폭. 높이는 내용에 따라 늘어난다. */
private val DialogWidth = 320.dp

/** 마스터의 위 여백 28. 배지가 위에 있어 아래보다 넓다. */
private val TopPadding = 28.dp
