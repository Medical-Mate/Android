package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Figma `Add Row`(`1129:9199`) 320x48.
 *
 * 목록 끝에 붙어 항목을 하나 더 만드는 줄이다. 테두리도 면색도 없다. 새 입력 필드를
 * 띄우는 대신 목록에 빈 항목이 하나 생기는 것이 문서의 추가 방식이라, 이 줄은 버튼처럼
 * 보이지 않아야 한다.
 *
 * 높이 48이 마스터 값이면서 접근성 기준의 터치 하한과 같다. 그래서 [heightIn]으로 하한만
 * 준다. 글꼴 배율이 커지면 그만큼 늘어난다.
 *
 * 아이콘 18은 마스터 값이다. 문서의 인라인 아이콘 규칙은 `Body/L` 17 옆에 20을 쓰라고
 * 하는데 마스터가 18이다. 값의 정본이 Figma라 마스터를 따랐고, 디자인 트랙에 확인을
 * 넘겼다.
 */
@Composable
fun MedicalMateAddRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = MedicalMateSize.touchMin)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(MedicalMateIcons.Plus),
            contentDescription = null,
            tint = MedicalMateTheme.colors.fgPrimary,
            modifier = Modifier.size(MedicalMateSize.iconSm),
        )
        Text(
            text = label,
            style = MedicalMateTheme.typography.bodyLStrong,
            color = MedicalMateTheme.colors.fgPrimary,
        )
    }
}
