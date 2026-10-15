package com.mist.medicalmate.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceSuggestion
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.core.voice.parseVoiceClock
import java.time.LocalTime

/** 내 정보(1s-1)에서 한 말(#320). 전에는 이 화면에 알아듣는 말이 하나도 없었다. */
internal sealed interface ProfileVoice {
    /** 알림을 켜고 끈다. [settings]가 둘이면 알림 둘 다다. */
    data class Toggle(val settings: List<AppSetting>, val enabled: Boolean) : ProfileVoice

    /** 알림 시간(1s-3)을 연다. */
    data object ReminderTime : ProfileVoice

    /** 건강 정보(1s-2)를 연다. */
    data object HealthEdit : ProfileVoice

    /** 로그아웃 · 탈퇴 · 헤이 메이트 끄기처럼 말로 하지 않는 것. 화면에서 하라고 한다. */
    data object ByHand : ProfileVoice
}

/**
 * 내 정보의 말을 읽는다. 아니면 null이다.
 *
 * "진료 후 기록 알림 꺼 줘", "진료 전 알림 켜", "알림 다 꺼 줘", "알림 시간 바꿀래", "먹는 약 바꿀래".
 * 켤지 끌지를 말하지 않은 알림 말("알림 설정")은 알림 시간이 아니라 그냥 이 화면이다 — null이다.
 */
internal fun parseProfileVoice(text: String): ProfileVoice? {
    val heard = VoiceWords.compact(text)
    val enabled = when {
        OFF.any { heard.contains(it) } -> false
        ON.any { heard.contains(it) } -> true
        else -> null
    }
    val reminder = heard.contains("알림")
    return when {
        BY_HAND.any { heard.contains(it) } -> ProfileVoice.ByHand
        reminder && (heard.contains("시간") || heard.contains("몇시") || CLOCK_HINT.containsMatchIn(heard)) ->
            ProfileVoice.ReminderTime

        reminder && enabled != null -> ProfileVoice.Toggle(remindersIn(heard), enabled)
        HEALTH.any { heard.contains(it) } -> ProfileVoice.HealthEdit
        else -> null
    }
}

/** 말에 든 알림. 진료 후 기록이 먼저다 — "진료 후 기록 알림"에 "진료"가 들어 있다. 어느 쪽도 아니면 둘 다다. */
private fun remindersIn(heard: String): List<AppSetting> = when {
    AFTER.any { heard.contains(it) } -> listOf(AppSetting.RECORD_REMINDER)
    BEFORE.any { heard.contains(it) } -> listOf(AppSetting.VISIT_REMINDER)
    else -> listOf(AppSetting.VISIT_REMINDER, AppSetting.RECORD_REMINDER)
}

/** 화면이 버튼과 같은 일을 한다. 알림을 켤 때 권한을 묻는 것까지 버튼과 같다. */
internal class ProfileVoiceRoutes(
    val onSettingChange: (AppSetting, Boolean) -> Unit,
    val onReminderTime: (spoken: String) -> Unit,
    val onHealthEdit: () -> Unit,
)

@Composable
internal fun MyProfileVoice(routes: ProfileVoiceRoutes) {
    val labels = AppSetting.entries.associateWith { stringResource(it.labelRes) }
    val both = stringResource(R.string.voice_profile_reminders_both)
    VoiceTargetEffect(PROFILE_SUGGESTIONS) { text ->
        when (val voice = parseProfileVoice(text)) {
            null -> VoiceReply.NotMine
            ProfileVoice.ByHand -> VoiceReply.Done(R.string.voice_profile_by_hand)
            ProfileVoice.HealthEdit -> {
                routes.onHealthEdit()
                VoiceReply.Done(R.string.voice_profile_health_opened)
            }

            ProfileVoice.ReminderTime -> {
                routes.onReminderTime(text)
                VoiceReply.Done(R.string.voice_profile_time_opened)
            }

            is ProfileVoice.Toggle -> {
                voice.settings.forEach { routes.onSettingChange(it, voice.enabled) }
                val name = voice.settings.singleOrNull()?.let(labels::getValue) ?: both
                VoiceReply.Done(
                    if (voice.enabled) R.string.voice_profile_turned_on else R.string.voice_profile_turned_off,
                    listOf(name),
                )
            }
        }
    }
}

/**
 * 알림 시간(1s-3)에서 한 말(#320). "오후 8시로 해 줘"는 휠을 그 시각으로 돌리고, "확인"·"저장"은 버튼과 같이
 * 저장한다. 휠이 10분 단위라 가까운 10분으로 맞춘다. 내 정보에서 "알림 시간 8시로 바꿔"로 들어오면 그 말을
 * 넘겨받아 같은 일을 한다.
 */
@Composable
internal fun ReminderTimeVoice(onTimeChange: (LocalTime) -> Unit, onConfirm: () -> Unit) {
    VoiceTargetEffect(REMINDER_TIME_SUGGESTIONS) { text ->
        val time = parseVoiceClock(text)?.roundedToWheel()
        when {
            time != null -> {
                onTimeChange(time)
                VoiceReply.Done(
                    R.string.voice_profile_time_set,
                    listOf(time.wheelLabel()),
                    question = VoiceText(R.string.voice_profile_ask_confirm),
                )
            }

            VoiceWords.isSave(text) || CONFIRM.any { VoiceWords.compact(text).contains(it) } -> {
                onConfirm()
                VoiceReply.Done(R.string.voice_profile_time_saved)
            }

            else -> VoiceReply.NotMine
        }
    }
}

/** 휠의 10분 단위로 맞춘다. 23시 55분은 넘기지 않고 23시 50분이다. */
private fun LocalTime.roundedToWheel(): LocalTime {
    val minutes = ((hour * MINUTES_IN_HOUR + minute + WHEEL_STEP / 2) / WHEEL_STEP) * WHEEL_STEP
    val capped = minutes.coerceAtMost(LAST_SLOT)
    return LocalTime.of(capped / MINUTES_IN_HOUR, capped % MINUTES_IN_HOUR)
}

/** "오후 8:00". 화면의 값 줄과 같은 꼴이다. */
private fun LocalTime.wheelLabel(): String {
    val meridiem = if (hour < NOON) "오전" else "오후"
    val hour12 = (hour % NOON).let { if (it == 0) NOON else it }
    return "$meridiem $hour12:${minute.toString().padStart(2, '0')}"
}

internal val PROFILE_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_profile_record_off,
            R.string.voice_suggest_profile_record_off_do,
            MedicalMateIcons.Bell,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_profile_time,
            R.string.voice_suggest_profile_time_do,
            MedicalMateIcons.Clock,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_profile_health,
            R.string.voice_suggest_profile_health_do,
            MedicalMateIcons.HeartPulse,
        ),
    )

internal val REMINDER_TIME_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_reminder_time,
            R.string.voice_suggest_reminder_time_do,
            MedicalMateIcons.Clock,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_reminder_confirm,
            R.string.voice_suggest_reminder_confirm_do,
            MedicalMateIcons.CheckCircle,
        ),
    )

private val OFF = listOf("꺼", "끄", "끌래", "안받", "그만받", "받지않", "받기싫", "해제", "중지", "없애")

private val ON = listOf("켜", "킬래", "받을래", "받고싶", "받을게", "알려줘", "다시받")

private val AFTER = listOf("진료후", "기록알림", "기록")

private val BEFORE = listOf("진료전", "전날", "하루전", "진료알림", "일정알림")

private val HEALTH = listOf("건강정보", "먹는약", "복용약", "기저질환", "알러지", "알레르기", "지병")

private val BY_HAND = listOf("로그아웃", "탈퇴", "헤이메이트", "메이트로부르", "정리모델", "음성모델", "기록정리")

private val CONFIRM = listOf("확인", "이걸로", "이대로", "그렇게해", "좋아")

/** 알림 말에 시각이 들어 있는지. "알림 8시로 바꿔". */
private val CLOCK_HINT = Regex("""\d{1,2}시|[한두세네섯곱덟홉열]시""")

private const val NOON = 12
private const val MINUTES_IN_HOUR = 60
private const val WHEEL_STEP = 10
private const val LAST_SLOT = 23 * 60 + 50
