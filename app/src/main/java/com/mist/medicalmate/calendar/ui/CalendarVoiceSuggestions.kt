package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.voice.VoiceSuggestion

/*
 * 막대가 이 화면들에서 추천하는 말(#292). 실제로 알아듣는 말만 적는다. 예시를 바꾸면 그 말을 알아듣는지
 * 시험으로 본다.
 */

/** 일정 추가(1r-4). */
internal val SCHEDULE_ADD_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_schedule_date,
            R.string.voice_suggest_schedule_date_do,
            MedicalMateIcons.Calendar,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_schedule_hospital,
            R.string.voice_suggest_schedule_hospital_do,
            MedicalMateIcons.Hospital,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_schedule_todo,
            R.string.voice_suggest_schedule_todo_do,
            MedicalMateIcons.Check,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_schedule_save,
            R.string.voice_suggest_schedule_save_do,
            MedicalMateIcons.CheckCircle,
        ),
    )

/** 캘린더 월(1r-1). */
internal val CALENDAR_MONTH_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_month_day,
            R.string.voice_suggest_month_day_do,
            MedicalMateIcons.Calendar,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_month_next,
            R.string.voice_suggest_month_next_do,
            MedicalMateIcons.ChevronRight,
        ),
        VoiceSuggestion(R.string.voice_suggest_month_add, R.string.voice_suggest_month_add_do, MedicalMateIcons.Plus),
    )

/** 캘린더 일자(1r-2). */
internal val CALENDAR_DAY_SUGGESTIONS =
    listOf(
        VoiceSuggestion(R.string.voice_suggest_day_record, R.string.voice_suggest_day_record_do, MedicalMateIcons.Edit),
        VoiceSuggestion(R.string.voice_suggest_day_card, R.string.voice_suggest_day_card_do, MedicalMateIcons.Note),
        VoiceSuggestion(R.string.voice_suggest_day_open, R.string.voice_suggest_day_open_do, MedicalMateIcons.Note),
    )
