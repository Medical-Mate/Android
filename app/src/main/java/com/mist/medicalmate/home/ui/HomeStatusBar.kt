package com.mist.medicalmate.home.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import com.mist.medicalmate.core.designsystem.component.MedicalMateHomeBannerHeight

/*
 * 홈 배너 사진을 상태바 뒤까지 잇는다(#312).
 *
 * 앱은 `enableEdgeToEdge()` 위에서 `Scaffold`가 `safeDrawing` 인셋만큼 모든 화면을 안쪽으로 민다. 홈만 그 위쪽
 * 여백을 되돌려 사진을 상태바 뒤에 깐다. 시안의 홈 프레임에는 상태바가 없고 배너가 y=0에서 400이라, 상태바
 * 아래의 배너는 그대로 400이고 상태바 높이만큼 사진이 위로 더 그려진다. 헤더는 상태바 아래에 선다.
 */

/** `Scaffold`가 위에 민 높이. 상태바(와 위쪽 컷아웃)다. */
@Composable
internal fun statusBarTop(): Dp = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

/**
 * [top]만큼 위로 늘려 그린다. 부모가 준 높이에 [top]을 더해 재고 그만큼 위에 놓는다. 그래서 아래 끝은 그대로이고
 * 위로만 상태바 뒤까지 나간다.
 */
internal fun Modifier.underStatusBar(top: Dp): Modifier = layout { measurable, constraints ->
    val extra = top.roundToPx()
    val grown =
        if (constraints.hasBoundedHeight) {
            constraints.copy(maxHeight = constraints.maxHeight + extra, minHeight = constraints.minHeight + extra)
        } else {
            constraints
        }
    val placeable = measurable.measure(grown)
    layout(placeable.width, (placeable.height - extra).coerceAtLeast(0)) { placeable.place(0, -extra) }
}

/**
 * 상태바 아이콘 색. 어두운 사진 위에서는 흰 아이콘([light])이어야 보인다. 홈을 떠나면 앱의 기본(밝은 면 위의
 * 어두운 아이콘)으로 되돌린다.
 */
@Composable
internal fun HomeStatusBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = view.context.findActivity()?.window ?: return
    SideEffect { WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !light }
    DisposableEffect(window) {
        onDispose { WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true }
    }
}

/** 사진이 상태바 뒤에 있는 동안의 스크롤 한계. 시트가 배너에 28 겹쳐 올라오므로 그만큼 먼저 바뀐다. */
internal val PhotoUnderStatusBarScroll: Dp = MedicalMateHomeBannerHeight - SheetOverlap

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
