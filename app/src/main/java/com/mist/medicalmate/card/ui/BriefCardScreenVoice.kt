package com.mist.medicalmate.card.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceSuggestion
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.core.voice.rememberVoicePrompt
import java.time.LocalDate

/** 브리핑 카드(1e-1)에서 말로 하는 일(#320). 하단 버튼 넷과 내보내기 시트, 저장 직후 일정 시트(1e-1-S)다. */
internal enum class BriefCardVoiceAction {
    /** 카드를 저장(확정)한다. */
    SAVE,

    /** 이 카드로 일정을 잡는다. "진료 일정 잡아 줘". */
    SCHEDULE,

    /** 잡아 둔 일정을 본다. "진료 일정 보여 줘". */
    VIEW_SCHEDULE,

    /** 진료 후 기록을 남긴다. */
    RECORD_VISIT,

    /** 이미지로 내보낸다. "이미지로 저장해 줘", "공유해 줘". 내보내기 시트를 연다. */
    EXPORT,

    /** 저장 직후 시트의 "아직 안 정했어요". */
    LATER,
}

/**
 * 1e-1의 말을 읽는다. 아니면 null이다.
 *
 * **차례가 곧 우선순위다.** 전에는 저장하라는 말만 알아들어서 "진료 일정 잡아 줘"·"일정 추가해 줘"가 "잡아줘"·
 * "추가해줘"로 카드 저장이 됐고, "이미지로 저장해 줘"도 카드 저장이 됐다. 내보내기와 일정이 저장보다 먼저다.
 */
internal fun parseBriefCardVoice(text: String): BriefCardVoiceAction? {
    val heard = VoiceWords.compact(text)
    return when {
        EXPORT.any { heard.contains(it) } -> BriefCardVoiceAction.EXPORT
        LATER.any { heard.contains(it) } -> BriefCardVoiceAction.LATER
        RECORD_VISIT.any { heard.contains(it) } -> BriefCardVoiceAction.RECORD_VISIT
        SCHEDULE_ADD.any { heard.contains(it) } -> BriefCardVoiceAction.SCHEDULE
        SCHEDULE.any { heard.contains(it) } -> BriefCardVoiceAction.VIEW_SCHEDULE
        VoiceWords.isSave(text) -> BriefCardVoiceAction.SAVE
        else -> null
    }
}

/** 이 화면에서 버튼과 같은 일을 한다. 화면을 떠나는 것은 라우트가 넘긴다. */
internal class BriefCardVoiceRoutes(
    val onSave: () -> Unit,
    val onExport: () -> Unit,
    val onLater: () -> Unit,
    val callbacks: () -> BriefCardCallbacks,
    /** 문답에서 막 만들어 저장한 카드면 뒤로 가기가 홈이다. 아니면 null이고 전역 "뒤로"가 맡는다. */
    val onBackHome: (() -> Unit)? = null,
)

/**
 * 말로 저장하고 일정을 잡고 내보낸다(#284 · #320). 버튼이 그 카드의 일정으로 갈리듯 말도 그렇다 — 저장하지 않은
 * 카드에 일정을 잡자고 하면 먼저 저장할지 묻는다. 저장이 곧 확정이라 한 번 묻는다. 고치는 중이면 받지 않는다.
 * 아직 저장하지 않은 카드가 열리면 저장하라고 막대에 묻는다.
 */
@Composable
internal fun BriefCardVoice(content: BriefCardUiState.Content?, routes: BriefCardVoiceRoutes) {
    val today = remember { LocalDate.now() }
    val card = content?.card
    val action = card?.footerAction(today)
    VoiceTargetEffect(briefCardSuggestions(action)) { text ->
        val voice = parseBriefCardVoice(text)
        val home = routes.onBackHome
        when {
            // 기기 뒤로 가기와 같은 곳으로 간다(#320). 말로 뒤로 가면 끝난 문답(1c-5)이 나와 카드를 또 만들 수 있었다.
            home != null && BACK.matches(VoiceWords.compact(text)) -> {
                home()
                VoiceReply.Done(R.string.voice_moved_home)
            }

            content == null || content.editing || voice == null || card == null -> VoiceReply.NotMine
            else -> answer(voice, content, today, routes)
        }
    }
    val prompt = rememberVoicePrompt()
    val saved = card?.status == BriefCard.Status.CONFIRMED
    LaunchedEffect(card?.id, saved) {
        if (content != null && !saved) prompt(VoicePrompt(question = VoiceText(R.string.voice_card_ask_save)))
    }
    // 저장 직후 일정 시트가 뜨면 말로도 답할 수 있게 묻는다.
    LaunchedEffect(content?.scheduleSheet) {
        if (content?.scheduleSheet == true) prompt(VoicePrompt(question = VoiceText(R.string.voice_card_ask_schedule)))
    }
}

private fun answer(
    voice: BriefCardVoiceAction,
    content: BriefCardUiState.Content,
    today: LocalDate,
    routes: BriefCardVoiceRoutes,
): VoiceReply {
    val card = content.card
    val saved = card.status == BriefCard.Status.CONFIRMED
    val appointment = card.appointment
    val callbacks = routes.callbacks()
    return when {
        voice == BriefCardVoiceAction.EXPORT -> {
            routes.onExport()
            VoiceReply.Done(R.string.voice_card_export_opened, question = VoiceText(R.string.voice_card_ask_export))
        }

        voice == BriefCardVoiceAction.SAVE ->
            if (saved) {
                VoiceReply.Done(R.string.voice_card_already_saved)
            } else {
                VoiceReply.Confirm(R.string.voice_card_confirm_save, onConfirm = routes.onSave)
            }

        // 저장하지 않은 카드는 일정에 걸 수 없다. 저장부터 묻는다 — 저장하면 일정 시트가 뜬다.
        !saved -> VoiceReply.Confirm(R.string.voice_card_save_first, onConfirm = routes.onSave)
        voice == BriefCardVoiceAction.LATER -> {
            routes.onLater()
            VoiceReply.Done(R.string.voice_card_later)
        }

        voice == BriefCardVoiceAction.SCHEDULE -> {
            callbacks.onScheduleClick()
            VoiceReply.Done(R.string.voice_moved_schedule_add)
        }

        appointment == null -> VoiceReply.Done(R.string.voice_card_no_schedule, question = askSchedule())
        voice == BriefCardVoiceAction.VIEW_SCHEDULE -> {
            callbacks.onViewScheduleClick(appointment)
            VoiceReply.Done(R.string.voice_card_schedule_opened)
        }

        appointment.date.isAfter(today) -> VoiceReply.Done(R.string.voice_day_upcoming)
        else -> {
            callbacks.onRecordVisitClick(appointment)
            VoiceReply.Done(R.string.voice_day_record_start)
        }
    }
}

private fun askSchedule() = VoiceText(R.string.voice_card_ask_schedule)

/** 지금 하단 버튼에 맞는 추천. 저장한 카드에 "저장해 줘"를 권하지 않는다. */
private fun briefCardSuggestions(action: BriefCardFooterAction?): List<VoiceSuggestion> {
    val main = when (action) {
        null, BriefCardFooterAction.SAVE ->
            VoiceSuggestion(
                R.string.voice_suggest_card_save,
                R.string.voice_suggest_card_save_do,
                MedicalMateIcons.CheckCircle,
            )

        BriefCardFooterAction.SCHEDULE ->
            VoiceSuggestion(
                R.string.voice_suggest_card_schedule,
                R.string.voice_suggest_card_schedule_do,
                MedicalMateIcons.Calendar,
            )

        BriefCardFooterAction.VIEW_SCHEDULE ->
            VoiceSuggestion(
                R.string.voice_suggest_card_view_schedule,
                R.string.voice_suggest_card_view_schedule_do,
                MedicalMateIcons.Calendar,
            )

        BriefCardFooterAction.RECORD_VISIT ->
            VoiceSuggestion(
                R.string.voice_suggest_card_record,
                R.string.voice_suggest_card_record_do,
                MedicalMateIcons.Note,
            )
    }
    return listOf(
        main,
        VoiceSuggestion(
            R.string.voice_suggest_card_export,
            R.string.voice_suggest_card_export_do,
            MedicalMateIcons.Export,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_card_down,
            R.string.voice_suggest_card_down_do,
            MedicalMateIcons.ChevronDown,
        ),
    )
}

private val BACK = Regex("""(?:뒤로|이전|이전화면|앞화면|돌아가|나가)(?:가|가줘|가자|줘|요|기|갈래|갈게)?""")

/** 내보내자는 말. 카드 저장("저장해 줘")보다 먼저 본다. */
private val EXPORT = listOf("이미지", "사진", "갤러리", "앨범", "공유", "내보내", "캡처", "캡쳐", "보내줘")

/** 저장 직후 시트의 "아직 안 정했어요". */
private val LATER = listOf("아직안정", "안정했", "나중에", "다음에할", "정해지면", "아직몰라", "안잡을")

private val RECORD_VISIT =
    listOf("진료후기록", "기록할", "기록하기", "기록남", "기록쓸", "기록해", "진료끝", "진료다녀", "진료받았", "병원다녀왔", "들은말")

private val SCHEDULE_ADD =
    listOf("일정잡", "일정추가", "일정등록", "일정만들", "일정넣", "예약잡", "예약추가", "예약등록", "진료잡", "날짜잡", "날잡", "예약해")

/** 일정을 보자는 말. 잡는 말을 먼저 걸러 둔 뒤에 본다. */
private val SCHEDULE = listOf("일정", "예약", "진료날", "언제")
