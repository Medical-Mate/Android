package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mist.medicalmate.core.designsystem.MedicalMateIcons

/**
 * 삭제 ×를 붙이는 방법. 동작과 접근성 이름을 함께 받는다.
 *
 * 둘을 따로 받으면 이름 없이 버튼만 켜는 호출이 가능해진다. 아이콘 하나뿐인 버튼은 이름이
 * 없으면 스크린 리더에서 무엇을 지우는지 알 수 없다.
 */
data class MedicalMateRowDelete(val contentDescription: String, val onClick: () -> Unit)

/**
 * Figma `Todo Row`(`1129:9197`) 320x54.
 *
 * 진료 전 할 일 한 줄이다. 체크박스와 할 일, 그리고 편집 모드의 삭제 ×로 이뤄진다.
 *
 * 체크 부분은 [MedicalMateCheckbox]를 그대로 쓴다. 마스터의 상자 24 · 반경 8 · 간격 12 ·
 * 행 높이 54가 그 컴포넌트와 같은 값이고, 행 전체를 hit area로 삼는 규칙도 같다. 여기서
 * 다시 그리면 두 곳이 갈린다.
 *
 * [delete]를 주면 오른쪽에 ×가 붙는다. 문서가 이 ×를 화면 단위 액션으로 봐서 L 크기(48
 * 상자 · 24 아이콘)를 쓴다. 목록 안 항목이라 S로 보일 수 있지만, 문서의 삭제 × 배치
 * 기준이 진료 전 할 일을 L로 못박았다.
 *
 * 삭제에 확인을 붙이지 않는다. 개체가 아니라 안의 항목이고, 편집 모드를 벗어나기 전이면
 * 취소가 실행 취소를 대신한다.
 */
@Composable
fun MedicalMateTodoRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    delete: MedicalMateRowDelete? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MedicalMateCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            label = label,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        if (delete != null) {
            MedicalMateIconButton(
                onClick = delete.onClick,
                icon = MedicalMateIcons.Close,
                contentDescription = delete.contentDescription,
                style = MedicalMateIconButtonStyle.GHOST,
                size = MedicalMateIconButtonSize.L,
                enabled = enabled,
            )
        }
    }
}
