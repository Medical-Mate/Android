package com.mist.medicalmate.core.designsystem

import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import com.mist.medicalmate.R
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 폰트 리소스와 무게가 어긋나지 않는지 본다.
 *
 * 파일 이름과 무게를 잘못 짝지어도 빌드는 통과하고, 화면에서는 굵기만 조금 달라 보인다.
 * DESIGN.md 3절이 `Strong`을 같은 크기의 SemiBold로 규정하므로 600이 실물로 붙어 있어야
 * 합성 굵기가 걸리지 않는다.
 */
class PretendardFontTest {
    @Test
    fun `Pretendard 4종이 무게별로 연결되어 있다`() {
        val fonts = (MedicalMateFontFamily as FontListFontFamily).fonts
        val actual = fonts.map { (it as ResourceFont).resId to it.weight }

        val expected =
            listOf(
                R.font.pretendard_regular to FontWeight.Normal,
                R.font.pretendard_medium to FontWeight.Medium,
                R.font.pretendard_semibold to FontWeight.SemiBold,
                R.font.pretendard_bold to FontWeight.Bold,
            )
        assertEquals(expected, actual)
    }

    @Test
    fun `무게 숫자가 문서 값과 같다`() {
        assertEquals(400, FontWeight.Normal.weight)
        assertEquals(500, FontWeight.Medium.weight)
        assertEquals(600, FontWeight.SemiBold.weight)
        assertEquals(700, FontWeight.Bold.weight)
    }

    @Test
    fun `타이포 15종이 모두 Pretendard를 쓴다`() {
        with(DefaultMedicalMateTypography) {
            val styles =
                listOf(
                    displayM, headingL, headingM, headingS,
                    bodyL, bodyLStrong, bodyM, bodyMStrong, bodyS, bodySStrong,
                    labelL, labelM, labelS, numericL, numericM,
                )
            assertEquals(15, styles.size)
            styles.forEach { assertEquals(MedicalMateFontFamily, it.fontFamily) }
        }
    }

    @Test
    fun `문서가 쓰는 무게만 스타일에 나타난다`() {
        val used =
            with(DefaultMedicalMateTypography) {
                listOf(
                    displayM, headingL, headingM, headingS,
                    bodyL, bodyLStrong, bodyM, bodyMStrong, bodyS, bodySStrong,
                    labelL, labelM, labelS, numericL, numericM,
                ).mapNotNull { it.fontWeight }.toSet()
            }
        val bundled = (MedicalMateFontFamily as FontListFontFamily).fonts.map { it.weight }.toSet()
        // 동봉하지 않은 무게를 쓰면 합성 굵기로 렌더된다.
        assertEquals(emptySet<FontWeight>(), used - bundled)
    }
}
