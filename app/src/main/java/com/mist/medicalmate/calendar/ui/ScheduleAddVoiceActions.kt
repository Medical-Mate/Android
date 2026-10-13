package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.calendar.share.AppointmentTextReader
import com.mist.medicalmate.calendar.share.SharedAppointment
import com.mist.medicalmate.calendar.share.timeIn
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 일정 추가에서 말로 하는 일 가운데 화면을 떠나는 것들. 라우트가 버튼과 같은 길을 넘긴다.
 *
 * @param save 저장. 알림 권한을 묻고 저장한다. 확인을 받은 뒤에 부른다.
 * @param exit 화면을 닫는다.
 * @param openHospital 지금 병원 이름으로 병원 찾기를 연다.
 * @param newCard 새 브리핑 카드를 만들러 간다.
 */
internal data class ScheduleAddVoiceRoutes(
    val save: () -> Unit = {},
    val exit: () -> Unit = {},
    val openHospital: () -> Unit = {},
    val newCard: () -> Unit = {},
)

/**
 * 일정 추가의 말을 화면 동작으로(#282). [ScheduleAddTodoActions]처럼 조작만 따로 든다.
 *
 * **날짜·시간·병원은 말한 대로 덮는다.** 예약 문자는 빈 칸만 채웠는데, 그것은 사람이 고른 값이
 * 나중의 뜻이어서였다. 말로 고친 것도 사람이 고른 것이고 더 나중이다. 병원을 말하면 그 이름으로
 * 병원 찾기를 연다. 같은 이름의 다른 지점이 있어 주소를 보고 고른다. 날짜나 시간 시트가 열린 채
 * 말하면 채우고 시트를 닫는다.
 *
 * 저장과, 적은 것을 버리고 나가기는 확인을 받는다. 나머지는 버튼을 누른 것과 같아 바로 한다.
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
     * 말 하나를 처리한다. **이 화면에 머무는 일이면 빠진 칸을 이어서 묻는다**(#282). 병원이 없으면
     * 병원을, 날짜가 없으면 날짜를, 다 찼으면 저장하라고 한다. 사용자는 막대를 보고 다음 말을 한다.
     * 병원 찾기·새 카드처럼 화면을 떠나는 일이면 묻지 않는다. 옮겨 간 화면이 묻는다.
     */
    suspend fun handle(text: String, routes: ScheduleAddVoiceRoutes): VoiceReply {
        val voice = parseScheduleAddVoice(text)
        val reply =
            when (voice) {
                null -> VoiceReply.NotMine
                is ScheduleAddVoice.Fill -> fill(voice.text, routes.save)
                ScheduleAddVoice.Save, ScheduleAddVoice.Exit, ScheduleAddVoice.OpenHospital, ScheduleAddVoice.CardNew ->
                    route(voice, routes)

                else -> form(voice)
            }
        val staying = voice?.let { it !in LEAVING } == true && state().hospitalSearch == null
        return if (reply is VoiceReply.Done && reply.question == null && staying) {
            reply.copy(question = state().nextQuestion())
        } else {
            reply
        }
    }

    /** 화면을 떠나거나 서버에 남기는 일. 저장과, 적은 것을 버리고 나가기는 확인을 받는다. */
    private fun route(voice: ScheduleAddVoice, routes: ScheduleAddVoiceRoutes): VoiceReply = when (voice) {
        ScheduleAddVoice.Save -> save(routes.save)
        ScheduleAddVoice.Exit ->
            if (state().hasInput) {
                VoiceReply.Confirm(R.string.voice_schedule_confirm_exit, onConfirm = routes.exit)
            } else {
                routes.exit()
                VoiceReply.Done(R.string.voice_command_moved)
            }

        ScheduleAddVoice.OpenHospital -> {
            routes.openHospital()
            VoiceReply.Done(R.string.voice_schedule_hospital_open)
        }

        ScheduleAddVoice.CardNew -> {
            routes.newCard()
            VoiceReply.Done(R.string.voice_schedule_card_new)
        }

        else -> VoiceReply.NotMine
    }

    /** 화면 안의 칸과 목록. 버튼을 누른 것과 같아 묻지 않는다. */
    private fun form(voice: ScheduleAddVoice): VoiceReply = when (voice) {
        is ScheduleAddVoice.OpenSheet -> {
            update { it.copy(sheet = voice.sheet) }
            val date = voice.sheet == ScheduleAddSheet.DATE
            VoiceReply.Done(if (date) R.string.voice_schedule_sheet_date else R.string.voice_schedule_sheet_time)
        }

        ScheduleAddVoice.ClearTime -> {
            update { it.copy(time = null, sheet = ScheduleAddSheet.NONE) }
            VoiceReply.Done(R.string.voice_schedule_time_cleared)
        }

        is ScheduleAddVoice.CardPick -> card(state().cards.matchVoice(voice.ordinal, voice.words), pick = true)
        is ScheduleAddVoice.CardDrop -> card(state().cards.toDrop(voice), pick = false)
        is ScheduleAddVoice.TodoAdd -> addTodo(voice.label)
        is ScheduleAddVoice.TodoCheck -> checkTodo(voice)
        is ScheduleAddVoice.TodoDelete -> deleteTodo(state().todos.matchVoice(voice.ordinal, voice.words))
        is ScheduleAddVoice.TodoEdit -> editTodo(voice)
        else -> VoiceReply.NotMine
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

    /** 맞는 카드가 없거나 여럿이면 하지 않고 되묻는다. 몇 번째인지로 답하면 된다. */
    private fun card(matches: List<ScheduleAddCard>, pick: Boolean): VoiceReply {
        val card = matches.singleOrNull() ?: return whichOne(matches.map { it.title }, ScheduleAddTarget.CARD)
        update { it.withCardPicked(card.id, picked = pick) }
        val message = if (pick) R.string.voice_schedule_card_picked else R.string.voice_schedule_card_dropped
        return VoiceReply.Done(message, listOf(card.title))
    }

    /** 글이 비면 더하지 않고 무엇을 적을지 묻는다. 빈 줄이 남으면 저장할 때 걸러야 한다. */
    private fun addTodo(label: String): VoiceReply {
        if (label.isEmpty()) return VoiceReply.Done(R.string.voice_schedule_todo_ask)
        update { it.copy(todos = it.todos + ScheduleAddTodo(id = nextTodoId(), label = label)) }
        return VoiceReply.Done(R.string.voice_schedule_todo_added, listOf(label))
    }

    private fun checkTodo(voice: ScheduleAddVoice.TodoCheck): VoiceReply {
        val matches = state().todos.matchVoice(voice.ordinal, voice.words)
        val todo = matches.singleOrNull() ?: return whichOne(matches.map { it.label }, ScheduleAddTarget.TODO)
        update { it.withTodo(todo.id) { found -> found.copy(done = voice.done) } }
        val message = if (voice.done) R.string.voice_schedule_todo_done else R.string.voice_schedule_todo_undone
        return VoiceReply.Done(message, listOf(todo.label))
    }

    private fun deleteTodo(matches: List<ScheduleAddTodo>): VoiceReply {
        val todo = matches.singleOrNull() ?: return whichOne(matches.map { it.label }, ScheduleAddTarget.TODO)
        update { it.copy(todos = it.todos.filterNot { found -> found.id == todo.id }) }
        return VoiceReply.Done(R.string.voice_schedule_todo_deleted, listOf(todo.label))
    }

    /** 고칠 줄은 몇 번째로 말한다. 할 일이 하나뿐이면 말하지 않아도 그 줄이다. */
    private fun editTodo(voice: ScheduleAddVoice.TodoEdit): VoiceReply {
        val todos = state().todos
        val todo = todos.byVoice(voice.ordinal, "") ?: todos.singleOrNull()
        return when {
            voice.label.isEmpty() -> VoiceReply.Done(R.string.voice_schedule_todo_edit_ask)
            todo == null -> VoiceReply.Done(R.string.voice_schedule_todo_missing)
            else -> {
                update { it.withTodo(todo.id) { found -> found.copy(label = voice.label, editing = false) } }
                VoiceReply.Done(R.string.voice_schedule_todo_edited, listOf(voice.label))
            }
        }
    }

    /** 날짜·시간·병원을 읽어 칸을 채운다. 말로 고친 값은 이미 있던 값을 덮는다. */
    private suspend fun fill(text: String, save: () -> Unit): VoiceReply {
        val (hospital, date, time) = reader.readSpoken(text)
        if (hospital == null && date == null && time == null) {
            // "저장해 줘 20일"처럼 숫자가 섞여 저장으로 못 읽은 말이 여기 온다. 날짜를 못 찾았으면 저장이다.
            return if (VoiceWords.isSave(text)) save(save) else VoiceReply.NotMine
        }
        update { current ->
            current.copy(
                date = date ?: current.date,
                time = time ?: current.time,
                hospital = hospital ?: current.hospital,
                hospitalSearch = hospital ?: current.hospitalSearch,
                // 말로 찾은 병원은 병원 찾기가 하나로 좁혀지면 알아서 고른다.
                hospitalSearchByVoice = hospital != null || current.hospitalSearchByVoice,
                sheet = ScheduleAddSheet.NONE,
            )
        }
        val filled = listOfNotNull(date?.format(VOICE_DATE), time?.format(VOICE_TIME)).joinToString(" ")
        return when {
            hospital != null && filled.isNotEmpty() ->
                VoiceReply.Done(R.string.voice_schedule_filled_hospital, listOf(filled, hospital))

            hospital != null -> VoiceReply.Done(R.string.voice_schedule_hospital_search, listOf(hospital))
            else -> VoiceReply.Done(R.string.voice_schedule_filled, listOf(filled))
        }
    }
}

/**
 * 말을 읽는다. 예약 문자와 같은 추출기를 쓰되 말에 맞게 고친다.
 *
 * 우리말 수로 받아쓴 시각("세 시")을 숫자로 바꿔 읽는다. **말에 날짜가 없으면 날짜를 버린다.**
 * 추출기가 "오후 3시"를 오늘 오후 3시로 돌려줄 수 있어서 그대로 쓰면 적어 둔 날짜가 오늘로 바뀐다.
 * 시각은 추출기가 못 읽어도 말에서 바로 읽는다. 예약 문자는 날짜에 붙은 시각만 받지만, 말로
 * "오후 3시로 해 줘"라고 할 때는 날짜가 따로 없다.
 */
private suspend fun AppointmentTextReader.readSpoken(text: String): SharedAppointment {
    val spoken = spokenNumbers(text)
    val read = read(spoken)
    return read.copy(date = read.date?.takeIf { mentionsDay(spoken) }, time = read.time ?: timeIn(spoken))
}

/** 적은 것이 있는지. 말로 나갈 때 버려도 되는지 물을지 정한다. */
private val ScheduleAddUiState.hasInput: Boolean
    get() = hospital != null || date != null || time != null || cards.any { it.picked } || todos.isNotEmpty()

/** 뺄 카드. 말한 카드를 먼저 찾고, 못 찾았는데 골라 둔 카드가 한 장이면 그 카드다. */
private fun List<ScheduleAddCard>.toDrop(voice: ScheduleAddVoice.CardDrop): List<ScheduleAddCard> =
    matchVoice(voice.ordinal, voice.words).ifEmpty { listOfNotNull(filter { it.picked }.singleOrNull()) }

/** 화면을 떠나는 일. 옮겨 간 화면이 물으므로 이 화면은 다음 칸을 묻지 않는다. */
private val LEAVING = setOf(ScheduleAddVoice.Exit, ScheduleAddVoice.OpenHospital, ScheduleAddVoice.CardNew)

/** 어느 것을 가리키는지 되물을 대상. */
private enum class ScheduleAddTarget { CARD, TODO }

/**
 * 맞는 것이 없거나 여럿일 때의 되물음. 여럿이면 후보를 적고 몇 번째인지 묻는다.
 */
private fun whichOne(candidates: List<String>, target: ScheduleAddTarget): VoiceReply {
    val card = target == ScheduleAddTarget.CARD
    if (candidates.isEmpty()) {
        val missing = if (card) R.string.voice_schedule_card_missing else R.string.voice_schedule_todo_missing
        return VoiceReply.Done(missing)
    }
    val which = if (card) R.string.voice_schedule_card_which else R.string.voice_schedule_todo_which
    val ask = if (card) R.string.voice_schedule_card_ask_ordinal else R.string.voice_schedule_todo_ask_ordinal
    return VoiceReply.Done(which, listOf(candidates.joinToString(" · ")), question = VoiceText(ask))
}

/**
 * 이어서 물을 것. 일정에 꼭 있어야 하는 병원과 날짜를 차례로 묻고, 다 찼으면 저장하라고 한다.
 * 시간·카드·할 일은 없어도 저장되므로 묻지 않고 마지막 안내에 함께 적는다.
 */
internal fun ScheduleAddUiState.nextQuestion(): VoiceText = when {
    hospital == null -> VoiceText(R.string.voice_ask_hospital)
    date == null -> VoiceText(R.string.voice_ask_date)
    else -> VoiceText(R.string.voice_ask_done)
}

private fun ScheduleAddUiState.withTodo(id: String, change: (ScheduleAddTodo) -> ScheduleAddTodo) =
    copy(todos = todos.map { if (it.id == id) change(it) else it })
