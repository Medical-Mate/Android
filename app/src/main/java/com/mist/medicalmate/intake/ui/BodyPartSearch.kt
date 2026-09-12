package com.mist.medicalmate.intake.ui

/**
 * 목록에서 부위를 찾는 검색. AI 트랙의 `docs/android-body-search.md`를 옮긴 것이다.
 *
 * **서버를 부르지 않는다.** 같은 규칙의 `GET /v1/ontology/search`가 있지만 한 글자마다
 * 왕복하게 된다. 데이터가 앵커 9개 · 구역 25개 · 별칭 122개뿐이라 한 번 찾는 데 문자열
 * 비교 156회다. 색인 없이 전수로 충분하다.
 *
 * **한/영 자판 오타 복원은 넣지 않는다.** 서버는 `qo`를 "배"로 고쳐 주지만 두벌식 조합
 * 오토마타를 옮기는 비용이 얻는 것보다 크다고 AI 트랙과 정했다(2026-09-11). 그래서 이
 * 규칙과 검증 벡터에는 그 기능이 없고, 앱과 서버 결과가 이 지점에서 갈린다. 사고가 아니라
 * 결정이다.
 *
 * 규칙이 바뀌면 AI 쪽이 벡터를 다시 만들어 준다. `BodyPartSearchTest`가 165케이스를
 * 순서까지 대조하므로 어긋나면 그 시험이 잡는다.
 */

/**
 * 부위를 찾는다. 못 찾으면 빈 목록이다.
 *
 * 증상이나 병명은 걸리지 않는다. 데이터에 부위 이름과 별칭만 있어서 저절로 그렇게 된다.
 * "감기"나 "무릅"(오타)이 0건인 것이 맞는 동작이다.
 */
internal fun searchBodyParts(query: String): List<BodyPartMatch> {
    val normalized = normalizeForSearch(query.take(QUERY_MAX_LENGTH))
    if (normalized.isEmpty()) return emptyList()

    val direct =
        bodyMapGroups
            .flatMap { group -> listOf(group.anchor to true) + group.zones.map { it to false } }
            .mapNotNull { (part, isAnchor) -> part.match(normalized)?.let { it to isAnchor } }
            .sortedWith(matchOrder)

    return (direct.map { it.first } + direct.expandAnchors()).take(RESULT_LIMIT)
}

/**
 * 노드 하나의 점수.
 *
 * 이름과 별칭을 모두 후보로 보고 가장 높은 점수 하나만 쓴다. 같은 점수가 여럿이면 **먼저
 * 나온 후보**가 걸린 것으로 둔다. 이름이 별칭보다 앞이고 별칭은 응답 차례다. 서버가 그렇게
 * 하고 있어서 `matched`가 갈린다. `목`이 `손목`에 걸릴 때 별칭 `손목 관절`이 아니라 이름
 * `손목`이 나오는 것이 그 결과다.
 */
private fun BodyPart.match(query: String): BodyPartMatch? {
    val best =
        (listOf(label) + aliases)
            .map { candidate -> candidate to scoreOf(normalizeForSearch(candidate), query) }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?: return null
    return BodyPartMatch(id = id, matched = best.first, score = best.second)
}

/**
 * 후보 하나의 점수. 높을수록 가깝다.
 *
 * 양쪽 방향을 본다. 질의가 후보보다 길 수도 있다. "왼쪽 아랫배가 아파요"처럼 문장을 넣는
 * 경우가 그것이고, 그때 후보 "아랫배"가 질의 안에 들어 있다.
 */
private fun scoreOf(candidate: String, query: String): Int = when {
    candidate == query -> SCORE_EXACT
    candidate.startsWith(query) || query.startsWith(candidate) -> SCORE_PREFIX
    candidate.contains(query) || query.contains(candidate) -> SCORE_CONTAINS
    else -> 0
}

/**
 * 걸린 앵커의 구역을 뒤에 붙인다.
 *
 * 목록에서 "다리"를 치면 무릎·종아리도 후보로 보여야 한다. 붙는 구역의 점수는 0이라 직접
 * 걸린 것과 구분된다.
 *
 * **점수 1(부분 포함)인 앵커는 펼치지 않는다.** AI 트랙이 여기서 한 번 틀렸다. "전체"가
 * "머리 전체"·"팔 전체"에 스쳐서 앵커 넷이 펼쳐졌고, 23건이 되어 상한에서 잘리는 바람에
 * 정작 눈·귀만 남았다.
 */
private fun List<Pair<BodyPartMatch, Boolean>>.expandAnchors(): List<BodyPartMatch> {
    val already = map { it.first.id }.toMutableSet()
    return flatMap { (match, isAnchor) ->
        if (!isAnchor || match.score < SCORE_PREFIX) {
            emptyList()
        } else {
            bodyMapGroups
                .first { it.anchor.id == match.id }
                .zones
                .filter { already.add(it.id) }
                .map { BodyPartMatch(id = it.id, matched = match.matched, score = 0) }
        }
    }
}

/**
 * 차례.
 *
 * 점수 다음이 **걸린 문자열이 긴 것**이다. 구체적인 쪽을 앞에 둔다. "왼쪽 아랫배가 아파요"는
 * "아랫배"와 "배"에 모두 걸리는데 앞엣것이 먼저여야 한다.
 */
private val matchOrder =
    compareByDescending<Pair<BodyPartMatch, Boolean>> { it.first.score }
        .thenByDescending { it.first.matched.length }
        .thenByDescending { it.second }
        .thenBy { it.first.id }

/**
 * 질의와 후보에 같이 거는 정규화.
 *
 * 띄어쓰기와 구분 기호를 지운다. "허리 옆(옆구리)"가 "허리옆옆구리"가 되어 "옆구리"로도
 * 걸린다. 사람이 띄어쓰기를 맞춰 칠 이유가 없다.
 */
private fun normalizeForSearch(value: String): String =
    value.filterNot { it.isWhitespace() || it in SEPARATORS }.lowercase()

private const val SEPARATORS = "·()（）,."

private const val SCORE_EXACT = 3

private const val SCORE_PREFIX = 2

private const val SCORE_CONTAINS = 1

/** 결과 상한. 지금 데이터에서 8건을 넘는 질의는 없다. */
private const val RESULT_LIMIT = 8

/** 발화 상한과 같은 값이다. 거절하지 않고 자른다. */
private const val QUERY_MAX_LENGTH = 300
