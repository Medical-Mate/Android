package com.mist.medicalmate.core.voice

import com.mist.medicalmate.R

/**
 * 말로 한 저장의 결과(#350). 확인의 "저장하고 있어요"([VoiceReply.Confirm.confirmed]) 뒤에 화면이 서버의 답을 받고
 * 알린다. 막대가 닫혀 있으면 버려지므로 손으로 누른 저장에도 같이 걸어 둔다.
 */
object VoiceSave {
    val saved = VoicePrompt(message = VoiceText(R.string.voice_saved))

    /** 다시 하면 될 수 있는 실패. */
    val failed =
        VoicePrompt(message = VoiceText(R.string.voice_save_failed), question = VoiceText(R.string.voice_save_retry))
}
