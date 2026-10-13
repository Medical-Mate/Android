package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 증상 정리를 말로 할 때의 말 풀이(#284). 화면 동작은 [IntakeVoiceCommands]가 한다.
 *
 * 단계마다 같은 말이 다른 뜻이다. 1단계에서 "무릎"은 부위이고 4단계에서는 질문의 한 낱말이다.
 * 그래서 단계별로 나눠 읽는다. 단계를 넘나드는 말은 "다음"과 "뒤로" 둘뿐이다.
 */

/** 말로 짚은 부위를 어떻게 할지. */
internal sealed interface BodyVoicePick {
    /** 하나로 정해졌다. */
    data class Picked(val selection: BodyMapSelection) : BodyVoicePick

    /** 큰 부위만 말했다. 그 부위로 들어가 어디인지 묻는다. "다리가 아파요". */
    data class Focus(val anchor: BodyMapSelection) : BodyVoicePick

    /** 좌우가 갈리는데 말하지 않았다. 고를 수 있는 것들을 들고 묻는다. */
    data class AskSide(val choices: List<BodyMapSelection>) : BodyVoicePick

    /** 부위가 아니다. */
    data object None : BodyVoicePick
}

/**
 * 말에서 부위를 찾는다. 목록의 부위 검색([searchBodyParts])을 그대로 쓴다. 이름과 별칭 122개라
 * "명치", "뒷목", "옆구리"도 찾는다.
 *
 * 좌우는 검색어가 아니라 따로 읽는다. 부위 이름에 좌우가 없어서 "오른쪽 어깨"로 찾으면 걸리는
 * 것이 줄지 않는다. 좌우를 말하지 않았으면 [focus]가 들고 있는 좌우를 쓴다. 다리를 짚어 둔
 * 채 "무릎"이라고 하면 그 다리의 무릎이다.
 */
internal fun pickBodyPartByVoice(text: String, focus: BodyMapSelection?): BodyVoicePick {
    val side = sideOf(text) ?: focus?.side?.takeIf { it == BodyMapSide.LEFT || it == BodyMapSide.RIGHT }
    val ordinal = VoiceWords.ordinal(text)
    val top = searchBodyParts(text.replace(SIDE_WORDS, " ")).firstOrNull { it.score > 0 }
    val anchorId = top?.let { bodyMapAnchorIdOfZone(it.id) }
    return when {
        ordinal != null -> ordinalPick(ordinal, focus, side)
        top == null -> BodyVoicePick.None
        anchorId == null -> anchorPick(top.id, side)
        else -> zonePick(anchorId, top.id, side)
    }
}

/**
 * 막대가 읽어 준 목록에서 몇 번째로 골랐다. 큰 부위로 들어가 있으면 그 안의 상세 부위, 아니면 큰
 * 부위 목록이다. 막대가 읽어 주는 차례([bodyVoiceAnchors]·[bodyVoiceZones])와 같다.
 */
private fun ordinalPick(ordinal: Int, focus: BodyMapSelection?, side: BodyMapSide?): BodyVoicePick {
    val names = if (focus == null) bodyVoiceAnchors() else bodyVoiceZones(focus.anchorId)
    val index = if (ordinal == VoiceWords.LAST) names.lastIndex else ordinal
    val id = names.getOrNull(index)?.first
    return when {
        id == null -> BodyVoicePick.None
        focus == null -> anchorPick(id, side)
        else -> zonePick(focus.anchorId, id, side)
    }
}

/** 큰 부위 목록. 막대가 읽어 주는 차례다. */
internal fun bodyVoiceAnchors(): List<Pair<String, String>> = bodyMapGroups.map { it.anchor.id to it.anchor.label }

/** 그 큰 부위 안의 상세 부위 목록. 막대가 읽어 주는 차례다. */
internal fun bodyVoiceZones(anchorId: String): List<Pair<String, String>> =
    bodyMapGroups.firstOrNull { it.anchor.id == anchorId }?.zones.orEmpty().map { it.id to it.label }

/** 큰 부위를 말했다. 구역이 없으면(전신·피부) 고르고, 있으면 그 부위로 들어간다. 팔·다리는 좌우가 먼저다. */
private fun anchorPick(anchorId: String, side: BodyMapSide?): BodyVoicePick {
    val mirrored = bodyMapAnchorOf(anchorId).detail?.mirrored == true
    return when {
        bodyMapZonesOf(anchorId).isEmpty() -> BodyVoicePick.Picked(BodyMapSelection(anchorId))
        mirrored && side == null ->
            BodyVoicePick.AskSide(
                listOf(BodyMapSide.LEFT, BodyMapSide.RIGHT).map {
                    BodyMapSelection(anchorId, side = it)
                },
            )

        else -> BodyVoicePick.Focus(BodyMapSelection(anchorId, side = side.takeIf { mirrored } ?: BodyMapSide.CENTER))
    }
}

/**
 * 구역을 말했다. 좌우가 갈리면 말한 쪽을 고르고, 말하지 않았으면 묻는다. 배의 명치·아랫배도 좌우
 * 점이 따로 있어 묻는다.
 */
private fun zonePick(anchorId: String, zoneId: String, side: BodyMapSide?): BodyVoicePick {
    val choices = bodyMapChoicesForZone(anchorId, zoneId)
    val sided = if (side == null) choices else choices.filter { it.side == side }.ifEmpty { choices }
    return when {
        sided.size == 1 -> BodyVoicePick.Picked(sided.single())
        sided.isEmpty() -> BodyVoicePick.None
        else -> BodyVoicePick.AskSide(sided)
    }
}

/** 말한 좌우. 없으면 null이다. */
internal fun sideOf(text: String): BodyMapSide? {
    val heard = VoiceWords.compact(text)
    return when {
        LEFT.any { heard.contains(it) } -> BodyMapSide.LEFT
        RIGHT.any { heard.contains(it) } -> BodyMapSide.RIGHT
        else -> null
    }
}

private val LEFT = listOf("왼쪽", "왼편", "좌측", "왼")

private val RIGHT = listOf("오른쪽", "오른편", "우측", "오른")

/** 검색어에서 뺄 좌우. "오른쪽 어깨"의 "오른쪽"이 다른 부위 이름에 스치지 않게 한다. */
private val SIDE_WORDS = Regex("""왼\s*쪽|오른\s*쪽|왼편|오른편|좌측|우측""")
