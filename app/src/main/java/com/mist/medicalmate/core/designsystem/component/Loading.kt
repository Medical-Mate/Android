package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Loading`의 `Type=Spinner`.
 *
 * 2초 이상 걸릴 것 같으면 [message]에 무슨 일을 하는 중인지 적는다(문서의 컴포넌트 규격, 9절).
 * 돌아가는 원만 보이면 사용자는 앱이 멈춘 것인지 판단할 근거가 없다.
 *
 * 실패했을 때 복구 행동을 함께 주는 것은 호출자 몫이다. 이 컴포넌트는 진행 중만 그린다.
 */
@Composable
fun MedicalMateLoadingSpinner(modifier: Modifier = Modifier, message: String? = null) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = LoadingMinHeight)
            .padding(MedicalMateSpace.s20),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(
            color = MedicalMateTheme.colors.bgPrimary,
            modifier = Modifier.size(MedicalMateSize.iconLg),
        )
        message?.let {
            Text(
                text = it,
                style = MedicalMateTheme.typography.bodyM,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
    }
}

/**
 * DESIGN.md의 `Loading`의 `Type=Skeleton`.
 *
 * [lineWidthFractions]에 실제로 들어올 줄의 폭 비율을 준다. 문서가 실제 줄 수와 폭을
 * 맞추라고 하는 이유는, 뼈대와 실제 내용의 모양이 다르면 로딩이 끝나는 순간 화면이
 * 덜컥 움직이기 때문이다.
 *
 * 반짝임을 넣지 않았다. 문서의 접근성 기준이 움직임 축소 설정에서 움직임을 줄이라고 하는데,
 * 애니메이션을 넣으면 그 설정을 읽어 끄는 처리가 함께 필요하다. 정지 상태로 두면 두
 * 경우가 같아진다.
 *
 * 접근성 트리에서는 "불러오는 중"으로 한 번만 읽는다. 회색 막대를 줄마다 읽으면 소리만
 * 길어진다.
 */
@Composable
fun MedicalMateLoadingSkeleton(
    loadingDescription: String,
    modifier: Modifier = Modifier,
    lineWidthFractions: List<Float> = DefaultSkeletonLines,
) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = LoadingMinHeight)
            .padding(MedicalMateSpace.s20)
            .clearAndSetSemantics { contentDescription = loadingDescription },
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
    ) {
        lineWidthFractions.forEach { fraction ->
            Surface(
                shape = MedicalMateRadius.xs,
                color = MedicalMateTheme.colors.bgSubtle,
                modifier =
                Modifier
                    .fillMaxWidth(fraction)
                    .height(SkeletonLineHeight),
                content = {},
            )
        }
    }
}

/** 문서의 116/132를 아우르는 최소 높이. */
private val LoadingMinHeight = 116.dp

private val SkeletonLineHeight = 16.dp

/** 제목 한 줄과 본문 두 줄. 목록이 아닌 카드 하나를 기다릴 때의 기본값이다. */
private val DefaultSkeletonLines = listOf(0.6f, 1f, 0.8f)
