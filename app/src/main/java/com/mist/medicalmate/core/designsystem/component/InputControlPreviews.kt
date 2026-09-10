package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.mist.medicalmate.core.designsystem.MedicalMateSpace

/** Input 묶음 중 선택·전환 컴포넌트 Preview. */
@Preview(showBackground = true, name = "Checkbox / Radio / Toggle", widthDp = 390)
@Composable
private fun SelectionPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        MedicalMateCheckbox(checked = true, onCheckedChange = {}, label = "이용약관에 동의해요")
        MedicalMateCheckbox(checked = false, onCheckedChange = {}, label = "마케팅 수신에 동의해요")
        MedicalMateCheckbox(
            checked = false,
            onCheckedChange = {},
            label = "선택할 수 없어요",
            enabled = false,
        )
        MedicalMateRadio(selected = true, onSelect = {}, label = "오른쪽")
        MedicalMateRadio(selected = false, onSelect = {}, label = "왼쪽")
        MedicalMateToggle(checked = true, onCheckedChange = {}, label = "복약 알림 받기")
        MedicalMateToggle(checked = false, onCheckedChange = {}, label = "가족에게 공유하기")
    }
}

@Preview(showBackground = true, name = "Segmented Control", widthDp = 390)
@Composable
private fun SegmentedControlPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        var index by remember { mutableStateOf(0) }
        MedicalMateSegmentedControl(
            options = listOf("진료 전", "진료 후"),
            selectedIndex = index,
            onSelect = { index = it },
        )
        MedicalMateSegmentedControl(
            options = listOf("전체", "확정", "작성 중", "지난 것"),
            selectedIndex = 1,
            onSelect = {},
        )
    }
}

@Preview(showBackground = true, name = "Date Cell", widthDp = 390)
@Composable
private fun DateCellPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s2)) {
            MedicalMateDateCell(day = 1, selected = false, onClick = {})
            MedicalMateDateCell(day = 2, selected = false, onClick = {}, isToday = true)
            MedicalMateDateCell(day = 3, selected = true, onClick = {})
            MedicalMateDateCell(day = 4, selected = false, onClick = {}, marker = MedicalMateDateMarker.RECORD)
            MedicalMateDateCell(day = 5, selected = true, onClick = {}, marker = MedicalMateDateMarker.RECORD)
            MedicalMateDateCell(day = 6, selected = false, onClick = {}, marker = MedicalMateDateMarker.PLANNED)
            MedicalMateDateCell(day = 7, selected = false, onClick = {}, enabled = false)
        }
    }
}

@Preview(showBackground = true, name = "Social Login", widthDp = 390)
@Composable
private fun SocialLoginPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        // 백엔드가 카카오만 지원한다. 나머지는 variant 확인용이다.
        MedicalMateSocialProvider.entries.forEach { provider ->
            MedicalMateSocialLoginButton(
                provider = provider,
                label = "${provider.name} 로그인",
                contentDescription = "${provider.name}로 로그인",
                onClick = {},
            )
        }
        MedicalMateSocialLoginButton(
            provider = MedicalMateSocialProvider.KAKAO,
            label = "카카오 로그인",
            contentDescription = "카카오로 로그인",
            onClick = {},
            inProgress = true,
        )
    }
}

@Preview(showBackground = true, name = "Tooltip Trigger", widthDp = 390)
@Composable
private fun TooltipTriggerPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        var active by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8)) {
            MedicalMateTooltipTrigger(
                active = false,
                onClick = {},
                contentDescription = "이 항목 설명 보기",
            )
            MedicalMateTooltipTrigger(
                active = true,
                onClick = {},
                contentDescription = "이 항목 설명 닫기",
            )
            MedicalMateTooltipTrigger(
                active = active,
                onClick = { active = !active },
                contentDescription = "눌러서 열고 닫기",
            )
        }
    }
}

@Preview(showBackground = true, name = "Tooltip Bubble", widthDp = 390)
@Composable
private fun TooltipBubblePreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        MedicalMateTooltipBubble(text = "여기에 짧은 설명이 들어갑니다")
        // 꼬리 위치는 트리거 위치에 맞춰 옮긴다.
        MedicalMateTooltipBubble(
            text = "꼬리를 왼쪽으로",
            arrowOffsetFromEnd = MedicalMateSpace.s40 * 3,
        )
    }
}
