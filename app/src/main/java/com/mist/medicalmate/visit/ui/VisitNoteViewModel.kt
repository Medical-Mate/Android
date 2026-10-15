package com.mist.medicalmate.visit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import com.mist.medicalmate.core.speech.Dictation
import com.mist.medicalmate.core.speech.SpeechToText
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceText
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/**
 * 진료 후 메모 상태 보유자.
 *
 * 맨 위 카드가 **어떤 진료를 적는 것인지**를 말한다. 병원은 바로 앞 화면(1m)에서 고른 곳,
 * 카드 제목은 이 기록이 붙을 카드다. 둘 다 라우트를 타고 따라오므로 서버를 다시 부르지
 * 않는다.
 *
 * **메모를 나누는 일은 여기서 하지 않는다**(#183). 하단 저장하기가 1q-1로 보내고, `POST /api/visits/classify`는 결과를 그리는 그 화면이 부른다. 축 맵을 라우트에
 * 실어 나르지 않아도 된다.
 *
 * 원문을 지우지 않는다. 1q-1이 원문을 그대로 보여주므로 흐름 끝까지 남아 있어야 한다.
 */
@HiltViewModel
class VisitNoteViewModel
@Inject
constructor(private val clock: Clock, private val speech: SpeechToText) : ViewModel() {
    private val mutableUiState = MutableStateFlow(VisitNoteUiState(visit = VisitHeadline(LocalDate.now(clock))))
    val uiState: StateFlow<VisitNoteUiState> = mutableUiState.asStateFlow()

    /**
     * 무엇을 적는 진료인지 채운다.
     *
     * 적던 글은 건드리지 않는다. 화면이 다시 조합돼 이 호출이 한 번 더 오더라도 그 글은
     * 남아야 한다.
     */
    fun load(clinic: String?, cardTitle: String?, visitedOn: LocalDate?) {
        val today = LocalDate.now(clock)
        mutableUiState.update {
            it.copy(
                // 흐름이 시작된 캘린더 일자다. 없으면 오늘로 둔다 — 그 경로가 지금은 없지만
                // 라우트가 선택값이라 열려 있다.
                visit =
                VisitHeadline(
                    visitedOn = visitedOn ?: today,
                    clinic = clinic,
                    cardTitle = cardTitle,
                    today = (visitedOn ?: today) == today,
                ),
            )
        }
    }

    fun onNoteChange(note: String) {
        mutableUiState.update { it.copy(note = note) }
    }

    /** 말로 적은 것이 시작한 자리. "방금 거 지워"가 여기까지 되돌린다. */
    private var spokenFrom: Int? = null

    /**
     * "헤이 메이트" 다음의 말(#288). **막대가 메모를 받아 적는다.** 짧은 명령("다 했어", "방금 거 지워",
     * "다 지워", "뒤로") 말고는 모두 메모 뒤에 이어 붙인다. 다 했으면 적은 원문을 들고 1q-1로 간다.
     */
    internal fun onVoiceNote(text: String, save: (String) -> Unit): VoiceReply {
        val note = mutableUiState.value.note
        val more = VoiceText(R.string.voice_note_ask_more)
        val from = spokenFrom
        return when (parseNoteVoice(text)) {
            NoteVoice.BACK -> VoiceReply.NotMine
            NoteVoice.DONE ->
                if (note.isBlank()) {
                    VoiceReply.Done(R.string.voice_note_empty, question = VoiceText(R.string.voice_note_ask))
                } else {
                    save(note)
                    VoiceReply.Done(R.string.voice_note_done)
                }

            NoteVoice.UNDO ->
                if (from == null) {
                    VoiceReply.Done(R.string.voice_note_nothing, question = more)
                } else {
                    spokenFrom = null
                    onNoteChange(note.take(from).trimEnd())
                    VoiceReply.Done(R.string.voice_note_undone, question = more)
                }

            NoteVoice.CLEAR ->
                if (note.isBlank()) {
                    VoiceReply.Done(R.string.voice_note_nothing, question = more)
                } else {
                    VoiceReply.Confirm(R.string.voice_note_confirm_clear) {
                        spokenFrom = null
                        onNoteChange("")
                    }
                }

            null -> {
                spokenFrom = note.length
                onNoteChange(if (note.isBlank()) text.trim() else note.trimEnd() + " " + text.trim())
                VoiceReply.Done(R.string.voice_note_written, question = more)
            }
        }
    }

    /**
     * 받아쓰기.
     *
     * 받아쓴 글은 적던 메모 뒤에 붙는다. 말하는 중에 부분 결과가 계속 갱신되므로 누를 때의
     * 글을 기준으로 매번 새로 붙인다.
     */
    private val dictation =
        Dictation(
            speech = speech,
            scope = viewModelScope,
            onVoice = { voice -> mutableUiState.update { it.copy(voice = voice) } },
            onDictated = { note, _ ->
                mutableUiState.update { it.copy(note = note) }
                closeAfterSilence()
            },
        )

    /** 조용한 시간을 세는 일(#318). 받아쓴 글이 들어올 때마다 처음부터 다시 센다. */
    private var silence: Job? = null

    /**
     * **5초 동안 아무 말이 없으면 패널을 닫는다**(#318, 사용자 결정 — 증상 문답과 같다). 켜 둔 채 잊은 마이크가 계속
     * 듣지 않게 한다. 받아쓴 글은 메모에 남는다.
     */
    private fun closeAfterSilence() {
        silence?.cancel()
        silence =
            viewModelScope.launch {
                delay(SILENCE_CLOSE_MS)
                onTypeInsteadClick()
            }
    }

    /**
     * 우하단 마이크. 권한이 확인된 뒤에 불린다.
     *
     * 패널을 열면서 **바로 듣기 시작한다.** 누른 사람은 패널이 뜨자마자 말한다. 한 번 더
     * 눌러야 듣기 시작하면 그 사이에 한 말이 사라진다.
     */
    fun onVoiceClick() {
        mutableUiState.update { it.copy(voice = MedicalMateVoiceState.IDLE) }
        // 닫기를 먼저 건다. 듣자마자 말이 오면 그 말이 다시 센다.
        closeAfterSilence()
        dictation.start(mutableUiState.value.note)
    }

    /** 마이크 권한을 거부했다. */
    fun onMicDenied() {
        dictation.onDenied()
    }

    /** 이 기기에서 음성을 쓸 수 있는지. 쓸 수 없으면 마이크를 그리지 않는다. */
    fun checkVoice() {
        viewModelScope.launch {
            val usable = speech.available()
            mutableUiState.update { it.copy(voiceAvailable = usable) }
        }
    }

    /**
     * 화면이 가려졌다(#304). 다음 화면으로 갔거나 앱이 뒤로 갔다. 듣던 것을 멈춘다.
     *
     * 다음 화면으로 가도 이 ViewModel은 백스택에 남아 받아쓰기가 이어졌고, 그동안 호출어 감지기가 마이크를
     * 내주고 쉬어서 "헤이 메이트"가 듣히지 않았다. 패널은 그대로 두고, 돌아와 마이크를 누르면 다시 듣는다.
     */
    fun onScreenHidden() {
        silence?.cancel()
        dictation.stop()
    }

    /** 음성 패널의 "직접 입력할게요". 패널을 닫고 적던 글로 돌아간다. */
    fun onTypeInsteadClick() {
        silence?.cancel()
        dictation.stop()
        mutableUiState.update { it.copy(voice = null) }
    }
}

/** 아무 말이 없을 때 패널을 닫기까지 기다리는 시간(#318, 사용자 결정). */
private const val SILENCE_CLOSE_MS = 5_000L
