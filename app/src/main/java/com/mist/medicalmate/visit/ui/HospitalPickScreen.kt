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
 * 같고 문구와 CTA만 [HospitalPickUiState.purpose]에 따라 갈린다. 화면을 둘로 만들면 검색
 * 규칙이 두 곳에 생긴다.
 *
 * **상단 바에는 뒤로가기만 둔다**(#228). 진료 전에 병원 없이 카드로 넘어가는 건너뛰기가
 * 있었는데 시안의 1m-B에 그 자리가 없다. 나가는 길은 뒤로가기다.
 */
@Composable
fun HospitalPickScreen(
    state: HospitalPickUiState,
    onQueryChange: (String) -> Unit,
    onHospitalClick: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // CTA만 목적마다 다르다. 1m-B는 카드로 이어지고, 일정 추가는 필드를 채우고 돌아간다.
    val submitLabel =
        when {
            state.purpose != HospitalPickPurpose.BEFORE_VISIT -> R.string.hospital_pick_submit
            // 카드 안에서 병원만 바꾸러 온 것이면 만들 카드가 없다.
            state.forExistingCard -> R.string.hospital_pick_submit
            else -> R.string.hospital_pick_submit_before
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
        )
        PickContent(
            state = state,
            onQueryChange = onQueryChange,
            onHospitalClick = onHospitalClick,
        )
        if (state.showSubmit) {
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
}

@Composable
private fun ColumnScope.PickContent(
    state: HospitalPickUiState,
    onQueryChange: (String) -> Unit,
    onHospitalClick: (String) -> Unit,
) {
    val before = state.purpose.beforeVisit
    val empty = state.results.isEmpty()
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            // 찾은 것이 없으면 스크롤하지 않는다. 빈 상태가 남은 높이를 받으려면 높이가
            // 정해져 있어야 하고, 그 화면에는 넘칠 것도 없다.
            .let { if (empty) it else it.verticalScroll(rememberScrollState()) }
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
        Text(
            text = resultLabel(state),
            style = MedicalMateTheme.typography.labelM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
        if (empty) {
            EmptyResults()
        } else {
            Results(state = state, onHospitalClick = onHospitalClick)
        }
    }
}

/**
 * 결과 줄의 문구.
 *
 * 못 닿은 것과 못 찾은 것을 가른다. 앞은 잠시 뒤 다시 할 일이고 뒤는 검색어를 바꿀 일이다.
 * 한 문구로 뭉개면 사용자가 병원 이름을 계속 고쳐 친다.
 *
 * 받은 것보다 많으면 그 사실을 적는다. 부분 일치라 "서울"이면 4천 건이 넘는데, 그때 필요한
 * 것은 목록을 더 보는 것이 아니라 검색어를 좁히는 것이다.
 */
@Composable
private fun resultLabel(state: HospitalPickUiState): String = when {
    state.searching -> stringResource(R.string.hospital_pick_result_searching)
    state.failed -> stringResource(R.string.hospital_pick_result_failed)
    state.results.isEmpty() -> stringResource(R.string.hospital_pick_result_none)
    state.truncated -> stringResource(R.string.hospital_pick_result_truncated, state.total, state.results.size)
    else -> stringResource(R.string.hospital_pick_result_count, state.results.size)
}

/**
 * 찾은 것이 없을 때. 1m-B의 입력 전 상태이고, 검색해서 안 나온 경우도 같은 자리다.
 *
 * **병원 아이콘이다**(#228). 시안의 인스턴스가 `Empty State`의 `search-off`를 `hospital`로
 * 갈아 끼워 뒀다. 전에는 변이가 정한 그림을 그대로 쓰고 디자인 트랙에 넘겨 뒀던 자리다.
 *
 * **남은 높이를 받아 그 안에서 가운데에 선다.** 시안(`1092:3919`)의 높이가 496인데 그것은
 * 검색창 아래 남은 자리 전부다. 목록이 없는 화면은 목록의 첫 줄 자리에 글이 뜬 것이 아니라
 * 한 화면이다.
 */
@Composable
private fun ColumnScope.EmptyResults() {
    MedicalMateEmptyState(
        type = MedicalMateEmptyStateType.NO_RESULT,
        title = stringResource(R.string.hospital_pick_empty_title),
        description = stringResource(R.string.hospital_pick_empty_description),
        icon = MedicalMateIcons.Hospital,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun Results(state: HospitalPickUiState, onHospitalClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        state.results.forEachIndexed { index, hospital ->
            if (index > 0) MedicalMateDivider()
            ResultRow(
                hospital = hospital,
                selected = hospital == state.selected,
                onClick = { onHospitalClick(hospital.name) },
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
        // 주소는 같은 이름의 다른 지점을 구별하는 값이라 이름 아래에 붙인다. 심평원에 없는
        // 곳은 비어 있고 그때는 줄을 그리지 않는다 — 빈 줄이 남으면 주소가 없는 병원이 아니라
        // 주소가 빈 병원으로 읽힌다.
        Column(
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = hospital.name,
                style = MedicalMateTheme.typography.headingS,
                color = if (selected) colors.fgPrimary else colors.fgDefault,
            )
            hospital.address?.takeIf { it.isNotBlank() }?.let { address ->
                Text(
                    text = address,
                    style = MedicalMateTheme.typography.bodyS,
                    color = colors.fgSubtle,
                )
            }
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

/** Preview에서만 쓰는 결과. 서버가 이름만 주므로 이름뿐이다. */
private val previewHospitals =
    listOf(
        Hospital("서울OO병원 내과"),
        Hospital("서울OO병원 이비인후과"),
        Hospital("OO이비인후과의원"),
        Hospital("OO정형외과의원"),
    )

@MedicalMateScreenPreviews
@Composable
private fun HospitalPickScreenPreview() {
    MedicalMateTheme {
        HospitalPickScreen(
            state =
            HospitalPickUiState(
                query = "서울OO병원",
                results = previewHospitals,
                selected = previewHospitals.first(),
            ),
            onQueryChange = {},
            onHospitalClick = {},
            onSubmitClick = {},
            onBackClick = {},
        )
    }
}
