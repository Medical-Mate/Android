package com.mist.medicalmate.visit.ui

import java.time.LocalDate

/**
 * 진료 후 기록 플로우의 픽스처. 서버·AI 연동 시 이 파일을 삭제한다.
 *
 * Figma 1m·1p·1q-1의 내용을 그대로 옮겼다. Preview와 ViewModel이 함께 쓴다.
 */

internal val previewVisitHeadline =
    VisitHeadline(
        visitedOn = LocalDate.of(2026, 9, 12),
        clinic = "서울OO병원 내과",
        cardTitle = "복부 통증 · 3주",
    )

/** 1p에 적힌 메모. 1q-1의 원문 인용과 같은 문장이어야 흐름이 이어진다. */
internal const val PREVIEW_VISIT_NOTE =
    "위염 초기라고 하셨고, 2주 약 먹고 다시 오라고 했어요. 커피랑 매운 거 줄이라고. " +
        "피검사는 다음에 결과 보자고 하셨음."

internal val previewVisitRecord =
    VisitRecord(
        id = "visit-1",
        clinic = "서울OO병원 내과",
        clinicLine = "서울OO병원 내과 · 2026.09.12",
        items =
        listOf(
            VisitRecordItem(key = "소견", value = "위염 초기 소견"),
            VisitRecordItem(key = "검사", value = "혈액검사 시행\n결과는 다음 방문 때 확인"),
            VisitRecordItem(key = "약", value = "2주분 처방\n커피·매운 음식 줄이기"),
            VisitRecordItem(
                key = "재방문",
                value = "2주 뒤 재방문 (9월 26일 전후)",
                tone = VisitRecordItem.Tone.LINK,
            ),
        ),
        memo = PREVIEW_VISIT_NOTE,
        classifiedCount = 4,
    )
