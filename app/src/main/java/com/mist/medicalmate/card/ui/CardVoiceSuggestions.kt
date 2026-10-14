package com.mist.medicalmate.card.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.voice.VoiceSuggestion

/*
 * 막대가 이 화면들에서 추천하는 말(#292). 실제로 알아듣는 말만 적는다. 예시를 바꾸면 그 말을 알아듣는지
 * 시험으로 본다.
 */

/** 브리핑 카드(1e-1). */
internal val BRIEF_CARD_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_card_save,
            R.string.voice_suggest_card_save_do,
            MedicalMateIcons.CheckCircle,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_card_down,
            R.string.voice_suggest_card_down_do,
            MedicalMateIcons.ChevronDown,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_card_back,
            R.string.voice_suggest_card_back_do,
            MedicalMateIcons.ChevronLeft,
        ),
    )

/** 브리핑 카드 목록(1j-4). */
internal val CARD_LIST_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_cards_ordinal,
            R.string.voice_suggest_cards_ordinal_do,
            MedicalMateIcons.Note,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_cards_title,
            R.string.voice_suggest_cards_title_do,
            MedicalMateIcons.Search,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_cards_date,
            R.string.voice_suggest_cards_date_do,
            MedicalMateIcons.Calendar,
        ),
    )
