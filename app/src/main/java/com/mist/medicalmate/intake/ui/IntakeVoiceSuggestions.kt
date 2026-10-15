package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.voice.VoiceSuggestion

/*
 * 막대가 이 화면들에서 추천하는 말(#292). 실제로 알아듣는 말만 적는다. 예시를 바꾸면 그 말을 알아듣는지
 * 시험으로 본다.
 */

/** 증상 정리 1단계(1l). */
internal val BODY_PART_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_body_pick,
            R.string.voice_suggest_body_pick_do,
            MedicalMateIcons.BodyPoint,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_body_focus,
            R.string.voice_suggest_body_focus_do,
            MedicalMateIcons.BodyPoint,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_body_unsure,
            R.string.voice_suggest_body_unsure_do,
            MedicalMateIcons.Info,
        ),
    )

/** 증상 정리 3단계 문답(1c). 말한 것은 모두 답이다. */
internal val CHAT_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_chat_answer,
            R.string.voice_suggest_chat_answer_do,
            MedicalMateIcons.Chat,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_chat_repeat,
            R.string.voice_suggest_chat_repeat_do,
            MedicalMateIcons.Info,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_chat_next,
            R.string.voice_suggest_chat_next_do,
            MedicalMateIcons.ArrowRight,
        ),
    )

/** 증상 정리 2단계(1d). */
internal val SEVERITY_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_severity_word,
            R.string.voice_suggest_severity_word_do,
            MedicalMateIcons.HeartPulse,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_severity_score,
            R.string.voice_suggest_severity_score_do,
            MedicalMateIcons.HeartPulse,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_severity_next,
            R.string.voice_suggest_severity_next_do,
            MedicalMateIcons.ArrowRight,
        ),
    )

/** 증상 정리 3단계에서 문답이 끝난 뒤의 추천 질문 고르기(1c-D). */
internal val QUESTIONS_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_questions_add,
            R.string.voice_suggest_questions_add_do,
            MedicalMateIcons.Plus,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_questions_remove,
            R.string.voice_suggest_questions_remove_do,
            MedicalMateIcons.Trash,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_questions_next,
            R.string.voice_suggest_questions_next_do,
            MedicalMateIcons.ArrowRight,
        ),
    )

/** 정리 완료(1c-5). */
internal val INTAKE_DONE_SUGGESTIONS =
    listOf(
        VoiceSuggestion(R.string.voice_suggest_done_card, R.string.voice_suggest_done_card_do, MedicalMateIcons.Note),
        VoiceSuggestion(
            R.string.voice_suggest_done_hospital,
            R.string.voice_suggest_done_hospital_do,
            MedicalMateIcons.Hospital,
        ),
    )

/** 지금 단계의 추천. 세 단계가 한 목적지라 단계마다 다르다. 문답이 끝나면 질문 고르기 추천이다. */
internal fun IntakeUiState.voiceSuggestions(): List<VoiceSuggestion> = when (step) {
    IntakeStep.BODY_PART -> BODY_PART_SUGGESTIONS
    IntakeStep.SEVERITY -> SEVERITY_SUGGESTIONS
    IntakeStep.SYMPTOM_CHAT -> if (chatFinished) QUESTIONS_SUGGESTIONS else CHAT_SUGGESTIONS
}
