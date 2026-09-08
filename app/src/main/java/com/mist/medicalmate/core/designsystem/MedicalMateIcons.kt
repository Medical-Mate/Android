package com.mist.medicalmate.core.designsystem

import com.mist.medicalmate.R

/**
 * DESIGN.md 6절 아이콘 45종에 Figma가 나중에 더한 `arrow-up`을 합쳐 46종.
 *
 * 호출부가 `R.drawable`을 직접 쓰지 않게 이름을 한곳에 모았다. 이름은 Figma의
 * `Icon/{name}`을 PascalCase로 옮긴 것이다.
 *
 * 기본 크기 24, 인라인 20, 좁은 자리 18이다. 16 이하로 줄이지 않는다. 원본 색은
 * `fg/default`이고 문맥에 따라 `Icon`의 tint로 바꾼다. 라벨 없는 아이콘 버튼에는
 * 접근성 이름을 반드시 준다(DESIGN.md 9절).
 *
 * 값은 drawable 리소스 id다. `painterResource`에 그대로 넘긴다.
 *
 * drawable 파일은 Figma에서 내보낸 것이다. 손으로 고치지 말고 원본에서 다시 내보낸다.
 */
object MedicalMateIcons {
    // 음성 입력
    val Mic = R.drawable.ic_mic
    val MicListening = R.drawable.ic_mic_listening
    val MicOff = R.drawable.ic_mic_off
    val Waveform = R.drawable.ic_waveform

    // 내비게이션
    val ChevronLeft = R.drawable.ic_chevron_left
    val ChevronRight = R.drawable.ic_chevron_right
    val ChevronUp = R.drawable.ic_chevron_up
    val ChevronDown = R.drawable.ic_chevron_down
    val ArrowRight = R.drawable.ic_arrow_right
    val ArrowUp = R.drawable.ic_arrow_up
    val Close = R.drawable.ic_close
    val Plus = R.drawable.ic_plus
    val Minus = R.drawable.ic_minus
    val MoreHorizontal = R.drawable.ic_more_horizontal

    // 상태
    val Info = R.drawable.ic_info
    val Check = R.drawable.ic_check
    val CheckCircle = R.drawable.ic_check_circle
    val AlertCircle = R.drawable.ic_alert_circle
    val AlertTriangle = R.drawable.ic_alert_triangle
    val Spinner = R.drawable.ic_spinner

    // 하단 탭·구조
    val Home = R.drawable.ic_home
    val Note = R.drawable.ic_note
    val Calendar = R.drawable.ic_calendar
    val User = R.drawable.ic_user

    // 하단 탭 활성. Tab Bar 전용이며 본문·버튼에는 획형을 쓴다
    val HomeFilled = R.drawable.ic_home_filled
    val NoteFilled = R.drawable.ic_note_filled
    val CalendarFilled = R.drawable.ic_calendar_filled
    val UserFilled = R.drawable.ic_user_filled

    // 임상. 다른 도메인의 일반 아이콘처럼 재해석하지 않는다
    val Stethoscope = R.drawable.ic_stethoscope
    val Pill = R.drawable.ic_pill
    val HeartPulse = R.drawable.ic_heart_pulse
    val BodyPoint = R.drawable.ic_body_point
    val Hospital = R.drawable.ic_hospital

    // 액션
    val Edit = R.drawable.ic_edit
    val Share = R.drawable.ic_share
    val Copy = R.drawable.ic_copy
    val Trash = R.drawable.ic_trash
    val Search = R.drawable.ic_search
    val Bell = R.drawable.ic_bell
    val Clock = R.drawable.ic_clock
    val Camera = R.drawable.ic_camera
    val Chat = R.drawable.ic_chat
    val Lock = R.drawable.ic_lock

    // 빈 상태
    val EmptyBox = R.drawable.ic_empty_box
    val SearchOff = R.drawable.ic_search_off
    val WifiOff = R.drawable.ic_wifi_off
}
