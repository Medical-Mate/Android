package com.mist.medicalmate.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceSuggestion
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceWords
import kotlinx.coroutines.flow.StateFlow

/**
 * 건강 정보 수정(1s-2)이 받는 말(#334). 버튼과 같은 일을 한다.
 *
 * 전에는 이 화면에 받는 말이 없어 "먹는 약 바꿀래"로 화면을 연 뒤에는 칩 하나도 말로 고를 수 없었고 "저장해 줘"도
 * "못 알아들었어요"였다.
 */
internal sealed interface HealthEditVoice {
    /** "고혈압 체크해 줘". 칩을 고른다. 이미 골라져 있으면 그대로다 — 누를 때처럼 뒤집지 않는다. */
    data class Choose(val step: ProfileSetupStep, val option: String) : HealthEditVoice

    /** "고혈압 빼 줘". 고른 칩을 푼다. */
    data class Remove(val step: ProfileSetupStep, val option: String) : HealthEditVoice

    /** "알러지에 복숭아 추가해 줘". 목록에 없는 것을 직접 넣는다. 화면의 "직접 추가"와 같다. */
    data class Add(val step: ProfileSetupStep, val value: String) : HealthEditVoice

    data object Save : HealthEditVoice
}

/**
 * 말을 읽는다. [options]는 갈래마다 화면에 선 칩이다(기본 목록과 직접 넣은 것).
 *
 * **칩 이름이 먼저다.** "위염·역류"처럼 둘을 묶은 칩은 어느 한쪽만 말해도 된다. 갈래를 말했으면("알러지에") 그 갈래의
 * 칩만 본다 — 아스피린은 약이기도 하고 알러지이기도 하다. 칩이 없으면 넣으라는 말일 때 갈래를 정해 새로 넣는다.
 * 저장은 맨 뒤에 본다. 공용 저장 말에 "추가해 줘"가 들어 있어 앞에서 보면 "고혈압 추가해 줘"가 저장이 된다.
 */
internal fun parseHealthEditVoice(text: String, options: Map<ProfileSetupStep, List<String>>): HealthEditVoice? {
    val heard = VoiceWords.compact(text)
    val stated = statedStep(heard)
    val match =
        options.entries
            .filter { stated == null || it.key == stated }
            .firstNotNullOfOrNull { (step, list) -> list.firstOrNull { it.matchesIn(heard) }?.let { step to it } }
    val remove = REMOVE.any { heard.contains(it) }
    val value = addedValue(text)
    return when {
        match != null && remove -> HealthEditVoice.Remove(match.first, match.second)
        match != null -> HealthEditVoice.Choose(match.first, match.second)
        stated != null && value != null && ADD.any { heard.contains(it) } -> HealthEditVoice.Add(stated, value)
        VoiceWords.isSave(text) -> HealthEditVoice.Save
        else -> null
    }
}

/** 칩 이름이 말에 들어 있는지. "위염·역류"는 "위염"이나 "역류"만 말해도 된다. */
private fun String.matchesIn(heard: String): Boolean =
    split('·').map { VoiceWords.compact(it) }.filter { it.length >= 2 }.any { heard.contains(it) }

/** 말한 갈래. 알러지를 먼저 본다 — "아스피린 알러지"에 "약"은 없지만 "진통제 알러지"처럼 섞일 수 있다. */
private fun statedStep(heard: String): ProfileSetupStep? = when {
    ALLERGY_WORDS.any { heard.contains(it) } -> ProfileSetupStep.ALLERGIES
    CONDITION_WORDS.any { heard.contains(it) } -> ProfileSetupStep.CONDITIONS
    MEDICATION_WORDS.any { heard.contains(it) } -> ProfileSetupStep.MEDICATIONS
    else -> null
}

/** 넣을 것. 갈래 말과 넣으라는 말을 떼고 남은 것이다. "알러지에 복숭아 추가해 줘" → "복숭아". */
private fun addedValue(text: String): String? = text
    .replace(STEP_PHRASE, " ")
    .replace(ADD_PHRASE, " ")
    .trim { it.isWhitespace() || it in TRIM }
    .takeIf { it.length >= 2 }

/** 화면을 연 동안 받는 말. 칩 이름은 화면이 읽은 목록이다. */
@Composable
internal fun HealthEditVoiceTarget(
    uiState: StateFlow<HealthEditUiState>,
    viewModel: HealthEditViewModel,
    onSave: () -> Unit,
) {
    val base = ProfileSetupStep.entries.associateWith { stringArrayResource(it.optionsRes).toList() }
    VoiceTargetEffect(HEALTH_EDIT_SUGGESTIONS) { text ->
        val state = uiState.value
        val options = base.mapValues { (step, list) -> (list + state.extrasIn(step)).distinct() }
        when (val voice = parseHealthEditVoice(text, options)) {
            null -> VoiceReply.NotMine
            is HealthEditVoice.Choose -> {
                viewModel.onVoiceChoose(voice.step, voice.option, chosen = true)
                VoiceReply.Done(R.string.voice_health_chosen, listOf(voice.option))
            }

            is HealthEditVoice.Remove -> {
                viewModel.onVoiceChoose(voice.step, voice.option, chosen = false)
                VoiceReply.Done(R.string.voice_health_removed, listOf(voice.option))
            }

            is HealthEditVoice.Add -> {
                viewModel.onVoiceAdd(voice.step, voice.value)
                VoiceReply.Done(R.string.voice_health_added, listOf(voice.value))
            }

            // 계정의 건강 정보를 통째로 덮어쓰는 저장이라 확인을 받는다. 카드 저장과 같다.
            HealthEditVoice.Save -> VoiceReply.Confirm(
                R.string.voice_health_confirm_save,
                confirmed = R.string.voice_saving,
                onConfirm = onSave,
            )
        }
    }
}

internal val HEALTH_EDIT_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_health_choose,
            R.string.voice_suggest_health_choose_do,
            MedicalMateIcons.CheckCircle,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_health_add,
            R.string.voice_suggest_health_add_do,
            MedicalMateIcons.HeartPulse,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_health_save,
            R.string.voice_suggest_health_save_do,
            MedicalMateIcons.CheckCircle,
        ),
    )

private const val TRIM = ".,!?~"

private val REMOVE = listOf("빼", "지워", "없애", "삭제", "해제", "풀어", "안먹")
private val ADD = listOf("추가", "넣어", "넣을", "있어", "적어")

private val ALLERGY_WORDS = listOf("알러지", "알레르기")
private val CONDITION_WORDS = listOf("질환", "진단", "지병")
private val MEDICATION_WORDS = listOf("먹는약", "복용약", "약에", "약으로", "약도")

/** 갈래를 가리키는 말과 뒤의 조사. */
private val STEP_PHRASE =
    Regex("""(?:알러지|알레르기|기저\s*질환|질환|진단받은\s*병|지병|먹는\s*약|복용\s*약|약)\s*(?:에|으로|로|도|은|는|이|가)?(?=\s|$)""")

/** 넣으라는 말과 그 뒤. "추가해 줘", "넣어 줘", "있어", "적어 줘". */
private val ADD_PHRASE = Regex("""\s*(?:을|를)?\s*(?:추가|넣어|넣을래|있어|적어)\S*(?:\s*(?:줘|주세요))?\s*$""")
