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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint

/**
 * DESIGN.md의 `KV Row`의 `Type` variant.
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
 * 행에 삭제 ×를 붙이는 방법. 동작과 접근성 이름을 함께 받는다.
 *
 * 둘을 따로 받으면 이름 없이 버튼만 켜는 호출이 가능해진다. 아이콘 하나뿐인 버튼은 이름이
 * 없으면 스크린 리더에서 무엇을 지우는지 알 수 없다.
 *
 * [MedicalMateTodoRow]와 [MedicalMateEditingKvRow]가 함께 쓴다.
 */
data class MedicalMateRowDelete(val contentDescription: String, val onClick: () -> Unit)

/**
 * 편집 모드의 KV 행. 값 오른쪽에 삭제 ×가 붙는다.
 *
 * 브리핑 카드(1e-1-E)와 진료 후 기록(1q-1-E)이 같은 모양을 쓴다. 두 화면이 각자 조립하면
 * ×의 크기나 값 폭이 갈린다.
 *
 * ×를 [MedicalMateKvRow] 안에 넣지 않는다. 그 컴포넌트는 키 열을 72로 고정해 값의 정렬을
 * 맞추는 것이 일이고, 오른쪽에 버튼이 들어가면 값 폭이 행마다 달라진다. 그래서 행을 감싸서
 * 바깥에 둔다. 값의 밑줄은 그만큼 짧아지고, 시안도 그렇게 그려져 있다.
 *
 * ×는 S 크기(32 상자 · 18 아이콘)다. 문서가 항목 안의 삭제를 S로, 화면·필드 단위 삭제를
 * L로 못박았다. 크기 차이가 곧 위계다.
 */
@Composable
fun MedicalMateEditingKvRow(
    key: String,
    value: String,
    onValueChange: (String) -> Unit,
    delete: MedicalMateRowDelete,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MedicalMateKvRow(
            key = key,
            value = value,
            type = MedicalMateKvRowType.EDITING,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
        )
        MedicalMateIconButton(
            onClick = delete.onClick,
            icon = MedicalMateIcons.Close,
            contentDescription = delete.contentDescription,
            size = MedicalMateIconButtonSize.S,
        )
    }
}

/**
 * DESIGN.md의 `KV Row`.
 *
 * 의사가 훑어보는 자리다. key 열을 72로 고정해서 값이 세로로 정렬된다. 정렬이 깨지면
 * 여러 행을 눈으로 훑는 속도가 떨어진다.
 *
 * [MedicalMateKvRowType.EMPHASIS]는 카드당 최대 하나만 쓴다. 둘 이상이면 강조가 사라진다.
 *
 * [MedicalMateKvRowType.EDITING]은 값 아래에 `border/strong` 밑줄을 둔다. 고칠 수 있는
 * 값이라는 표시다. 밑줄만으로는 눌러야 한다는 것이 약하므로 [onClick]을 함께 준다.
 *
 * [onValueChange]를 주면 그 자리에서 고친다. 브리핑 카드의 전체 수정(1e-1-E)이 그 경우다.
 * 값만 입력으로 바뀌고 키 폭과 행 높이는 그대로여서 다른 행과의 정렬이 흐트러지지 않는다.
 * 주지 않으면 밑줄만 그린 읽기 값이다.
 */
@Composable
fun MedicalMateKvRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
    type: MedicalMateKvRowType = MedicalMateKvRowType.DEFAULT,
    onClick: (() -> Unit)? = null,
    onValueChange: ((String) -> Unit)? = null,
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
            KvValue(
                value = value,
                style = valueStyle.copy(color = valueColor),
                onValueChange = onValueChange.takeIf { type == MedicalMateKvRowType.EDITING },
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

/** DESIGN.md의 `List Row`의 `Type` variant. */
enum class MedicalMateListRowType {
    DEFAULT,
    BADGE,
    PLAIN,
}

/**
 * DESIGN.md의 `List Row`.
 *
 * 제목과 메타를 2단으로 둔다. chevron은 들어갈 상세가 있을 때만 붙인다. 눌러도 아무 일이
 * 없는데 chevron이 있으면 사용자가 눌러본다.
 *
 * **[badge]는 제목 바로 옆에 붙는다**(#227). 마스터의 `Title Row`가 제목과 배지를 간격 6으로
 * 한 줄에 묶는다. 오른쪽 끝에 두면 chevron과 나란히 서서 누르는 것으로 보이고, 제목이 길어질
 * 때 제목과 배지 사이가 벌어져 어느 줄의 상태인지가 흐려진다.
 *
 * [MedicalMateListRowType.BADGE]는 그 배지를 켜고, [MedicalMateListRowType.PLAIN]은
 * chevron을 두지 않는다.
 *
 * [badgeTone]은 배지가 무엇을 뜻하는지에 따라 고른다. 완료는 Success, 남은 일수처럼
 * 브랜드 정보는 Brand다. 기본은 중립이다.
 */
@Composable
fun MedicalMateListRow(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    badge: String? = null,
    badgeTone: MedicalMateBadgeTone = MedicalMateBadgeTone.NEUTRAL,
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
                .padding(start = RowStartPadding, end = RowEndPadding),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 마스터가 `Heading/S`다. `Body/L`은 같은 17이지만 Regular라 제목이 메타와
                    // 같은 무게로 읽힌다.
                    Text(text = title, style = typography.headingS, color = colors.fgDefault)
                    if (type == MedicalMateListRowType.BADGE && badge != null) {
                        MedicalMateBadge(label = badge, tone = badgeTone)
                    }
                }
                meta?.let {
                    Text(text = it, style = typography.bodyS, color = colors.fgSubtle)
                }
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

    RowSurface(onClick = onClick, modifier = modifier, content = row)
}

/** 줄의 면. 마스터가 카드 모양으로 띄운다 — 반경 16에 `Elevation/Card`다. */
@Composable
private fun RowSurface(onClick: (() -> Unit)?, modifier: Modifier, content: @Composable () -> Unit) {
    val colors = MedicalMateTheme.colors
    val surface =
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = MedicalMateElevation.card,
                shape = MedicalMateRadius.md,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            )

    if (onClick == null) {
        Surface(
            color = colors.bgSurface,
            contentColor = colors.fgDefault,
            shape = MedicalMateRadius.md,
            modifier = surface,
            content = content,
        )
    } else {
        Surface(
            onClick = onClick,
            color = colors.bgSurface,
            contentColor = colors.fgDefault,
            shape = MedicalMateRadius.md,
            modifier = surface,
            content = content,
        )
    }
}

/**
 * DESIGN.md의 `Section Header`.
 *
 * 목록 제목과 오른쪽 슬롯을 한 줄에 둔다. 위 여백 24, 아래 10이다.
 *
 * 오른쪽은 두 갈래다. [actionLabel]은 누르는 링크이고 [caption]은 읽기만 하는 표시다.
 * 마스터(`334:1156`)의 `Action` 슬롯이 `fg/link`인데 1r-4가 그것을 `fg/subtle`로 덮어
 * "선택 안 함"·"1개"를 적는다. 누를 수 없는 글자를 링크 색으로 두면 눌러 보게 된다.
 * 둘을 함께 주지 않는다. 한 자리라 뒤에 오는 것이 앞을 덮는다.
 */
@Composable
fun MedicalMateSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    caption: String? = null,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(top = MedicalMateSpace.s24, bottom = MedicalMateSpace.s10),
        horizontalArrangement = Arrangement.SpaceBetween,
        // 마스터가 가운데 맞춤이다(#235). 아래 맞춤으로 두면 제목이 `Heading/M` 28이고
        // 액션이 `Body/M Strong` 24라 액션이 위로 떠 보인다.
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MedicalMateTheme.typography.headingM,
            color = MedicalMateTheme.colors.fgDefault,
        )
        when {
            actionLabel != null && onActionClick != null ->
                MedicalMateButton(
                    onClick = onActionClick,
                    label = actionLabel,
                    type = MedicalMateButtonType.GHOST,
                    size = MedicalMateButtonSize.S,
                )

            caption != null ->
                Text(
                    text = caption,
                    style = MedicalMateTheme.typography.bodyMStrong,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
        }
    }
}

/**
 * DESIGN.md의 `Divider`.
 *
 * 고밀도 목록 사이에만 쓴다. 목록 구분은 우선 여백으로 해결하고, 인터랙션 행의 경계에는
 * 쓰지 않는다. 모든 행에 선을 두르면 화면이 선으로 가득 찬다(문서의 고도 규칙).
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
 * 값 칸. [onValueChange]가 있으면 그 자리에서 고치고 없으면 읽기 값이다.
 *
 * 두 경우의 글자 모양이 같아야 한다. 수정으로 열릴 때 글자가 움직이면 무엇이 바뀐 것인지
 * 알기 어렵다.
 */
@Composable
private fun KvValue(
    value: String,
    style: TextStyle,
    onValueChange: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (onValueChange == null) {
        Text(text = value, style = style, modifier = modifier)
        return
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        cursorBrush = SolidColor(MedicalMateTheme.colors.borderFocus),
        modifier = modifier,
    )
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

/** 문서의 KV Row와 Severity Readout이 쓰는 행 높이. */
private val RowHeightSm = 54.dp

/** 문서의 List Row 높이. */
private val RowHeightLg = 79.dp

/** 마스터 `List Row`의 좌우 여백. 오른쪽 끝의 chevron·배지가 제 여백을 갖고 있다. */
private val RowStartPadding = 18.dp

private val RowEndPadding = 14.dp

/** 문서의 컴포넌트 규격이 고정한 key 열 폭. 값 열이 세로로 정렬되게 만든다. */
private val KeyColumnWidth = 72.dp
