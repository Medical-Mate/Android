package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md 8.3 `Severity Readout`.
 *
 * 출력 전용이다. 고르는 자리는 Severity Slider, Scale, Select다.
 *
 * 단계 숫자, 환자가 고른 낱말, NRS 등가를 함께 보여준다. 색만으로 단계를 전달하지
 * 않는다는 문서 1항 6번을 이 세 가지가 함께 지킨다. 색을 지워도 숫자와 낱말로 단계를
 * 알 수 있어야 한다.
 *
 * 접근성 이름은 "3단계, 꽤 아파요" 형태로 한 번에 읽는다. 숫자 칩과 낱말을 따로 읽으면
 * 순서가 흐트러진다. NRS는 의료진용 표기라 읽지 않는다.
 */
@Composable
fun MedicalMateSeverityReadout(severity: MedicalMateSeverity, modifier: Modifier = Modifier) {
    val label = stringResource(severity.labelRes)
    val spoken = stringResource(R.string.severity_level_content_description, severity.level, label)

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = ReadoutHeight)
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelChip(severity)
        Text(
            text = label,
            style = MedicalMateTheme.typography.bodyLStrong,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.severity_nrs, severity.nrsFirst, severity.nrsLast),
            style = MedicalMateTheme.typography.bodyS,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * 단계 숫자 칩.
 *
 * 글자색은 `fg/default`로 둔다. severity base가 옅은 1~2단계에서 흰 글자를 쓰면 대비가
 * 무너진다. 5단계까지 같은 색을 쓰면 단계마다 대비가 뒤집히지 않는다.
 */
@Composable
private fun LevelChip(severity: MedicalMateSeverity) {
    Surface(
        shape = MedicalMateRadius.xs,
        color = severity.base,
        contentColor = MedicalMateTheme.colors.fgDefault,
        modifier = Modifier.size(LevelChipSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = severity.level.toString(),
                style = MedicalMateTheme.typography.labelM,
            )
        }
    }
}

/** 문서 8.3이 지정한 높이. */
private val ReadoutHeight = 32.dp

private val LevelChipSize = 28.dp
