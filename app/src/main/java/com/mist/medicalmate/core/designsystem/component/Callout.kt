package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Callout`.
 *
 * 브리핑 카드에서 "환자가 묻고 싶어 하는 것"을 한 번만 보여준다. 화면에 두 번 이상 두면
 * 무엇이 환자의 질문인지 흐려진다.
 *
 * 질문마다 번호와 흰색 pill을 쓴다. 의사가 진료실에서 순서대로 짚어야 하므로 번호가
 * 필요하다.
 *
 * 제목 문구는 파라미터로 받는다. 제품 카피를 디자인 시스템이 들고 있으면 문구를 바꿀 때
 * 공용 계층을 고쳐야 한다.
 */
/**
 * 질문을 고치는 조작. `Callout` 마스터의 `Editing` variant에 대응한다.
 *
 * 셋을 한 값으로 묶는다. 따로 받으면 지우기만 되고 고치지는 못하는 반쪽 편집 모드를 만들 수
 * 있다. 세 조작이 한 상태에 함께 붙는다.
 *
 * [addLabel]과 [deleteContentDescription]을 받는 이유는 제품 카피를 디자인 시스템이 들고
 * 있지 않기 때문이다. `%1$d`에 질문 번호가 들어간다.
 */
data class MedicalMateCalloutEdit(
    val addLabel: String,
    val deleteContentDescription: (Int) -> String,
    val placeholder: String,
    val onQuestionChange: (Int, String) -> Unit,
    val onQuestionDelete: (Int) -> Unit,
    val onQuestionAdd: () -> Unit,
)

@Composable
fun MedicalMateCallout(
    title: String,
    questions: List<String>,
    modifier: Modifier = Modifier,
    edit: MedicalMateCalloutEdit? = null,
) {
    Surface(
        shape = MedicalMateRadius.lg,
        color = MedicalMateTheme.colors.bgPrimarySubtle,
        contentColor = MedicalMateTheme.colors.fgDefault,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MedicalMateSpace.s16),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(MedicalMateIcons.Chat),
                    contentDescription = null,
                    tint = MedicalMateTheme.colors.fgPrimary,
                    modifier = Modifier.size(MedicalMateSize.iconSm),
                )
                // 마스터가 "헤더 라벨은 Label/S + fg/primary로 가라앉히고 질문 본문을
                // fg/default로 세운다"고 적는다. 주인공은 질문이다.
                Text(
                    text = title,
                    style = MedicalMateTheme.typography.labelS,
                    color = MedicalMateTheme.colors.fgPrimary,
                )
            }
            questions.forEachIndexed { index, question ->
                QuestionPill(number = index + 1, question = question, index = index, edit = edit)
            }
            if (edit != null) {
                MedicalMateAddRow(label = edit.addLabel, onClick = edit.onQuestionAdd)
            }
        }
    }
}

@Composable
private fun QuestionPill(number: Int, question: String, index: Int, edit: MedicalMateCalloutEdit?) {
    Surface(
        shape = MedicalMateRadius.sm,
        // 문서의 컴포넌트 규격이 지정한 흰색 75%다. 뒤의 브랜드 tint가 살짝 배어 나온다.
        color = MedicalMateTheme.colors.bgSurface.copy(alpha = QUESTION_PILL_ALPHA),
        contentColor = MedicalMateTheme.colors.fgDefault,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MedicalMateSpace.s12),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MedicalMateRadius.full,
                color = MedicalMateTheme.colors.bgPrimary,
                contentColor = MedicalMateTheme.colors.fgOnPrimary,
                modifier = Modifier.size(NumberSize),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = number.toString(), style = MedicalMateTheme.typography.labelS)
                }
            }
            if (edit == null) {
                Text(text = question, style = MedicalMateTheme.typography.bodyM)
            } else {
                QuestionField(
                    value = question,
                    placeholder = edit.placeholder,
                    onValueChange = { edit.onQuestionChange(index, it) },
                    modifier = Modifier.weight(1f),
                )
                MedicalMateIconButton(
                    onClick = { edit.onQuestionDelete(index) },
                    icon = MedicalMateIcons.Close,
                    contentDescription = edit.deleteContentDescription(number),
                    style = MedicalMateIconButtonStyle.GHOST,
                    size = MedicalMateIconButtonSize.S,
                )
            }
        }
    }
}

/**
 * 고칠 수 있는 질문 칸.
 *
 * `Text Field`를 쓰지 않는다. 면을 채우고 테두리를 두르면 pill 안에 또 하나의 필드가 생겨
 * 번호와 글자의 정렬이 어긋난다. `KV Row`의 편집 값과 같은 방식이다.
 *
 * 빈 질문에는 안내 문구를 겹쳐 둔다. `+ 질문 추가`가 빈 항목을 만들기 때문에 처음에는
 * 반드시 빈 칸이 하나 있고, 그 칸이 무엇을 적는 자리인지 알려야 한다.
 */
@Composable
private fun QuestionField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = MedicalMateTheme.typography.bodyM.copy(color = MedicalMateTheme.colors.fgDefault)
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MedicalMateTheme.typography.bodyM,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style,
            cursorBrush = SolidColor(MedicalMateTheme.colors.borderFocus),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val QUESTION_PILL_ALPHA = 0.75f

private val NumberSize = 24.dp
