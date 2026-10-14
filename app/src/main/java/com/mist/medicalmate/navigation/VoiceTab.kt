package com.mist.medicalmate.navigation

import com.mist.medicalmate.core.designsystem.component.MedicalMateTab
import com.mist.medicalmate.voice.ui.VoiceCommand

/**
 * 말로 연 화면이 올라설 탭(#298). 손으로 그 화면에 들어가는 탭이다.
 *
 * - 홈: 증상 정리("증상 정리 시작하기"), 브리핑 카드 목록("전체 보기"), 내 정보(머리의 아바타)
 * - 캘린더: 일정 추가(+ 버튼), 진료 후 기록(그 날의 일자 화면)
 * - 기록: 기록 탭 자신
 *
 * "뒤로"는 탭을 옮기지 않지만 갈래를 모두 채우려고 홈으로 둔다. 쓰지 않는다.
 */
internal fun voiceTabOf(command: VoiceCommand): MedicalMateTab = when (command) {
    VoiceCommand.CALENDAR, VoiceCommand.SCHEDULE_ADD, VoiceCommand.VISIT_RECORD -> MedicalMateTab.CALENDAR
    VoiceCommand.RECORD -> MedicalMateTab.RECORD
    VoiceCommand.HOME, VoiceCommand.PROFILE, VoiceCommand.INTAKE, VoiceCommand.BRIEF_CARDS, VoiceCommand.BACK ->
        MedicalMateTab.HOME
}
