package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** `Question Row`의 `State`. */
enum class MedicalMateQuestionRowState {
    /** 체크하지 않은 줄. */
    DEFAULT,

    /** 체크한 줄. 브리핑 카드에 담긴다. */
    SELECTED,

    /** 빈 칸에 직접 적는 중. 줄에는 "작성 중…"이 선다. */
    WRITING,

    /** 빈 칸. "질문 직접 추가"를 누르면 적기 시작한다. */
    ADD,

    /** 빈 칸을 눌렀고 아직 아무것도 적지 않았다. */
    FOCUS,
}

/**
 * DESIGN.md의 `Question Row`(`3644:7446`). 증상 정리 3/3 추천 질문 고르기 줄(1c-D~D4).
 *
 * 선택은 체크 + 테두리 + 번호로 알린다. 색만으로 가르지 않는다(D11).
 *
 * **줄 전체가 눌린다.** 체크가 있는 줄은 [onClick]이 체크를 바꾸고, 빈 칸은 적기를 시작한다. 적는
 * 중인 줄은 눌리지 않는다 — 글은 하단 입력칸에 적는다.
 *
 * 높이는 마스터의 64를 최소로 두고 글이 길면 늘어난다. 글자 배율 130%까지 잘리지 않아야 한다.
 *
 * @param question 줄의 글. 빈 칸이면 "질문 직접 추가", 적는 중이면 "작성 중…"을 부르는 쪽이 넣는다.
 */
@Composable
fun MedicalMateQuestionRow(
    number: Int,
    question: String,
    state: MedicalMateQuestionRowState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val blank = state == MedicalMateQuestionRowState.ADD || state == MedicalMateQuestionRowState.FOCUS

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight)
            .background(color = MedicalMateTheme.colors.bgSurface, shape = MedicalMateRadius.md)
            .border(width = BorderWidth, color = state.borderColor(), shape = MedicalMateRadius.md)
            .questionRowClick(state, onClick)
            .padding(
                start = MedicalMateSpace.s16,
                end = MedicalMateSpace.s14,
                top = MedicalMateSpace.s12,
                bottom = MedicalMateSpace.s12,
            ),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedicalMateNumberBadge(
            number = number,
            tone = if (blank) MedicalMateNumberBadgeTone.SUBTLE else MedicalMateNumberBadgeTone.PRIMARY,
        )
        if (state != MedicalMateQuestionRowState.FOCUS) {
            Text(
                text = question,
                style = MedicalMateTheme.typography.bodyM,
                color = state.textColor(),
                modifier = Modifier.weight(1f),
            )
        }
        if (!blank) {
            MedicalMateCheckboxBox(checked = state == MedicalMateQuestionRowState.SELECTED)
        }
    }
}

/** 체크가 있는 줄은 체크를, 빈 칸은 적기를 시작한다. 적는 중인 줄은 눌리지 않는다. */
private fun Modifier.questionRowClick(state: MedicalMateQuestionRowState, onClick: () -> Unit): Modifier =
    when (state) {
        MedicalMateQuestionRowState.DEFAULT, MedicalMateQuestionRowState.SELECTED ->
            toggleable(
                value = state == MedicalMateQuestionRowState.SELECTED,
                role = Role.Checkbox,
                onValueChange = { onClick() },
            )

        MedicalMateQuestionRowState.ADD -> clickable(role = Role.Button, onClick = onClick)
        MedicalMateQuestionRowState.WRITING, MedicalMateQuestionRowState.FOCUS -> this
    }

@Composable
private fun MedicalMateQuestionRowState.borderColor(): Color = when (this) {
    MedicalMateQuestionRowState.DEFAULT -> MedicalMateTheme.colors.borderSubtle
    MedicalMateQuestionRowState.ADD -> MedicalMateTheme.colors.borderDefault
    else -> MedicalMateTheme.colors.borderPrimary
}

@Composable
private fun MedicalMateQuestionRowState.textColor(): Color = when (this) {
    MedicalMateQuestionRowState.WRITING -> MedicalMateTheme.colors.fgPrimary
    // 마스터의 빈 칸 글자가 `fg/muted`다.
    MedicalMateQuestionRowState.ADD -> MedicalMateTheme.colors.fgMuted
    else -> MedicalMateTheme.colors.fgDefault
}

/** 마스터의 줄 높이 64. */
private val RowHeight = 64.dp

private val BorderWidth = 1.dp
