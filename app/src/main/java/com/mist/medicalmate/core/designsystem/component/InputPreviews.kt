package com.mist.medicalmate.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.MedicalMateSpace

/** 8.2 Input 컴포넌트 Preview. 렌더링만 하는 코드라 이것이 유일한 확인 수단이다. */
@Preview(showBackground = true, name = "Voice Input - State", widthDp = 390)
@Composable
private fun VoiceInputPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        MedicalMateVoiceState.entries.forEach { state ->
            MedicalMateVoiceInput(state = state, onMicClick = {}, onTypeInsteadClick = {})
        }
    }
}

@Preview(showBackground = true, name = "Text Field - State", widthDp = 390)
@Composable
private fun TextFieldPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        MedicalMateTextField(
            value = "",
            onValueChange = {},
            label = "복용 중인 약",
            placeholder = "약 이름을 적어주세요",
            helperText = "여러 개면 쉼표로 구분해요",
        )
        MedicalMateTextField(
            value = "타이레놀 500mg",
            onValueChange = {},
            label = "복용 중인 약",
        )
        MedicalMateTextField(
            value = "타",
            onValueChange = {},
            label = "복용 중인 약",
            errorText = "약 이름을 두 글자 이상 적어주세요",
        )
        MedicalMateTextField(
            value = "수정할 수 없어요",
            onValueChange = {},
            label = "복용 중인 약",
            enabled = false,
        )
    }
}

@Preview(showBackground = true, name = "Text Area", widthDp = 390)
@Composable
private fun TextAreaPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        MedicalMateTextArea(
            value = "",
            onValueChange = {},
            placeholder = "의사에게 더 말하고 싶은 내용이 있으면 적어주세요",
        )
        MedicalMateTextArea(
            value = "3주 전부터 오른쪽 손가락이 아침에 잘 안 펴져요. 요즘은 저녁에도 그래요.",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Severity Slider", widthDp = 390)
@Composable
private fun SeveritySliderPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        var severity by remember { mutableStateOf(MedicalMateSeverity.LEVEL_3) }
        MedicalMateSeveritySlider(
            severity = severity,
            onSeverityChange = { severity = it },
            showNrs = true,
        )
    }
}

@Preview(showBackground = true, name = "Severity Slider - 5단계", widthDp = 390, heightDp = 900)
@Composable
private fun SeveritySliderAllPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        MedicalMateSeverity.entries.forEach { level ->
            MedicalMateSeveritySlider(severity = level, onSeverityChange = {})
        }
    }
}

@Preview(showBackground = true, name = "Severity Select", widthDp = 390, heightDp = 700)
@Composable
private fun SeveritySelectPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s16) {
        MedicalMateSeveritySelect(severity = MedicalMateSeverity.LEVEL_4, onSeverityChange = {})
    }
}
