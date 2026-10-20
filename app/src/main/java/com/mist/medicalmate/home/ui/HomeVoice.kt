package com.mist.medicalmate.home.ui

import androidx.compose.runtime.Composable
import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** 홈(1n)에서 한 말(#320). 전에는 홈에 알아듣는 말이 없어 홈에만 있는 버튼을 말로 부를 수 없었다. */
internal enum class HomeVoice {
    /** "이어서 하기". 하던 증상 정리를 연다. */
    RESUME,

    /** 다음 진료가 언제인지. 배너가 "다음 진료가 언제인지 물을 수 있어요"라고 적는다. */
    NEXT_VISIT,

    /** 내일 진료 배너의 "카드 다시 보기". */
    TOMORROW_CARD,

    /** 카드 이미지 저장 배너의 "카드 저장하기". */
    EXPORT_CARD,

    /** 예약 문자 배너의 "방법 보기". */
    MESSAGE_GUIDE,

    /** 일정 없는 카드 배너의 "진료 일정 잡기"(#336). 그 카드를 골라 둔 채 일정 추가를 연다. */
    CARD_SCHEDULE,

    /** 오늘 진료 배너의 "진료 후 기록하기"(#336). 배너를 누를 때와 같은 길로 들어간다. */
    TODAY_RECORD,
}

/** 홈의 말을 읽는다. 아니면 null이고 전역 명령으로 넘긴다. */
internal fun parseHomeVoice(text: String): HomeVoice? {
    val heard = VoiceWords.compact(text)
    val card = heard.contains("카드") || heard.contains("브리핑")
    if (RESUME.any { heard.contains(it) }) return HomeVoice.RESUME
    return bannerVoice(heard, card) ?: when {
        NEXT_VISIT.any { heard.contains(it) } -> HomeVoice.NEXT_VISIT
        card && heard.contains("내일") -> HomeVoice.TOMORROW_CARD
        card && EXPORT.any { heard.contains(it) } -> HomeVoice.EXPORT_CARD
        GUIDE.any { heard.contains(it) } -> HomeVoice.MESSAGE_GUIDE
        else -> null
    }
}

/** 배너 버튼 둘의 말(#336). 카드나 "진료 일정"과 함께인 일정 잡기, 알림이 아닌 진료 후 기록이다. */
private fun bannerVoice(heard: String, card: Boolean): HomeVoice? = when {
    (card || heard.contains("진료일정")) && CARD_SCHEDULE.any { heard.contains(it) } -> HomeVoice.CARD_SCHEDULE
    TODAY_RECORD.any { heard.contains(it) } && !heard.contains("알림") -> HomeVoice.TODAY_RECORD
    else -> null
}

/**
 * 홈의 말을 버튼과 같은 일로(#320). 다른 화면에서 "이어서 할래", "다음 진료 언제야"라고 하면 전역 명령이 홈을 열고
 * 그 말을 넘긴다. 넘겨받을 때는 아직 홈을 읽기 전일 수 있어 읽을 때까지 기다린다.
 */
@Composable
internal fun HomeVoiceTarget(uiState: StateFlow<HomeUiState>, callbacks: HomeCallbacks) {
    VoiceTargetEffect { text ->
        val voice = parseHomeVoice(text) ?: return@VoiceTargetEffect VoiceReply.NotMine
        val home = withTimeoutOrNull(HOME_WAIT_MS) { uiState.first { it !is HomeUiState.Loading } }
        if (home !is HomeUiState.Content) return@VoiceTargetEffect VoiceReply.Done(R.string.voice_home_not_ready)
        home.bannerAnswer(voice, callbacks) ?: when (voice) {
            HomeVoice.RESUME -> home.resume?.let {
                callbacks.onResumeClick(it.intakeId, it.ended)
                VoiceReply.Done(R.string.voice_home_resumed, listOf(it.symptomTitle))
            } ?: VoiceReply.Done(R.string.voice_home_no_resume)

            HomeVoice.NEXT_VISIT -> home.upcoming?.let { next ->
                // "다음 일정 열어 줘"는 다가오는 일정 카드를 누른 것과 같다(#336). 묻기만 하면 날짜만 답한다.
                if (LOOK.any { VoiceWords.compact(text).contains(it) }) {
                    callbacks.onScheduleClick(next.date, next.id)
                    VoiceReply.Done(R.string.voice_home_next_opened)
                } else {
                    nextVisitReply(next)
                }
            } ?: VoiceReply.Done(R.string.voice_home_no_visit, question = VoiceText(R.string.voice_home_ask_add))

            HomeVoice.EXPORT_CARD -> {
                callbacks.onSaveCardClick(home.savedCards.firstOrNull()?.id)
                VoiceReply.Done(R.string.voice_home_export)
            }

            HomeVoice.MESSAGE_GUIDE -> {
                callbacks.onMessageGuideClick()
                VoiceReply.Done(R.string.voice_home_guide)
            }

            // 배너의 말은 위에서 답했다.
            else -> VoiceReply.NotMine
        }
    }
}

/**
 * 배너 버튼의 말. 배너의 말이 아니면 null이다. 그 배너가 없으면 전역 명령이 받는다(#336) — 카드 없이 일정 추가,
 * 캘린더를 거쳐 진료 후 기록이다. 내일 진료 배너는 없으면 없다고 한다.
 */
private fun HomeUiState.Content.bannerAnswer(voice: HomeVoice, callbacks: HomeCallbacks): VoiceReply? = when (voice) {
    HomeVoice.CARD_SCHEDULE -> banners.filterIsInstance<HomeBanner.CardReady>().firstOrNull()?.let {
        callbacks.onScheduleAddClick(it.cardId)
        VoiceReply.Done(R.string.voice_home_card_schedule, listOf(it.symptom))
    } ?: VoiceReply.NotMine

    HomeVoice.TODAY_RECORD -> banners.filterIsInstance<HomeBanner.Today>().firstOrNull()?.let {
        callbacks.onRecordVisitClick(it.visit)
        VoiceReply.Done(R.string.voice_home_today_record)
    } ?: VoiceReply.NotMine

    HomeVoice.TOMORROW_CARD -> banners.filterIsInstance<HomeBanner.Tomorrow>().firstOrNull()?.let {
        callbacks.onSavedCardClick(it.cardId)
        VoiceReply.Done(R.string.voice_home_tomorrow_card)
    } ?: VoiceReply.Done(R.string.voice_home_no_tomorrow)

    else -> null
}

/** "10월 20일 (월) 오후 3:00, 서울OO병원 내과예요". 시각이나 병원이 없으면 그 자리를 뺀다. */
private fun nextVisitReply(schedule: HomeSchedule): VoiceReply {
    val day =
        schedule.date.format(DAY) + " (" + schedule.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN) + ")"
    val time = schedule.time?.let { " " + it.format(TIME) }.orEmpty()
    val clinic = listOfNotNull(schedule.clinic, schedule.department).joinToString(" ")
    return if (clinic.isEmpty()) {
        VoiceReply.Done(R.string.voice_home_next_visit, listOf(day + time))
    } else {
        VoiceReply.Done(R.string.voice_home_next_visit_at, listOf(day + time, clinic))
    }
}

private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

/** 홈을 읽기를 기다리는 한도. */
private const val HOME_WAIT_MS = 5_000L

private val RESUME = listOf("이어서", "이어하", "이어할", "하던거", "하던정리", "하다만")

private val NEXT_VISIT =
    listOf("다음진료", "다음예약", "다음일정", "진료언제", "진료가언제", "병원언제", "언제가야", "진료날언제")

private val EXPORT = listOf("저장", "이미지", "사진", "갤러리", "공유", "내보내")

/** 보거나 열자는 말. "다음 일정 열어 줘". */
private val LOOK = listOf("보여", "열어", "볼래", "띄워")

/** 일정을 잡자는 말. 카드나 "진료 일정"과 함께일 때만 이 배너의 일이다. */
private val CARD_SCHEDULE = listOf("일정잡", "일정추가", "일정만들", "일정넣", "예약잡")

/** 진료 후 기록을 남기자는 말. 전역 명령과 같은 말이다. */
private val TODAY_RECORD = listOf("진료후기록", "기록할", "기록남", "진료끝", "진료다녀", "진료받고왔", "들은말")

private val GUIDE = listOf("예약문자", "문자로", "문자를", "문자어떻게", "문자넣", "문자로일정")
