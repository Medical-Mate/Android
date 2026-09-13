package com.mist.medicalmate.intake.ui

/**
 * 3D 인체도의 구역 점 좌표. AI 트랙의 `anchors3d.json`(스키마 humanmap-3d-anchors/2.0)에서 생성한 파일이다.
 *
 * **손으로 고치지 않는다.** 2D 좌표표와 같은 규칙이고, 좌표가 바뀌면 새 파일로 다시 생성한다.
 * 이름은 담지 않는다 — 표시 문구의 출처는 온톨로지 하나로 둔다. 좌표를 담는 타입은
 * 생성 대상이 아니라 [BodyMap3dPoint]에 손으로 적혀 있다.
 *
 * 좌표계는 Y 업 · 신장 1.0 · 원점이 발바닥과 정수리의 가운데(y는 −0.5~+0.5) · 앞면이 +Z다.
 * 거리가 신장 기준이라 [BODY_3D_PICK_MAX_DISTANCE]와 그대로 견줄 수 있다.
 *
 * 구역 점 46개 · 구역 25종이다. 좌표가 없는 전신·피부(ANC:010·ANC:011)는 3D 판정과
 * 무관해서 빠져 있다.
 */

/**
 * 짚은 지점에서 이 거리를 넘으면 구역을 채택하지 않는다. 신장의 10%다.
 *
 * 표면의 3.6%가 여기 걸리는데 대부분 온톨로지에 대응 구역이 없는 등 가운데와 어깨 뒤다.
 * 자료가 앱에서 조정할 수 있는 값이라고 적어 두었다.
 */
internal const val BODY_3D_PICK_MAX_DISTANCE: Float = 0.1f

internal val bodyMap3dRegions: List<BodyMap3dPoint> =
    listOf(
        BodyMap3dPoint(
            "SUR:001", "ANC:001", BodyMapSide.CENTER,
            0.0f, 0.465f, 0.05684f, -0.0013f, 0.3893f, 0.9211f,
        ),
        BodyMap3dPoint(
            "SUR:002", "ANC:001", BodyMapSide.RIGHT,
            -0.0178f, 0.4374f, 0.05194f, -0.1343f, -0.2865f, 0.9486f,
        ),
        BodyMap3dPoint(
            "SUR:002", "ANC:001", BodyMapSide.LEFT,
            0.0178f, 0.4374f, 0.05184f, 0.2137f, -0.4006f, 0.891f,
        ),
        BodyMap3dPoint(
            "SUR:003", "ANC:001", BodyMapSide.RIGHT,
            -0.047f, 0.4291f, -0.01037f, -0.6632f, -0.3895f, 0.6392f,
        ),
        BodyMap3dPoint(
            "SUR:003", "ANC:001", BodyMapSide.LEFT,
            0.047f, 0.4291f, -0.01112f, 0.8437f, -0.0734f, 0.5317f,
        ),
        BodyMap3dPoint(
            "SUR:004", "ANC:001", BodyMapSide.CENTER,
            0.0f, 0.4136f, 0.07106f, 0.0064f, 0.4446f, 0.8957f,
        ),
        BodyMap3dPoint(
            "SUR:005", "ANC:001", BodyMapSide.CENTER,
            0.0f, 0.3907f, 0.06047f, -0.072f, -0.1389f, 0.9877f,
        ),
        BodyMap3dPoint(
            "SUR:011", "ANC:002", BodyMapSide.CENTER,
            0.0f, 0.342f, 0.02388f, -0.0056f, -0.1985f, 0.9801f,
        ),
        BodyMap3dPoint(
            "SUR:012", "ANC:002", BodyMapSide.RIGHT,
            -0.026f, 0.373f, 0.01604f, -0.6975f, -0.6097f, 0.3765f,
        ),
        BodyMap3dPoint(
            "SUR:012", "ANC:002", BodyMapSide.LEFT,
            0.026f, 0.373f, 0.01611f, 0.7207f, -0.5962f, 0.3537f,
        ),
        BodyMap3dPoint(
            "SUR:021", "ANC:003", BodyMapSide.CENTER,
            0.0f, 0.275f, 0.04534f, -0.0048f, 0.4303f, 0.9027f,
        ),
        BodyMap3dPoint(
            "SUR:022", "ANC:003", BodyMapSide.RIGHT,
            -0.072f, 0.2f, 0.04538f, -0.714f, -0.0036f, 0.7001f,
        ),
        BodyMap3dPoint(
            "SUR:022", "ANC:003", BodyMapSide.LEFT,
            0.072f, 0.2f, 0.04529f, 0.7163f, 0.0167f, 0.6976f,
        ),
        BodyMap3dPoint(
            "SUR:031", "ANC:004", BodyMapSide.RIGHT,
            -0.048f, 0.16f, 0.06307f, -0.5111f, -0.0341f, 0.8588f,
        ),
        BodyMap3dPoint(
            "SUR:031", "ANC:004", BodyMapSide.LEFT,
            0.048f, 0.16f, 0.06258f, 0.512f, -0.0276f, 0.8585f,
        ),
        BodyMap3dPoint(
            "SUR:032", "ANC:004", BodyMapSide.RIGHT,
            -0.043f, 0.06f, 0.06081f, -0.3713f, -0.1768f, 0.9115f,
        ),
        BodyMap3dPoint(
            "SUR:032", "ANC:004", BodyMapSide.LEFT,
            0.043f, 0.06f, 0.0608f, 0.3681f, -0.171f, 0.9139f,
        ),
        BodyMap3dPoint(
            "SUR:051", "ANC:013", BodyMapSide.RIGHT,
            -0.128f, 0.3f, -0.00509f, -0.6939f, 0.4075f, 0.5937f,
        ),
        BodyMap3dPoint(
            "SUR:051", "ANC:013", BodyMapSide.LEFT,
            0.128f, 0.3f, -0.00603f, 0.6835f, 0.4205f, 0.5966f,
        ),
        BodyMap3dPoint(
            "SUR:055", "ANC:013", BodyMapSide.RIGHT,
            -0.142f, 0.21f, -0.01364f, -0.7734f, 0.3089f, 0.5535f,
        ),
        BodyMap3dPoint(
            "SUR:055", "ANC:013", BodyMapSide.LEFT,
            0.142f, 0.21f, -0.01299f, 0.7475f, 0.3464f, 0.5668f,
        ),
        BodyMap3dPoint(
            "SUR:061", "ANC:013", BodyMapSide.RIGHT,
            -0.154f, 0.12f, 0.00479f, 0.6099f, 0.1497f, 0.7782f,
        ),
        BodyMap3dPoint(
            "SUR:061", "ANC:013", BodyMapSide.LEFT,
            0.154f, 0.12f, 0.00537f, -0.5757f, 0.0731f, 0.8144f,
        ),
        BodyMap3dPoint(
            "SUR:065", "ANC:013", BodyMapSide.RIGHT,
            -0.164f, 0.06f, 0.01327f, 0.6519f, 0.0207f, 0.758f,
        ),
        BodyMap3dPoint(
            "SUR:065", "ANC:013", BodyMapSide.LEFT,
            0.164f, 0.06f, 0.01335f, -0.6093f, 0.0496f, 0.7914f,
        ),
        BodyMap3dPoint(
            "SUR:071", "ANC:013", BodyMapSide.RIGHT,
            -0.175f, 0.005f, 0.04029f, 0.0663f, 0.5662f, 0.8216f,
        ),
        BodyMap3dPoint(
            "SUR:071", "ANC:013", BodyMapSide.LEFT,
            0.175f, 0.005f, 0.03995f, -0.1003f, 0.5504f, 0.8289f,
        ),
        BodyMap3dPoint(
            "SUR:072", "ANC:013", BodyMapSide.RIGHT,
            -0.17f, -0.07f, 0.05927f, -0.1258f, 0.0792f, 0.9889f,
        ),
        BodyMap3dPoint(
            "SUR:072", "ANC:013", BodyMapSide.LEFT,
            0.17f, -0.07f, 0.05899f, 0.109f, 0.0999f, 0.989f,
        ),
        BodyMap3dPoint(
            "SUR:090", "ANC:014", BodyMapSide.RIGHT,
            -0.057f, -0.15f, 0.04102f, -0.2543f, -0.2854f, 0.924f,
        ),
        BodyMap3dPoint(
            "SUR:090", "ANC:014", BodyMapSide.LEFT,
            0.057f, -0.15f, 0.04104f, 0.2704f, -0.2746f, 0.9228f,
        ),
        BodyMap3dPoint(
            "SUR:091", "ANC:014", BodyMapSide.RIGHT,
            -0.0585f, -0.22f, 0.02481f, 0.0853f, -0.4124f, 0.907f,
        ),
        BodyMap3dPoint(
            "SUR:091", "ANC:014", BodyMapSide.LEFT,
            0.0585f, -0.22f, 0.02506f, -0.0214f, -0.4125f, 0.9107f,
        ),
        BodyMap3dPoint(
            "SUR:097", "ANC:014", BodyMapSide.RIGHT,
            -0.072f, -0.31f, 0.00562f, 0.2405f, -0.1219f, 0.963f,
        ),
        BodyMap3dPoint(
            "SUR:097", "ANC:014", BodyMapSide.LEFT,
            0.072f, -0.31f, 0.0057f, -0.18f, -0.1273f, 0.9754f,
        ),
        BodyMap3dPoint(
            "SUR:101", "ANC:014", BodyMapSide.RIGHT,
            -0.069f, -0.42f, -0.00187f, -0.0457f, 0.2882f, 0.9565f,
        ),
        BodyMap3dPoint(
            "SUR:101", "ANC:014", BodyMapSide.LEFT,
            0.069f, -0.42f, -0.00198f, 0.0026f, 0.298f, 0.9546f,
        ),
        BodyMap3dPoint(
            "SUR:102", "ANC:014", BodyMapSide.RIGHT,
            -0.077f, -0.48f, 0.07517f, -0.0352f, 0.8521f, 0.5223f,
        ),
        BodyMap3dPoint(
            "SUR:102", "ANC:014", BodyMapSide.LEFT,
            0.077f, -0.48f, 0.07332f, 0.0076f, 0.8744f, 0.4852f,
        ),
        BodyMap3dPoint(
            "SUR:041", "ANC:012", BodyMapSide.CENTER,
            0.0f, 0.08f, -0.05174f, -0.0081f, 0.2604f, -0.9655f,
        ),
        BodyMap3dPoint(
            "SUR:042", "ANC:012", BodyMapSide.RIGHT,
            -0.062f, 0.12f, 0.04966f, -0.6389f, 0.1399f, 0.7565f,
        ),
        BodyMap3dPoint(
            "SUR:042", "ANC:012", BodyMapSide.RIGHT,
            -0.062f, 0.12f, -0.04237f, -0.3804f, 0.1677f, -0.9095f,
        ),
        BodyMap3dPoint(
            "SUR:042", "ANC:012", BodyMapSide.LEFT,
            0.062f, 0.12f, 0.04943f, 0.6521f, 0.1423f, 0.7446f,
        ),
        BodyMap3dPoint(
            "SUR:042", "ANC:012", BodyMapSide.LEFT,
            0.062f, 0.12f, -0.04222f, 0.3979f, 0.1785f, -0.8999f,
        ),
        BodyMap3dPoint(
            "SUR:081", "ANC:012", BodyMapSide.RIGHT,
            -0.075f, -0.02f, -0.06143f, -0.5803f, -0.3849f, -0.7177f,
        ),
        BodyMap3dPoint(
            "SUR:081", "ANC:012", BodyMapSide.LEFT,
            0.075f, -0.02f, -0.06091f, 0.5867f, -0.4097f, -0.6985f,
        ),
    )
