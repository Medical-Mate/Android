package com.mist.medicalmate.profile.ui

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.mist.medicalmate.R

/**
 * 온보딩 네 장. Figma `A2 · 온보딩`(`1157:4246`~`1157:4249`).
 *
 * 로그인 뒤 한 번 지나가는 흐름이다. 첫 장은 무엇을 하는 앱인지 글로 말하고, 나머지 셋은
 * 그림 하나에 한 가지씩 보여준다.
 *
 * 문구를 열거형이 들고 있다. 장마다 자리가 같고 들어가는 글자만 달라서, 화면이 장 번호로
 * 분기하면 같은 `when`이 자리마다 하나씩 생긴다.
 *
 * [INTRO]만 그림이 없고 아래에 단계 셋이 붙는다. 그래서 화면이 첫 장과 나머지를 나눠
 * 그린다. 시안도 그 장만 구조가 다르다.
 */
@Keep
enum class OnboardingPage(@StringRes val eyebrow: Int, @StringRes val title: Int, @StringRes val description: Int) {
    INTRO(
        eyebrow = R.string.onboarding_intro_eyebrow,
        title = R.string.onboarding_intro_title,
        description = R.string.onboarding_intro_lead,
    ),
    BODY(
        eyebrow = R.string.onboarding_body_eyebrow,
        title = R.string.onboarding_body_title,
        description = R.string.onboarding_body_description,
    ),
    CARD(
        eyebrow = R.string.onboarding_card_eyebrow,
        title = R.string.onboarding_card_title,
        description = R.string.onboarding_card_description,
    ),
    TIMELINE(
        eyebrow = R.string.onboarding_timeline_eyebrow,
        title = R.string.onboarding_timeline_title,
        description = R.string.onboarding_timeline_description,
    ),
    ;

    /** 진행 표시의 현재 단계. 1부터 센다. */
    val step: Int get() = ordinal + 1

    val isLast: Boolean get() = this == entries.last()

    /** 하단 버튼. 마지막 장만 시작하기다. */
    @get:StringRes
    val ctaLabel: Int get() = if (isLast) R.string.onboarding_start else R.string.onboarding_next

    companion object {
        val total: Int = entries.size
    }
}
