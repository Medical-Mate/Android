package com.mist.medicalmate.calendar.ui

import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceWords
import kotlin.reflect.KClass

/**
 * 되물은 후보(#346). 카드나 할 일이 여럿 걸려 "몇 번째인가요?"라고 물었으면, 다음 말의 번호를 **그 후보 안에서**
 * 센다. 전에는 전체 목록에서 세어 "두 번째 카드"가 후보가 아니던 카드를 골랐고, 번호만 답하면 알아듣지 못했다.
 *
 * 물을 때 후보 id와, 고른 후보로 할 일을 함께 맡긴다. 다음 말이 번호뿐이거나 같은 종류의 말에 번호만 붙었으면
 * 그 후보로 그 일을 한다. 다른 말이면 후보를 버리고 그 말을 그대로 처리한다. 한 번 답하면 버린다.
 */
internal class VoiceCandidates {
    private var ids: List<String> = emptyList()
    private var kind: KClass<*>? = null
    private var act: ((String) -> VoiceReply)? = null

    /** 되물었다. [kind]는 물음을 부른 말의 종류다 — 같은 종류의 말로 답해도 후보 안에서 센다. */
    fun ask(ids: List<String>, kind: KClass<*>, act: (String) -> VoiceReply) {
        this.ids = ids
        this.kind = kind
        this.act = act
    }

    /**
     * 되물은 뒤의 답이면 그 후보로 한 일을, 아니면 null을 돌려준다. [parsed]는 이 말을 화면이 읽은 것이다 — 아무것도
     * 아니거나 칸을 채우는 말이면 번호만 답한 것으로 본다. 범위 밖 번호면 [missing]이다.
     */
    fun answer(text: String, parsed: Any?, missing: VoiceReply): VoiceReply? {
        val run = act
        val pool = ids
        val expected = kind
        act = null
        ids = emptyList()
        kind = null
        val ordinal = VoiceWords.ordinal(text)
        val answering = parsed == null || parsed is ScheduleAddVoice.Fill || (expected?.isInstance(parsed) == true)
        if (run == null || ordinal == null || !answering) return null
        val id = if (ordinal == VoiceWords.LAST) pool.lastOrNull() else pool.getOrNull(ordinal)
        return id?.let(run) ?: missing
    }
}
