package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoiceDate
import com.mist.medicalmate.core.voice.VoiceFocus
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.core.voice.parseVoiceDate
import com.mist.medicalmate.visit.data.AXIS_FOLLOW_UP
import com.mist.medicalmate.visit.data.VisitFollowUp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 진료 후 기록 확인(1q-1)에서 한 말(#288). */
internal sealed interface RecordVoice {
    data object Save : RecordVoice

    /** 재방문 날짜를 바꾼다. "재방문 11월 3일로", "다음 진료 2주 뒤 화요일". */
    data class FollowUp(val date: LocalDate) : RecordVoice

    /** 항목 값을 바꾼다. "약은 타이레놀로 바꿔 줘", "소견 고쳐, 단순 근육통". */
    data class Edit(val target: String, val ordinal: Int?, val value: String) : RecordVoice

    /** 항목을 지운다. "검사 지워 줘". */
    data class Delete(val target: String, val ordinal: Int?) : RecordVoice
}

/**
 * 1q-1의 말을 읽는다. 아니면 null이다.
 *
 * 저장은 그 말만 있으면 된다. 재방문은 "재방문"·"다음 진료"·"다시 오" 같은 말과 날짜가 함께 있어야 한다.
 * 고치기는 "고쳐"·"바꿔"·"수정", 지우기는 "지워"·"빼"·"삭제"가 있어야 한다. 무엇을 고칠지는 항목 이름이나
 * 몇 번째로 가리킨다.
 */
internal fun parseRecordVoice(text: String, today: LocalDate): RecordVoice? {
    val heard = VoiceWords.compact(text)
    val date = (parseVoiceDate(text, today) as? VoiceDate.Day)?.date
    val ordinal = VoiceWords.ordinal(text)
    val edit = EDIT.find(text)
    val asWord = AS_VALUE.find(text)
    return when {
        FOLLOW_UP.any { heard.contains(it) } && date != null -> RecordVoice.FollowUp(date)
        asWord != null -> RecordVoice.Edit(asWord.groupValues[1].trim(), ordinal, asWord.groupValues[2].trim())
        edit != null ->
            RecordVoice.Edit(text.substring(0, edit.range.first), ordinal, text.substring(edit.range.last + 1).clean())

        DELETE.containsMatchIn(text) -> RecordVoice.Delete(text, ordinal)
        VoiceWords.isSave(text) -> RecordVoice.Save
        else -> null
    }
}

/**
 * 1q-1의 말을 화면 동작으로(#288). 편집 중이면 사본을, 아니면 기록을 바로 고친다 — 말로 고친 것은 손으로
 * `편집`→고치기→`확인`을 한 것과 같다. 고친 줄로 화면이 옮겨 가고 테를 두른다.
 *
 * 저장은 확인을 받는다. 서버에 남고 재방문 일정까지 만든다.
 */
internal class VisitRecordVoiceActions(
    private val state: () -> VisitRecordUiState,
    private val update: ((VisitRecordUiState.Content) -> VisitRecordUiState.Content) -> Unit,
) {
    fun handle(text: String, today: LocalDate, save: () -> Unit): VoiceReply {
        val content = state() as? VisitRecordUiState.Content ?: return VoiceReply.NotMine
        return when (val voice = parseRecordVoice(text, today)) {
            null -> VoiceReply.NotMine
            RecordVoice.Save -> {
                // 편집 중이면 고친 것을 먼저 옮긴다. 손으로는 `확인`을 누른 뒤에야 저장하기가 선다.
                update { it.commit() }
                VoiceReply.Confirm(R.string.voice_record_confirm_save, onConfirm = save)
            }

            is RecordVoice.FollowUp -> followUp(voice.date)
            is RecordVoice.Edit -> {
                val index = content.items.indexByVoice(voice.target, voice.ordinal)
                when {
                    index == null -> VoiceReply.Done(R.string.voice_record_which, question = content.itemQuestion())
                    voice.value.isEmpty() -> VoiceReply.Done(
                        R.string.voice_record_ask_value,
                        listOf(content.items[index].key),
                    )
                    else -> {
                        update { it.changeItems { items -> items.replace(index, voice.value) }.focusing(index) }
                        VoiceReply.Done(R.string.voice_record_edited, listOf(content.items[index].key))
                    }
                }
            }

            is RecordVoice.Delete -> {
                val index = content.items.indexByVoice(voice.target, voice.ordinal)
                if (index == null) {
                    VoiceReply.Done(R.string.voice_record_which, question = content.itemQuestion())
                } else {
                    update { it.changeItems { items -> items.filterIndexed { at, _ -> at != index } } }
                    VoiceReply.Done(R.string.voice_record_deleted, listOf(content.items[index].key))
                }
            }
        }
    }

    /** 재방문 날짜를 바꾼다. 줄이 없으면 만든다. 저장할 때 이 날짜로 일정이 만들어진다. */
    private fun followUp(date: LocalDate): VoiceReply {
        val value = date.format(FOLLOW_UP_DATE)
        update { content ->
            val items = content.items
            val at = items.indexOfFirst { it.axis == AXIS_FOLLOW_UP }
            val changed =
                if (at >= 0) {
                    items.replace(at, value)
                } else {
                    items + VisitRecordItem(FOLLOW_UP_KEY, value, VisitRecordItem.Tone.LINK, AXIS_FOLLOW_UP)
                }
            content.changeItems { changed }
                .let { it.copy(record = it.record.copy(followUp = VisitFollowUp(date))) }
                .focusing(changed.indexOfFirst { it.axis == AXIS_FOLLOW_UP })
        }
        return VoiceReply.Done(R.string.voice_record_follow_up, listOf(value))
    }
}

/** 편집 중이면 사본을, 아니면 기록의 줄을 고친다. */
private fun VisitRecordUiState.Content.changeItems(
    change: (List<VisitRecordItem>) -> List<VisitRecordItem>,
): VisitRecordUiState.Content {
    val editing = draft
    return if (editing != null) {
        copy(draft = editing.copy(items = change(editing.items)))
    } else {
        copy(record = record.copy(items = change(record.items)))
    }
}

/** 사본을 기록으로 옮기고 편집을 닫는다. 편집 중이 아니면 그대로다. */
private fun VisitRecordUiState.Content.commit(): VisitRecordUiState.Content =
    draft?.let { copy(record = record.copy(items = it.items), draft = null) } ?: this

private fun VisitRecordUiState.Content.focusing(index: Int) = copy(voiceFocus = VoiceFocus(listOf(index)))

private fun List<VisitRecordItem>.replace(index: Int, value: String) =
    mapIndexed { at, item -> if (at == index) item.copy(value = value) else item }

/**
 * 말이 가리키는 줄. 몇 번째로 말했으면 그 줄이고, 아니면 항목 이름이 말 속 낱말과 같은 줄이다. "약은"의
 * "약"처럼 조사를 떼고 견준다. 하나가 아니면 null이다.
 */
private fun List<VisitRecordItem>.indexByVoice(text: String, ordinal: Int?): Int? {
    if (ordinal != null) return (if (ordinal == VoiceWords.LAST) lastIndex else ordinal).takeIf { it in indices }
    val words = text.split(' ', ',').map {
        VoiceWords.compact(it.replace(PARTICLE_TAIL, ""))
    }.filter { it.isNotEmpty() }
    val matched = indices.filter { index -> VoiceWords.compact(this[index].key) in words }
    return matched.singleOrNull()
}

/** 어느 줄인지 되물을 때 항목을 한 줄에 하나씩 적는다. */
private fun VisitRecordUiState.Content.itemQuestion(): VoiceText =
    VoiceText(R.string.voice_record_ask_which, listOf(items.joinToString("\n") { "· ${it.key}" }))

private fun String.clean(): String = trim { it.isWhitespace() || it in ".,!?~" }

/** 재방문 줄의 이름. 서버가 이름을 준 줄이 없을 때만 쓴다. */
private const val FOLLOW_UP_KEY = "재방문"

private val FOLLOW_UP_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

private val FOLLOW_UP = listOf("재방문", "다음진료", "다시오", "다시와", "재진")

/** "고쳐, …" 뒤가 새 값이다. */
private val EDIT = Regex("""(?:고쳐|바꿔|수정)(?:\s*줘|\s*주세요|할래|할게)?[\s,]*""")

/** "약은 타이레놀로 바꿔 줘". 항목 이름과 새 값이 앞에 온다. */
private val AS_VALUE = Regex("""^(.+?)(?:은|는|을|를)\s+(.+?)(?:으로|로)\s*(?:바꿔|고쳐|수정)""")

private val DELETE = Regex("""지워|빼\s*줘|없애|삭제""")

private val PARTICLE_TAIL = Regex("""(?:은|는|을|를|이|가|에|도|의)+$""")
