package com.mist.medicalmate.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * DESIGN.md의 시맨틱 컬러 40개.
 *
 * Material3 `ColorScheme`에는 `bg/canvas`, `fg/subtle`, `border/subtle`, `severity/1~5`에
 * 대응하는 역할이 없어서 별도 홀더를 둔다. 매핑되는 역할은 `ColorScheme`에도 같은 값으로
 * 채워 두므로(`Theme.kt`) M3 컴포넌트가 내부에서 참조하는 색도 브랜드 값이 된다.
 *
 * 이름은 Figma 경로를 camelCase로 옮긴 것이다(DESIGN.md의 이름 변환 규칙).
 * `bg/primary` → `bgPrimary`.
 */
@Immutable
data class MedicalMateColors(
    val bgCanvas: Color,
    val bgSurface: Color,
    val bgSubtle: Color,
    val bgPrimary: Color,
    val bgPrimaryPressed: Color,
    val bgPrimarySubtle: Color,
    val bgPrimaryFaint: Color,
    val bgInfo: Color,
    val bgSuccess: Color,
    val bgWarning: Color,
    val bgDanger: Color,
    val bgInverse: Color,
    /**
     * Tooltip Bubble의 면. `bg/inverse`보다 옅다.
     *
     * Figma가 나중에 늘린 토큰이라 처음 옮긴 40개에 없었다. 화면에 겹쳐 뜨는 짧은
     * 설명이라 Toast만큼 무겁지 않게 둔 것으로 보인다.
     */
    val bgInverseSoft: Color,
    /** Overlay Scrim. 컴포넌트에서 opacity 50%를 적용한다(DESIGN.md의 컴포넌트 규격). */
    val bgScrim: Color,
    val fgDefault: Color,
    val fgSubtle: Color,
    /** 비활성 아이콘·장식 획 전용. 텍스트에 쓰지 않는다(DESIGN.md의 접근성 기준). */
    val fgMuted: Color,
    val fgDisabled: Color,
    val fgOnPrimary: Color,
    val fgOnInverse: Color,
    val fgPrimary: Color,
    val fgLink: Color,
    val fgInfo: Color,
    val fgSuccess: Color,
    val fgWarning: Color,
    val fgDanger: Color,
    val borderSubtle: Color,
    val borderDefault: Color,
    val borderStrong: Color,
    val borderFocus: Color,
    val borderPrimary: Color,
    val severity1: Color,
    val severity2: Color,
    val severity3: Color,
    val severity4: Color,
    val severity5: Color,
    val severity1Tint: Color,
    val severity2Tint: Color,
    val severity3Tint: Color,
    val severity4Tint: Color,
    val severity5Tint: Color,
)

/**
 * Light 모드 값. DESIGN.md에 다크 모드 값이 없다.
 * 값이 정해지고 컴포넌트 QA가 끝나기 전까지 다크를 지원 대상으로 표시하지 않는다.
 */
internal val LightMedicalMateColors =
    MedicalMateColors(
        bgCanvas = Neutral50,
        bgSurface = Neutral0,
        bgSubtle = Neutral100,
        bgPrimary = Primary500,
        bgPrimaryPressed = Primary600,
        bgPrimarySubtle = Primary100,
        bgPrimaryFaint = Primary50,
        bgInfo = Primary50,
        bgSuccess = Green50,
        bgWarning = Amber50,
        bgDanger = Red50,
        bgInverse = Neutral900,
        bgInverseSoft = Neutral700,
        bgScrim = Neutral900,
        fgDefault = Neutral900,
        fgSubtle = Neutral600,
        fgMuted = Neutral500,
        fgDisabled = Neutral300,
        fgOnPrimary = Neutral0,
        fgOnInverse = Neutral0,
        fgPrimary = Primary700,
        fgLink = Primary700,
        fgInfo = Primary700,
        fgSuccess = Green700,
        fgWarning = Amber700,
        fgDanger = Red700,
        borderSubtle = Neutral200,
        borderDefault = Neutral300,
        borderStrong = Neutral500,
        borderFocus = Primary500,
        borderPrimary = Primary500,
        severity1 = Severity1Base,
        severity2 = Severity2Base,
        severity3 = Severity3Base,
        severity4 = Severity4Base,
        severity5 = Severity5Base,
        severity1Tint = Severity1TintBase,
        severity2Tint = Severity2TintBase,
        severity3Tint = Severity3TintBase,
        severity4Tint = Severity4TintBase,
        severity5Tint = Severity5TintBase,
    )

/**
 * 테마 밖에서 읽으면 개발 중에 바로 드러나도록 기본값을 두지 않는다.
 * `MedicalMateTheme`으로 감싸지 않은 컴포저블에서 접근하면 예외가 난다.
 */
internal val LocalMedicalMateColors =
    staticCompositionLocalOf<MedicalMateColors> {
        error("MedicalMateColors를 찾을 수 없습니다. MedicalMateTheme으로 감싸세요.")
    }
