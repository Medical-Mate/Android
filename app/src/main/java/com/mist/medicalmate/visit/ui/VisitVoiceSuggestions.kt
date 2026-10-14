package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.voice.VoiceSuggestion

/*
 * 막대가 이 화면들에서 추천하는 말(#292). 실제로 알아듣는 말만 적는다. 예시를 바꾸면 그 말을 알아듣는지
 * 시험으로 본다.
 */

/** 병원 찾기(1m · 1m-B). */
internal val HOSPITAL_PICK_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_pick_ordinal,
            R.string.voice_suggest_pick_ordinal_do,
            MedicalMateIcons.Hospital,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_pick_area,
            R.string.voice_suggest_pick_area_do,
            MedicalMateIcons.Hospital,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_pick_search,
            R.string.voice_suggest_pick_search_do,
            MedicalMateIcons.Search,
        ),
    )

/** 병원 확인(1m-12). */
internal val CLINIC_CONFIRM_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_clinic_yes,
            R.string.voice_suggest_clinic_yes_do,
            MedicalMateIcons.CheckCircle,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_clinic_other,
            R.string.voice_suggest_clinic_other_do,
            MedicalMateIcons.Search,
        ),
    )

/** 진료 후 메모(1p). 짧은 명령 말고는 메모에 적힌다. */
internal val VISIT_NOTE_SUGGESTIONS =
    listOf(
        VoiceSuggestion(R.string.voice_suggest_note_write, R.string.voice_suggest_note_write_do, MedicalMateIcons.Edit),
        VoiceSuggestion(R.string.voice_suggest_note_undo, R.string.voice_suggest_note_undo_do, MedicalMateIcons.Trash),
        VoiceSuggestion(
            R.string.voice_suggest_note_done,
            R.string.voice_suggest_note_done_do,
            MedicalMateIcons.ArrowRight,
        ),
    )

/** 진료 후 기록 확인(1q-1). 저장만 말로 한다(#296). 고치는 것은 `편집`으로 손으로 한다. */
internal val VISIT_RECORD_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_record_save,
            R.string.voice_suggest_record_save_do,
            MedicalMateIcons.CheckCircle,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_record_down,
            R.string.voice_suggest_record_down_do,
            MedicalMateIcons.ChevronDown,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_record_back,
            R.string.voice_suggest_record_back_do,
            MedicalMateIcons.ChevronLeft,
        ),
    )
