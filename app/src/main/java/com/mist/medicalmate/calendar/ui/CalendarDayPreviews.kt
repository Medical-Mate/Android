package com.mist.medicalmate.calendar.ui

import androidx.compose.runtime.Composable
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/**
 * 일자 화면의 Preview 모음.
 *
 * `CalendarDayScreen`에서 나눴다. 상태가 넷(진료 전 · 편집 · 다녀온 날 · 다음 일정 확정)이
 * 되면서 한 파일에 함수가 열둘이 됐고 detekt의 파일당 상한에 닿았다.
 */

@MedicalMateScreenPreviews
@Composable
private fun CalendarDayScreenPreview() {
    MedicalMateTheme {
        CalendarDayScreen(state = previewCalendarDayState, callbacks = CalendarDayCallbacks())
    }
}

/** 1r-2-A. 다녀온 날이다. 할 일이 없어지고 기록과 다음 일정이 붙는다. */
@MedicalMateScreenPreviews
@Composable
private fun CalendarDayVisitedPreview() {
    MedicalMateTheme {
        CalendarDayScreen(state = previewCalendarDayVisitedState, callbacks = CalendarDayCallbacks())
    }
}

/** 1r-2-E. 편집 상태다. 할 일 줄에 x가 붙고 하단에 일정 삭제가 선다. */
@MedicalMateScreenPreviews
@Composable
private fun CalendarDayEditingPreview() {
    MedicalMateTheme {
        CalendarDayScreen(state = previewCalendarDayEditingState, callbacks = CalendarDayCallbacks())
    }
}

/** 1r-2-A2. 다음 일정의 시간까지 정해진 상태다. */
@MedicalMateScreenPreviews
@Composable
private fun CalendarDayConfirmedPreview() {
    MedicalMateTheme {
        CalendarDayScreen(state = previewCalendarDayConfirmedState, callbacks = CalendarDayCallbacks())
    }
}
