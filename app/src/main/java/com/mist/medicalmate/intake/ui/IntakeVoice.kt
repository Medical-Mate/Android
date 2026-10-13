package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 증상 정리를 말로 할 때의 말 풀이(#284). 화면 동작은 [IntakeVoiceCommands]가 한다.
 *
 * 단계마다 같은 말이 다른 뜻이다. 1단계에서 "무릎"은 부위이고 4단계에서는 질문의 한 낱말이다.
 * 그래서 단계별로 나눠 읽는다. 단계를 넘나드는 말은 "다음"과 "뒤로" 둘뿐이다.
 */

/** "다음"으로 들리는 말. 짧을 때만 본다. "다음 주에 다시 올게요" 같은 답이 걸리면 안 된다. */
internal fun isNextVoice(text: String): Boolean {
    val heard = VoiceWords.compact(text)
    return heard.length <= NEXT_MAX && NEXT.any { heard.startsWith(it) }
}

/** "뒤로"로 들리는 말. 짧을 때만 본다. 문답의 답에 "뒤로"가 섞일 수 있다. */
internal fun isBackVoice(text: String): Boolean {
    val heard = VoiceWords.compact(text)
    return heard.length <= NEXT_MAX && BACK.any { heard.startsWith(it) }
}

/** 부위를 다시 고르겠다는 말. */
internal fun isResetVoice(text: String): Boolean {
    val heard = VoiceWords.compact(text)
    return heard.length <= RESET_MAX && RESET.any { heard.contains(it) }
}

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
    val top = searchBodyParts(text.replace(SIDE_WORDS, " ")).firstOrNull { it.score > 0 } ?: return BodyVoicePick.None
    val anchorId = bodyMapAnchorIdOfZone(top.id)
    return if (anchorId == null) anchorPick(top.id, side) else zonePick(anchorId, top.id, side)
}

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

/**
 * 말로 고른 아픈 정도(1d). 숫자와 낱말을 모두 받는다.
 *
 * - "7점", "10점 중에 7점" → 0~10점(NRS)을 다섯 단계로. 카드가 NRS로도 적는 값이라 사람이 점수로
 *   말할 수 있다.
 * - "4단계" → 그 단계.
 * - "많이 아파요", "견디기 힘들어요", "조금 불편해요" → 화면의 다섯 낱말과 그 비슷한 말.
 *
 * 낱말은 센 것부터 본다. "많이"가 "조금 많이"에도 들어 있다.
 */
internal fun parseSeverityVoice(text: String): MedicalMateSeverity? {
    val heard = VoiceWords.compact(text)
    val score = NRS.findAll(heard).lastOrNull()?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..NRS_MAX }
    val step = STEP.find(heard)?.groupValues?.get(1)?.toIntOrNull()
    return when {
        // 0점은 단계 밖이라 가장 낮은 단계로 둔다.
        score != null ->
            MedicalMateSeverity.entries.firstOrNull { score in it.nrsFirst..it.nrsLast } ?: MedicalMateSeverity.LEVEL_1

        step != null -> MedicalMateSeverity.entries.firstOrNull { it.level == step }
        else -> SEVERITY_WORDS.firstOrNull { (_, words) -> words.any { heard.contains(it) } }?.first
    }
}

private val NEXT = listOf("다음", "넘어가", "넘겨", "다됐", "다했", "없어", "없습니다", "됐어요", "계속")

private val BACK = listOf("뒤로", "이전", "전으로", "돌아가")

private val RESET = listOf("다시고를", "다시선택", "처음부터", "다른부위", "다른데", "취소")

private const val NEXT_MAX = 8

private const val RESET_MAX = 12

private val LEFT = listOf("왼쪽", "왼편", "좌측", "왼")

private val RIGHT = listOf("오른쪽", "오른편", "우측", "오른")

/** 검색어에서 뺄 좌우. "오른쪽 어깨"의 "오른쪽"이 다른 부위 이름에 스치지 않게 한다. */
private val SIDE_WORDS = Regex("""왼\s*쪽|오른\s*쪽|왼편|오른편|좌측|우측""")

private val NRS = Regex("""(\d{1,2})점""")

private val STEP = Regex("""([1-5])(?:단계|번)""")

private const val NRS_MAX = 10

private val SEVERITY_WORDS =
    listOf(
        MedicalMateSeverity.LEVEL_5 to
            listOf("견디기힘들", "참을수없", "못참", "죽겠", "너무아파", "엄청아파", "극심", "최악", "제일아파"),
        MedicalMateSeverity.LEVEL_4 to listOf("많이아파", "많이", "심해", "심하게", "힘들어"),
        MedicalMateSeverity.LEVEL_3 to listOf("꽤", "보통", "중간", "적당히", "그럭저럭"),
        MedicalMateSeverity.LEVEL_2 to listOf("은근", "좀아파", "자꾸", "신경쓰"),
        MedicalMateSeverity.LEVEL_1 to listOf("조금", "살짝", "약간", "불편"),
    )
