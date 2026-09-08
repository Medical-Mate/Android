package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.3 `KV Row`의 `Type` variant.
 *
 * [EDITING]은 문서에 없다. Figma가 나중에 늘린 variant(`597:4800`)를 옮긴 것이다.
 */
enum class MedicalMateKvRowType {
    DEFAULT,
    EMPHASIS,
    LINK,
    EDITING,
}

/**
 * DESIGN.md 8.3 `KV Row`.
 *
 * 의사가 훑어보는 자리다. key 열을 72로 고정해서 값이 세로로 정렬된다. 정렬이 깨지면
 * 여러 행을 눈으로 훑는 속도가 떨어진다.
 *
 * [MedicalMateKvRowType.EMPHASIS]는 카드당 최대 하나만 쓴다. 둘 이상이면 강조가 사라진다.
 *
 * [MedicalMateKvRowType.EDITING]은 값 아래에 `border/strong` 밑줄을 둔다. 고칠 수 있는
 * 값이라는 표시다. 밑줄만으로는 눌러야 한다는 것이 약하므로 [onClick]을 함께 준다.
 */
@Composable
fun MedicalMateKvRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
    type: MedicalMateKvRowType = MedicalMateKvRowType.DEFAULT,
    onClick: (() -> Unit)? = null,
) {
    val colors = MedicalMateTheme.colors
    val typography = MedicalMateTheme.typography
    val valueStyle =
        when (type) {
            MedicalMateKvRowType.EMPHASIS -> typography.bodyLStrong
            else -> typography.bodyL
        }
    val valueColor =
        when (type) {
            MedicalMateKvRowType.LINK -> colors.fgLink
            else -> colors.fgDefault
        }
    val valueModifier =
        if (type == MedicalMateKvRowType.EDITING) {
            Modifier.editingUnderline(colors.borderStrong)
        } else {
            Modifier
        }

    val row: @Composable () -> Unit = {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowHeightSm),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = key,
                style = typography.bodyM,
                color = colors.fgSubtle,
                modifier = Modifier.width(KeyColumnWidth),
            )
            Text(
                text = value,
                style = valueStyle,
                color = valueColor,
                modifier = valueModifier,
            )
        }
    }

    if (onClick == null) {
        Box(modifier = modifier, content = { row() })
    } else {
        Surface(
            onClick = onClick,
            color = colors.bgSurface,
            contentColor = colors.fgDefault,
            modifier = modifier,
            content = row,
        )
    }
}

/** DESIGN.md 8.3 `List Row`의 `Type` variant. */
enum class MedicalMateListRowType {
    DEFAULT,
    BADGE,
    PLAIN,
}

/**
 * DESIGN.md 8.3 `List Row`.
 *
 * 제목과 메타를 2단으로 둔다. chevron은 들어갈 상세가 있을 때만 붙인다. 눌러도 아무 일이
 * 없는데 chevron이 있으면 사용자가 눌러본다.
 *
 * [MedicalMateListRowType.BADGE]는 [badge]를 오른쪽에 놓고,
 * [MedicalMateListRowType.PLAIN]은 chevron을 두지 않는다.
 */
@Composable
fun MedicalMateListRow(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    badge: String? = null,
    type: MedicalMateListRowType = MedicalMateListRowType.DEFAULT,
    onClick: (() -> Unit)? = null,
) {
    val colors = MedicalMateTheme.colors
    val typography = MedicalMateTheme.typography

    val row: @Composable () -> Unit = {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowHeightLg)
                .padding(horizontal = MedicalMateSpace.s16),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
            ) {
                Text(text = title, style = typography.bodyL, color = colors.fgDefault)
                meta?.let {
                    Text(text = it, style = typography.bodyS, color = colors.fgSubtle)
                }
            }
            if (type == MedicalMateListRowType.BADGE && badge != null) {
                MedicalMateBadge(label = badge)
            }
            if (type != MedicalMateListRowType.PLAIN && onClick != null) {
                Icon(
                    painter = painterResource(MedicalMateIcons.ChevronRight),
                    contentDescription = null,
                    tint = colors.fgMuted,
                    modifier = Modifier.size(MedicalMateSize.iconMd),
                )
            }
        }
    }

    if (onClick == null) {
        Box(modifier = modifier, content = { row() })
    } else {
        Surface(
            onClick = onClick,
            color = colors.bgSurface,
            contentColor = colors.fgDefault,
            modifier = modifier,
            content = row,
        )
    }
}

/**
 * DESIGN.md 8.3 `Section Header`.
 *
 * 목록 제목과 보조 텍스트 링크를 한 줄에 둔다. 위 여백 24, 아래 10이다.
 */
@Composable
fun MedicalMateSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(top = MedicalMateSpace.s24, bottom = MedicalMateSpace.s10),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MedicalMateTheme.typography.headingS,
            color = MedicalMateTheme.colors.fgDefault,
        )
        if (actionLabel != null && onActionClick != null) {
            MedicalMateButton(
                onClick = onActionClick,
                label = actionLabel,
                type = MedicalMateButtonType.GHOST,
                size = MedicalMateButtonSize.S,
            )
        }
    }
}

/**
 * DESIGN.md 8.3 `Divider`.
 *
 * 고밀도 목록 사이에만 쓴다. 목록 구분은 우선 여백으로 해결하고, 인터랙션 행의 경계에는
 * 쓰지 않는다. 모든 행에 선을 두르면 화면이 선으로 가득 찬다(문서 5절).
 */
@Composable
fun MedicalMateDivider(modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        Surface(
            color = MedicalMateTheme.colors.borderSubtle,
            modifier = Modifier.fillMaxWidth().height(1.dp),
            content = {},
        )
    }
}

/**
 * 값 아래 밑줄.
 *
 * `Modifier.border`는 네 변을 다 두르므로 아래만 그린다. 인라인으로 고치는 칸이라
 * Text Field처럼 면을 채우면 행 높이가 늘어나 다른 KV Row와 정렬이 어긋난다.
 */
private fun Modifier.editingUnderline(color: Color): Modifier = this
    .fillMaxWidth()
    .drawBehind {
        val stroke = UnderlineWidth.toPx()
        val y = size.height - stroke / 2f
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = stroke,
        )
    }

private val UnderlineWidth = 1.dp

/** 문서 8.3의 KV Row와 Severity Readout이 쓰는 행 높이. */
private val RowHeightSm = 54.dp

/** 문서 8.3의 List Row 높이. */
private val RowHeightLg = 79.dp

/** 문서 8.3이 고정한 key 열 폭. 값 열이 세로로 정렬되게 만든다. */
private val KeyColumnWidth = 72.dp
