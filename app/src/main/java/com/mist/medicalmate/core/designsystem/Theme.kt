package com.mist.medicalmate.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * 시맨틱 토큰을 Material3 역할에 얹은 것.
 *
 * M3 컴포넌트가 내부에서 `MaterialTheme.colorScheme`을 읽으므로, 채우지 않은 역할에는
 * M3 기본 팔레트(보라)가 남는다. 그래서 화면에 나올 수 있는 역할을 전부 지정한다.
 *
 * DESIGN.md에는 secondary·tertiary 브랜드 단계가 없다. 두 역할을 쓰는 M3 컴포넌트에
 * 기본 팔레트가 새지 않도록 primary 계열로 맞췄다.
 *
 * 다크 스킴과 dynamic color는 두지 않는다. dynamic color는 Android 12 이상에서
 * 사용자 월페이퍼 색으로 브랜드 컬러를 덮어버린다. 다크 모드는 DESIGN.md 2.1과 11.7에
 * 따라 값이 정해지기 전까지 지원하지 않는다.
 */
private val MedicalMateColorScheme =
    with(LightMedicalMateColors) {
        lightColorScheme(
            primary = bgPrimary,
            onPrimary = fgOnPrimary,
            primaryContainer = bgPrimarySubtle,
            onPrimaryContainer = fgPrimary,
            inversePrimary = Primary300,
            secondary = bgPrimary,
            onSecondary = fgOnPrimary,
            secondaryContainer = bgPrimarySubtle,
            onSecondaryContainer = fgPrimary,
            tertiary = bgPrimary,
            onTertiary = fgOnPrimary,
            tertiaryContainer = bgPrimarySubtle,
            onTertiaryContainer = fgPrimary,
            background = bgCanvas,
            onBackground = fgDefault,
            surface = bgSurface,
            onSurface = fgDefault,
            surfaceVariant = bgSubtle,
            onSurfaceVariant = fgSubtle,
            surfaceTint = bgPrimary,
            surfaceBright = bgSurface,
            surfaceDim = bgSubtle,
            surfaceContainerLowest = bgSurface,
            surfaceContainerLow = bgCanvas,
            surfaceContainer = bgSubtle,
            surfaceContainerHigh = bgSubtle,
            surfaceContainerHighest = bgSubtle,
            inverseSurface = bgInverse,
            inverseOnSurface = fgOnInverse,
            error = fgDanger,
            onError = fgOnPrimary,
            errorContainer = bgDanger,
            onErrorContainer = fgDanger,
            outline = borderDefault,
            outlineVariant = borderSubtle,
            scrim = bgScrim,
        )
    }

/**
 * 앱 테마.
 *
 * 시맨틱 컬러와 타이포그래피는 [MedicalMateTheme] 오브젝트로 읽는다. `colorScheme`과
 * `typography`에도 같은 값이 들어가 있어 기존 M3 호출은 그대로 동작한다.
 *
 * 간격(DESIGN.md 4.1), 반경(4.2), 크기(4.3), 고도(5절)는 아직 토큰으로 없다.
 */
@Composable
fun MedicalMateTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMedicalMateColors provides LightMedicalMateColors,
        LocalMedicalMateTypography provides DefaultMedicalMateTypography,
    ) {
        MaterialTheme(
            colorScheme = MedicalMateColorScheme,
            typography = MaterialTypography,
            content = content,
        )
    }
}

/**
 * 시맨틱 토큰 접근점.
 *
 * `MedicalMateTheme.colors.fgSubtle`처럼 쓴다. Material3의 `MaterialTheme`과 같은 구조로,
 * 같은 이름의 컴포저블 함수와 오브젝트가 공존한다.
 */
object MedicalMateTheme {
    val colors: MedicalMateColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMedicalMateColors.current

    val typography: MedicalMateTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalMedicalMateTypography.current
}
