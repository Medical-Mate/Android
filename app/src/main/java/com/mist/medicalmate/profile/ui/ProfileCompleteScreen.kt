package com.mist.medicalmate.profile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateLogo
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import kotlinx.coroutines.delay

/**
 * 와이어프레임 1b-4. Figma `676:2519`.
 *
 * 신상정보 세 화면(복용약·기저질환·알러지)을 끝내고 완료를 누르면 나온다. 버튼이 없어서
 * 머문 뒤 스스로 [onFinished]를 부른다. 다음은 `1b-4 · 홈 · 등록 완료 토스트`다.
 *
 * 머무는 시간은 모션이 끝나는 시점([ProfileCompleteMotion]의 660ms) 뒤로 문구를 읽을
 * 만큼을 더해 잡았다. 스플래시와 같은 값이다. Figma에 값이 없어서 정한 것이다.
 *
 * 들어올 때 [ProfileCompleteMotion]이 한 번 돈다. 화면이 다시 조합될 때마다 다시 돌지
 * 않도록 [Animatable]을 `remember`로 들고 있는다.
 */
@Composable
fun ProfileCompleteScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
        delay(DWELL_MILLIS)
        onFinished()
    }

    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgPrimary),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s16),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s20, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileCompleteMotion()
        }
        Box(modifier = Modifier.height(MedicalMateSize.safeBottom))
    }
}

/**
 * 완료 모션. Figma 프로토타입 전용 섹션 `704:3251`의 세 프레임이 키프레임이다.
 *
 * | 상태 | 왼쪽 조각 | 오른쪽 조각 | 제목 | 부제 |
 * | -- | -- | -- | -- | -- |
 * | M1 진입 | −148.5 | +148.5 | 0 | 0 |
 * | M2 충돌 | +4.0 | −4.0 | 0.35 | 0.12 |
 * | M3 반동 | −1.5 | +1.5 | 0.80 | 0.55 |
 * | 1b-4 정지 | 0 | 0 | 1.0 | 0.85 |
 *
 * 두 조각이 화면 밖에서 서로를 향해 날아와 지나쳐 부딪히고 되튕긴 뒤 제자리에 선다.
 * 네 프레임의 y가 모두 같아서 가로 이동만 있다. 조각은 항상 대칭이라 값 하나로 둘을
 * 움직인다.
 *
 * **스프링 하나로는 두 번의 흔들림을 낼 수 없다.** 감쇠 진동의 첫 오버슈트 비율은
 * `exp(-πζ/√(1-ζ²))`인데, 148.5에서 출발해 4.0만 지나치려면 ζ가 0.76이어야 하고 그러면
 * 두 번째 흔들림이 0.1도 안 남아 M3가 사라진다. 반대로 4.0 → 1.5 비율(0.375)에 ζ를
 * 맞추면 0.30이고, 그 값으로 148.5에서 출발하면 55를 지나쳐 버린다. 그래서 진입은
 * tween으로 충돌 지점까지 보내고, 거기서 정지까지를 스프링에 맡긴다. ζ = 0.30이면 첫
 * 반동이 `4.0 × exp(-π×0.3/√0.91) = 1.49`로 M3와 맞는다.
 *
 * 진입 easing은 [FastOutLinearInEasing]이다. 끝에서 가속해야 부딪히는 것으로 읽힌다.
 *
 * 문구 두 개는 같은 시간선 위의 [keyframes]다. 이동과 물리적으로 얽혀 있지 않고 네 상태의
 * 불투명도만 지나가면 된다.
 */
@Composable
private fun ProfileCompleteMotion() {
    val travel = remember { Animatable(-ENTRY_TRAVEL) }

    LaunchedEffect(Unit) {
        travel.animateTo(
            targetValue = OVERSHOOT,
            animationSpec = tween(durationMillis = ENTRY_DURATION_MS, easing = FastOutLinearInEasing),
        )
        travel.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = REBOUND_DAMPING_RATIO, stiffness = REBOUND_STIFFNESS),
        )
    }

    val titleAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = alphaKeyframes(contact = 0.35f, rebound = 0.80f, rest = 1f),
        label = "titleAlpha",
    )
    val subAlpha by animateFloatAsState(
        targetValue = SUB_REST_ALPHA,
        animationSpec = alphaKeyframes(contact = 0.12f, rebound = 0.55f, rest = SUB_REST_ALPHA),
        label = "subAlpha",
    )

    LogoMarkPair(offset = travel.value.dp)
    Text(
        text = stringResource(R.string.profile_complete_title),
        style = MedicalMateTheme.typography.headingL,
        color = MedicalMateTheme.colors.fgOnPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(titleAlpha),
    )
    Text(
        text = stringResource(R.string.profile_complete_description),
        style = MedicalMateTheme.typography.bodyM,
        color = MedicalMateTheme.colors.fgOnPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(subAlpha),
    )
}

/**
 * 마크의 두 조각. [offset]은 왼쪽 조각의 가로 이동이고 오른쪽은 부호만 뒤집는다.
 *
 * 조각을 겹쳐 두면 원래 마크가 된다. 둘 다 장식이라 접근성 이름을 주지 않는다. 화면의
 * 뜻은 아래 제목이 전한다.
 */
@Composable
private fun LogoMarkPair(offset: Dp) {
    Box(modifier = Modifier.size(MarkSize)) {
        Image(
            painter = painterResource(MedicalMateLogo.MarkUpper),
            contentDescription = null,
            colorFilter = markTint(),
            modifier = Modifier
                .fillMaxSize()
                .offset(x = offset),
        )
        Image(
            painter = painterResource(MedicalMateLogo.MarkLower),
            contentDescription = null,
            colorFilter = markTint(),
            modifier = Modifier
                .fillMaxSize()
                .offset(x = -offset),
        )
    }
}

@Composable
private fun markTint() = ColorFilter.tint(MedicalMateTheme.colors.fgOnPrimary)

/**
 * 문구 불투명도. 이동과 같은 시간선을 쓴다.
 *
 * 충돌은 진입이 끝나는 순간이고, 반동 정점은 스프링의 반주기 뒤다. 감쇠 진동의 반주기는
 * `π / (√stiffness × √(1-ζ²))`이라 지금 값으로 110ms가 나온다. 정지는 시간상수
 * `1 / (ζ√stiffness)`의 네 배를 더한 지점으로 뒀다.
 */
private fun alphaKeyframes(contact: Float, rebound: Float, rest: Float): KeyframesSpec<Float> = keyframes {
    durationMillis = REST_MS
    0f at 0
    contact at CONTACT_MS
    rebound at REBOUND_MS
    rest at REST_MS
}

/** Figma 마크 크기. 1b-4의 인스턴스가 106이다. */
private val MarkSize = 106.dp

/** M1의 조각 위치. 정지 기준 ±148.5로, 마크 폭보다 멀어 화면 밖에서 들어온다. */
private const val ENTRY_TRAVEL = 148.5f

/** M2에서 서로를 지나친 거리. */
private const val OVERSHOOT = 4f

/** M3의 반동이 1.5가 되는 값. 위 KDoc의 계산 결과다. */
private const val REBOUND_DAMPING_RATIO = 0.30f

/** 반동이 눈에 남되 늘어지지 않는 값. 반주기 110ms. */
private const val REBOUND_STIFFNESS = 900f

private const val ENTRY_DURATION_MS = 220
private const val CONTACT_MS = ENTRY_DURATION_MS
private const val REBOUND_MS = 330
private const val REST_MS = 660

/** 부제는 정지 상태에서도 완전히 불투명하지 않다. Figma `677:4017`이 0.85다. */
private const val SUB_REST_ALPHA = 0.85f

/** 화면에 머무는 시간. 모션 660ms가 끝나고 문구를 읽을 만큼 남는다. */
private const val DWELL_MILLIS = 2_000L

@MedicalMateScreenPreviews
@Composable
private fun ProfileCompleteScreenPreview() {
    MedicalMateTheme {
        ProfileCompleteScreen(onFinished = {})
    }
}
