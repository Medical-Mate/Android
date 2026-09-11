package com.mist.medicalmate.visit.ui

/**
 * 진료 후 기록 플로우의 픽스처. 서버·AI 연동 시 이 파일을 삭제한다.
 *
 * Figma 1m·1p·1q-1·1k의 내용을 그대로 옮겼다. Preview와 ViewModel이 함께 쓴다.
 */

/** 1m의 검색 결과 4곳. 검색어 "서울OO병원"으로 좁힌 상태다. */
internal val previewHospitals =
    listOf(
        Hospital(
            id = "hospital-1",
            name = "서울OO병원 내과",
            address = "서울 관악구 남부순환로 1820, 3층",
        ),
        Hospital(
            id = "hospital-2",
            name = "서울OO병원 이비인후과",
            address = "서울 관악구 남부순환로 1820, 4층",
        ),
        Hospital(
            id = "hospital-3",
            name = "OO이비인후과의원",
            address = "서울 관악구 봉천로 412, 2층",
        ),
        Hospital(
            id = "hospital-4",
            name = "OO정형외과의원",
            address = "서울 관악구 신림로 245, 5층",
        ),
    )

internal val previewVisitHeadline =
    VisitHeadline(
        label = "오늘 진료",
        title = "서울OO병원 내과 · 9월 12일",
        detail = "복부 통증 · 3주 브리핑 카드로 진료받았어요",
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
        caption = "AI가 메모를 4가지로 나눴어요",
    )

internal val previewVisitSummary =
    VisitSummaryUiState(
        previous =
        VisitCompareCard(
            label = "지난 진료 · 8.21",
            title = "두통 · 어지러움",
            detail = "진통제 처방 · 경과 관찰",
        ),
        current =
        VisitCompareCard(
            label = "이번 진료 · 9.12",
            title = "복부통 · 위염 초기",
            detail = "2주 약 · 9.26 재방문",
        ),
        hospital =
        HospitalSummary(
            name = "서울OO병원 내과",
            address = "서울 관악구 남부순환로 1820, 3층",
            visitDate = "09.12",
            revisitDate = "09.26",
        ),
    )
