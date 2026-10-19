package com.mist.medicalmate.card.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportVoiceTest {
    @Test
    fun `시트의 버튼과 토글을 말로 가린다`() {
        assertEquals(ExportVoice.SAVE, parseExportVoice("이미지로 저장해 줘"))
        assertEquals(ExportVoice.SHARE, parseExportVoice("카톡으로 보내 줘"))
        assertEquals(ExportVoice.MASK_ON, parseExportVoice("이름 가려 줘"))
        assertEquals(ExportVoice.MASK_ON, parseExportVoice("나이 안 보이게 해 줘"))
        assertEquals(ExportVoice.CLOSE, parseExportVoice("뒤로"))
    }

    @Test
    fun `가리지 말라는 말은 가리라는 말보다 먼저 본다`() {
        // "가리지 마"에도 "가리"가 들어 있다(#334).
        assertEquals(ExportVoice.MASK_OFF, parseExportVoice("가리지 마"))
        assertEquals(ExportVoice.MASK_OFF, parseExportVoice("안 가려도 돼"))
    }

    @Test
    fun `시트의 말이 아니면 넘긴다`() {
        assertNull(parseExportVoice("홈으로 가"))
    }
}
