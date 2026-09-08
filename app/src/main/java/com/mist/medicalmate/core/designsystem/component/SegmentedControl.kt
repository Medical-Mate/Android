package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.2 `Segmented Control`.
 *
 * 화면 전체의 뷰를 바꾸는 자리에만 쓴다. 문서가 예로 든 것은 진료 전/후 전환이다.
 * 부분 필터에 쓰지 않는다. 목록 일부만 걸러내는 동작은 Chip이 맡는다.
 *
 * 모바일에서 최대 4칸이다. 그 이상은 각 칸의 글자가 잘린다.
 *
 * 고른 칸은 흰 면과 고도로 떠 있고 라벨이 굵어진다. 색만 바꾸면 어느 쪽이 켜졌는지
 * 구분이 약하다(문서 1항 6번).
 */
@Composable
fun MedicalMateSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(isValidSegmentCount(options.size)) {
        "칸은 2..${maxSegments()} 개여야 합니다. 받은 값: ${options.size}"
    }
    require(selectedIndex in options.indices) {
        "선택 위치가 범위를 벗어났습니다. 받은 값: $selectedIndex"
    }

    Surface(
        shape = MedicalMateRadius.buttonM,
        color = MedicalMateTheme.colors.bgSubtle,
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = MedicalMateSize.controlMd),
    ) {
        Row(
            modifier = Modifier.padding(InsetPadding),
            horizontalArrangement = Arrangement.spacedBy(InsetPadding),
        ) {
            options.forEachIndexed { index, option ->
                Segment(
                    label = option,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MedicalMateTheme.colors

    Surface(
        shape = MedicalMateRadius.sm,
        color = if (selected) colors.bgSurface else colors.bgSubtle,
        contentColor = if (selected) colors.fgDefault else colors.fgSubtle,
        modifier =
        modifier
            .fillMaxHeight()
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style =
                if (selected) {
                    MedicalMateTheme.typography.labelL
                } else {
                    MedicalMateTheme.typography.bodyM
                },
            )
        }
    }
}

/** 문서 8.2의 inset padding 4. */
private val InsetPadding = MedicalMateSpace.s4

/** 문서 8.2가 지정한 모바일 최대 칸 수. */
private const val MAX_SEGMENTS = 4

/** 칸 수 검사는 그리는 코드가 아니라 판단이라 밖에서도 확인할 수 있게 둔다. */
internal fun maxSegments(): Int = MAX_SEGMENTS

internal fun isValidSegmentCount(count: Int): Boolean = count in 2..MAX_SEGMENTS
