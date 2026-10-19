package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 부위 고르기 화면의 버튼을 말로(#336). 앞면·뒷면 전환, "목록에서 고르기", "다른 부위 보기"다. 이 화면의 일이
 * 아니면 null이고 부위 이름으로 고르는 말로 넘어간다. 이어서 물을 것은 단계가 붙인다.
 *
 * - 앞면·뒷면은 말 전체가 그 말일 때만이다. "목 뒤쪽이 아파"는 부위를 고르는 말이다. 3D 판은 카메라를
 *   화면이 들고 있어 상태의 [BodyMapUiState.viewAsked]를 올리면 판이 그 면으로 돈다. 확대 중에는 받지 않는다.
 * - "목록"이면 목록으로, 목록에서 "인체도로"면 판으로 돌아간다.
 * - "다른 부위"는 버튼처럼 확대만 닫고 고른 부위는 남긴다. 전에는 "다시 고를래"와 같이 고른 것까지 지웠다.
 */
internal fun BodyMapUiState.viewVoice(text: String, update: ((BodyMapUiState) -> BodyMapUiState) -> Unit): VoiceReply? {
    val heard = VoiceWords.compact(text)
    val face = FACE.matchEntire(heard)?.groupValues?.get(1)
    return when {
        face != null && focus == null && !byList -> {
            val view = if (face in BACK_FACES) BodyMapView.BACK else BodyMapView.FRONT
            update { it.copy(view = view, viewAsked = it.viewAsked + 1) }
            val message = if (view ==
                BodyMapView.BACK
            ) {
                R.string.voice_intake_view_back
            } else {
                R.string.voice_intake_view_front
            }
            VoiceReply.Done(message)
        }

        !byList && heard.contains("목록") -> {
            update { it.copy(byList = true, byMap3d = false, search = "", searchResults = emptyList()) }
            VoiceReply.Done(R.string.voice_intake_list_mode)
        }

        byList && TO_MAP.any { heard.contains(it) } -> {
            update { it.copy(byList = false, byMap3d = true, search = "", searchResults = emptyList()) }
            VoiceReply.Done(R.string.voice_intake_map_mode)
        }

        focus != null && isOtherPartVoice(text) -> {
            update { it.copy(focus = null) }
            VoiceReply.Done(R.string.voice_intake_other_part)
        }

        else -> null
    }
}

/** "다른 부위 보기", "다른 데 볼래". 짧은 말일 때만이다 — "다른 데도 아파요"는 답이다. */
internal fun isOtherPartVoice(text: String): Boolean {
    val heard = VoiceWords.compact(text)
    return heard.length <= OTHER_MAX && OTHER.any { heard.startsWith(it) } && !heard.contains("아파")
}

private val BACK_FACES = setOf("뒷면", "뒤쪽", "등쪽", "뒷모습", "등")

/** "뒷면 보여 줘", "등 쪽으로 돌려 줘", "앞면". */
private val FACE =
    Regex("""(앞면|앞쪽|정면|앞모습|뒷면|뒤쪽|등쪽|뒷모습|등)(?:으로|을|를|이)?(?:보여줘|보여|볼래|봐|보기|돌려줘|돌려|돌릴래)?(?:요|줘|주세요)?""")

private val TO_MAP = listOf("인체도", "그림으로", "몸으로", "3d")

private val OTHER = listOf("다른부위", "다른데", "다른곳")

private const val OTHER_MAX = 12
