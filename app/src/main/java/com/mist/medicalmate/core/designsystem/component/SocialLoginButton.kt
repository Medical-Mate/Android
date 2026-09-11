package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.AppleContainer
import com.mist.medicalmate.core.designsystem.AppleLabel
import com.mist.medicalmate.core.designsystem.GoogleBorder
import com.mist.medicalmate.core.designsystem.GoogleContainer
import com.mist.medicalmate.core.designsystem.GoogleLabel
import com.mist.medicalmate.core.designsystem.KakaoContainer
import com.mist.medicalmate.core.designsystem.KakaoLabel
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.NaverContainer
import com.mist.medicalmate.core.designsystem.NaverLabel

/**
 * DESIGN.md의 `Social Login Button`의 `Provider` variant.
 *
 * 색은 각 사 공식 브랜드 가이드가 정한 값이고 변경이 금지된다. 문서도 이 컴포넌트를
 * 브랜드 가이드 예외로 표시했다.
 *
 * Naver 조합은 공식 규격 때문에 WCAG 대비 기준을 벗어난다. 문서의 컴포넌트 규격이 그 사실을 밝히고
 * 다른 안내 요소는 기준을 지키라고 한다.
 */
enum class MedicalMateSocialProvider(
    internal val container: Color,
    internal val label: Color,
    internal val border: Color?,
    @DrawableRes internal val symbol: Int? = null,
) {
    KAKAO(KakaoContainer, KakaoLabel, null, R.drawable.ic_kakao_symbol),
    NAVER(NaverContainer, NaverLabel, null),
    APPLE(AppleContainer, AppleLabel, null),
    GOOGLE(GoogleContainer, GoogleLabel, GoogleBorder),
}

/**
 * DESIGN.md의 `Social Login Button`.
 *
 * 350x56에 radius 16이다. 컨테이너 규격은 문서를 따르고 색은 각 사 가이드를 따른다.
 *
 * 카카오 가이드는 radius 12를 적는다. DESIGN.md의 컴포넌트 규격이 16으로 정하고 같은 항목을 브랜드
 * 가이드 예외로 표시했으며, #39에서 문서 값으로 결정했다.
 *
 * 심볼은 각 사 공식 키트에서 받은 것만 넣는다. 임의로 그리면 가이드 위반이다. 카카오는
 * 디자인 시스템의 `Provider=Kakao` 마스터(`383:1290`)에 말풍선이 들어 있어 그것을 내보내
 * 썼다. 나머지 셋은 아직 없어서 라벨만 둔다.
 *
 * [contentDescription]을 따로 받는다. 라벨이 "카카오 로그인"이어도 스크린 리더에는
 * "카카오로 로그인"처럼 동작이 드러나는 문장이 낫다.
 */
@Composable
fun MedicalMateSocialLoginButton(
    provider: MedicalMateSocialProvider,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inProgress: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = enabled && !inProgress,
        shape = MedicalMateRadius.md,
        color = provider.container,
        contentColor = provider.label,
        border = provider.border?.let { BorderStroke(width = 1.dp, color = it) },
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = MedicalMateSize.controlLg)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MedicalMateSpace.s16),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (inProgress) {
                CircularProgressIndicator(
                    color = provider.label,
                    strokeWidth = ProgressStroke,
                    modifier = Modifier.size(MedicalMateSize.iconMd),
                )
            } else {
                provider.symbol?.let { symbol ->
                    Icon(
                        painter = painterResource(symbol),
                        contentDescription = null,
                        tint = provider.label,
                        modifier = Modifier.size(width = SymbolWidth, height = SymbolHeight),
                    )
                }
                Text(text = label, style = MedicalMateTheme.typography.labelL)
            }
        }
    }
}

/**
 * DESIGN.md의 `Social Login Stack`.
 *
 * 카카오 → 네이버 → Apple 세로 순서다. gap 10이고 화면에서는 폭 Fill에 좌우 거터 20이다.
 *
 * [providers]를 받는 이유는 백엔드가 지원하는 것만 노출해야 하기 때문이다. 지금은
 * `User` 엔티티 식별자가 `kakaoId` 단독이라 카카오만 동작한다. 동작하지 않는 버튼을
 * 그려두면 사용자가 눌러보고 실패한다.
 */
@Composable
fun MedicalMateSocialLoginStack(
    providers: List<MedicalMateSocialProvider>,
    labelOf: (MedicalMateSocialProvider) -> String,
    descriptionOf: (MedicalMateSocialProvider) -> String,
    onClick: (MedicalMateSocialProvider) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inProgressProvider: MedicalMateSocialProvider? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
    ) {
        providers.forEach { provider ->
            MedicalMateSocialLoginButton(
                provider = provider,
                label = labelOf(provider),
                contentDescription = descriptionOf(provider),
                onClick = { onClick(provider) },
                enabled = enabled,
                inProgress = provider == inProgressProvider,
            )
        }
    }
}

private val ProgressStroke = 2.dp

/** 카카오 말풍선. 마스터에서 20x18.67로 놓인다. 가로세로가 달라 size 하나로 못 쓴다. */
private val SymbolWidth = 20.dp

private val SymbolHeight = 18.67.dp
