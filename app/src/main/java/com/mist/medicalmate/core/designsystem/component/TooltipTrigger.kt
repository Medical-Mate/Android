package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * Figma `Tooltip`(`542:1295`)의 트리거.
 *
 * **말풍선은 이 컴포넌트에 없다.** 마스터에 `State=Idle`과 `State=Active` 두 상태만
 * 있고 둘 다 48x48 트리거다. 이전에는 `State=Open` 260x100 말풍선이 있었는데 지금
 * 마스터에서 빠졌다. 무엇을 어떻게 띄우는지는 정해지지 않았고 DESIGN.md 8절에도 항목이
 * 없다. 그래서 지우기 전 모양을 되살리지 않고 지금 마스터만 옮겼다.
 *
 * 열고 닫는 것과 띄울 내용은 호출자가 정한다. [active]로 열림 상태를 받아 배경만 바꾼다.
 * 안내를 어디에 어떻게 붙일지 확정되면 그때 이 트리거를 감싸는 컴포넌트를 만든다.
 *
 * 규격은 Figma에서 픽셀로 재서 맞췄다. 히트 영역 48, Active 채움 32, 아이콘 24다.
 * 채움은 `bg/primary-faint`이고 아이콘은 두 상태 모두 `fg/default`다. Active에서 글자색이
 * 바뀌지 않는 점이 [MedicalMateIconButton]의 Tonal과 다르다.
 *
 * 열림 여부를 `expand`/`collapse` semantics 액션으로 알린다. 배경 색 변화만으로는
 * 스크린 리더에 전달되지 않고, `bg/primary-faint`는 흰 면에서 눈으로도 구분이 약하다.
 * 상태를 문구로 읽히게 하는 대신 액션을 쓰는 이유는, 보조기기가 읽기만 하지 않고 직접
 * 열고 닫을 수 있어야 하기 때문이다.
 */
@Composable
fun MedicalMateTooltipTrigger(
    active: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
        modifier.sizeIn(
            minWidth = MedicalMateSize.touchMin,
            minHeight = MedicalMateSize.touchMin,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            shape = MedicalMateRadius.full,
            color = Color.Transparent,
            contentColor = MedicalMateTheme.colors.fgDefault,
            modifier =
            Modifier
                .size(FillSize)
                .semantics {
                    if (active) {
                        collapse {
                            onClick()
                            true
                        }
                    } else {
                        expand {
                            onClick()
                            true
                        }
                    }
                },
        ) {
            Box(
                modifier =
                Modifier.background(
                    color =
                    if (active) {
                        MedicalMateTheme.colors.bgPrimaryFaint
                    } else {
                        Color.Transparent
                    },
                    shape = MedicalMateRadius.full,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(MedicalMateIcons.Info),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(MedicalMateSize.iconLg),
                )
            }
        }
    }
}

/** Active 채움 지름. Figma 렌더에서 48 박스 안 8~39 구간으로 측정했다. */
private val FillSize = 32.dp
