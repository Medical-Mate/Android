package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateIconButton

/**
 * 고른 부위 목록.
 *
 * 여러 곳을 고를 수 있게 되면서 필요해졌다. 확대 화면 안에서는 고른 점이 브랜드색으로
 * 바뀌어 보이지만, 앵커 화면으로 돌아오거나 다른 부위를 고르러 옮기면 앞서 고른 것이
 * 어디였는지 확인할 곳이 없다. 그래서 인체도·확대·목록 세 화면 모두 아래에 이 목록을
 * 둔다.
 *
 * 줄마다 지우기를 둔 이유는 뺄 방법이 인체도에만 있으면 안 되기 때문이다. 다른 앵커에서
 * 고른 것을 빼려면 그 앵커로 다시 들어가야 한다.
 *
 * 생김새는 같은 흐름의 "적어둔 질문"(1i)과 맞췄다. 한 흐름 안에서 지울 수 있는 목록이
 * 두 군데 나오는데 서로 다르게 보이면 같은 조작으로 읽히지 않는다.
 */
@Composable
internal fun BodyMapSelectedParts(
    selected: List<BodyMapSelection>,
    callbacks: IntakeCallbacks,
    modifier: Modifier = Modifier,
) {
    if (selected.isEmpty()) return
    SelectedHeader(count = selected.size, modifier = modifier)
    selected.forEach { selection ->
        SelectedRow(selection = selection, onRemoveClick = { callbacks.onBodyPartToggle(selection) })
    }
}

/** 목록의 머리. 개수는 누를 수 없는 표시라 오른쪽에 본문 크기로 둔다. */
@Composable
private fun SelectedHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.body_map_selected_title),
            style = MedicalMateTheme.typography.headingS,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.body_map_selected_count, count),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * 고른 부위 한 줄.
 *
 * 진료과를 함께 보여준다. 여러 곳을 고르면 과도 여러 개가 되는데, 어느 부위가 어느 과인지
 * 아래 안내에 합쳐 놓으면 알 수 없다.
 *
 * 지우기는 아이콘만 두고 접근성 이름에 어느 부위인지 담는다. "지우기" 버튼이 여러 개
 * 나란히 있으면 스크린 리더로 무엇을 지우는지 알 수 없다.
 */
@Composable
private fun SelectedRow(selection: BodyMapSelection, onRemoveClick: () -> Unit) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(color = MedicalMateTheme.colors.bgPrimaryFaint, shape = MedicalMateRadius.sm)
            .padding(start = MedicalMateSpace.s12, end = MedicalMateSpace.s4),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = selection.title(),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
        val departments = selection.departments()
        if (departments.isNotEmpty()) {
            Text(
                text = departments.first(),
                style = MedicalMateTheme.typography.labelS,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
        MedicalMateIconButton(
            onClick = onRemoveClick,
            icon = MedicalMateIcons.Close,
            contentDescription = stringResource(R.string.body_map_selected_remove, selection.title()),
        )
    }
}
