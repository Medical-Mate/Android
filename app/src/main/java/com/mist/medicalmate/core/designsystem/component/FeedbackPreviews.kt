package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** Feedback 묶음 컴포넌트 Preview. 렌더링만 하는 코드라 이것이 유일한 확인 수단이다. */
@Preview(showBackground = true, name = "Notice - Tone", widthDp = 390)
@Composable
private fun NoticePreview() {
    FeedbackPreviewSurface {
        MedicalMateNotice(
            title = "9/3 검사 결과 확인일이에요",
            tone = MedicalMateNoticeTone.INFO,
        )
        MedicalMateNotice(
            title = "브리핑 카드를 저장했어요",
            body = "홈에서 다시 열 수 있어요",
            tone = MedicalMateNoticeTone.SUCCESS,
        )
        MedicalMateNotice(
            title = "아직 답하지 않은 항목이 있어요",
            body = "빈 칸이 있으면 의사가 물어볼 내용이 늘어나요",
            tone = MedicalMateNoticeTone.WARNING,
        )
        MedicalMateNotice(
            title = "저장하지 못했어요",
            body = "인터넷 연결을 확인해주세요",
            tone = MedicalMateNoticeTone.DANGER,
        )
    }
}

@Preview(showBackground = true, name = "Toast - Tone", widthDp = 390)
@Composable
private fun ToastPreview() {
    FeedbackPreviewSurface {
        MedicalMateToastTone.entries.forEach { tone ->
            MedicalMateToast(message = "${tone.name} 토스트 메시지", tone = tone)
        }
        MedicalMateToast(
            message = "카드를 삭제했어요",
            tone = MedicalMateToastTone.NEGATIVE,
            actionLabel = "실행 취소",
            onActionClick = {},
        )
        MedicalMateToast(
            message = "두 줄을 넘기는 긴 메시지는 잘린다. 두 줄에 담기지 않는 내용은 " +
                "Toast가 아니라 화면에 남을 안내여야 한다. 이 문장은 잘린다.",
        )
    }
}

@Preview(showBackground = true, name = "Empty State", widthDp = 390)
@Composable
private fun EmptyStatePreview() {
    FeedbackPreviewSurface {
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.NO_RECORD,
            title = "아직 저장된 카드가 없어요",
            description = "진료 준비를 시작하면 여기에 쌓여요",
            actionLabel = "새 진료 준비하기",
            onActionClick = {},
        )
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.OFFLINE,
            note = "작성하던 내용은 그대로 있어요",
            title = "연결이 끊겼어요",
            description = "다시 연결되면 이어서 저장할게요",
            actionLabel = "다시 시도",
            onActionClick = {},
        )
        MedicalMateEmptyState(
            type = MedicalMateEmptyStateType.MIC_DENIED,
            title = "마이크를 쓸 수 없어요",
            description = "설정에서 권한을 켜거나 직접 입력할 수 있어요",
            actionLabel = "직접 입력하기",
            onActionClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading", widthDp = 390)
@Composable
private fun LoadingPreview() {
    FeedbackPreviewSurface {
        MedicalMateLoadingSpinner(message = "증상을 정리하고 있어요")
        MedicalMateLoadingSkeleton(loadingDescription = "카드를 불러오는 중")
    }
}

@Preview(showBackground = true, name = "Progress Indicator", widthDp = 390)
@Composable
private fun ProgressIndicatorPreview() {
    FeedbackPreviewSurface {
        for (step in 1..4) {
            MedicalMateProgressIndicator(current = step)
        }
        // 6단계를 넘으면 연속 막대로 바뀐다.
        MedicalMateProgressIndicator(current = 3, total = 8)
    }
}

@Preview(showBackground = true, name = "Dialog", widthDp = 390, heightDp = 400)
@Composable
private fun DialogPreview() {
    MedicalMateTheme {
        MedicalMateDialog(
            title = "정말 탈퇴하시겠어요?",
            message = "저장된 브리핑 카드와 진료 기록이 모두 삭제되고 되돌릴 수 없어요",
            confirmLabel = "탈퇴하기",
            onConfirm = {},
            dismissLabel = "취소",
            onDismissRequest = {},
        )
    }
}

@Composable
private fun FeedbackPreviewSurface(content: @Composable ColumnScope.() -> Unit) {
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
