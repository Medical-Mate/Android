package com.mist.medicalmate.voice.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** 막대가 묻는 말의 줄바꿈(#284). */
class VoicePanelTextTest {
    @Test
    fun `문장마다 줄을 바꾼다`() {
        assertEquals("어디가 아프세요?\n이 중에서 말씀해 주세요", "어디가 아프세요? 이 중에서 말씀해 주세요".sentencePerLine())
        assertEquals(
            "다 됐으면 \"저장\"이라고 해 주세요.\n시간도 말로 바꿀 수 있어요",
            "다 됐으면 \"저장\"이라고 해 주세요. 시간도 말로 바꿀 수 있어요".sentencePerLine(),
        )
        assertEquals("언제 가세요?\n\"10월 20일\"처럼요", "언제 가세요? \"10월 20일\"처럼요".sentencePerLine())
    }

    @Test
    fun `문장 안의 띄어쓰기와 이미 바꾼 줄은 그대로 둔다`() {
        assertEquals("말로 답해 주세요", "말로 답해 주세요".sentencePerLine())
        assertEquals("고르세요\n· 어깨\n· 손", "고르세요\n· 어깨\n· 손".sentencePerLine())
        assertEquals("3.5점", "3.5점".sentencePerLine())
    }
}
