package com.mist.medicalmate.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mist.medicalmate.R

/**
 * DESIGN.md의 기본 서체.
 *
 * Pretendard v1.3.9의 static OTF를 동봉한다. Google Fonts에 없어 Downloadable Fonts로는
 * 받을 수 없다.
 *
 * DESIGN.md가 쓰는 무게만 넣었다. Regular 400, Medium 500, SemiBold 600, Bold 700이다.
 * 무게가 모두 실물로 있으므로 `Strong` 스타일에 합성 굵기가 걸리지 않는다.
 *
 * 라이선스는 SIL Open Font License 1.1이고 원문은 `licenses/Pretendard-OFL.txt`에 있다.
 * 재배포 시 저작권 표시와 라이선스를 함께 배포해야 한다.
 */
internal val MedicalMateFontFamily =
    FontFamily(
        Font(R.font.pretendard_regular, FontWeight.Normal),
        Font(R.font.pretendard_medium, FontWeight.Medium),
        Font(R.font.pretendard_semibold, FontWeight.SemiBold),
        Font(R.font.pretendard_bold, FontWeight.Bold),
    )

/**
 * Figma의 행간을 그대로 재현하기 위한 공통 설정.
 *
 * 기본값은 행간 여백을 첫 줄 위와 마지막 줄 아래에도 넣어서 Figma의 텍스트 박스보다
 * 높이가 커진다. [LineHeightStyle.Trim.None]으로 두고 글자를 행 안에서 가운데 정렬한다.
 */
private val MedicalMateLineHeightStyle =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )

/**
 * 줄바꿈 규칙.
 *
 * 기기 폭은 360부터 412까지 갈리고 사용자가 글꼴 배율을 2배까지 올린다. Figma 캔버스는
 * 390 하나라서, 한 줄에 맞춰 그린 문장이 실제 기기에서 넘어간다. 넘어가는 것을 막을 수는
 * 없으니 어디서 끊길지를 정한다.
 *
 * 규칙이 없으면 한국어가 어절 중간에서 끊긴다. "몰라도 괜찮 / 아요"처럼 읽힌다.
 *
 * [LineBreak.Heading]은 `Balanced + Loose + Phrase`다. 어절 단위로 끊고 줄 길이를 고르게
 * 맞춘다. 제목과 라벨처럼 짧은 문장에 쓴다. [LineBreak.Paragraph]는 더 느리지만 긴 글의
 * 가독성이 낫다.
 *
 * 어절 단위(`WordBreak.Phrase`)는 `StaticLayout`이 API 33 이상에서만 적용하고 그 아래에서는
 * 조용히 무시된다. 줄 길이를 고르게 하는 `Balanced`는 API 23부터라 minSdk 24에서도 먹는다.
 * 그래서 낮은 버전에서도 절반은 효과가 있다.
 */
private fun mmTextStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacingPercent: Double,
    lineBreak: LineBreak = LineBreak.Heading,
): TextStyle = TextStyle(
    fontFamily = MedicalMateFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = (letterSpacingPercent / 100).em,
    lineHeightStyle = MedicalMateLineHeightStyle,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineBreak = lineBreak,
)

/**
 * DESIGN.md의 타이포그래피 15종.
 *
 * 이름은 Figma 스타일 이름을 camelCase로 옮겼다. `Body/L Strong` → `bodyLStrong`.
 * `Strong`은 크기를 바꾸지 않고 같은 크기의 SemiBold로 강조한다.
 */
@Immutable
data class MedicalMateTypography(
    val displayM: TextStyle,
    val headingL: TextStyle,
    val headingM: TextStyle,
    val headingS: TextStyle,
    val bodyL: TextStyle,
    val bodyLStrong: TextStyle,
    val bodyM: TextStyle,
    val bodyMStrong: TextStyle,
    val bodyS: TextStyle,
    val bodySStrong: TextStyle,
    val labelL: TextStyle,
    val labelM: TextStyle,
    val labelS: TextStyle,
    val numericL: TextStyle,
    val numericM: TextStyle,
)

internal val DefaultMedicalMateTypography =
    MedicalMateTypography(
        displayM = mmTextStyle(FontWeight.Bold, 30, 40, -2.0),
        headingL = mmTextStyle(FontWeight.Bold, 24, 34, -2.0),
        headingM = mmTextStyle(FontWeight.SemiBold, 20, 28, -1.5),
        headingS = mmTextStyle(FontWeight.SemiBold, 17, 24, -1.0),
        bodyL = mmTextStyle(FontWeight.Normal, 17, 26, 0.0, LineBreak.Paragraph),
        bodyLStrong = mmTextStyle(FontWeight.SemiBold, 17, 26, 0.0, LineBreak.Paragraph),
        bodyM = mmTextStyle(FontWeight.Normal, 15, 24, 0.0, LineBreak.Paragraph),
        bodyMStrong = mmTextStyle(FontWeight.SemiBold, 15, 24, 0.0, LineBreak.Paragraph),
        bodyS = mmTextStyle(FontWeight.Normal, 13, 20, 0.0, LineBreak.Paragraph),
        bodySStrong = mmTextStyle(FontWeight.SemiBold, 13, 20, 0.0, LineBreak.Paragraph),
        labelL = mmTextStyle(FontWeight.SemiBold, 15, 20, 0.0),
        labelM = mmTextStyle(FontWeight.SemiBold, 13, 18, 0.0),
        labelS = mmTextStyle(FontWeight.Medium, 11, 16, 2.0),
        numericL = mmTextStyle(FontWeight.Bold, 24, 30, -1.5),
        numericM = mmTextStyle(FontWeight.SemiBold, 17, 22, -1.0),
    )

internal val LocalMedicalMateTypography =
    staticCompositionLocalOf { DefaultMedicalMateTypography }

/**
 * Material3 슬롯에 위 15종을 얹은 것.
 *
 * M3 컴포넌트가 내부에서 `MaterialTheme.typography`를 읽는다. Button은 `labelLarge`,
 * AlertDialog는 `headlineSmall`과 `bodyMedium`을 쓴다. 비워 두면 그 자리에 M3 기본값인
 * Roboto가 남는다. 그래서 15개 슬롯을 모두 채운다.
 *
 * `Numeric/L`·`Numeric/M`과 `Body/M Strong`·`Body/S Strong`은 대응하는 M3 슬롯이 없어
 * [MedicalMateTypography]에만 있다. 새 화면은 [MedicalMateTheme]의 `typography`를 쓴다.
 */
internal val MaterialTypography =
    with(DefaultMedicalMateTypography) {
        Typography(
            displayLarge = displayM,
            displayMedium = displayM,
            displaySmall = headingL,
            headlineLarge = headingL,
            headlineMedium = headingM,
            headlineSmall = headingM,
            titleLarge = headingM,
            titleMedium = headingS,
            titleSmall = bodyLStrong,
            bodyLarge = bodyL,
            bodyMedium = bodyM,
            bodySmall = bodyS,
            labelLarge = labelL,
            labelMedium = labelM,
            labelSmall = labelS,
        )
    }
