package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
 * DESIGN.md 8.2 `Social Login Button`의 `Provider` variant.
 *
 * 색은 각 사 공식 브랜드 가이드가 정한 값이고 변경이 금지된다. 문서도 이 컴포넌트를
 * 브랜드 가이드 예외로 표시했다.
 *
 * Naver 조합은 공식 규격 때문에 WCAG 대비 기준을 벗어난다. 문서 8.2가 그 사실을 밝히고
 * 다른 안내 요소는 기준을 지키라고 한다.
 */
enum class MedicalMateSocialProvider(
    internal val container: Color,
    internal val label: Color,
    internal val border: Color?,
) {
    KAKAO(KakaoContainer, KakaoLabel, null),
    NAVER(NaverContainer, NaverLabel, null),
    APPLE(AppleContainer, AppleLabel, null),
    GOOGLE(GoogleContainer, GoogleLabel, GoogleBorder),
}

/**
 * DESIGN.md 8.2 `Social Login Button`.
 *
 * 350x56에 radius 16이다. 컨테이너 규격은 문서를 따르고 색은 각 사 가이드를 따른다.
 *
 * **로고 자리가 비어 있다.** Figma의 로고는 임시 사각형이고(문서 11.5) 각 사 공식
 * 개발자 키트에서 받아 교체해야 한다. 임의로 그린 로고를 넣으면 가이드 위반이라, 받기
 * 전까지는 라벨만 둔다.
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
            .height(MedicalMateSize.controlLg)
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
                // 로고 자리. 각 사 공식 asset을 받으면 여기에 Icon을 넣는다.
                Text(text = label, style = MedicalMateTheme.typography.labelL)
            }
        }
    }
}

/**
 * DESIGN.md 8.2 `Social Login Stack`.
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
