package com.mist.medicalmate.intake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 인체도를 담는 판. Figma `Body Map`(`387:4164`) 266x396.
 *
 * [height]를 밖에서 받는다. 좌표에서 계산한 값이라([bodyMapMinHeight]) 보여주는 면과
 * 확대하는 부위마다 다르다.
 *
 * 판의 높이가 화면에 남은 높이보다 클 수 있다. 그때는 본문이 스크롤된다. 인체도를
 * 화면에 맞춰 줄이면 점의 조작 영역이 겹치므로 줄이는 쪽을 택하지 않는다.
 */
@Composable
internal fun BodyMapCard(
    height: Dp,
    orientationLabels: Boolean,
    caption: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(MedicalMateRadius.md)
            .background(MedicalMateTheme.colors.bgSubtle),
    ) {
        content()
        if (orientationLabels) {
            OrientationLabel(
                text = stringResource(R.string.body_map_orientation_right),
                modifier = Modifier.align(Alignment.TopStart).padding(LabelInset),
            )
            OrientationLabel(
                text = stringResource(R.string.body_map_orientation_left),
                modifier = Modifier.align(Alignment.TopEnd).padding(LabelInset),
            )
        }
        if (caption != null) {
            SelectionCaption(
                text = caption,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = LabelInset),
            )
        }
    }
}

/**
 * 좌우 안내. 시안이 `RIGHT` · `LEFT`를 영문 대문자로 뒀다.
 *
 * 앞면은 화면 왼쪽이 본인의 오른쪽이라 반대로 읽힌다. 그 혼동을 막는 것이 이 라벨의
 * 유일한 목적이다. 뒷면은 방향이 같아서 붙이지 않는다.
 *
 * 스크린 리더에서는 읽히지 않게 한다. 점마다 "왼쪽·오른쪽"이 이름에 들어 있어서 이
 * 라벨은 화면을 보는 사람에게만 필요하다.
 */
@Composable
private fun OrientationLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MedicalMateTheme.typography.labelS,
        color = MedicalMateTheme.colors.fgMuted,
        modifier =
        modifier
            .clearAndSetSemantics { }
            .clip(MedicalMateRadius.xs)
            .background(MedicalMateTheme.colors.bgSurface)
            .padding(horizontal = MedicalMateSpace.s6, vertical = MedicalMateSpace.s2),
    )
}

/**
 * 고른 부위를 판 안에 겹쳐 보여주는 알약. 시안의 `Selected Label`이다.
 *
 * 판 밖에 두지 않는 이유는 짚은 점과 이름이 한눈에 이어져야 하기 때문이다. 점은 작고
 * 이름이 없어서, 눌린 것이 무엇인지 확인할 곳이 필요하다.
 */
@Composable
private fun SelectionCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MedicalMateTheme.typography.labelM,
        color = MedicalMateTheme.colors.fgOnInverse,
        modifier =
        modifier
            .clip(MedicalMateRadius.md)
            .background(MedicalMateTheme.colors.bgInverse)
            .padding(horizontal = MedicalMateSpace.s12, vertical = MedicalMateSpace.s6),
    )
}

/** 시안이 라벨을 판 안쪽 12에 뒀다. */
private val LabelInset = 12.dp
