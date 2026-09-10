package com.mist.medicalmate.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mist.medicalmate.core.designsystem.MedicalMateSpace

/**
 * DESIGN.md 3.0이 더한 컴포넌트 7종 Preview.
 *
 * 기존 묶음 파일에 나눠 넣지 않고 한곳에 모았다. 넣으려던 `ContentPreviews`가 파일당 함수
 * 상한(11)에 걸렸고, 문서도 이 7종을 신규로 묶어 표시한다. 폭은 360이다. 다른 Preview
 * 파일은 아직 390으로 남아 있다.
 */
@Preview(showBackground = true, name = "Add Row", widthDp = 360)
@Composable
private fun AddRowPreview() {
    ComponentPreviewSurface {
        MedicalMateAddRow(label = "할 일 추가", onClick = {})
        MedicalMateAddRow(label = "질문 추가", onClick = {})
    }
}

@Preview(showBackground = true, name = "Select Bar", widthDp = 360)
@Composable
private fun SelectBarPreview() {
    ComponentPreviewSurface {
        MedicalMateSelectBar(text = "2건 선택됨")
    }
}

@Preview(showBackground = true, name = "Todo Row", widthDp = 360)
@Composable
private fun TodoRowPreview() {
    ComponentPreviewSurface {
        MedicalMateTodoRow(checked = true, onCheckedChange = {}, label = "달라진 증상 있으면 카드 수정")
        MedicalMateTodoRow(checked = false, onCheckedChange = {}, label = "복용 중인 약 사진 찍기")
        MedicalMateTodoRow(
            checked = false,
            onCheckedChange = {},
            label = "편집 모드에서는 지울 수 있다",
            delete = MedicalMateRowDelete(contentDescription = "할 일 삭제", onClick = {}),
        )
    }
}

@Preview(showBackground = true, name = "Picker Field", widthDp = 360)
@Composable
private fun PickerFieldPreview() {
    ComponentPreviewSurface {
        MedicalMatePickerField(value = null, placeholder = "병원을 골라주세요", onClick = {})
        MedicalMatePickerField(value = "서울OO병원 내과", placeholder = "병원을 골라주세요", onClick = {})
        MedicalMatePickerField(
            value = "서울OO병원 내과",
            placeholder = "병원을 골라주세요",
            onClick = {},
            enabled = false,
        )
    }
}

@Preview(showBackground = true, name = "Card Pick", widthDp = 360)
@Composable
private fun CardPickPreview() {
    ComponentPreviewSurface(gap = MedicalMateSpace.s12) {
        MedicalMateCardPick(
            selected = true,
            onSelectedChange = {},
            title = "복부 통증 · 3주",
            meta = "09.04 작성 · 서울OO병원 내과",
        )
        MedicalMateCardPick(
            selected = false,
            onSelectedChange = {},
            title = "두통 · 잦은 어지러움",
            meta = "08.21 작성 · 카드만 작성됨",
        )
    }
}

@Preview(showBackground = true, name = "Hospital Card", widthDp = 360)
@Composable
private fun HospitalCardPreview() {
    ComponentPreviewSurface {
        MedicalMateHospitalCard(
            name = "서울OO병원 내과",
            address = "서울 관악구 남부순환로 1820, 3층",
            chips =
            listOf(
                MedicalMateHospitalChip("09.12 진료", MedicalMateHospitalChipTone.PAST),
                MedicalMateHospitalChip("09.26 재방문", MedicalMateHospitalChipTone.PLANNED),
            ),
        )
        MedicalMateHospitalCard(name = "서울OO병원 내과", address = "서울 관악구 남부순환로 1820, 3층")
    }
}

@Preview(showBackground = true, name = "Onboarding Progress", widthDp = 360)
@Composable
private fun OnboardingProgressPreview() {
    ComponentPreviewSurface {
        repeat(4) { index -> MedicalMateOnboardingProgress(current = index + 1) }
    }
}
