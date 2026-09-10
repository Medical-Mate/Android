package com.mist.medicalmate.card.ui

/**
 * Figma 1j-3(735:3829)의 내용. Preview도 이것을 쓴다.
 *
 * 재방문 예정 → 진료 후 기록 → 브리핑 카드가 다 있는 건이다. **최신 날짜가 위로 온다.**
 *
 * 증상 정리 단계는 타임라인에 넣지 않는다. 문답에서 답한 내용은 브리핑 카드에 담기므로
 * 카드 위에 같은 값을 한 번 더 보여주는 자리가 된다.
 */
internal val previewRecordDetail =
    RecordDetail(
        id = "card-1",
        title = "복부 통증 · 3주",
        status = RecordItem.Status.CONFIRMED,
        clinicLine = "서울OO병원 내과 · 09.12 진료",
        steps =
        listOf(
            RecordStep.Pending(
                at = "09.26 예정",
                message = "다음 진료가 예약돼 있어요",
                detail = "9월 26일 (토) 오전 10:30",
            ),
            RecordStep.Block(
                at = "09.12 · 진료 후 기록",
                title = "진료 후 기록",
                items =
                listOf(
                    RecordDetailItem(key = "소견", value = "위염 초기 소견"),
                    RecordDetailItem(key = "검사", value = "혈액검사 시행 · 다음 방문 때 확인"),
                    RecordDetailItem(key = "약", value = "2주분 처방 · 커피·매운 음식 줄이기"),
                    RecordDetailItem(
                        key = "재방문",
                        value = "2주 뒤 (9월 26일 전후)",
                        tone = RecordDetailItem.Tone.LINK,
                    ),
                ),
            ),
            RecordStep.Block(
                at = "09.04 작성 · 09.12 진료실에서 보여줌",
                title = "브리핑 카드",
                items =
                listOf(
                    RecordDetailItem(key = "복용약", value = "혈압약 · 진통제(증상 시)"),
                    RecordDetailItem(key = "알러지", value = "페니실린", tone = RecordDetailItem.Tone.WARNING),
                    RecordDetailItem(key = "질문", value = "검사를 받아야 하나요? 외 2개"),
                ),
                action = RecordStepAction(label = "카드 전체 보기", target = RecordStepAction.Target.BRIEF_CARD),
            ),
        ),
    )

/** 카드까지 만들었고 아직 진료를 받지 않은 건. 진료 후 기록 자리가 예정으로 남아 맨 위에 온다. */
private val beforeVisitRecordDetail =
    RecordDetail(
        id = "card-2",
        title = "두통 · 잦은 어지러움",
        status = RecordItem.Status.BEFORE_VISIT,
        clinicLine = "08.21 작성 · 병원 미정",
        steps =
        listOf(
            RecordStep.Pending(at = "진료 예정", message = "병원을 정하면 진료 일정이 여기에 표시돼요"),
            RecordStep.Block(
                at = "08.21 작성",
                title = "브리핑 카드",
                items =
                listOf(
                    RecordDetailItem(key = "복용약", value = "혈압약"),
                    RecordDetailItem(key = "알러지", value = "페니실린", tone = RecordDetailItem.Tone.WARNING),
                    RecordDetailItem(key = "질문", value = "혈압약과 관련이 있나요? 외 2개"),
                ),
                action = RecordStepAction(label = "카드 전체 보기", target = RecordStepAction.Target.BRIEF_CARD),
            ),
        ),
    )

/**
 * 문답을 하다 멈춘 건. 카드가 없으니 열 것도 없다.
 *
 * 타임라인에 예정 한 단계만 남는다. 증상 정리를 넣지 않으므로 아직 아무것도 만들어지지
 * 않은 상태이고, 그 사실을 그 한 줄이 말한다.
 */
private val draftRecordDetail =
    RecordDetail(
        id = "card-3",
        title = "무릎 통증",
        status = RecordItem.Status.DRAFT,
        clinicLine = "오늘 작성 중 · 4단계 중 2단계",
        steps =
        listOf(
            RecordStep.Pending(at = "카드 예정", message = "증상 정리를 마치면 브리핑 카드가 만들어져요"),
        ),
    )

/** 재방문 없이 끝난 건. 예정 단계가 없어 타임라인이 진료 후 기록에서 끝난다. */
private val closedRecordDetail =
    RecordDetail(
        id = "card-0",
        title = "목 통증 · 삼킬 때 아픔",
        status = RecordItem.Status.CONFIRMED,
        clinicLine = "OO이비인후과 · 07.18 진료",
        steps =
        listOf(
            RecordStep.Block(
                at = "07.18 · 진료 후 기록",
                title = "진료 후 기록",
                items =
                listOf(
                    RecordDetailItem(key = "소견", value = "인후염"),
                    RecordDetailItem(key = "약", value = "5일분 처방"),
                    RecordDetailItem(key = "재방문", value = "없음 · 안 나으면 다시 오기"),
                ),
            ),
            RecordStep.Block(
                at = "07.17 작성 · 07.18 진료실에서 보여줌",
                title = "브리핑 카드",
                items =
                listOf(
                    RecordDetailItem(key = "복용약", value = "없음"),
                    RecordDetailItem(key = "알러지", value = "페니실린", tone = RecordDetailItem.Tone.WARNING),
                    RecordDetailItem(key = "질문", value = "며칠이면 나아요? 외 1개"),
                ),
                action = RecordStepAction(label = "카드 전체 보기", target = RecordStepAction.Target.BRIEF_CARD),
            ),
        ),
    )

/**
 * 기록 상세 픽스처. 서버 연동 시 이 파일을 삭제한다.
 *
 * 목록(1j-1)의 네 건에 각각 대응한다. 목록의 모든 줄이 눌리므로 상세도 네 건이 다 있어야
 * 한다. 상태에 따라 타임라인의 길이가 달라지는 것도 여기서 드러난다. 작성 중인 건은 예정
 * 한 단계뿐이고, 재방문이 없는 건은 예정 단계가 없다.
 *
 * 네 건 모두 **최신 날짜가 위**다.
 *
 * 위의 픽스처들을 참조하므로 파일 끝에 둔다. 최상위 프로퍼티는 선언 순서대로 초기화된다.
 */
internal val recordDetailFixtures: Map<String, RecordDetail> =
    listOf(
        previewRecordDetail,
        beforeVisitRecordDetail,
        draftRecordDetail,
        closedRecordDetail,
    ).associateBy { it.id }
