package com.mist.medicalmate.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateFontFamily
import com.mist.medicalmate.core.designsystem.MedicalMateLogo
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 와이어프레임 스플래시. Figma `V2 · 스플래시`(`1320:4553`). 옛 `1a-1`(`397:1179`)이
 * 삭제되고 이 프레임이 그 자리다. 내용은 그대로다.
 *
 * 저장된 토큰으로 세션을 복구하는 동안 보이는 화면이다. `auth`에 두는 이유는 이 화면이
 * 세션 복구 상태([SessionUiState.Checking])의 그림이기 때문이다. Figma는 진입 플로우의
 * 첫 장으로 그려 뒀고 다음이 로그인(1o)이다.
 *
 * **진행 표시를 넣지 않았다.** Figma에 없다. 복구는 보통 한 프레임 안에 끝나고, 그때
 * 스피너는 깜빡임으로만 남는다.
 *
 * 복구가 빨리 끝나면 이 화면도 잠깐 스쳤다 사라진다. 최소 노출 시간을 두는 문제는
 * 디자인 트랙에 물어야 해서 지금은 인위적인 지연을 넣지 않았다(#63).
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgPrimary),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = MedicalMateSize.gutter, vertical = MedicalMateSpace.s16),
            verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s20, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MonoLightLockup()
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MedicalMateTheme.typography.bodyM,
                color = MedicalMateTheme.colors.fgOnPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(modifier = Modifier.height(MedicalMateSize.safeBottom))
    }
}

/**
 * 어두운 면 위의 로고 락업. Figma `351:1303`의 Mono Light 변형이다.
 *
 * `ic_logo_lockup`을 쓰지 않는다. 그 드로어블은 워드마크 먹색과 심볼 판까지 색이 박혀
 * 있어서 tint를 주면 세 색이 흰색 하나로 뭉개진다. 그래서 마크만 흰색으로 칠하고 워드마크는
 * 글자로 얹는다. Figma의 락업 컴포넌트도 심볼 인스턴스와 텍스트 노드 두 개다.
 *
 * 워드마크 서체를 본문과 같은 Pretendard Bold로 두는 것은 로고 규정이다. 별도 레터링을
 * 만들지 않는다.
 *
 * 자간 -3%는 한글 다섯 자를 기본 자간으로 두면 워드마크가 심볼보다 넓어져 무게중심이
 * 오른쪽으로 쏠리기 때문이다.
 *
 * 크기는 마스터(심볼 36 · 워드마크 22 · 간격 10)의 1.4배다. Figma 인스턴스를 키운 값이라
 * 반올림하지 않았다.
 *
 * **워드마크가 글꼴 배율을 따라가지 않는다.** 심볼은 `dp`라서 그대로인데 글자만 커지면
 * 락업의 비율이 깨진다. 로고는 읽는 문장이 아니라 그림이고, 락업 규정도 비율을 고정하라고
 * 한다. `dp`를 그 시점 밀도로 `sp`로 바꿔서 배율에서 떼어낸다. 태그라인은 문장이므로
 * 그대로 배율을 따른다.
 */
@Composable
private fun MonoLightLockup() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LockupGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(MedicalMateLogo.Mark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MedicalMateTheme.colors.fgOnPrimary),
            modifier = Modifier.size(LockupSymbolSize),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = wordmarkStyle(),
            color = MedicalMateTheme.colors.fgOnPrimary,
        )
    }
}

/** 마스터 36의 1.4배. */
private val LockupSymbolSize = 50.4.dp

/** 마스터 10의 1.4배. 심볼 높이의 약 1/4을 유지한다. */
private val LockupGap = 14.dp

/** 마스터 22 Bold의 1.4배. 자간 -3%는 락업 규정이다. */
@Composable
private fun wordmarkStyle(): TextStyle {
    val density = LocalDensity.current
    return TextStyle(
        fontFamily = MedicalMateFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = with(density) { WordmarkSize.toSp() },
        lineHeight = with(density) { WordmarkLineHeight.toSp() },
        letterSpacing = (-0.03).em,
    )
}

/** 마스터 22의 1.4배. `dp`로 두는 이유는 위 KDoc에 있다. */
private val WordmarkSize = 30.8.dp

private val WordmarkLineHeight = 39.2.dp

@MedicalMateScreenPreviews
@Composable
private fun SplashScreenPreview() {
    MedicalMateTheme {
        SplashScreen()
    }
}
