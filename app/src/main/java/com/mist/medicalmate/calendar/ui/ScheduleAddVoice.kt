package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.calendar.share.AppointmentTextReader
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 일정 추가(1r-4)에서 알아듣는 말(#282).
 *
 * 화면의 칸과 버튼을 말로 부른다. 날짜·시간·병원은 예약 문자 공유(#270)의 읽기를 그대로 써서,
 * "10월 20일 오후 3시 고려튼튼통증의학과"처럼 한 번에 말해도 칸이 채워진다.
 */
internal sealed interface ScheduleAddVoice {
    /** 저장. 확인을 받고 한다. */
    data object Save : ScheduleAddVoice

    /** 할 일 한 줄을 더한다. "할 일 추가, 복용 중인 약 챙기기". */
    data class Todo(val label: String) : ScheduleAddVoice

    /** 가져갈 카드를 고른다. "첫 번째 카드", "두통 카드". */
    data class Card(val ordinal: Int?, val words: String) : ScheduleAddVoice

    /** 날짜·시간·병원일 수 있는 말. 읽어 봐서 없으면 이 화면의 말이 아니다. */
    data class Fill(val text: String) : ScheduleAddVoice
}

/**
 * 받아쓴 말을 일정 추가의 할 일로.
 *
 * 차례가 곧 우선순위다. "할 일"로 시작하면 그 뒤가 무엇이든 할 일이다("할 일, 10시 전에 금식"의
 * 10시가 진료 시각이 되면 안 된다). 카드는 "카드"라는 말이 있을 때만이다. 저장은 날짜가 함께
 * 없을 때만이다. "20일로 저장해 줘"는 날짜를 고치는 말이 먼저다.
 */
internal fun parseScheduleAddVoice(text: String): ScheduleAddVoice? {
    val todo = TODO_PREFIX.find(text)?.groupValues?.get(1)?.trim { it in TRIM }
    return when {
        text.isBlank() -> null
        todo != null -> todo.takeIf { it.isNotEmpty() }?.let(ScheduleAddVoice::Todo)
        text.contains("카드") -> ScheduleAddVoice.Card(VoiceWords.ordinal(text), text)
        VoiceWords.isSave(text) && !DIGIT.containsMatchIn(text) -> ScheduleAddVoice.Save
        else -> ScheduleAddVoice.Fill(text)
    }
}

/**
 * 말로 고른 카드. 몇 번째로 말했으면 그 차례, 아니면 제목의 낱말이 말에 든 카드다.
 *
 * 제목 낱말은 두 글자 이상만 본다. "복부 통증 · 3주"의 "3주"나 "·"로 엉뚱한 카드를 고르지 않는다.
 * 여러 장이 걸리면 가장 많이 겹친 카드다. 하나도 안 걸리면 null이다.
 */
internal fun List<ScheduleAddCard>.pickByVoice(voice: ScheduleAddVoice.Card): ScheduleAddCard? {
    voice.ordinal?.let { ordinal ->
        return if (ordinal == VoiceWords.LAST) lastOrNull() else getOrNull(ordinal)
    }
    val heard = VoiceWords.compact(voice.words)
    return map { card -> card to card.titleWords().count { it in heard } }
        .filter { (_, hits) -> hits > 0 }
        .maxByOrNull { (_, hits) -> hits }
        ?.first
}

private fun ScheduleAddCard.titleWords(): List<String> =
    title.split(' ', '·', ',', '/').map(VoiceWords::compact).filter { it.length >= 2 && !DIGIT.containsMatchIn(it) }

private val TODO_PREFIX = Regex("""^\s*할\s*일\s*(?:추가|더해|적어)?\s*(?:해\s*줘|해\s*주세요)?\s*[,:]?\s*(.*)$""")

private val DIGIT = Regex("""\d""")

private const val TRIM = " .,!?"

/**
 * 일정 추가의 말을 화면 동작으로(#282). [ScheduleAddTodoActions]처럼 조작만 따로 든다.
 *
 * **날짜·시간·병원은 말한 대로 덮는다.** 예약 문자는 빈 칸만 채웠는데, 그것은 사람이 고른 값이
 * 나중의 뜻이어서였다. 말로 고친 것도 사람이 고른 것이고 더 나중이다. 병원을 말하면 그 이름으로
 * 병원 찾기를 연다. 같은 이름의 다른 지점이 있어 주소를 보고 고른다.
 *
 * 읽을 것이 하나도 없으면 [VoiceReply.NotMine]이다. "뒤로", "홈" 같은 전역 명령이 그 길로 간다.
 */
internal class ScheduleAddVoiceActions(
    private val reader: AppointmentTextReader,
    private val state: () -> ScheduleAddUiState,
    private val update: ((ScheduleAddUiState) -> ScheduleAddUiState) -> Unit,
    private val nextTodoId: () -> String,
) {
    /**
     * @param save 저장. 확인을 받은 뒤에 부른다. 라우트가 버튼과 같은 일(알림 권한 묻기와 저장)을
     *   넘긴다.
     */
    suspend fun handle(text: String, save: () -> Unit): VoiceReply = when (val voice = parseScheduleAddVoice(text)) {
        null -> VoiceReply.NotMine
        ScheduleAddVoice.Save -> save(save)
        is ScheduleAddVoice.Todo -> {
            update { it.copy(todos = it.todos + ScheduleAddTodo(id = nextTodoId(), label = voice.label)) }
            VoiceReply.Done(R.string.voice_schedule_todo_added, listOf(voice.label))
        }

        is ScheduleAddVoice.Card -> card(voice)
        is ScheduleAddVoice.Fill -> fill(voice.text, save)
    }

    /** 빠진 칸이 있으면 묻지 않고 알린다. 저장을 눌렀을 때처럼 그 칸이 붉어진다. */
    private fun save(save: () -> Unit): VoiceReply {
        val current = state()
        if (!current.canSave) {
            update { it.copy(showErrors = true) }
            return VoiceReply.Done(R.string.voice_schedule_missing)
        }
        return VoiceReply.Confirm(
            R.string.voice_schedule_confirm_save,
            listOf(current.voiceSummary()),
            onConfirm = save,
        )
    }

    private fun card(voice: ScheduleAddVoice.Card): VoiceReply {
        val card = state().cards.pickByVoice(voice) ?: return VoiceReply.Done(R.string.voice_schedule_card_missing)
        update { it.withCardPicked(card.id, picked = true) }
        return VoiceReply.Done(R.string.voice_schedule_card_picked, listOf(card.title))
    }

    private suspend fun fill(text: String, save: () -> Unit): VoiceReply {
        val read = reader.read(text)
        if (read.hospital == null && read.date == null && read.time == null) {
            // "저장해 줘 20일"처럼 숫자가 섞여 저장으로 못 읽은 말이 여기 온다. 날짜를 못 찾았으면 저장이다.
            return if (VoiceWords.isSave(text)) save(save) else VoiceReply.NotMine
        }
        update { current ->
            current.copy(
                date = read.date ?: current.date,
                time = read.time ?: current.time,
                hospital = read.hospital ?: current.hospital,
                hospitalSearch = read.hospital ?: current.hospitalSearch,
            )
        }
        val filled = listOfNotNull(read.date?.format(VOICE_DATE), read.time?.format(VOICE_TIME)).joinToString(" ")
        val hospital = read.hospital
        return when {
            hospital != null && filled.isNotEmpty() ->
                VoiceReply.Done(R.string.voice_schedule_filled_hospital, listOf(filled, hospital))

            hospital != null -> VoiceReply.Done(R.string.voice_schedule_hospital_search, listOf(hospital))
            else -> VoiceReply.Done(R.string.voice_schedule_filled, listOf(filled))
        }
    }
}
