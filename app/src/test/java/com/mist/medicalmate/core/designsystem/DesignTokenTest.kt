package com.mist.medicalmate.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    @Test
    fun `간격 12단계가 문서 값과 같다`() {
        val expected = listOf(2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 32, 40).map { it.dp }
        val actual =
            with(MedicalMateSpace) {
                listOf(s2, s4, s6, s8, s10, s12, s14, s16, s20, s24, s32, s40)
            }
        assertEquals(expected, actual)
    }

    @Test
    fun `반경이 문서 값과 같다`() {
        with(MedicalMateRadius) {
            assertEquals(RoundedCornerShape(8.dp), xs)
            assertEquals(RoundedCornerShape(12.dp), sm)
            assertEquals(RoundedCornerShape(16.dp), md)
            assertEquals(RoundedCornerShape(20.dp), lg)
            assertEquals(RoundedCornerShape(24.dp), xl)
            assertEquals(RoundedCornerShape(28.dp), xxl)
            assertEquals(CircleShape, full)
            // 4.2 Scale에 없는 중간값. 문서 8.1 Button M 전용이다.
            assertEquals(RoundedCornerShape(14.dp), buttonM)
        }
    }

    @Test
    fun `크기와 레이아웃이 문서 값과 같다`() {
        with(MedicalMateSize) {
            assertEquals(48.dp, touchMin)
            assertEquals(18.dp, iconSm)
            assertEquals(20.dp, iconMd)
            assertEquals(24.dp, iconLg)
            assertEquals(40.dp, controlSm)
            assertEquals(48.dp, controlMd)
            assertEquals(56.dp, controlLg)
            assertEquals(88.dp, mic)
            assertEquals(390.dp, screenWidth)
            assertEquals(20.dp, gutter)
            assertEquals(350.dp, contentWidth)
            assertEquals(24.dp, safeBottom)
            assertEquals(56.dp, navBarHeight)
        }
    }

    @Test
    fun `Tab Bar 높이는 토큰이 아니라 활성 마스터 값을 쓴다`() {
        // 문서 4.3의 layout/tabbar-h는 82이지만 Figma variant 3개는 390x79다.
        // 문서 11.2가 확정을 남긴 항목이고, 그때까지 마스터 값을 따른다.
        assertEquals(79.dp, MedicalMateSize.tabBarHeight)
    }

    @Test
    fun `Glass 알파와 블러 최소 SDK가 문서 값과 같다`() {
        assertEquals(24.dp, MedicalMateGlass.blurRadius)
        assertEquals(0.78f, MedicalMateGlass.BOTTOM_CTA_ALPHA, 0f)
        assertEquals(0.82f, MedicalMateGlass.NAV_BAR_ALPHA, 0f)
        assertEquals(0.86f, MedicalMateGlass.TAB_BAR_ALPHA, 0f)
        // RenderEffect가 API 31에 추가됐다. 아래에서는 Opaque 변형을 쓴다.
        assertEquals(31, MedicalMateGlass.MIN_BLUR_SDK)
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
