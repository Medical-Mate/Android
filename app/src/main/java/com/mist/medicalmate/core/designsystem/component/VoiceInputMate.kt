package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
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
 *
 * **파형이 움직인다**(#318). 디자인 트랙이 넘긴 [MateWaveform]의 `Large` 모양이다 — 듣는 중에는 천천히 숨 쉬고,
 * [speaking](받아쓰는 글)이 새로 들어오면 말하는 움직임으로 바뀐다. 기기에서 애니메이션을 껐으면 시안 모양 그대로
 * 멈춘다. 화면이 [title]·[description]으로 글을 고정할 수 있다.
 */
@Composable
fun MedicalMateVoiceInputMate(
    state: MedicalMateVoiceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    speaking: String = "",
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
        MateWaveform(level = rememberSpeechActivityLevel(partialText = speaking), shape = MateWaveformShape.Large)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                text = title ?: stringResource(state.titleRes),
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

private val TopPadding = 28.dp
