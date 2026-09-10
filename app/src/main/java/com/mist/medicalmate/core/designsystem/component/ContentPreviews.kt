package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** Content 묶음 컴포넌트 Preview. 렌더링만 하는 코드라 이것이 유일한 확인 수단이다. */
@Preview(showBackground = true, name = "Card - Emphasis", widthDp = 390)
@Composable
private fun CardPreview() {
    ContentPreviewSurface {
        MedicalMateCardEmphasis.entries.forEach { emphasis ->
            MedicalMateCard(emphasis = emphasis) {
                Text(text = emphasis.name, style = MedicalMateTheme.typography.headingS)
                Text(
                    text = "카드 본문이 들어간다. 최소 높이 116을 지킨다.",
                    style = MedicalMateTheme.typography.bodyM,
                    color = MedicalMateTheme.colors.fgSubtle,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Callout", widthDp = 390)
@Composable
private fun CalloutPreview() {
    ContentPreviewSurface {
        MedicalMateCallout(
            title = "환자가 묻고 싶어 하는 것",
            questions =
            listOf(
                "검사를 받아야 하나요?",
                "지금 진통제 계속 먹어도 되나요?",
                "어떤 증상이면 바로 다시 와야 하나요?",
            ),
        )
    }
}

@Preview(showBackground = true, name = "KV Row", widthDp = 390)
@Composable
private fun KvRowPreview() {
    ContentPreviewSurface {
        MedicalMateKvRow(key = "기간", value = "3주 전 시작")
        MedicalMateKvRow(
            key = "가장 아픈 곳",
            value = "오른쪽 손가락 관절",
            type = MedicalMateKvRowType.EMPHASIS,
        )
        MedicalMateKvRow(
            key = "복용 약",
            value = "타이레놀 500mg",
            type = MedicalMateKvRowType.LINK,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Severity Readout", widthDp = 390)
@Composable
private fun SeverityReadoutPreview() {
    ContentPreviewSurface {
        MedicalMateSeverity.entries.forEach { severity ->
            MedicalMateSeverityReadout(severity = severity)
        }
    }
}

@Preview(showBackground = true, name = "List Row", widthDp = 390)
@Composable
private fun ListRowPreview() {
    ContentPreviewSurface {
        MedicalMateListRow(title = "손가락 경직·부종", meta = "06.20 작성", onClick = {})
        MedicalMateListRow(
            title = "재방문·검사 결과",
            meta = "09.01 작성",
            badge = "이어서 작성",
            type = MedicalMateListRowType.BADGE,
            onClick = {},
        )
        MedicalMateListRow(
            title = "누를 수 없는 행",
            meta = "chevron 없음",
            type = MedicalMateListRowType.PLAIN,
        )
    }
}

@Preview(showBackground = true, name = "Doctor Card", widthDp = 390)
@Composable
private fun DoctorCardPreview() {
    ContentPreviewSurface {
        MedicalMateDoctorCard(
            name = "김민수",
            specialty = "정형외과",
            hours = "오늘 18:00까지",
            statusLabel = "진료 중",
        )
        MedicalMateDoctorCard(
            name = "이서연",
            specialty = "내과",
            hours = "내일 09:00 진료 시작",
            statusLabel = "진료 종료",
            state = MedicalMateDoctorCardState.CLOSED,
        )
    }
}

@Preview(showBackground = true, name = "Bubble", widthDp = 390)
@Composable
private fun BubblePreview() {
    ContentPreviewSurface {
        MedicalMateBubble(
            text = "언제부터 그러셨어요? 정확하지 않아도 괜찮아요.",
            senderLabel = "AI",
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            MedicalMateBubble(
                text = "한 3주쯤 됐어요. 요즘 더 아파요.",
                sender = MedicalMateBubbleSender.PATIENT,
            )
        }
    }
}

@Preview(showBackground = true, name = "Source Quote", widthDp = 390)
@Composable
private fun SourceQuotePreview() {
    ContentPreviewSurface {
        MedicalMateSourceQuote(
            fieldLabel = "기간",
            summary = "3주 전 시작 · 최근 악화",
            quoteLabel = "내가 말한 것",
            originalQuote = "\"한 3주쯤 됐나... 요즘 더 아파요\"",
            onEditClick = {},
            editContentDescription = "기간 수정",
        )
        MedicalMateSourceQuote(
            fieldLabel = "기간",
            summary = "3주 전 시작 · 최근 악화",
            quoteLabel = "내가 말한 것",
            originalQuote = "\"한 3주쯤 됐나... 요즘 더 아파요\"",
            onEditClick = {},
            editContentDescription = "기간 수정",
            editedBadge = "직접 고침",
        )
    }
}

@Preview(showBackground = true, name = "Badge / Avatar / Section Header / Divider", widthDp = 390)
@Composable
private fun SmallContentPreview() {
    ContentPreviewSurface {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedicalMateBadgeTone.entries.forEach { tone ->
                MedicalMateBadge(label = tone.name, tone = tone)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedicalMateAvatar(initial = "서연")
            MedicalMateAvatar(initial = "김", type = MedicalMateAvatarType.DOCTOR)
        }
        MedicalMateSectionHeader(title = "저장된 브리핑 카드", actionLabel = "전체 보기", onActionClick = {})
        MedicalMateDivider()
    }
}

@Composable
private fun ContentPreviewSurface(content: @Composable ColumnScope.() -> Unit) {
    MedicalMateTheme {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(MedicalMateTheme.colors.bgCanvas)
                .padding(MedicalMateSize.gutter),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            content = content,
        )
    }
}
