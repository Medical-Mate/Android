package com.mist.medicalmate.visit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateLoadingSpinner
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle

/**
 * 저장한 진료 후 기록 하나. 캘린더 일자의 "이 날 기록" 줄에서 들어온다(#256).
 *
 * 1q-1(`405:2193`)과 같은 카드다. 다른 것은 상단 우측에 `편집`이 없고 하단에 저장하기가
 * 없다는 것이다 — 저장한 기록은 고치지 않는다(#235). 기록 탭의 상세(1j-3)는 카드·기록·예정을
 * 타임라인으로 모아 보이는 자리라, 그 날의 기록 하나를 보는 이 줄과는 가는 곳이 다르다.
 */
@Composable
fun VisitDetailScreen(
    state: VisitRecordUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.visit_record_title),
            onLeadingClick = onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        when (state) {
            VisitRecordUiState.Loading -> MedicalMateLoadingSpinner(modifier = Modifier.weight(1f))
            VisitRecordUiState.Failed -> Failed(onRetryClick = onRetryClick, modifier = Modifier.weight(1f))
            is VisitRecordUiState.Content ->
                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
                ) {
                    // 조작을 하나도 넘기지 않는다. 사본이 없어 편집 줄이 열리지 않고, ×도 붙지 않는다.
                    VisitRecordCard(state = state, callbacks = VisitRecordCallbacks())
                }
        }
    }
}

@Composable
private fun Failed(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(MedicalMateSize.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.NO_RESULT,
            title = stringResource(R.string.visit_detail_failed_title),
            description = stringResource(R.string.visit_record_failed_description),
            actionLabel = stringResource(R.string.visit_record_retry),
            onActionClick = onRetryClick,
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun VisitDetailScreenPreview() {
    MedicalMateTheme {
        VisitDetailScreen(
            state = VisitRecordUiState.Content(record = previewVisitRecord),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}
