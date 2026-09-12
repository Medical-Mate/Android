package com.mist.medicalmate.intake.ui

/**
 * 부위 이름과 별칭. AI 트랙의 `docs/examples/body-map.json`에서 생성한 파일이다.
 *
 * **손으로 고치지 않는다.** 이름이나 별칭이 바뀌면 새 응답으로 다시 생성한다. 표시 문구의
 * 출처를 이 파일 하나로 둔다. 인체도 좌표 파일에도 이름이 들어 있는 판본이 있는데, 출처가
 * 둘이면 이름이 바뀔 때 어긋난다.
 *
 * 스냅샷 `f848848baea4`이고 앵커 9개 · 구역 25개 · 별칭 122개다.
 * 응답에는 부위별 진료과도 함께 오는데 안내할 화면이 없어서 담지 않았다.
 */

/** 앵커나 구역 하나. [aliases]는 검색에서만 쓰고 화면에는 [label]만 나온다. */
internal data class BodyPart(val id: String, val label: String, val aliases: List<String> = emptyList())

/** 앵커와 그 아래 구역. 차례는 응답의 배열 그대로이고 디자이너 인체도 배치 순서다. */
internal data class BodyPartGroup(val anchor: BodyPart, val zones: List<BodyPart> = emptyList())

/** 부위 온톨로지 전체. 검색이 이 차례를 그대로 쓴다. */
internal val bodyMapGroups: List<BodyPartGroup> =
    listOf(
        BodyPartGroup(
            anchor =
            BodyPart("ANC:001", "머리", listOf("두부", "머리통", "머리 전체")),
            zones =
            listOf(
                BodyPart("SUR:001", "머리 전체·이마", listOf("이마", "앞머리", "뒤통수", "정수리", "옆머리", "관자놀이")),
                BodyPart("SUR:002", "눈", listOf("안구", "눈알", "눈꺼풀", "눈두덩")),
                BodyPart("SUR:003", "귀", listOf("귓속", "귓바퀴", "귀 안", "귀 뒤")),
                BodyPart("SUR:004", "코", listOf("콧속", "콧등", "비강")),
                BodyPart("SUR:005", "입", listOf("입안", "구강", "입술", "혀", "잇몸", "치아", "이빨")),
            ),
        ),
        BodyPartGroup(
            anchor =
            BodyPart("ANC:002", "목", emptyList()),
            zones =
            listOf(
                BodyPart("SUR:011", "목 안(목구멍)", listOf("목구멍", "인후", "편도", "목 안쪽", "목 속")),
                BodyPart("SUR:012", "목 뒤·옆", listOf("뒷목", "목덜미", "옆목", "목 옆", "경추", "목뼈")),
            ),
        ),
        BodyPartGroup(
            anchor =
            BodyPart("ANC:003", "가슴", listOf("흉부", "가슴팍", "앞가슴")),
            zones =
            listOf(
                BodyPart("SUR:021", "가슴 가운데", listOf("가슴뼈", "흉골", "가슴 중앙", "심장 쪽")),
                BodyPart("SUR:022", "가슴 옆(갈비)", listOf("갈비뼈", "늑골", "옆가슴", "갈비")),
            ),
        ),
        BodyPartGroup(
            anchor =
            BodyPart("ANC:004", "배", listOf("복부", "뱃속", "배 전체", "배꼽")),
            zones =
            listOf(
                BodyPart("SUR:031", "윗배(명치)", listOf("명치", "상복부", "윗배", "명치끝", "배 위쪽")),
                BodyPart("SUR:032", "아랫배", listOf("하복부", "배 아래쪽", "골반", "배꼽 아래")),
            ),
        ),
        BodyPartGroup(
            anchor =
            BodyPart("ANC:013", "팔", listOf("상지", "팔 전체")),
            zones =
            listOf(
                BodyPart("SUR:051", "어깨", listOf("어깻죽지", "견관절", "어깨 앞", "어깨 뒤")),
                BodyPart("SUR:055", "위팔", listOf("윗팔", "상완", "팔 위쪽")),
                BodyPart("SUR:061", "팔꿈치", listOf("팔굽", "주관절")),
                BodyPart("SUR:065", "아래팔", listOf("아랫팔", "전완", "팔 아래쪽")),
                BodyPart("SUR:071", "손목", listOf("손목 관절")),
                BodyPart("SUR:072", "손", listOf("손가락", "손바닥", "손등", "엄지", "검지")),
            ),
        ),
        BodyPartGroup(
            anchor =
            BodyPart("ANC:014", "다리", listOf("하지", "다리 전체")),
            zones =
            listOf(
                BodyPart("SUR:090", "허벅지", listOf("대퇴", "넓적다리", "허벅다리")),
                BodyPart("SUR:091", "무릎", listOf("슬관절", "무릎 앞", "무릎 뒤", "오금")),
                BodyPart("SUR:097", "종아리", listOf("장딴지", "정강이", "정강이뼈")),
                BodyPart("SUR:101", "발목", listOf("발목 관절", "아킬레스")),
                BodyPart("SUR:102", "발", listOf("발가락", "발바닥", "발등", "발뒤꿈치", "뒤꿈치")),
            ),
        ),
        BodyPartGroup(
            anchor =
            BodyPart("ANC:012", "허리·엉덩이", listOf("요추", "등허리")),
            zones =
            listOf(
                BodyPart("SUR:041", "허리 가운데", listOf("허리", "허리 중앙", "꼬리뼈", "척추 아래", "등 아래")),
                BodyPart("SUR:042", "허리 옆", listOf("옆구리", "옆허리", "갈비뼈 아래 옆")),
                BodyPart("SUR:081", "엉덩이", listOf("둔부", "고관절", "엉치", "꽁무니")),
            ),
        ),
        BodyPartGroup(
            anchor =
            BodyPart("ANC:010", "전신", listOf("온몸", "몸 전체", "전체", "특정 부위 없음")),
        ),
        BodyPartGroup(
            anchor =
            BodyPart("ANC:011", "피부", listOf("살", "피부 표면", "겉")),
        ),
    )

/** 이름표. 화면이 id로 이름을 찾을 때 쓴다. */
internal val bodyMapOntology: Map<String, String> =
    bodyMapGroups
        .flatMap { group -> listOf(group.anchor) + group.zones }
        .associate { it.id to it.label }
