package com.mist.medicalmate.core.designsystem

/**
 * 남은 날을 배지 글로(#294). 0일은 "D-day", 그 밖은 "D-3"이다.
 *
 * 오늘 일정을 "D-0"으로 적으면 숫자가 빠진 것처럼 읽힌다. 캘린더의 그 날 일정 · 일자 화면 · 다음 일정 칩 ·
 * 홈의 다가오는 일정이 모두 이 글을 쓴다. 번역할 말이 아니라 표기라서 리소스에 두지 않는다 — ViewModel도
 * 같은 글을 만든다.
 */
fun ddayLabel(days: Long): String = if (days == 0L) "D-day" else "D-$days"
