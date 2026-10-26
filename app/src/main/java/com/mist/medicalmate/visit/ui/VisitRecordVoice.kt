package com.mist.medicalmate.visit.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoicePrompt
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceSave
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords

/** 진료 후 기록 확인(1q-1)에서 한 말(#288). */
internal enum class RecordVoice {
    SAVE,

    /** 항목을 고치거나 지우거나 재방문 날짜를 바꾸라는 말. 말로는 하지 않는다(#296). */
    BY_HAND,
}

/**
 * 1q-1의 말을 읽는다. 아니면 null이다.
 *
 * 이 화면은 AI가 나눈 것을 확인하고 저장하는 자리다. 고치는 것은 `편집`으로 들어가 손으로 한다.
 * 고치라는 말("바꿔"·"고쳐"·"수정"·"지워"·"빼"·"삭제")을 알아듣는 것은 하지 않는다고 알리기 위해서다 —
 * 그냥 넘기면 "저장" 같은 다른 말로 읽히거나 못 알아들었다고 한다.
 */
internal fun parseRecordVoice(text: String): RecordVoice? = when {
    BY_HAND.containsMatchIn(text) -> RecordVoice.BY_HAND
    VoiceWords.isSave(text) -> RecordVoice.SAVE
    else -> null
}

/**
 * 1q-1의 말을 화면 동작으로(#288). 저장만 한다. 저장은 서버에 남고 재방문 일정까지 만들어서 확인을 받는다.
 *
 * **말로는 고치거나 지우지 않는다**(#296). 처음에는 편집 상태가 아닌데도 말로 항목을 바꾸고 지울 수 있었다.
 * 편집 중이면 저장하기 버튼도 없는 자리라 받지 않는다.
 */
internal class VisitRecordVoiceActions(private val state: () -> VisitRecordUiState) {
    fun handle(text: String, save: () -> Unit): VoiceReply {
        val content = state() as? VisitRecordUiState.Content
        return when (parseRecordVoice(text)) {
            null -> VoiceReply.NotMine
            RecordVoice.BY_HAND -> VoiceReply.Done(R.string.voice_by_hand)
            RecordVoice.SAVE ->
                if (content == null) {
                    // 불러오는 중이거나 못 불러왔다. 고치라는 말이 아니라 "화면에서 직접"이 틀린 답이었다(#320).
                    VoiceReply.Done(R.string.voice_record_not_ready)
                } else if (content.editing) {
                    VoiceReply.Done(R.string.voice_by_hand)
                } else {
                    VoiceReply.Confirm(
                        R.string.voice_record_confirm_save,
                        confirmed = R.string.voice_saving,
                        onConfirm = save,
                    )
                }
        }
    }
}

/** 고치라는 말. "편집할래"도 그렇다(#320) — 화면이 "편집해 주세요"라고 적는데 그 말을 못 알아들었다. */
private val BY_HAND = Regex("""바꿔|바꿀래|고쳐|고칠래|수정|지워|지울래|빼\s*줘|없애|삭제|편집""")

/** 저장이 실패했을 때 막대에 거는 말(#350). 다시 해도 안 되는 실패는 다시 하라고 하지 않는다. */
internal fun VisitSaveFailure.voicePrompt(): VoicePrompt = when (this) {
    VisitSaveFailure.RETRYABLE -> VoiceSave.failed
    VisitSaveFailure.REJECTED ->
        VoicePrompt(message = VoiceText(R.string.voice_save_failed), question = VoiceText(R.string.voice_save_rejected))
}
