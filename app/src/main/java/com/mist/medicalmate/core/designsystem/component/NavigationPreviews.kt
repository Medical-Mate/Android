package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** 8.5 Navigation·Overlay 컴포넌트 Preview. */
@Preview(showBackground = true, name = "Nav Bar", widthDp = 390)
@Composable
private fun NavBarPreview() {
    MedicalMateTheme {
        Column(
            modifier = Modifier.background(MedicalMateTheme.colors.bgCanvas),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        ) {
            MedicalMateNavBar(title = "증상 정리", onLeadingClick = {})
            MedicalMateNavBar(
                title = "브리핑 카드",
                leading = MedicalMateNavLeading.CLOSE,
                onLeadingClick = {},
                actionLabel = "저장",
                onActionClick = {},
            )
            MedicalMateNavBar(
                title = "저장할 수 없는 상태",
                onLeadingClick = {},
                actionLabel = "저장",
                onActionClick = {},
                actionEnabled = false,
            )
            MedicalMateNavBar(title = "leading 없음", leading = MedicalMateNavLeading.NONE)
            MedicalMateNavBar(
                title = "제목이 아주 길어서 한 줄에 담기지 않는 경우에는 말줄임표로 잘린다",
                onLeadingClick = {},
                actionLabel = "완료",
                onActionClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Tab Bar", widthDp = 390)
@Composable
private fun TabBarPreview() {
    MedicalMateTheme {
        Column(
            modifier = Modifier.background(MedicalMateTheme.colors.bgCanvas),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        ) {
            MedicalMateTab.entries.forEach { tab ->
                MedicalMateTabBar(selected = tab, onSelect = {})
            }
        }
    }
}

@Preview(showBackground = true, name = "Tab Bar - 선택 동작", widthDp = 390)
@Composable
private fun TabBarInteractivePreview() {
    MedicalMateTheme {
        var selected by remember { mutableStateOf(MedicalMateTab.HOME) }
        MedicalMateTabBar(selected = selected, onSelect = { selected = it })
    }
}

@Preview(showBackground = true, name = "Sheet Actions", widthDp = 390)
@Composable
private fun SheetActionsPreview() {
    MedicalMateTheme {
        Column(
            modifier =
            Modifier
                .background(MedicalMateTheme.colors.bgSurface)
                .padding(MedicalMateSize.gutter),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s24),
        ) {
            Text(text = "STRONG", style = MedicalMateTheme.typography.labelS)
            MedicalMateSheetActions(
                action = MedicalMateSheetAction.STRONG,
                primaryLabel = "이어서 작성",
                onPrimaryClick = {},
                secondaryLabel = "새로 시작",
                onSecondaryClick = {},
            )
            Text(text = "NEUTRAL", style = MedicalMateTheme.typography.labelS)
            MedicalMateSheetActions(
                action = MedicalMateSheetAction.NEUTRAL,
                primaryLabel = "확인",
                onPrimaryClick = {},
                secondaryLabel = "취소",
                onSecondaryClick = {},
            )
            Text(text = "CANCEL", style = MedicalMateTheme.typography.labelS)
            MedicalMateSheetActions(
                action = MedicalMateSheetAction.CANCEL,
                primaryLabel = "닫기",
                onPrimaryClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Overlay Scrim", widthDp = 390, heightDp = 300)
@Composable
private fun OverlayScrimPreview() {
    MedicalMateTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(MedicalMateSize.gutter),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
            ) {
                repeat(SCRIM_PREVIEW_ROWS) {
                    Box(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(ScrimPreviewRowHeight)
                            .background(MedicalMateTheme.colors.bgSurface),
                    )
                }
            }
            MedicalMateOverlayScrim(onDismiss = {})
            Text(
                text = "스크림 50%",
                style = MedicalMateTheme.typography.bodyL,
                color = MedicalMateTheme.colors.fgOnInverse,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private const val SCRIM_PREVIEW_ROWS = 4

private val ScrimPreviewRowHeight = 48.dp
