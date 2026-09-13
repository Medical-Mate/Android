package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md의 `Empty State`의 `Type` variant. */
enum class MedicalMateEmptyStateType {
    NO_RECORD,
    NO_RESULT,
    OFFLINE,
    MIC_DENIED,
}

/**
 * DESIGN.md의 `Empty State`.
 *
 * 사과보다 다음 행동을 제시한다. "죄송합니다"로 시작하면 사용자는 무엇을 해야 할지
 * 모른 채 화면을 떠난다.
 *
 * [note]는 제목 위에 먼저 나온다. [MedicalMateEmptyStateType.OFFLINE]에서는 작성 내용이
 * 남아 있다는 사실을 가장 먼저 알려야 한다(문서의 컴포넌트 규격). 연결이 끊겼다는 말만 보이면
 * 환자는 방금 적은 증상이 날아갔다고 생각한다.
 *
 * 행동은 채움 없는 글자다. 높이 48 · 반경 14 · `Label/L`에 `fg/link`다. **마스터는 Tonal
 * 알약으로 그려져 있지만 시안의 인스턴스가 전부 채움을 지웠다**(`1j-2`·`1r-2` 등). 화면에
 * 실제로 그려진 쪽을 따랐고, 어느 쪽이 정본인지는 디자인 트랙 확인 대기다.
 *
 * **없을 수도 있다** — 화면에 이미 같은 행동을 부르는 버튼이 있으면 시안이 그 자리를
 * 꺼 둔다(홈의 `1n-2`가 그렇다).
 *
 * 아이콘이 옅은 브랜드 원 안에 들어간다. 맨 아이콘만 두면 빈 화면 가운데에 획만 남아
 * 무엇을 보라는 것인지 약하다.
 *
 * 세로로 가운데 정렬한다. 마스터가 `justify-center`이고, 남은 높이를 받으면 그 안에서
 * 가운데에 선다. 높이를 따로 주지 않으면 내용만큼만 차지해서 정렬이 드러나지 않는다.
 */
@Composable
fun MedicalMateEmptyState(
    type: MedicalMateEmptyStateType,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val colors = MedicalMateTheme.colors

    @DrawableRes val icon =
        when (type) {
            MedicalMateEmptyStateType.NO_RECORD -> MedicalMateIcons.EmptyBox
            MedicalMateEmptyStateType.NO_RESULT -> MedicalMateIcons.SearchOff
            MedicalMateEmptyStateType.OFFLINE -> MedicalMateIcons.WifiOff
            MedicalMateEmptyStateType.MIC_DENIED -> MedicalMateIcons.MicOff
        }

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(vertical = MedicalMateSpace.s40),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12, Alignment.CenterVertically),
    ) {
        IconCircle(icon)
        // 글 묶음은 마스터에서 한 칸이다. 원과의 간격이 12 + 4이고 제목과 설명 사이는 6이라,
        // 바깥 간격 하나로 셋을 벌리면 제목과 설명이 한 덩어리로 읽히지 않는다.
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = MedicalMateSpace.s4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
        ) {
            note?.let {
                Text(
                    text = it,
                    style = MedicalMateTheme.typography.bodyMStrong,
                    color = colors.fgPrimary,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = title,
                style = MedicalMateTheme.typography.headingM,
                color = colors.fgDefault,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = MedicalMateTheme.typography.bodyM,
                color = colors.fgSubtle,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onActionClick != null) {
            EmptyStateAction(label = actionLabel, onClick = onActionClick)
        }
    }
}

/**
 * 다음 행동.
 *
 * 채움 없는 글자다. 마스터는 Tonal 알약이지만 시안의 인스턴스가 전부 채움을 지웠다.
 * 크기와 반경은 마스터 그대로라 누를 수 있는 자리는 48로 남는다.
 */
@Composable
private fun EmptyStateAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(MedicalMateSize.controlMd)
            .clip(MedicalMateRadius.buttonM)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MedicalMateSpace.s20),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MedicalMateTheme.typography.labelL,
            color = MedicalMateTheme.colors.fgLink,
        )
    }
}

/** 옅은 브랜드 원 안의 아이콘. 맨 아이콘만 두면 빈 화면 가운데에 획만 남는다. */
@Composable
private fun IconCircle(@DrawableRes icon: Int) {
    Box(
        modifier =
        Modifier
            .size(EmptyStateCircleSize)
            .background(
                color = MedicalMateTheme.colors.bgPrimaryFaint,
                shape = MedicalMateRadius.full,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MedicalMateTheme.colors.fgPrimary,
            modifier = Modifier.size(EmptyStateIconSize),
        )
    }
}

/** 빈 상태 그림은 기본 아이콘보다 크게 둔다. 화면 가운데를 채우는 유일한 요소다. */
private val EmptyStateIconSize = 32.dp

/** 아이콘을 감싸는 옅은 원. 마스터가 72이고 반경이 36이다. */
private val EmptyStateCircleSize = 72.dp
