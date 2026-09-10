package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.R

/**
 * 인체도 좌표표. 디자인 트랙이 보낸 `humanmap_coords.json`에서 생성했다.
 *
 * 원본: schema `humanmap-coords/3.0` · 3차 — 발가락 리터치(텍스처 인페인팅 + 발등 찢김 제거)
 * 모델: Hi3D_스타일화된 근육질 남성 해부 베이스 3D 모델_allparts_20260909_171130.glb
 *
 * 좌표는 0..1로 정규화했다. 이미지를 화면 높이에 맞춰 줄여도 비율이 유지되므로
 * 화면 폭에 따라 다시 계산할 것이 없다. 원본 픽셀 폭·높이는 [BodyMapImage]에 남겨
 * 두었다. 표시 크기를 정할 때 종횡비가 필요하고, 좌표표와 대조할 때도 쓴다.
 *
 * **손으로 고치지 마세요.** 좌표가 바뀌면 새 `humanmap_coords.json`으로 다시
 * 생성합니다. 부위 이름과 진료과는 서버 응답(`docs/examples/body-map.json`)에서
 * 오므로 여기 두지 않고 [bodyMapOntology]에 픽스처로 있습니다.
 */

/** 전신 앞면. 앵커 8개가 이 위에 있다. */
internal val bodyMapFront: BodyMapImage =
    BodyMapImage(R.drawable.bodymap_front, 1080, 2480, BodyMapView.FRONT, mirrored = false)

/** 전신 뒷면. 허리·엉덩이 앵커만 이 위에 있다. */
internal val bodyMapBack: BodyMapImage =
    BodyMapImage(R.drawable.bodymap_back, 1080, 2480, BodyMapView.BACK, mirrored = false)

/** 앵커 9개. 순서가 부위 목록과 사이드 칩의 노출 순서다. */
internal val bodyMapAnchors: List<BodyMapAnchorGeometry> =
    listOf(
        BodyMapAnchorGeometry(
            id = "ANC:010",
            view = null,
            points = emptyList(),
            detail = null,
            zones = emptyList(),
        ),
        BodyMapAnchorGeometry(
            id = "ANC:011",
            view = null,
            points = emptyList(),
            detail = null,
            zones = emptyList(),
        ),
        BodyMapAnchorGeometry(
            id = "ANC:001",
            view = BodyMapView.FRONT,
            points = listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.0450f)),
            detail = BodyMapImage(R.drawable.bodymap_head, 1080, 1591, BodyMapView.FRONT, mirrored = false),
            zones =
            listOf(
                BodyMapZoneGeometry("SUR:001", listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.2727f))),
                BodyMapZoneGeometry(
                    "SUR:002",
                    listOf(
                        BodyMapPoint(BodyMapSide.RIGHT, 0.3411f, 0.4400f),
                        BodyMapPoint(BodyMapSide.LEFT, 0.6589f, 0.4400f),
                    ),
                ),
                BodyMapZoneGeometry(
                    "SUR:003",
                    listOf(
                        BodyMapPoint(BodyMapSide.RIGHT, 0.0804f, 0.4903f),
                        BodyMapPoint(BodyMapSide.LEFT, 0.9196f, 0.4903f),
                    ),
                ),
                BodyMapZoneGeometry("SUR:004", listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.5842f))),
                BodyMapZoneGeometry("SUR:005", listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.7230f))),
            ),
        ),
        BodyMapAnchorGeometry(
            id = "ANC:002",
            view = BodyMapView.FRONT,
            points = listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.1550f)),
            detail = BodyMapImage(R.drawable.bodymap_neck, 1080, 792, BodyMapView.FRONT, mirrored = false),
            zones =
            listOf(
                BodyMapZoneGeometry("SUR:011", listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.5273f))),
                BodyMapZoneGeometry(
                    "SUR:012",
                    listOf(
                        BodyMapPoint(BodyMapSide.RIGHT, 0.3267f, 0.2455f),
                        BodyMapPoint(BodyMapSide.LEFT, 0.6733f, 0.2455f),
                    ),
                ),
            ),
        ),
        BodyMapAnchorGeometry(
            id = "ANC:003",
            view = BodyMapView.FRONT,
            points = listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.2500f)),
            detail = BodyMapImage(R.drawable.bodymap_chest, 1080, 694, BodyMapView.FRONT, mirrored = false),
            zones =
            listOf(
                BodyMapZoneGeometry("SUR:021", listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.3611f))),
                BodyMapZoneGeometry(
                    "SUR:022",
                    listOf(
                        BodyMapPoint(BodyMapSide.RIGHT, 0.2429f, 0.7778f),
                        BodyMapPoint(BodyMapSide.LEFT, 0.7571f, 0.7778f),
                    ),
                ),
            ),
        ),
        BodyMapAnchorGeometry(
            id = "ANC:004",
            view = BodyMapView.FRONT,
            points = listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.4200f)),
            detail = BodyMapImage(R.drawable.bodymap_abdomen, 1080, 1080, BodyMapView.FRONT, mirrored = false),
            zones =
            listOf(
                BodyMapZoneGeometry(
                    "SUR:031",
                    listOf(
                        BodyMapPoint(BodyMapSide.RIGHT, 0.3286f, 0.2143f),
                        BodyMapPoint(BodyMapSide.LEFT, 0.6714f, 0.2143f),
                    ),
                ),
                BodyMapZoneGeometry(
                    "SUR:032",
                    listOf(
                        BodyMapPoint(BodyMapSide.RIGHT, 0.3464f, 0.5714f),
                        BodyMapPoint(BodyMapSide.LEFT, 0.6536f, 0.5714f),
                    ),
                ),
            ),
        ),
        BodyMapAnchorGeometry(
            id = "ANC:013",
            view = BodyMapView.FRONT,
            points =
            listOf(
                BodyMapPoint(BodyMapSide.RIGHT, 0.1372f, 0.3800f),
                BodyMapPoint(BodyMapSide.LEFT, 0.8628f, 0.3800f),
            ),
            detail = BodyMapImage(R.drawable.bodymap_arm, 1080, 2850, BodyMapView.FRONT, mirrored = true),
            zones =
            listOf(
                BodyMapZoneGeometry("SUR:051", listOf(BodyMapPoint(BodyMapSide.BASE, 0.5556f, 0.0947f))),
                BodyMapZoneGeometry("SUR:055", listOf(BodyMapPoint(BodyMapSide.BASE, 0.4611f, 0.2842f))),
                BodyMapZoneGeometry("SUR:061", listOf(BodyMapPoint(BodyMapSide.BASE, 0.4278f, 0.4737f))),
                BodyMapZoneGeometry("SUR:065", listOf(BodyMapPoint(BodyMapSide.BASE, 0.3944f, 0.6000f))),
                BodyMapZoneGeometry("SUR:071", listOf(BodyMapPoint(BodyMapSide.BASE, 0.3333f, 0.7158f))),
                BodyMapZoneGeometry("SUR:072", listOf(BodyMapPoint(BodyMapSide.BASE, 0.3611f, 0.8737f))),
            ),
        ),
        BodyMapAnchorGeometry(
            id = "ANC:014",
            view = BodyMapView.FRONT,
            points =
            listOf(
                BodyMapPoint(BodyMapSide.RIGHT, 0.3507f, 0.7500f),
                BodyMapPoint(BodyMapSide.LEFT, 0.6493f, 0.7500f),
            ),
            detail = BodyMapImage(R.drawable.bodymap_leg, 1080, 2795, BodyMapView.FRONT, mirrored = true),
            zones =
            listOf(
                BodyMapZoneGeometry("SUR:090", listOf(BodyMapPoint(BodyMapSide.BASE, 0.4882f, 0.1818f))),
                BodyMapZoneGeometry("SUR:091", listOf(BodyMapPoint(BodyMapSide.BASE, 0.4794f, 0.3409f))),
                BodyMapZoneGeometry("SUR:097", listOf(BodyMapPoint(BodyMapSide.BASE, 0.4000f, 0.5454f))),
                BodyMapZoneGeometry("SUR:101", listOf(BodyMapPoint(BodyMapSide.BASE, 0.4177f, 0.7955f))),
                BodyMapZoneGeometry("SUR:102", listOf(BodyMapPoint(BodyMapSide.BASE, 0.3706f, 0.9318f))),
            ),
        ),
        BodyMapAnchorGeometry(
            id = "ANC:012",
            view = BodyMapView.BACK,
            points = listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.4300f)),
            detail = BodyMapImage(R.drawable.bodymap_lower_back_hip, 1080, 1360, BodyMapView.BACK, mirrored = false),
            zones =
            listOf(
                BodyMapZoneGeometry("SUR:041", listOf(BodyMapPoint(BodyMapSide.CENTER, 0.5000f, 0.2647f))),
                BodyMapZoneGeometry(
                    "SUR:042",
                    listOf(
                        BodyMapPoint(BodyMapSide.LEFT, 0.2704f, 0.1471f),
                        BodyMapPoint(BodyMapSide.RIGHT, 0.7296f, 0.1471f),
                    ),
                ),
                BodyMapZoneGeometry(
                    "SUR:081",
                    listOf(
                        BodyMapPoint(BodyMapSide.LEFT, 0.2222f, 0.5588f),
                        BodyMapPoint(BodyMapSide.RIGHT, 0.7778f, 0.5588f),
                    ),
                ),
            ),
        ),
    )
