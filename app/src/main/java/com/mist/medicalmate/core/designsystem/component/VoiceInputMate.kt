package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateEffects
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Voice Input · Mate`(`3539:5647`, #318). 받아쓰기 중에 뜨는 카드다.
 *
 * 흰 면 · 반경 24 · `Elevation/Card` 두 겹 · 위 28 아래 24 좌우 20이고, 가운데에 `Waveform`(72 · 막대 다섯) 아래 16
 * 띄워 `Heading/S` 제목과 `Body/S` 설명이 4 간격으로 선다. 마스터는 `Listening` 하나뿐이라 다른 상태(듣기 전 ·
 * 정리 중 · 권한 없음)도 같은 모양에 글만 바꾼다. 권한이 없으면 설명이 `fg/danger`다.
 *
 * 마스터 설명이 "다 말씀하시면 한 번 더 눌러주세요"라 카드 전체를 눌러도 듣기를 끝낸다([onClick]). 진료 후
 * 메모(1p-T)에서는 마이크 FAB가 카드에 자리를 내주어 이것이 끝내는 자리다.
 */
@Composable
fun MedicalMateVoiceInputMate(
    state: MedicalMateVoiceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = MedicalMateTheme.colors
    val shape = MedicalMateRadius.xl
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .dropShadow(shape, MedicalMateEffects.cardWide)
            .dropShadow(shape, MedicalMateEffects.cardTight)
            .clip(shape)
            .background(colors.bgSurface)
            .clickable(enabled = state != MedicalMateVoiceState.PROCESSING, role = Role.Button, onClick = onClick)
            .padding(
                start = MedicalMateSpace.s20,
                end = MedicalMateSpace.s20,
                top = TopPadding,
                bottom = MedicalMateSpace.s24,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s16),
    ) {
        Waveform()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                text = stringResource(state.titleRes),
                style = MedicalMateTheme.typography.headingS,
                color = colors.fgDefault,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description ?: stringResource(state.descriptionRes),
                style = MedicalMateTheme.typography.bodyS,
                color = if (state == MedicalMateVoiceState.DENIED) colors.fgDanger else colors.fgSubtle,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** `Waveform`(`3539:5606`) Size=L · 말하는 중. 72 안에 8 폭 막대 다섯이 8 간격 · 반경 4로 선다. */
@Composable
private fun Waveform() {
    Row(
        modifier = Modifier.size(WaveformSize).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarHeights.forEach { height ->
            Box(
                modifier =
                Modifier
                    .size(width = BarWidth, height = height)
                    .background(MedicalMateTheme.colors.bgPrimary, BarShape),
            )
        }
    }
}

private val TopPadding = 28.dp

private val WaveformSize = 72.dp

private val BarWidth = 8.dp

private val BarShape = RoundedCornerShape(4.dp)

private val BarHeights = listOf(26.dp, 46.dp, 62.dp, 42.dp, 26.dp)
