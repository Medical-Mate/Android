package com.mist.medicalmate.intake.ui

/**
 * 부위 이름 픽스처. 서버 연동 시 이 파일을 삭제한다.
 *
 * `docs/examples/body-map.json`의 응답 예시에서 이름만 옮겼다. 스냅샷
 * `3cc57491f020`이고 앵커 9개 · 구역 25개다. 좌표와 이미지는 앱 에셋이라
 * [bodyMapAnchors]에 남는다.
 *
 * 응답에는 부위별 진료과도 함께 온다. 어느 과로 가야 하는지 안내하는 화면이 없어서
 * 여기 담지 않았다. 그 안내를 넣게 되면 응답에서 다시 가져온다.
 */
internal val bodyMapOntology: Map<String, String> =
    mapOf(
        "ANC:001" to "머리",
        "SUR:001" to "머리 전체·이마",
        "SUR:002" to "눈",
        "SUR:003" to "귀",
        "SUR:004" to "코",
        "SUR:005" to "입",
        "ANC:002" to "목",
        "SUR:011" to "목 안(목구멍)",
        "SUR:012" to "목 뒤·옆",
        "ANC:003" to "가슴",
        "SUR:021" to "가슴 가운데",
        "SUR:022" to "가슴 옆(갈비)",
        "ANC:004" to "배",
        "SUR:031" to "윗배(명치)",
        "SUR:032" to "아랫배",
        "ANC:013" to "팔",
        "SUR:051" to "어깨",
        "SUR:055" to "위팔",
        "SUR:061" to "팔꿈치",
        "SUR:065" to "아래팔",
        "SUR:071" to "손목",
        "SUR:072" to "손",
        "ANC:014" to "다리",
        "SUR:090" to "허벅지",
        "SUR:091" to "무릎",
        "SUR:097" to "종아리",
        "SUR:101" to "발목",
        "SUR:102" to "발",
        "ANC:012" to "허리·엉덩이",
        "SUR:041" to "허리 가운데",
        "SUR:042" to "허리 옆",
        "SUR:081" to "엉덩이",
        "ANC:010" to "전신",
        "ANC:011" to "피부",
    )
