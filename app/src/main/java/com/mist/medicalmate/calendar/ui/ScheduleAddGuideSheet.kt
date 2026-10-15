package com.mist.medicalmate.calendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomSheet
import com.mist.medicalmate.core.designsystem.component.MedicalMateShareSheetIllustration
import com.mist.medicalmate.core.designsystem.component.MedicalMateStepRow

/**
 * 예약 문자 방법 안내 시트. Figma 1r-4-S `3663:14813`.
 *
 * 하단 `Hint Row`를 누르거나 홈 배너의 "방법 보기"로 들어오면 뜬다. 버튼 없이 그림과 네 단계만 둔다 —
 * 할 일은 문자 앱에서 하는 것이라 시트 안에서 누를 것이 없다. 끌어내리거나 스크림을 눌러 닫는다.
 *
 * 그래버 아래 14 · 제목 묶음(사이 4) · 16 · 그림 · 16 · 단계(사이 12)다. 시트 본체의 자식 간격 12를 받지
 * 않도록 한 묶음으로 둔다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleAddGuideSheet(onDismissRequest: () -> Unit) {
    MedicalMateBottomSheet(onDismissRequest = onDismissRequest, grabberGap = MedicalMateSpace.s14) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4)) {
                Text(
                    text = stringResource(R.string.schedule_add_guide_title),
                    style = MedicalMateTheme.typography.headingM,
                    color = MedicalMateTheme.colors.fgDefault,
                )
                Text(
                    text = stringResource(R.string.schedule_add_guide_body),
                    style = MedicalMateTheme.typography.bodyM,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
            }
            MedicalMateShareSheetIllustration(modifier = Modifier.padding(vertical = MedicalMateSpace.s16))
            Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12)) {
                GuideSteps.forEachIndexed { index, step ->
                    MedicalMateStepRow(number = index + 1, text = stringResource(step))
                }
            }
        }
    }
}

private val GuideSteps =
    listOf(
        R.string.schedule_add_guide_step1,
        R.string.schedule_add_guide_step2,
        R.string.schedule_add_guide_step3,
        R.string.schedule_add_guide_step4,
    )

/**
 * 라우트가 안내 시트를 띄운 채 열라고 하면(홈 배너의 "방법 보기") 처음 한 번만 띄운다. 병원 찾기에 다녀오거나
 * 화면이 다시 만들어져도 다시 뜨지 않는다.
 */
@Composable
internal fun OpenGuideOnce(guide: Boolean, open: () -> Unit) {
    var shown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (guide && !shown) {
            shown = true
            open()
        }
    }
}
