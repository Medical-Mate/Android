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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.5 `Bottom Sheet`.
 *
 * `ModalBottomSheet` 위에 얹었다. 끌어내리기, 뒤로 가기, 스크림, 접근성 처리를 직접
 * 만들면 셋 다 빠뜨리기 쉽다.
 *
 * 위쪽 두 각만 28로 둥글다. grabber는 40x4다. 아래 여백 24는 문서가 지정한 값이고
 * 기기 inset과 중복되지 않도록 `ModalBottomSheet`의 기본 창 여백을 그대로 쓴다.
 *
 * 한 화면에서 떠 있는 층은 최대 두 단계다(문서 5절). 시트 위에 시트나 대화상자를 다시
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
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = MedicalMateTheme.colors.bgSurface,
        contentColor = MedicalMateTheme.colors.fgDefault,
        scrimColor = MedicalMateTheme.colors.bgScrim.copy(alpha = SCRIM_ALPHA),
        dragHandle = { SheetGrabber() },
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
private fun SheetGrabber() {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = MedicalMateSpace.s12)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
            Modifier
                .size(width = GrabberWidth, height = GrabberHeight)
                .background(
                    color = MedicalMateTheme.colors.borderDefault,
                    shape = MedicalMateRadius.full,
                ),
        )
    }
}

/** DESIGN.md 8.5 `Bottom Sheet`의 `Action` variant. */
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
 * [MedicalMateSheetAction.NEUTRAL]에서 왼쪽이 Outline, 오른쪽이 Primary다. 문서 8.1의
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

/** 문서 8.5가 지정한 위쪽 반경 28. */
private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

private val GrabberWidth = 40.dp

private val GrabberHeight = 4.dp
