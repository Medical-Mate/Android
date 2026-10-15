package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateLogo
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Hint Row`(`3645:2369`). 하단 버튼 위 한 줄 안내.
 *
 * 아이콘 18과 `Label/M` `fg/primary`를 8 간격으로 가운데에 둔다. [onClick]이 있으면 줄 전체가 버튼이다 —
 * 1r-4는 이 줄을 눌러 예약 문자 안내 시트(1r-4-S)를 연다. 높이는 마스터대로 18이다.
 */
@Composable
fun MedicalMateHintRow(
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val color = MedicalMateTheme.colors.fgPrimary
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(MedicalMateSize.iconSm),
        )
        Text(text = label, style = MedicalMateTheme.typography.labelM, color = color)
    }
}

/**
 * DESIGN.md의 `Step Row`(`3644:7457`). 번호와 한 줄 안내.
 *
 * Primary [MedicalMateNumberBadge]와 `Body/M` `fg/default`를 10 간격으로 둔다.
 */
@Composable
fun MedicalMateStepRow(number: Int, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedicalMateNumberBadge(number = number)
        Text(
            text = text,
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgDefault,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * DESIGN.md의 `Illustration / Share Sheet`(`3645:2450`). 문자 앱 공유 시트 그림 160 높이.
 *
 * 받은 예약 문자 말풍선 아래에 공유 대상 네 칸이 선다. 진료메이트만 실제 로고이고 나머지는 회색
 * 스켈레톤이다. 그림이라 스크린 리더에는 읽히지 않는다 — 같은 내용을 시트의 단계 목록이 글로 적는다.
 */
@Composable
fun MedicalMateShareSheetIllustration(modifier: Modifier = Modifier) {
    val colors = MedicalMateTheme.colors
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .height(IllustrationHeight)
            .clip(MedicalMateRadius.md)
            .background(colors.bgSubtle)
            .padding(MedicalMateSpace.s12)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
    ) {
        Text(
            text = stringResource(R.string.share_illustration_message),
            style = MedicalMateTheme.typography.bodyS,
            color = colors.fgDefault,
            modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.bgSurface, MedicalMateRadius.sm)
                .padding(horizontal = MedicalMateSpace.s12, vertical = MedicalMateSpace.s10),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MedicalMateSpace.s8),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ShareTarget(stringResource(R.string.share_illustration_memo)) { Skeleton() }
            ShareTarget(stringResource(R.string.share_illustration_app), mine = true) {
                Image(
                    painter = painterResource(MedicalMateLogo.Symbol),
                    contentDescription = null,
                    modifier = Modifier.size(MedicalMateSize.controlSm),
                )
            }
            ShareTarget(stringResource(R.string.share_illustration_email)) { Skeleton() }
            ShareTarget(stringResource(R.string.share_illustration_more)) { Skeleton() }
        }
    }
}

/** 공유 대상 한 칸. 아이콘 40 아래 6 띄워 `Label/M` 이름. 진료메이트만 `fg/default`다. */
@Composable
private fun ShareTarget(label: String, mine: Boolean = false, icon: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s6),
    ) {
        icon()
        Text(
            text = label,
            style = MedicalMateTheme.typography.labelM,
            color = if (mine) MedicalMateTheme.colors.fgDefault else MedicalMateTheme.colors.fgSubtle,
        )
    }
}

@Composable
private fun Skeleton() {
    Box(
        modifier =
        Modifier
            .size(MedicalMateSize.controlSm)
            .background(MedicalMateTheme.colors.borderSubtle, MedicalMateRadius.sm),
    )
}

/** 마스터의 그림 높이 160. 크기 토큰에 없는 값이다. */
private val IllustrationHeight = 160.dp
