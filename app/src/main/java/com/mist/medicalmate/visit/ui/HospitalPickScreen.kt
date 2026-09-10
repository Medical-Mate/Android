package com.mist.medicalmate.visit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomCtaBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateDivider
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSearchField
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle

/**
 * 와이어프레임 1m과 1m-B. Figma `489:5447`, `1041:3687`.
 *
 * 이름으로 찾으면 주소가 함께 등록돼서 환자가 주소를 따로 적을 일이 없다.
 *
 * 하나만 고른다. 진료 한 건에 병원이 둘일 수 없다.
 *
 * **한 화면이 두 자리에서 쓰인다.** 진료 후(1m)와 진료 전(1m-B)이다. 검색과 목록과 선택이
 * 같고 문구·CTA·건너뛰기만 [HospitalPickUiState.purpose]에 따라 갈린다. 화면을 둘로 만들면
 * 검색 규칙이 두 곳에 생긴다.
 *
 * [onSkipClick]은 진료 전에만 있다. 없으면 Nav 우측이 비고, 그러면 1m이 된다.
 */
@Composable
fun HospitalPickScreen(
    state: HospitalPickUiState,
    onQueryChange: (String) -> Unit,
    onHospitalClick: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkipClick: (() -> Unit)? = null,
) {
    // CTA만 목적마다 다르다. 1m-B는 카드로 이어지고, 일정 추가는 필드를 채우고 돌아간다.
    val submitLabel =
        when (state.purpose) {
            HospitalPickPurpose.AFTER_VISIT, HospitalPickPurpose.SCHEDULE -> R.string.hospital_pick_submit
            HospitalPickPurpose.BEFORE_VISIT -> R.string.hospital_pick_submit_before
        }
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.hospital_pick_title),
            onLeadingClick = onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
            actionLabel = onSkipClick?.let { stringResource(R.string.hospital_pick_skip) },
            onActionClick = onSkipClick,
        )
        PickContent(
            state = state,
            onQueryChange = onQueryChange,
            onHospitalClick = onHospitalClick,
        )
        MedicalMateBottomCtaBar {
            MedicalMateButton(
                label = stringResource(submitLabel),
                onClick = onSubmitClick,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColumnScope.PickContent(
    state: HospitalPickUiState,
    onQueryChange: (String) -> Unit,
    onHospitalClick: (String) -> Unit,
) {
    val before = state.purpose.beforeVisit
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s12),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s20),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8)) {
            Text(
                text =
                stringResource(
                    if (before) R.string.hospital_pick_question_before else R.string.hospital_pick_question,
                ),
                style = MedicalMateTheme.typography.headingL,
                color = MedicalMateTheme.colors.fgDefault,
            )
            Text(
                text =
                stringResource(
                    if (before) {
                        R.string.hospital_pick_description_before
                    } else {
                        R.string.hospital_pick_description
                    },
                ),
                style = MedicalMateTheme.typography.bodyM,
                color = MedicalMateTheme.colors.fgSubtle,
            )
        }
        MedicalMateSearchField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.hospital_pick_search_placeholder),
            clearContentDescription = stringResource(R.string.hospital_pick_search_clear),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12)) {
            Text(
                text =
                if (state.results.isEmpty()) {
                    stringResource(R.string.hospital_pick_result_none)
                } else {
                    stringResource(R.string.hospital_pick_result_count, state.results.size)
                },
                style = MedicalMateTheme.typography.labelM,
                color = MedicalMateTheme.colors.fgSubtle,
            )
            if (state.results.isEmpty()) {
                EmptyResults()
            } else {
                Results(state = state, onHospitalClick = onHospitalClick)
            }
        }
    }
}

/**
 * 찾은 것이 없을 때. 1m-B의 입력 전 상태이고, 검색해서 안 나온 경우도 같은 자리다.
 *
 * 아이콘이 시안과 다르다. 시안은 병원 아이콘을 쓰는데 `Empty State`의 아이콘은 변이가
 * 정한다(문서가 "제목·아이콘·액션은 변이가 정하고 본문만 갈아 끼운다"고 적었다). 한 화면을
 * 위해 아이콘을 열면 어느 화면이든 변이의 시각 언어를 벗어날 수 있게 된다. 그래서
 * `NoResult`의 `search-off`를 그대로 쓰고 디자인 트랙에 확인을 넘겼다.
 */
@Composable
private fun EmptyResults() {
    MedicalMateEmptyState(
        type = MedicalMateEmptyStateType.NO_RESULT,
        title = stringResource(R.string.hospital_pick_empty_title),
        description = stringResource(R.string.hospital_pick_empty_description),
    )
}

@Composable
private fun Results(state: HospitalPickUiState, onHospitalClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        state.results.forEachIndexed { index, hospital ->
            if (index > 0) MedicalMateDivider()
            ResultRow(
                hospital = hospital,
                selected = hospital.id == state.selectedId,
                onClick = { onHospitalClick(hospital.id) },
            )
        }
    }
}

/**
 * 결과 한 줄.
 *
 * `List Row`를 쓰지 않는다. 고른 줄은 제목이 브랜드색이 되고 오른쪽에 체크가 붙는데, 그
 * 컴포넌트에는 선택 상태가 없다. 시안도 `List Row` 인스턴스 옆에 체크를 따로 얹어 두었다.
 *
 * 행 전체가 hit area다. 체크만 누를 수 있으면 접근성 기준 48에 못 미친다.
 */
@Composable
private fun ResultRow(hospital: Hospital, selected: Boolean, onClick: () -> Unit) {
    val colors = MedicalMateTheme.colors
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = MedicalMateSpace.s16),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
        ) {
            Text(
                text = hospital.name,
                style = MedicalMateTheme.typography.bodyLStrong,
                color = if (selected) colors.fgPrimary else colors.fgDefault,
            )
            Text(
                text = hospital.address,
                style = MedicalMateTheme.typography.bodyS,
                color = colors.fgSubtle,
            )
        }
        if (selected) {
            Icon(
                painter = painterResource(MedicalMateIcons.Check),
                contentDescription = null,
                tint = colors.fgDefault,
                modifier = Modifier.size(MedicalMateSize.iconLg),
            )
        }
    }
}

@MedicalMateScreenPreviews
@Composable
private fun HospitalPickScreenPreview() {
    MedicalMateTheme {
        HospitalPickScreen(
            state =
            HospitalPickUiState(
                query = "서울OO병원",
                results = previewHospitals,
                selectedId = previewHospitals.first().id,
            ),
            onQueryChange = {},
            onHospitalClick = {},
            onSubmitClick = {},
            onBackClick = {},
        )
    }
}
