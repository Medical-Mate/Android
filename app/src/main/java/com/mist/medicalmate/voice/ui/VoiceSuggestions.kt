package com.mist.medicalmate.voice.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.voice.VoiceSuggestion

/*
 * 막대가 이 화면들에서 추천하는 말(#292). 실제로 알아듣는 말만 적는다. 예시를 바꾸면 그 말을 알아듣는지
 * 시험으로 본다.
 */

/** 화면이 따로 추천하지 않을 때의 공통 예시. 홈·기록·내 정보가 여기다. */
internal val GLOBAL_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_global_intake,
            R.string.voice_suggest_global_intake_do,
            // Mate Card 마스터의 가이드가 증상 정리에 note를 단다(#312).
            MedicalMateIcons.Note,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_global_visit,
            R.string.voice_suggest_global_visit_do,
            MedicalMateIcons.Edit,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_global_day,
            R.string.voice_suggest_global_day_do,
            MedicalMateIcons.Calendar,
        ),
    )
