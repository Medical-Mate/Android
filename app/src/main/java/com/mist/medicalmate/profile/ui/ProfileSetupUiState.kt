package com.mist.medicalmate.profile.ui

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.mist.medicalmate.R

/**
 * 신상정보 입력의 세 단계. Figma 1b-1·1b-2·1b-3.
 *
 * 세 화면이 같은 레이아웃에 문구와 칩 목록만 다르다. 화면을 세 개 만들지 않고 단계를
 * 값으로 두고 한 화면이 그린다. 화면을 나누면 같은 배치를 세 번 적고, 한 곳을 고칠 때
 * 세 곳을 고쳐야 한다.
 *
 * 문구를 여기에 리소스 id로 들고 있는 이유는 단계마다 어떤 문구가 붙는지가 이 목록에서
 * 한눈에 보이기 때문이다. 화면 안에서 `when`으로 갈라 쓰면 단계를 늘릴 때 빠뜨린다.
 *
 * [labelRes]는 건강 정보 수정(1s-2)에서 쓰는 짧은 이름이다. 그 화면은 세 갈래를 한 번에
 * 보여줘서 질문 문장이 아니라 이름이 붙는다.
 */
enum class ProfileSetupStep(
    @StringRes val questionRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val labelRes: Int,
    @ArrayRes val optionsRes: Int,
) {
    MEDICATIONS(
        questionRes = R.string.profile_setup_medications_question,
        descriptionRes = R.string.profile_setup_medications_description,
        labelRes = R.string.profile_setup_medications_label,
        optionsRes = R.array.profile_setup_medications_options,
    ),
    CONDITIONS(
        questionRes = R.string.profile_setup_conditions_question,
        descriptionRes = R.string.profile_setup_conditions_description,
        labelRes = R.string.profile_setup_conditions_label,
        optionsRes = R.array.profile_setup_conditions_options,
    ),
    ALLERGIES(
        questionRes = R.string.profile_setup_allergies_question,
        descriptionRes = R.string.profile_setup_allergies_description,
        labelRes = R.string.profile_setup_allergies_label,
        optionsRes = R.array.profile_setup_allergies_options,
    ),
    ;

    /** 화면에 보이는 단계 번호. Figma의 `1 / 3`. */
    val number: Int get() = ordinal + 1

    val isLast: Boolean get() = this == entries.last()

    companion object {
        val total: Int = entries.size
    }
}

/**
 * 한 단계의 답.
 *
 * 없다고 답한 것을 따로 두지 않는다. 고를 것이 없으면 아무것도 고르지 않고 넘어간다.
 *
 * **빈 답을 "없다"로 보내지 않는다.** 서버는 `NONE`("없다")과 `UNKNOWN`("모른다")을 가르는데
 * 아무것도 고르지 않고 넘어간 것이 둘 중 어느 쪽인지 화면이 알 수 없다. 그래서 빈 답은
 * 모른다로 나간다. 자세한 사정은 `HealthField`에 적었다.
 */
data class ProfileSetupAnswer(val chosen: Set<String> = emptySet(), val note: String = "")

/**
 * 신상정보 입력 화면의 상태.
 *
 * 세 단계의 답을 한 곳에 모아 둔다. 단계를 오갈 때 이전 답이 남아 있어야 하고, 마지막에
 * 한 번에 저장한다.
 */
data class ProfileSetupUiState(
    val step: ProfileSetupStep = ProfileSetupStep.MEDICATIONS,
    val answers: Map<ProfileSetupStep, ProfileSetupAnswer> = emptyMap(),
    val completed: Boolean = false,
    val saving: Boolean = false,
    /**
     * 저장이 안 된 채로 완료를 눌렀는지.
     *
     * 다음 단계가 없는 화면이라 조용히 실패하면 버튼이 죽은 것처럼 보인다. 하단에 한 줄을
     * 띄워 다시 누를 수 있다는 것을 알린다. 시안에 없는 문구다.
     */
    val saveFailed: Boolean = false,
) {
    val answer: ProfileSetupAnswer get() = answers[step] ?: ProfileSetupAnswer()

    /** 첫 단계에서 뒤로 가면 화면을 벗어난다. 그 판단은 호출자가 한다. */
    val canGoBack: Boolean get() = step != ProfileSetupStep.entries.first()
}
