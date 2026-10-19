package com.mist.medicalmate.profile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthEditVoiceTest {
    private val options =
        mapOf(
            ProfileSetupStep.MEDICATIONS to listOf("혈압약", "당뇨약", "진통제"),
            ProfileSetupStep.CONDITIONS to listOf("고혈압", "당뇨", "위염·역류"),
            ProfileSetupStep.ALLERGIES to listOf("페니실린", "아스피린", "땅콩"),
        )

    @Test
    fun `칩 이름으로 고르고 푼다`() {
        assertEquals(HealthEditVoice.Choose(ProfileSetupStep.CONDITIONS, "고혈압"), parse("고혈압 체크해 줘"))
        assertEquals(HealthEditVoice.Remove(ProfileSetupStep.CONDITIONS, "고혈압"), parse("고혈압 빼 줘"))
        assertEquals(HealthEditVoice.Choose(ProfileSetupStep.MEDICATIONS, "당뇨약"), parse("당뇨약 먹어"))
        assertEquals(HealthEditVoice.Choose(ProfileSetupStep.CONDITIONS, "당뇨"), parse("당뇨 있어"))
    }

    @Test
    fun `둘을 묶은 칩은 한쪽만 말해도 된다`() {
        assertEquals(HealthEditVoice.Choose(ProfileSetupStep.CONDITIONS, "위염·역류"), parse("역류 있어"))
    }

    @Test
    fun `갈래를 말하면 그 갈래의 칩만 보고 없으면 새로 넣는다`() {
        // 아스피린은 알러지 칩이지만 먹는 약으로 말했으면 약에 넣는다(#334).
        assertEquals(HealthEditVoice.Add(ProfileSetupStep.MEDICATIONS, "아스피린"), parse("먹는 약에 아스피린 넣어 줘"))
        assertEquals(HealthEditVoice.Add(ProfileSetupStep.ALLERGIES, "복숭아"), parse("알러지에 복숭아 추가해 줘"))
        assertEquals(HealthEditVoice.Choose(ProfileSetupStep.ALLERGIES, "아스피린"), parse("아스피린 알러지 있어"))
    }

    @Test
    fun `저장은 칩과 넣기를 본 뒤에 본다`() {
        // 공용 저장 말에 "추가해 줘"가 들어 있다. 앞에서 보면 "고혈압 추가해 줘"가 저장이 된다.
        assertEquals(HealthEditVoice.Choose(ProfileSetupStep.CONDITIONS, "고혈압"), parse("고혈압 추가해 줘"))
        assertEquals(HealthEditVoice.Save, parse("저장해 줘"))
        assertNull(parse("홈으로 가"))
    }

    private fun parse(text: String) = parseHealthEditVoice(text, options)
}
