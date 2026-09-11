package com.mist.medicalmate.profile.ui

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.mist.medicalmate.R

/**
 * 온보딩 네 장. Figma `V2-00`~`V2-03`(`1320:4570` · `1320:4605` · `1320:4635` · `1320:4675`).
 *
 * 로그인 뒤 한 번 지나가는 흐름이다. 장마다 제목 한 마디와 설명 세 줄, 그림 하나다.
 *
 * 문구를 열거형이 들고 있다. 장마다 자리가 같고 들어가는 글자만 달라서, 화면이 장 번호로
 * 분기하면 같은 `when`이 자리마다 하나씩 생긴다.
 *
 * 시안이 v1을 걷어내면서 장 위의 작은 말(eyebrow)과 첫 장의 단계 셋이 없어졌다. 네 장이
 * 같은 구조가 됐고, 첫 장만 제목이 두 줄이라 [PREPARE]의 Copy가 182, 나머지가 142다.
 * 그 차이는 화면이 남는 공간으로 흡수한다.
 */
@Keep
enum class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val illustration: Int,
) {
    PREPARE(
        title = R.string.onboarding_prepare_title,
        description = R.string.onboarding_prepare_description,
        illustration = R.drawable.img_onboarding_prepare,
    ),
    POINT(
        title = R.string.onboarding_point_title,
        description = R.string.onboarding_point_description,
        illustration = R.drawable.img_onboarding_point,
    ),
    CARD(
        title = R.string.onboarding_card_title,
        description = R.string.onboarding_card_description,
        illustration = R.drawable.img_onboarding_card,
    ),
    FOLLOW(
        title = R.string.onboarding_follow_title,
        description = R.string.onboarding_follow_description,
        illustration = R.drawable.img_onboarding_follow,
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
