package com.mist.medicalmate.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 시맨틱 토큰과 타이포그래피가 DESIGN.md 표의 값으로 해석되는지 본다.
 *
 * 기대값은 문서의 Resolved 열과 3절 표에서 따로 옮겨 적었다. 원시 팔레트를 다시 확인하려는
 * 것이 아니라 alias를 잘못 연결한 경우를 잡으려는 것이다. `bg/subtle`이 `neutral/100`
 * 대신 `neutral/200`을 가리켜도 빌드는 통과하고 화면만 조금 어두워진다.
 */
class DesignTokenTest {
    @Test
    fun `Background 시맨틱 컬러가 문서 값으로 해석된다`() {
        with(LightMedicalMateColors) {
            assertEquals(Color(0xFFF5F6FA), bgCanvas)
            assertEquals(Color(0xFFFFFFFF), bgSurface)
            assertEquals(Color(0xFFEDEFF5), bgSubtle)
            assertEquals(Color(0xFF5566D2), bgPrimary)
            assertEquals(Color(0xFF3B4FC0), bgPrimaryPressed)
            assertEquals(Color(0xFFE3E7FC), bgPrimarySubtle)
            assertEquals(Color(0xFFF2F4FE), bgPrimaryFaint)
            assertEquals(Color(0xFFF2F4FE), bgInfo)
            assertEquals(Color(0xFFE4F7ED), bgSuccess)
            assertEquals(Color(0xFFFFF4D6), bgWarning)
            assertEquals(Color(0xFFFFEDEB), bgDanger)
            assertEquals(Color(0xFF131722), bgInverse)
            assertEquals(Color(0xFF131722), bgScrim)
        }
    }

    @Test
    fun `Foreground 시맨틱 컬러가 문서 값으로 해석된다`() {
        with(LightMedicalMateColors) {
            assertEquals(Color(0xFF131722), fgDefault)
            assertEquals(Color(0xFF585F73), fgSubtle)
            assertEquals(Color(0xFF7C8397), fgMuted)
            assertEquals(Color(0xFFC6CAD8), fgDisabled)
            assertEquals(Color(0xFFFFFFFF), fgOnPrimary)
            assertEquals(Color(0xFFFFFFFF), fgOnInverse)
            assertEquals(Color(0xFF2E3E9E), fgPrimary)
            assertEquals(Color(0xFF2E3E9E), fgLink)
            assertEquals(Color(0xFF2E3E9E), fgInfo)
            assertEquals(Color(0xFF0E7A4A), fgSuccess)
            assertEquals(Color(0xFF8A5A0B), fgWarning)
            assertEquals(Color(0xFFC4302B), fgDanger)
        }
    }

    @Test
    fun `Border 시맨틱 컬러가 문서 값으로 해석된다`() {
        with(LightMedicalMateColors) {
            assertEquals(Color(0xFFDEE1EB), borderSubtle)
            assertEquals(Color(0xFFC6CAD8), borderDefault)
            assertEquals(Color(0xFF7C8397), borderStrong)
            assertEquals(Color(0xFF5566D2), borderFocus)
            assertEquals(Color(0xFF5566D2), borderPrimary)
        }
    }

    @Test
    fun `Severity 단계와 tint가 문서 값으로 해석된다`() {
        with(LightMedicalMateColors) {
            assertEquals(Color(0xFFFFE3A8), severity1)
            assertEquals(Color(0xFFFFC79B), severity2)
            assertEquals(Color(0xFFFFA894), severity3)
            assertEquals(Color(0xFFF58079), severity4)
            assertEquals(Color(0xFFDC5A55), severity5)
            assertEquals(Color(0xFFFFF6E4), severity1Tint)
            assertEquals(Color(0xFFFFEFE4), severity2Tint)
            assertEquals(Color(0xFFFFE9E3), severity3Tint)
            assertEquals(Color(0xFFFDE4E2), severity4Tint)
            assertEquals(Color(0xFFF9DEDD), severity5Tint)
        }
    }

    @Test
    fun `카카오 버튼 색은 공식 가이드 값이다`() {
        assertEquals(Color(0xFFFEE500), KakaoContainer)
        assertEquals(Color(0xFF191600), KakaoLabel)
    }

    @Test
    fun `타이포그래피 15종이 문서 값과 같다`() {
        with(DefaultMedicalMateTypography) {
            assertStyle(displayM, FontWeight.Bold, 30f, 40f, -2.0)
            assertStyle(headingL, FontWeight.Bold, 24f, 34f, -2.0)
            assertStyle(headingM, FontWeight.SemiBold, 20f, 28f, -1.5)
            assertStyle(headingS, FontWeight.SemiBold, 17f, 24f, -1.0)
            assertStyle(bodyL, FontWeight.Normal, 17f, 26f, 0.0)
            assertStyle(bodyLStrong, FontWeight.SemiBold, 17f, 26f, 0.0)
            assertStyle(bodyM, FontWeight.Normal, 15f, 24f, 0.0)
            assertStyle(bodyMStrong, FontWeight.SemiBold, 15f, 24f, 0.0)
            assertStyle(bodyS, FontWeight.Normal, 13f, 20f, 0.0)
            assertStyle(bodySStrong, FontWeight.SemiBold, 13f, 20f, 0.0)
            assertStyle(labelL, FontWeight.SemiBold, 15f, 20f, 0.0)
            assertStyle(labelM, FontWeight.SemiBold, 13f, 18f, 0.0)
            assertStyle(labelS, FontWeight.Medium, 11f, 16f, 2.0)
            assertStyle(numericL, FontWeight.Bold, 24f, 30f, -1.5)
            assertStyle(numericM, FontWeight.SemiBold, 17f, 22f, -1.0)
        }
    }

    @Test
    fun `Material3 슬롯이 채워져 있다`() {
        // 비어 있으면 그 자리에 M3 기본값(Roboto)이 남는다.
        with(MaterialTypography) {
            val slots =
                listOf(
                    displayLarge, displayMedium, displaySmall,
                    headlineLarge, headlineMedium, headlineSmall,
                    titleLarge, titleMedium, titleSmall,
                    bodyLarge, bodyMedium, bodySmall,
                    labelLarge, labelMedium, labelSmall,
                )
            slots.forEach { style ->
                assertEquals(MedicalMateFontFamily, style.fontFamily)
            }
        }
    }

    private fun assertStyle(
        style: TextStyle,
        weight: FontWeight,
        sizeSp: Float,
        lineHeightSp: Float,
        letterSpacingPercent: Double,
    ) {
        assertEquals(weight, style.fontWeight)
        assertEquals(sizeSp, style.fontSize.value, 0f)
        assertEquals(lineHeightSp, style.lineHeight.value, 0f)
        // 문서는 퍼센트로 적고 코드는 em으로 넣는다. -2% = -0.02em
        assertEquals((letterSpacingPercent / 100).toFloat(), style.letterSpacing.value, 1e-6f)
        assertEquals(MedicalMateFontFamily, style.fontFamily)
    }
}
