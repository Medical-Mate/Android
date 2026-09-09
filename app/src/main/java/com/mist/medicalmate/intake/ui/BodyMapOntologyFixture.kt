package com.mist.medicalmate.intake.ui

/**
 * 부위 온톨로지 픽스처. 서버 연동 시 이 파일을 삭제한다.
 *
 * `docs/examples/body-map.json`의 응답 예시를 그대로 옮겼다. 스냅샷
 * `3cc57491f020`이고 앵커 9개 · 구역 25개다. 좌표와 이미지는 앱 에셋이라
 * [bodyMapAnchors]에 남고, 여기에는 이름과 진료과만 둔다.
 *
 * 진료과가 빈 구역이 있다. 앵커의 값을 쓰라는 뜻이고 그 규칙은
 * [BodyMapSelection.departments]에 있다.
 */
internal val bodyMapOntology: Map<String, BodyMapLabel> =
    mapOf(
        "ANC:001" to BodyMapLabel("머리", listOf("신경과", "가정의학과")),
        "SUR:001" to BodyMapLabel("머리 전체·이마", listOf("신경과", "가정의학과")),
        "SUR:002" to BodyMapLabel("눈", listOf("안과")),
        "SUR:003" to BodyMapLabel("귀", listOf("이비인후과")),
        "SUR:004" to BodyMapLabel("코", listOf("이비인후과")),
        "SUR:005" to BodyMapLabel("입", listOf("이비인후과")),
        "ANC:002" to BodyMapLabel("목", emptyList()),
        "SUR:011" to BodyMapLabel("목 안(목구멍)", listOf("이비인후과", "내과")),
        "SUR:012" to BodyMapLabel("목 뒤·옆", listOf("정형외과", "신경외과")),
        "ANC:003" to BodyMapLabel("가슴", listOf("내과", "심장내과", "호흡기내과")),
        "SUR:021" to BodyMapLabel("가슴 가운데", listOf("내과", "심장내과", "호흡기내과")),
        "SUR:022" to BodyMapLabel("가슴 옆(갈비)", listOf("내과", "정형외과")),
        "ANC:004" to BodyMapLabel("배", listOf("내과", "소화기내과")),
        "SUR:031" to BodyMapLabel("윗배(명치)", listOf("내과", "소화기내과")),
        "SUR:032" to BodyMapLabel("아랫배", listOf("내과", "소화기내과", "산부인과", "비뇨의학과")),
        "ANC:013" to BodyMapLabel("팔", listOf("정형외과")),
        "SUR:051" to BodyMapLabel("어깨", emptyList()),
        "SUR:055" to BodyMapLabel("위팔", emptyList()),
        "SUR:061" to BodyMapLabel("팔꿈치", emptyList()),
        "SUR:065" to BodyMapLabel("아래팔", emptyList()),
        "SUR:071" to BodyMapLabel("손목", emptyList()),
        "SUR:072" to BodyMapLabel("손", emptyList()),
        "ANC:014" to BodyMapLabel("다리", listOf("정형외과")),
        "SUR:090" to BodyMapLabel("허벅지", emptyList()),
        "SUR:091" to BodyMapLabel("무릎", emptyList()),
        "SUR:097" to BodyMapLabel("종아리", emptyList()),
        "SUR:101" to BodyMapLabel("발목", emptyList()),
        "SUR:102" to BodyMapLabel("발", emptyList()),
        "ANC:012" to BodyMapLabel("허리·엉덩이", listOf("정형외과", "신경외과")),
        "SUR:041" to BodyMapLabel("허리 가운데", emptyList()),
        "SUR:042" to BodyMapLabel("허리 옆", listOf("내과", "비뇨의학과", "정형외과")),
        "SUR:081" to BodyMapLabel("엉덩이", emptyList()),
        "ANC:010" to BodyMapLabel("전신", listOf("내과", "가정의학과")),
        "ANC:011" to BodyMapLabel("피부", listOf("피부과", "내과")),
    )
