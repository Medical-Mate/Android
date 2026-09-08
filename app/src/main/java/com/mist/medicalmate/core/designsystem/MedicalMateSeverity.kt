package com.mist.medicalmate.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.mist.medicalmate.R

/**
 * DESIGN.md 2.2와 8.2의 통증 5단계.
 *
 * Severity Slider, Scale, Select, Readout이 함께 쓴다. 네 컴포넌트가 같은 단계 정의를
 * 따로 들면 문구나 색이 갈린다.
 *
 * [nrsFirst]와 [nrsLast]는 문서에 없다. Figma의 Severity Readout 마스터(`333:1126`)에서
 * 읽은 값이다. 임상 척도라서 추정하지 않았다.
 *
 * 단계를 색만으로 전달하지 않는다. 문서 1항 6번과 9절이 숫자, 낱말, 상황 설명 중 하나
 * 이상을 색과 함께 제공하라고 한다. 그래서 [labelRes]와 [descriptionRes]를 항상 함께
 * 노출할 수 있게 묶어 뒀다.
 *
 * [base]는 칩과 트랙 채움에, [tint]는 Severity Slider의 판독 영역에 쓴다(문서 2.2).
 */
enum class MedicalMateSeverity(
    val level: Int,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val nrsFirst: Int,
    val nrsLast: Int,
) {
    LEVEL_1(1, R.string.severity_1_label, R.string.severity_1_description, 1, 2),
    LEVEL_2(2, R.string.severity_2_label, R.string.severity_2_description, 3, 4),
    LEVEL_3(3, R.string.severity_3_label, R.string.severity_3_description, 5, 6),
    LEVEL_4(4, R.string.severity_4_label, R.string.severity_4_description, 7, 8),
    LEVEL_5(5, R.string.severity_5_label, R.string.severity_5_description, 9, 10),
    ;

    val base: Color
        @Composable
        @ReadOnlyComposable
        get() = with(MedicalMateTheme.colors) {
            when (this@MedicalMateSeverity) {
                LEVEL_1 -> severity1
                LEVEL_2 -> severity2
                LEVEL_3 -> severity3
                LEVEL_4 -> severity4
                LEVEL_5 -> severity5
            }
        }

    val tint: Color
        @Composable
        @ReadOnlyComposable
        get() = with(MedicalMateTheme.colors) {
            when (this@MedicalMateSeverity) {
                LEVEL_1 -> severity1Tint
                LEVEL_2 -> severity2Tint
                LEVEL_3 -> severity3Tint
                LEVEL_4 -> severity4Tint
                LEVEL_5 -> severity5Tint
            }
        }

    companion object {
        /** 1~5 밖의 값은 없다. 저장된 값이 범위를 벗어나면 부르는 쪽이 알아야 한다. */
        fun ofLevel(level: Int): MedicalMateSeverity = entries.firstOrNull { it.level == level }
            ?: error("통증 단계는 1~5입니다. 받은 값: $level")
    }
}
