package com.mist.medicalmate.card.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity

/**
 * Figma 1j-3(735:3829)의 내용. Preview도 이것을 쓴다.
 *
 * 재방문 예정 → 진료 후 기록 → 브리핑 카드가 다 있는 건이다. **최신 날짜가 위로 온다.**
 *
 * 증상 정리 단계는 타임라인에 넣지 않는다. 문답에서 답한 내용은 브리핑 카드에 담기므로
 * 카드 위에 같은 값을 한 번 더 보여주는 자리가 된다.
 */
/**
 * 접혀 있을 때 보이는 줄 수. 시안이 부위·기간·양상 셋을 남긴다.
 */
private const val COLLAPSED_CARD_ITEMS = 3

/** 1j-3의 카드 다섯 줄. 시안 `1038:2768`이 펼쳤을 때 보여주는 그대로다. */
private val abdomenCardItems =
    listOf(
        RecordDetailItem(key = "부위", value = "복부 (명치 아래 · 배꼽 위)"),
        RecordDetailItem(key = "기간", value = "3주 전 시작 · 최근 악화", tone = RecordDetailItem.Tone.LINK),
        RecordDetailItem(key = "양상", value = "식후 30분 뒤 쓰림 · 밤에 심해짐"),
        RecordDetailItem(key = "복용약", value = "혈압약 · 진통제(증상 시)"),
        RecordDetailItem(key = "기저질환", value = "고혈압"),
    )

private val abdomenCard =
    RecordStepCard(
        collapsedItemCount = COLLAPSED_CARD_ITEMS,
        severity = MedicalMateSeverity.LEVEL_3,
        allergies = listOf("페니실린"),
        questions =
        listOf(
            "검사를 받아야 하나요?",
            "지금 진통제 계속 먹어도 되나요?",
            "어떤 증상이면 바로 다시 와야 하나요?",
        ),
    )

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
                items = abdomenCardItems,
                card = abdomenCard,
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
                    RecordDetailItem(key = "부위", value = "머리 (관자놀이 양쪽)"),
                    RecordDetailItem(key = "기간", value = "2주 전 시작 · 잦아짐"),
                    RecordDetailItem(key = "양상", value = "일어설 때 핑 돌고 욱신거림"),
                    RecordDetailItem(key = "복용약", value = "혈압약"),
                    RecordDetailItem(key = "기저질환", value = "고혈압"),
                ),
                card =
                RecordStepCard(
                    collapsedItemCount = COLLAPSED_CARD_ITEMS,
                    severity = MedicalMateSeverity.LEVEL_2,
                    allergies = listOf("페니실린"),
                    questions =
                    listOf(
                        "혈압약과 관련이 있나요?",
                        "검사를 받아야 하나요?",
                        "어떤 증상이면 바로 다시 와야 하나요?",
                    ),
                ),
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

/**
 * 재방문까지 다녀온 건. Figma 1j-3-R `1039:2799`.
 *
 * 진료 후 기록이 두 개 쌓이고 예정 단계가 없어진다. 배지가 "진료 완료"가 아니라 "진료
 * 2회"이고 병원 줄에 초진과 재방문 날짜가 함께 온다.
 *
 * 타임라인은 다른 건과 같이 최신이 위다. 재방문 기록이 맨 위, 초진 기록이 가운데,
 * 카드가 맨 아래다.
 */
private val revisitedRecordDetail =
    RecordDetail(
        id = "card-4",
        title = "복부 통증 · 3주",
        status = RecordItem.Status.CONFIRMED,
        badge = "진료 2회",
        clinicLine = "서울OO병원 내과 · 09.12 초진 · 09.26 재방문",
        steps =
        listOf(
            RecordStep.Block(
                at = "09.26 · 진료 후 기록 · 재방문",
                title = "진료 후 기록",
                items =
                listOf(
                    RecordDetailItem(key = "소견", value = "염증 호전 · 경과 양호"),
                    RecordDetailItem(key = "검사", value = "혈액검사 정상 범위"),
                    RecordDetailItem(key = "약", value = "1주분 추가 처방"),
                    RecordDetailItem(key = "재방문", value = "증상 재발 시에만"),
                ),
            ),
            RecordStep.Block(
                at = "09.12 · 진료 후 기록 · 초진",
                title = "진료 후 기록",
                items =
                listOf(
                    RecordDetailItem(key = "소견", value = "위염 초기 소견"),
                    RecordDetailItem(key = "검사", value = "혈액검사 시행 · 다음 방문 때 확인"),
                    RecordDetailItem(key = "약", value = "2주분 처방\n커피·매운 음식 줄이기"),
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
                items = abdomenCardItems,
                card = abdomenCard,
            ),
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
                    RecordDetailItem(key = "부위", value = "목 (삼킬 때 안쪽)"),
                    RecordDetailItem(key = "기간", value = "3일 전 시작"),
                    RecordDetailItem(key = "양상", value = "삼킬 때 찌르듯 아픔"),
                    RecordDetailItem(key = "복용약", value = "없음"),
                    RecordDetailItem(key = "기저질환", value = "고혈압"),
                ),
                card =
                RecordStepCard(
                    collapsedItemCount = COLLAPSED_CARD_ITEMS,
                    severity = MedicalMateSeverity.LEVEL_2,
                    allergies = listOf("페니실린"),
                    questions = listOf("며칠이면 나아요?", "항생제를 꼭 먹어야 하나요?"),
                ),
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
 * 재방문까지 다녀온 건(`card-4`)은 목록에 대응하는 줄이 없다. 1j-3-R을 화면에서 확인할
 * 길이 있어야 해서 넣었다. 목록에 넣지 않은 이유는 1j-1의 시안이 네 건이기 때문이다.
 *
 * 다섯 건 모두 **최신 날짜가 위**다.
 *
 * 위의 픽스처들을 참조하므로 파일 끝에 둔다. 최상위 프로퍼티는 선언 순서대로 초기화된다.
 */
internal val recordDetailFixtures: Map<String, RecordDetail> =
    listOf(
        previewRecordDetail,
        beforeVisitRecordDetail,
        draftRecordDetail,
        closedRecordDetail,
        revisitedRecordDetail,
    ).associateBy { it.id }
