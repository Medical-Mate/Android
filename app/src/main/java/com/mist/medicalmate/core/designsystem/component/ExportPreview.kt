package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateEffects
import com.mist.medicalmate.core.designsystem.MedicalMateFontFamily
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Export Preview / Long Image`(`3610:10245`). 1e-1-X 시트의 미리보기 300 높이.
 *
 * `bg/subtle` 면(반경 16) 가운데에 축소 카드(`Card Thumbnail / Long Image`, 160×250)를 두고, 면의 오른쪽 아래
 * (오른쪽 14 · 아래 12)에 `Chip Type=Info`("긴 이미지 1장")를 둔다. 칩은 축소 카드가 아니라 면에 붙는다 — 화면이
 * 360보다 넓어 면이 넓어져도 칩은 면 모서리에 남는다. 카드 전체가 긴 이미지 한 장으로 나간다는 것을 모양으로 보이는
 * 그림이다. 제목과 환자 줄만 글이고 나머지는 회색 막대다.
 *
 * 축소 카드의 값(글자 9.492 · 8.136, 막대 폭 131.525 같은 것)은 마스터가 실제 카드를 0.678배로 줄여 그린
 * 값이라 그대로 옮긴다. 그림이라 기기 글꼴 크기를 따라 커지지 않게 글자도 dp로 잰다. 스크린 리더에는 읽히지
 * 않는다 — 같은 내용을 시트 설명이 글로 적는다.
 */
@Composable
fun MedicalMateExportPreview(title: String, patientLine: String, chipLabel: String, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(PreviewHeight)
            .clip(MedicalMateRadius.md)
            .background(MedicalMateTheme.colors.bgSubtle)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        CardThumbnail(title = title, patientLine = patientLine)
        MedicalMateInfoChip(
            label = chipLabel,
            modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = MedicalMateSpace.s14, bottom = MedicalMateSpace.s12),
        )
    }
}

/**
 * `Chip Type=Info`(`3610:10205`). 누르지 않는 정보 칩이다. `bg/primary-strong` 면 · 반경 20 · 좌우 10 위아래 4에
 * `Label/XS` `fg/on-inverse` 글자다.
 */
@Composable
fun MedicalMateInfoChip(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MedicalMateTheme.typography.labelXS,
        color = MedicalMateTheme.colors.fgOnInverse,
        modifier =
        modifier
            .background(MedicalMateTheme.colors.bgPrimaryStrong, MedicalMateRadius.lg)
            .padding(horizontal = MedicalMateSpace.s10, vertical = MedicalMateSpace.s4),
    )
}

/** `Card Thumbnail / Long Image`(`3610:10202`). 흰 면 · `Elevation/Card` · 안쪽 7 · 줄 사이 4. */
@Composable
private fun CardThumbnail(title: String, patientLine: String) {
    val colors = MedicalMateTheme.colors
    val shape = RoundedCornerShape(8.136.dp)
    Column(
        modifier =
        Modifier
            .size(width = ThumbnailWidth, height = ThumbnailHeight)
            .dropShadow(shape, MedicalMateEffects.cardWide)
            .dropShadow(shape, MedicalMateEffects.cardTight)
            .clip(shape)
            .background(colors.bgSurface)
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, style = thumbnailText(9.492.dp, FontWeight.Bold), color = colors.fgDefault, maxLines = 1)
        Text(
            text = patientLine,
            style = thumbnailText(8.136.dp, FontWeight.Normal),
            color = colors.fgMuted,
            maxLines = 1,
        )
        thumbnailBars().forEach { bar ->
            Box(
                modifier =
                Modifier
                    .size(width = bar.width, height = bar.height)
                    .background(bar.color, RoundedCornerShape(bar.radius)),
            )
        }
    }
}

/** 마스터의 글자는 `leading-[normal]`이다. 줄 높이를 따로 두지 않는다. */
@Composable
private fun thumbnailText(size: Dp, weight: FontWeight): TextStyle = TextStyle(
    fontFamily = MedicalMateFontFamily,
    fontWeight = weight,
    fontSize = with(LocalDensity.current) {
        size.toSp()
    },
)

private class ThumbnailBar(val width: Dp, val height: Dp, val radius: Dp, val color: Color)

/** 마스터의 막대 열넷을 위에서부터. 노란 막대는 알러지 경고, 옅은 파란 막대는 질문 묶음 자리다. */
@Composable
private fun thumbnailBars(): List<ThumbnailBar> {
    val colors = MedicalMateTheme.colors
    fun line(width: Dp) = ThumbnailBar(width, 4.068.dp, 2.034.dp, colors.bgSubtle)
    return listOf(
        line(131.525.dp),
        line(103.051.dp),
        line(116.61.dp),
        line(88.136.dp),
        line(73.22.dp),
        ThumbnailBar(146.441.dp, 21.695.dp, 7.232.dp, colors.bgWarning),
        line(88.136.dp),
        line(124.746.dp),
        line(109.831.dp),
        ThumbnailBar(146.441.dp, 59.661.dp, 10.847.dp, colors.bgPrimaryFaint),
        line(73.22.dp),
        line(116.61.dp),
        ThumbnailBar(146.441.dp, 28.475.dp, 9.492.dp, colors.bgSubtle),
        line(66.441.dp),
    )
}

private val PreviewHeight = 300.dp

private val ThumbnailWidth = 160.dp

private val ThumbnailHeight = 250.dp
