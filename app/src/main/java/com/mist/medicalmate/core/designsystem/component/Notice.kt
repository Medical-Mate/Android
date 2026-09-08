package com.mist.medicalmate.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md 8.4 `Notice`의 `Tone` variant. */
enum class MedicalMateNoticeTone {
    INFO,
    SUCCESS,
    WARNING,
    DANGER,
}

/**
 * DESIGN.md 8.4 `Notice`.
 *
 * 화면에 남는 인라인 안내다. 행동 직후 사라지는 피드백은 [MedicalMateToast]다.
 *
 * 톤을 색으로만 알리지 않는다. tone마다 아이콘이 다르고 제목이 tone foreground 색을
 * 갖는다(문서 1항 6번).
 *
 * [body]는 `fg/subtle`이다. 제목과 본문의 위계가 뒤집히면 훑을 때 무엇이 요점인지
 * 흐려진다.
 */
@Composable
fun MedicalMateNotice(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    tone: MedicalMateNoticeTone = MedicalMateNoticeTone.INFO,
) {
    val colors = MedicalMateTheme.colors
    val style = noticeStyle(tone)

    Surface(
        shape = MedicalMateRadius.md,
        color = style.container,
        contentColor = colors.fgDefault,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
            Modifier
                .heightIn(min = NoticeMinHeight)
                .padding(MedicalMateSpace.s16),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
        ) {
            Icon(
                painter = painterResource(style.icon),
                contentDescription = null,
                tint = style.accent,
                modifier = Modifier.size(MedicalMateSize.iconMd),
            )
            Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4)) {
                Text(
                    text = title,
                    style = MedicalMateTheme.typography.bodyMStrong,
                    color = style.accent,
                )
                body?.let {
                    Text(
                        text = it,
                        style = MedicalMateTheme.typography.bodyS,
                        color = colors.fgSubtle,
                    )
                }
            }
        }
    }
}

private class NoticeStyle(val container: Color, val accent: Color, @DrawableRes val icon: Int)

/** tone마다 면, 강조색, 아이콘이 함께 바뀐다. 색만 바꾸면 색각 이상에서 구분이 사라진다. */
@Composable
private fun noticeStyle(tone: MedicalMateNoticeTone): NoticeStyle {
    val colors = MedicalMateTheme.colors
    return when (tone) {
        MedicalMateNoticeTone.INFO ->
            NoticeStyle(colors.bgInfo, colors.fgInfo, MedicalMateIcons.Info)

        MedicalMateNoticeTone.SUCCESS ->
            NoticeStyle(colors.bgSuccess, colors.fgSuccess, MedicalMateIcons.CheckCircle)

        MedicalMateNoticeTone.WARNING ->
            NoticeStyle(colors.bgWarning, colors.fgWarning, MedicalMateIcons.AlertTriangle)

        MedicalMateNoticeTone.DANGER ->
            NoticeStyle(colors.bgDanger, colors.fgDanger, MedicalMateIcons.AlertCircle)
    }
}

/** 문서 8.4가 지정한 높이. 본문이 길어지면 늘어난다. */
private val NoticeMinHeight = 79.dp
