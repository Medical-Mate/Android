package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Bottom Sheet`.
 *
 * `ModalBottomSheet` 위에 얹었다. 끌어내리기, 뒤로 가기, 스크림, 접근성 처리를 직접
 * 만들면 셋 다 빠뜨리기 쉽다.
 *
 * 위쪽 두 각만 28로 둥글다. grabber는 36x4다(DESIGN.md 4.0이 적은 10-15 시트 규격). 아래
 * 여백 24는 문서가 지정한 값이고 기기 inset과 중복되지 않도록 `ModalBottomSheet`의 기본 창 여백을 그대로 쓴다.
 *
 * 10-15 시트에는 게시 컴포넌트가 없고 그래버 아래 간격과 그래버 색이 시트마다 다르다. [grabberGap]과
 * [grabberColor]로 받는다. 1e-1-S · 1e-1-X는 `border/default`, 1r-4-S는 `border/subtle`이다.
 *
 * **처음부터 내용 높이만큼 다 열린다.** `ModalBottomSheet`의 기본값은 내용이 길면 반만 열어 두고, 끌어올린
 * 뒤 뒤로 가기를 누르면 다시 반으로 내려간다. 10-15 시트는 모두 내용 높이(Hug)로 그려져 있고, 반만 열리면
 * 아래 내용이 기기 내비게이션 바 뒤로 들어간다(1r-4-S에서 기기 확인). 그래서 반 열림을 건너뛴다.
 *
 * 한 화면에서 떠 있는 층은 최대 두 단계다(문서의 고도 규칙). 시트 위에 시트나 대화상자를 다시
 * 띄우지 않는다.
 *
 * 행동 영역은 [MedicalMateSheetActions]로 따로 둔다. Figma의 `Action` variant가 버튼
 * 배치만 바꾸므로 시트 본체와 분리하는 편이 조합하기 쉽다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalMateBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    grabberGap: Dp = MedicalMateSpace.s12,
    grabberColor: Color = MedicalMateTheme.colors.borderDefault,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = MedicalMateTheme.colors.bgSurface,
        contentColor = MedicalMateTheme.colors.fgDefault,
        scrimColor = MedicalMateTheme.colors.bgScrim.copy(alpha = SCRIM_ALPHA),
        dragHandle = { SheetGrabber(gap = grabberGap, color = grabberColor) },
        modifier = modifier,
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = MedicalMateSize.gutter,
                    end = MedicalMateSize.gutter,
                    bottom = MedicalMateSize.safeBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            content = content,
        )
    }
}

/**
 * grabber.
 *
 * 접근성 트리에서 지운다. 스크린 리더 사용자는 끌어내리기 대신 뒤로 가기를 쓰고,
 * `ModalBottomSheet`가 그 처리를 이미 갖고 있다. 회색 막대를 읽어봐야 할 일이 없다.
 */
@Composable
private fun SheetGrabber(gap: Dp, color: Color) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(top = MedicalMateSpace.s12, bottom = gap)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
            Modifier
                .size(width = GrabberWidth, height = GrabberHeight)
                .background(
                    color = color,
                    shape = MedicalMateRadius.full,
                ),
        )
    }
}

/** DESIGN.md의 `Bottom Sheet`의 `Action` variant. */
enum class MedicalMateSheetAction {
    /** 주 행동과 보조 행동을 위아래로 둔다. 둘의 무게가 다를 때 쓴다. */
    STRONG,

    /** 대등한 선택지를 좌우로 둔다. */
    NEUTRAL,

    /** 단순 종료. 버튼 하나다. */
    CANCEL,
}

/**
 * 시트의 행동 영역.
 *
 * [MedicalMateSheetAction.NEUTRAL]에서 왼쪽이 Outline, 오른쪽이 Primary다. 문서의
 * "2개 병렬 시 왼쪽 Outline, 오른쪽 Primary"를 따른다.
 *
 * [MedicalMateSheetAction.STRONG]은 주 행동을 위에 둔다. 세로 배치에서는 위가 먼저
 * 읽힌다.
 */
@Composable
fun MedicalMateSheetActions(
    action: MedicalMateSheetAction,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
) {
    val hasSecondary = secondaryLabel != null && onSecondaryClick != null

    when (action) {
        MedicalMateSheetAction.STRONG ->
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
            ) {
                MedicalMateButton(
                    onClick = onPrimaryClick,
                    label = primaryLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hasSecondary) {
                    MedicalMateButton(
                        onClick = onSecondaryClick,
                        label = secondaryLabel,
                        type = MedicalMateButtonType.GHOST,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

        MedicalMateSheetAction.NEUTRAL ->
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            ) {
                if (hasSecondary) {
                    MedicalMateButton(
                        onClick = onSecondaryClick,
                        label = secondaryLabel,
                        type = MedicalMateButtonType.OUTLINE,
                        modifier = Modifier.weight(1f),
                    )
                }
                MedicalMateButton(
                    onClick = onPrimaryClick,
                    label = primaryLabel,
                    modifier = Modifier.weight(1f),
                )
            }

        MedicalMateSheetAction.CANCEL ->
            MedicalMateButton(
                onClick = onPrimaryClick,
                label = primaryLabel,
                type = MedicalMateButtonType.OUTLINE,
                modifier = modifier.fillMaxWidth(),
            )
    }
}

/** 문서의 컴포넌트 규격이 지정한 위쪽 반경 28. */
private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

private val GrabberWidth = 36.dp

private val GrabberHeight = 4.dp
