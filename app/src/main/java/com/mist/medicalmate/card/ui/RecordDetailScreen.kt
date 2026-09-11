package com.mist.medicalmate.card.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBadge
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyState
import com.mist.medicalmate.core.designsystem.component.MedicalMateEmptyStateType
import com.mist.medicalmate.core.designsystem.component.MedicalMateLoadingSpinner
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle

/**
 * 와이어프레임 1j-3. Figma `735:3829`.
 *
 * 기록 목록에서 한 건을 누르면 이 화면이 열린다. 전에는 브리핑 카드로 바로 갔는데, 카드는
 * 이 흐름의 한 조각일 뿐이다. 증상을 정리하고, 카드를 만들어 진료실에서 보여주고, 진료 후
 * 들은 것을 적고, 재방문이 잡히는 과정이 한 줄로 이어져야 무엇이 어떻게 흘러왔는지 읽힌다.
 *
 * 왼쪽 세로선이 그 이어짐을 만든다. 점만 두면 단계가 따로 떨어진 카드로 보인다.
 */
@Composable
fun RecordDetailScreen(
    state: RecordDetailUiState,
    onBackClick: () -> Unit,
    expandedSteps: Set<Int>,
    onExpandToggle: (Int) -> Unit,
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
            title = stringResource(R.string.record_detail_title),
            onLeadingClick = onBackClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        when (state) {
            RecordDetailUiState.Loading ->
                MedicalMateLoadingSpinner(modifier = Modifier.weight(1f))

            RecordDetailUiState.Failed ->
                FailedContent(onRetryClick = onRetryClick, modifier = Modifier.weight(1f))

            is RecordDetailUiState.Content ->
                DetailContent(
                    detail = state.detail,
                    expandedSteps = expandedSteps,
                    onExpandToggle = onExpandToggle,
                )
        }
    }
}

@Composable
private fun ColumnScope.DetailContent(detail: RecordDetail, expandedSteps: Set<Int>, onExpandToggle: (Int) -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s16,
                bottom = ContentBottom,
            ),
    ) {
        Head(detail)
        Timeline(steps = detail.steps, expandedSteps = expandedSteps, onExpandToggle = onExpandToggle)
    }
}

/** 제목·상태·병원 줄. Figma `735:3837`. */
@Composable
private fun Head(detail: RecordDetail) {
    Column(
        modifier = Modifier.padding(bottom = HeadBottom),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = detail.title,
                style = MedicalMateTheme.typography.headingL,
                color = MedicalMateTheme.colors.fgDefault,
            )
            MedicalMateBadge(
                // 재방문이 쌓이면 "진료 완료" 대신 "진료 2회"가 온다. 몇 번인지는 상태가
                // 아니라 세어 봐야 아는 값이라 데이터가 문장으로 준다.
                label = detail.badge ?: recordStatusLabel(detail.status),
                tone = recordStatusTone(detail.status),
            )
        }
        Text(
            text = detail.clinicLine,
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

/**
 * 타임라인. Figma `735:3843`.
 *
 * 세로선은 단계마다 [Modifier.drawBehind]로 그린다. 점 가운데에서 시작해 단계 사이 간격까지
 * 덮으므로 다음 단계의 선과 이어지고, 마지막 단계는 그리지 않는다. Figma의 `Rail Line`도
 * 첫 점 가운데에서 마지막 점 가운데까지만 있다.
 *
 * 선을 단계 전체를 감싸는 한 겹으로 두지 않은 이유는, 아래에서 잘라낼 높이가 마지막 단계
 * 높이에 따라 달라져 고정값으로 둘 수 없기 때문이다.
 */
@Composable
private fun Timeline(steps: List<RecordStep>, expandedSteps: Set<Int>, onExpandToggle: (Int) -> Unit) {
    val railColor = MedicalMateTheme.colors.borderSubtle
    Column(modifier = Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val trailing = index != steps.lastIndex
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .drawBehind { if (trailing) drawRail(railColor) }
                    .padding(bottom = if (trailing) MedicalMateSpace.s16 else 0.dp),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
            ) {
                WhenRow(step)
                Box(modifier = Modifier.padding(start = IndentStart)) {
                    when (step) {
                        is RecordStep.Block ->
                            RecordStepBlock(
                                step = step,
                                expanded = index in expandedSteps,
                                onExpandToggle = { onExpandToggle(index) },
                            )
                        is RecordStep.Pending -> RecordStepPending(step)
                    }
                }
            }
        }
    }
}

/** 점 가운데부터 아래 끝까지. 아래 끝에 단계 사이 간격이 들어 있다. */
private fun DrawScope.drawRail(color: Color) {
    val top = RailTop.toPx()
    drawRect(
        color = color,
        topLeft = Offset(RailOffset.toPx(), top),
        size = Size(RailWidth.toPx(), size.height - top),
    )
}

/**
 * 점과 때.
 *
 * 지난 단계와 오지 않은 단계를 점 색으로 구분한다. Figma는 앞의 셋에 `bg/primary`를,
 * 재방문 예정에 `border/strong`을 썼다.
 */
@Composable
private fun WhenRow(step: RecordStep) {
    val dotColor =
        when (step) {
            is RecordStep.Block -> MedicalMateTheme.colors.bgPrimary
            is RecordStep.Pending -> MedicalMateTheme.colors.borderStrong
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(DotSize)
                .background(dotColor, MedicalMateRadius.full),
        )
        Text(
            text = step.at,
            style = MedicalMateTheme.typography.labelM,
            color = MedicalMateTheme.colors.fgSubtle,
        )
    }
}

@Composable
private fun FailedContent(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MedicalMateSize.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.NO_RESULT,
            title = stringResource(R.string.record_detail_failed_title),
            description = stringResource(R.string.record_detail_failed_description),
            actionLabel = stringResource(R.string.record_detail_retry),
            onActionClick = onRetryClick,
        )
    }
}

private val ContentBottom = 28.dp

private val HeadBottom = 18.dp

/** 점 지름. */
private val DotSize = 10.dp

/** 세로선이 점 가운데를 지나도록 둔 값. 점 지름 10의 절반에서 선 두께의 절반을 뺀다. */
private val RailOffset = 4.dp

private val RailWidth = 2.dp

/** 점 가운데. When Row 높이 18 안에서 점이 4부터 시작해 지름이 10이다. */
private val RailTop = 9.dp

/** 점과 세로선 오른쪽에서 블록이 시작하는 자리. */
private val IndentStart = 20.dp

/** 1j-3-X. 브리핑 카드를 펼친 상태다. */
@MedicalMateScreenPreviews
@Composable
private fun RecordDetailExpandedPreview() {
    MedicalMateTheme {
        RecordDetailScreen(
            state = RecordDetailUiState.Content(previewRecordDetail),
            onBackClick = {},
            expandedSteps = setOf(2),
            onExpandToggle = {},
            onRetryClick = {},
        )
    }
}

/** 1j-3-R. 재방문까지 다녀와 진료 후 기록이 둘인 상태다. */
@MedicalMateScreenPreviews
@Composable
private fun RecordDetailRevisitedPreview() {
    MedicalMateTheme {
        RecordDetailScreen(
            state = RecordDetailUiState.Content(recordDetailFixtures.getValue("card-4")),
            onBackClick = {},
            expandedSteps = emptySet(),
            onExpandToggle = {},
            onRetryClick = {},
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun RecordDetailScreenPreview() {
    MedicalMateTheme {
        RecordDetailScreen(
            state = RecordDetailUiState.Content(detail = previewRecordDetail),
            onBackClick = {},
            expandedSteps = emptySet(),
            onExpandToggle = {},
            onRetryClick = {},
        )
    }
}
