package com.mist.medicalmate.home.ui

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.component.MedicalMateBannerLink
import com.mist.medicalmate.core.designsystem.component.MedicalMateHomeBannerHeader
import com.mist.medicalmate.core.designsystem.component.MedicalMateHomeBannerPage
import kotlinx.coroutines.delay
import java.time.LocalTime

/*
 * 홈 맨 위 사진 배너(#312). Figma `Home Banner` 다섯 장과 각 장의 문구 · 사진 · 링크.
 */

/**
 * 맨 위 사진 배너. 장이 넘어가도 헤더는 그대로 있다.
 *
 * **4초마다 다음 장으로 0.5초에 걸쳐 넘어가고 끝에서 첫 장으로 이음매 없이 돈다**(문서의 모션 규칙). 그래서
 * 장 수를 아주 크게 잡고 가운데에서 시작한다. 손으로 넘기는 동안은 세지 않는다. 기기에서 애니메이션을 껐으면
 * 저절로 넘기지 않는다. 한 장뿐이면(처음 온 사람) 넘기지 않는다.
 */
@Composable
internal fun HomeBannerPager(content: HomeUiState.Content, callbacks: HomeCallbacks, modifier: Modifier = Modifier) {
    val banners = content.banners
    val looping = banners.size > 1
    val pager =
        rememberPagerState(initialPage = if (looping) LOOP_START - LOOP_START % banners.size else 0) {
            if (looping) LOOP_PAGES else banners.size
        }
    val animated = rememberAnimationsEnabled()

    if (looping && animated) {
        LaunchedEffect(pager.settledPage, pager.isScrollInProgress) {
            if (pager.isScrollInProgress) return@LaunchedEffect
            delay(BANNER_DWELL_MILLIS)
            pager.animateScrollToPage(
                pager.currentPage + 1,
                animationSpec = tween(BANNER_SLIDE_MILLIS, easing = MotionEasing),
            )
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth()) { page ->
            val index = page % banners.size
            BannerPage(
                banner = banners[index],
                page = stringResource(R.string.home_banner_page, index + 1, banners.size),
                callbacks = callbacks,
            )
        }
        MedicalMateHomeBannerHeader(
            userInitial = content.userInitial,
            notificationLabel =
            stringResource(
                if (content.hasUnreadNotification) R.string.home_notification_unread else R.string.home_notification,
            ),
            profileLabel = stringResource(R.string.home_profile),
            onNotificationClick = callbacks.onNotificationClick,
            onProfileClick = callbacks.onProfileClick,
        )
    }
}

/** 배너 한 장. 문구와 사진과 링크를 고른다. */
@Composable
private fun BannerPage(banner: HomeBanner, page: String, callbacks: HomeCallbacks) {
    val copy = bannerCopy(banner)
    MedicalMateHomeBannerPage(
        image = banner.image(),
        title = copy.title,
        body = copy.body,
        link = copy.link?.let {
            MedicalMateBannerLink(label = it, page = page, onClick = { callbacks.onBannerClick(banner) })
        },
    )
}

private class BannerCopy(val title: String, val body: String, val link: String?)

@Composable
private fun bannerCopy(banner: HomeBanner): BannerCopy = when (banner) {
    HomeBanner.Welcome ->
        BannerCopy(
            stringResource(R.string.home_banner_welcome_title),
            stringResource(R.string.home_banner_welcome_body),
            link = null,
        )

    is HomeBanner.Today ->
        BannerCopy(
            stringResource(R.string.home_banner_today_title),
            banner.clinic?.let { stringResource(R.string.home_banner_today_body, it) }
                ?: stringResource(R.string.home_banner_today_body_no_clinic),
            stringResource(R.string.home_banner_today_link),
        )

    is HomeBanner.Tomorrow ->
        BannerCopy(
            stringResource(R.string.home_banner_tomorrow_title),
            tomorrowDetail(banner.time, banner.clinic)?.let { stringResource(R.string.home_banner_tomorrow_body, it) }
                ?: stringResource(R.string.home_banner_tomorrow_body_no_detail),
            stringResource(R.string.home_banner_tomorrow_link),
        )

    is HomeBanner.CardReady ->
        BannerCopy(
            stringResource(R.string.home_banner_card_title),
            stringResource(R.string.home_banner_card_body, banner.symptom),
            stringResource(R.string.home_banner_card_link),
        )

    HomeBanner.Message ->
        BannerCopy(
            stringResource(R.string.home_banner_message_title),
            stringResource(R.string.home_banner_message_body),
            stringResource(R.string.home_banner_message_link),
        )

    HomeBanner.Schedule ->
        BannerCopy(
            stringResource(R.string.home_banner_schedule_title),
            stringResource(R.string.home_banner_schedule_body),
            stringResource(R.string.home_banner_schedule_link),
        )

    is HomeBanner.SaveCard ->
        BannerCopy(
            stringResource(R.string.home_banner_save_title),
            stringResource(R.string.home_banner_save_body),
            stringResource(R.string.home_banner_save_link),
        )

    HomeBanner.Mate ->
        BannerCopy(
            stringResource(R.string.home_banner_mate_title),
            stringResource(R.string.home_banner_mate_body),
            stringResource(R.string.home_banner_mate_link),
        )
}

/** "오전 10:30 서울OO병원 내과". 있는 것만 잇는다. 둘 다 없으면 null이다. */
internal fun tomorrowDetail(time: LocalTime?, clinic: String?): String? =
    listOfNotNull(time?.format(bannerTime), clinic).joinToString(" ").ifEmpty { null }

/** 장마다 다른 사진. 첫 장 자리의 셋은 같은 사진이고, 처음 홈은 그 사진을 짧은 칸에 맞춰 뽑은 것이다. */
@DrawableRes
private fun HomeBanner.image(): Int = when (this) {
    HomeBanner.Welcome -> R.drawable.img_home_banner_first
    is HomeBanner.Today, is HomeBanner.Tomorrow, is HomeBanner.CardReady -> R.drawable.img_home_banner_today
    HomeBanner.Message -> R.drawable.img_home_banner_message
    HomeBanner.Schedule -> R.drawable.img_home_banner_schedule
    is HomeBanner.SaveCard -> R.drawable.img_home_banner_save
    HomeBanner.Mate -> R.drawable.img_home_banner_mate
}

/** 기기에서 애니메이션을 껐는지. 개발자 옵션의 배율이 0이면 움직임을 멈춘다(문서의 모션 규칙). */
@Composable
private fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

/** 문서의 기본 곡선 `cubic-bezier(0.22, 1, 0.36, 1)`. */
private val MotionEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private const val BANNER_DWELL_MILLIS = 4_000L
private const val BANNER_SLIDE_MILLIS = 500

/** 끝없이 도는 것처럼 보이게 잡은 장 수와 시작점. */
private const val LOOP_PAGES = 10_000
private const val LOOP_START = LOOP_PAGES / 2
