package com.mist.medicalmate.core.voice

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * 막대가 추천하는 말 한 줄(#292). "이렇게 말해도 돼요" 아래에 아이콘 · 말 · 하는 일 순으로 선다.
 *
 * 부를 때와 못 알아들었을 때 보인다. 사람은 무슨 말을 해야 할지 모를 때 막힌다. 지금 화면에서 실제로
 * 알아듣는 말만 적는다 — 예시대로 말했는데 못 알아들으면 예시가 거짓말이 된다.
 *
 * @param phrase 말할 것. 막대가 따옴표로 감싼다.
 * @param action 그 말이 하는 일. 오른쪽에 작게 선다.
 * @param icon 무슨 일인지 먼저 읽히게 하는 아이콘. `MedicalMateIcons`의 값이다.
 */
@Immutable
data class VoiceSuggestion(@StringRes val phrase: Int, @StringRes val action: Int, @DrawableRes val icon: Int)
