package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * DESIGN.md의 `Notice`의 `Tone` variant.
 *
 * [BRAND]는 마스터(`292:668`)에 없는 톤이다. 기록 상세(`1j-3`)의 예정 알림 인스턴스
 * (`1076:4047`)가 면을 `bg/primary`로, 글자를 `fg/on-primary`로 덮어 그려서 그 모양을
 * 톤으로 올렸다. 마스터에 변이로 올려 달라고 디자인 트랙에 넘길 항목이다.
 */
enum class MedicalMateNoticeTone {
    INFO,
    SUCCESS,
    WARNING,
    DANGER,
    BRAND,
}

/**
 * DESIGN.md의 `Notice`. Figma `292:668`.
 *
 * 화면에 남는 인라인 안내다. 행동 직후 사라지는 피드백은 [MedicalMateToast]다.
 *
 * **아이콘은 흰 원형 배지 위에 놓는다**(#243). 마스터가 32 원에 20 아이콘이다. 파스텔 면에
 * 아이콘을 바로 놓으면 대비가 낮아 묻히고, 배지가 아이콘 뒤에 밝은 바탕을 깔아 준다. 여백은
 * 왼쪽 14 · 오른쪽 16 · 위아래 14이고 글 묶음은 위 4를 띄워 배지의 세로 가운데에 첫 줄이 온다.
 *
 * 톤을 색으로만 알리지 않는다. tone마다 아이콘이 다르고 제목이 tone foreground 색을
 * 갖는다(문서의 D11).
 *
 * [body]는 `fg/subtle`이다. 제목과 본문의 위계가 뒤집히면 훑을 때 무엇이 요점인지
 * 흐려진다. 채운 면([MedicalMateNoticeTone.BRAND])에서는 둘 다 `fg/on-primary`다 — 어두운
 * 면 위에서 `fg/subtle`은 읽히지 않는다.
 */
@Composable
fun MedicalMateNotice(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    tone: MedicalMateNoticeTone = MedicalMateNoticeTone.INFO,
) {
    val style = noticeStyle(tone)

    Surface(
        shape = MedicalMateRadius.md,
        color = style.container,
        contentColor = style.title,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
            Modifier.padding(
                start = MedicalMateSpace.s14,
                end = MedicalMateSpace.s16,
                top = MedicalMateSpace.s14,
                bottom = MedicalMateSpace.s14,
            ),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                Modifier
                    .size(IconBadgeSize)
                    .background(MedicalMateTheme.colors.bgSurface, MedicalMateRadius.full),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(style.icon),
                    contentDescription = null,
                    tint = style.accent,
                    modifier = Modifier.size(MedicalMateSize.iconMd),
                )
            }
            Column(
                modifier = Modifier.padding(top = MedicalMateSpace.s4),
                verticalArrangement = Arrangement.spacedBy(TextGap),
            ) {
                Text(
                    text = title,
                    style = MedicalMateTheme.typography.bodyMStrong,
                    color = style.title,
                )
                body?.let {
                    Text(
                        text = it,
                        style = MedicalMateTheme.typography.bodyS,
                        color = style.body,
                    )
                }
            }
        }
    }
}

/**
 * @param accent 배지 위 아이콘 색.
 * @param title 제목 색. 파스텔 톤은 [accent]와 같고 채운 톤은 흰색이다.
 * @param body 본문 색.
 */
private class NoticeStyle(
    val container: Color,
    val accent: Color,
    val title: Color,
    val body: Color,
    @param:DrawableRes val icon: Int,
)

/** tone마다 면, 강조색, 아이콘이 함께 바뀐다. 색만 바꾸면 색각 이상에서 구분이 사라진다. */
@Composable
private fun noticeStyle(tone: MedicalMateNoticeTone): NoticeStyle {
    val colors = MedicalMateTheme.colors
    return when (tone) {
        MedicalMateNoticeTone.INFO -> pastel(colors.bgInfo, colors.fgInfo, MedicalMateIcons.Info)
        MedicalMateNoticeTone.SUCCESS -> pastel(colors.bgSuccess, colors.fgSuccess, MedicalMateIcons.CheckCircle)
        MedicalMateNoticeTone.WARNING -> pastel(colors.bgWarning, colors.fgWarning, MedicalMateIcons.AlertTriangle)
        MedicalMateNoticeTone.DANGER -> pastel(colors.bgDanger, colors.fgDanger, MedicalMateIcons.AlertCircle)
        // 채운 면. 아이콘은 흰 배지 위라 브랜드색 그대로고, 글자만 흰색으로 뒤집힌다.
        MedicalMateNoticeTone.BRAND ->
            NoticeStyle(
                container = colors.bgPrimary,
                accent = colors.bgPrimary,
                title = colors.fgOnPrimary,
                body = colors.fgOnPrimary,
                icon = MedicalMateIcons.Info,
            )
    }
}

@Composable
private fun pastel(container: Color, tone: Color, @DrawableRes icon: Int) = NoticeStyle(
    container = container,
    accent = tone,
    title = tone,
    body = MedicalMateTheme.colors.fgSubtle,
    icon = icon,
)

/** 아이콘 뒤의 흰 원. 마스터의 `Icon Badge`가 32다. */
private val IconBadgeSize = 32.dp

/** 제목과 본문 사이. 마스터가 3이라 간격 토큰(2 · 4) 사이에 있다. */
private val TextGap = 3.dp
