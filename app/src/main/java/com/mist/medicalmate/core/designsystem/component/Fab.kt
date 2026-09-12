package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Figma의 `FAB`. 48 원형에 브랜드 채움과 그림자다.
 *
 * 본문 흐름에 있지 않고 화면 위에 떠 있는 조작이다. 스크롤 어디에 있든 손이 닿아야 하는
 * 것에만 쓴다. 지금은 진료 후 메모(1p)의 마이크이고, 캘린더의 일정 추가도 이 자리다.
 *
 * [MedicalMateIconButton]의 `SOLID`와 크기·색이 같지만 그림자가 없다. 그쪽은 줄 안에 놓이는
 * 버튼이라 뜰 이유가 없고, 이쪽은 본문 위에 떠서 글을 가리므로 그림자로 층을 알린다.
 *
 * 그림자는 `Elevation/Card`와 같은 값을 쓴다. 시안에 FAB 전용 고도가 따로 없다.
 */
@Composable
fun MedicalMateFab(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MedicalMateRadius.full,
        color = MedicalMateTheme.colors.bgPrimary,
        contentColor = MedicalMateTheme.colors.fgOnPrimary,
        shadowElevation = FabElevation,
        modifier = modifier.size(MedicalMateSize.controlMd),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(MedicalMateSize.iconLg),
            )
        }
    }
}

private val FabElevation = 6.dp

@Preview(showBackground = true, backgroundColor = 0xFFF5F6FA)
@Composable
private fun MedicalMateFabPreview() {
    MedicalMateTheme {
        Surface(color = Color.Transparent) {
            MedicalMateFab(onClick = {}, icon = MedicalMateIcons.Mic, contentDescription = "음성으로 적기")
        }
    }
}
