package com.mist.medicalmate.card.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceSuggestion
import com.mist.medicalmate.core.voice.VoiceTargetEffect
import com.mist.medicalmate.core.voice.VoiceWords

/** 내보내기 시트(1e-1-X)가 받는 말(#334). 시트의 버튼 둘과 토글, 그리고 닫기다. */
internal enum class ExportVoice {
    SAVE,
    SHARE,
    MASK_ON,
    MASK_OFF,
    CLOSE,
}

/**
 * 말을 읽는다. **가리지 말라는 말을 먼저 본다** — "가리지 마"에도 "가리"가 들어 있다. 공유를 저장보다 먼저 본다 —
 * "공유하고 저장"처럼 섞이면 보내는 쪽이 사람이 고르는 단계가 하나 더 있어 덜 위험하다.
 */
internal fun parseExportVoice(text: String): ExportVoice? {
    val heard = VoiceWords.compact(text)
    return when {
        VoiceWords.isClose(text) -> ExportVoice.CLOSE
        MASK_OFF.any { heard.contains(it) } -> ExportVoice.MASK_OFF
        MASK_ON.any { heard.contains(it) } -> ExportVoice.MASK_ON
        SHARE.any { heard.contains(it) } -> ExportVoice.SHARE
        heard.contains("저장") -> ExportVoice.SAVE
        else -> null
    }
}

/**
 * 시트가 떠 있는 동안 등록한다. 맨 나중에 등록한 것이 말을 받아서 카드 화면보다 먼저 본다. 모르는 말은 넘긴다.
 *
 * 저장과 공유는 확인을 받지 않는다. 버튼도 바로 하고, 공유는 받는 앱을 사람이 한 번 더 고른다.
 */
@Composable
internal fun BriefCardExportVoice(
    export: BriefCardExport,
    onSave: (ImageBitmap) -> Unit,
    onShare: (ImageBitmap) -> Unit,
) {
    VoiceTargetEffect(EXPORT_SUGGESTIONS) { text ->
        when (parseExportVoice(text)) {
            null -> VoiceReply.NotMine
            ExportVoice.CLOSE -> {
                export.close()
                VoiceReply.Done(R.string.voice_sheet_closed)
            }

            ExportVoice.MASK_ON -> {
                export.maskPatient = true
                VoiceReply.Done(R.string.voice_export_mask_on)
            }

            ExportVoice.MASK_OFF -> {
                export.maskPatient = false
                VoiceReply.Done(R.string.voice_export_mask_off)
            }

            ExportVoice.SAVE -> {
                export.send(onSave)
                VoiceReply.Done(R.string.voice_export_saving)
            }

            ExportVoice.SHARE -> {
                export.send(onShare)
                VoiceReply.Done(R.string.voice_export_sharing)
            }
        }
    }
}

internal val EXPORT_SUGGESTIONS =
    listOf(
        VoiceSuggestion(
            R.string.voice_suggest_export_save,
            R.string.voice_suggest_export_save_do,
            MedicalMateIcons.Export,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_export_mask,
            R.string.voice_suggest_export_mask_do,
            MedicalMateIcons.Lock,
        ),
        VoiceSuggestion(
            R.string.voice_suggest_export_share,
            R.string.voice_suggest_export_share_do,
            MedicalMateIcons.Share,
        ),
    )

private val MASK_OFF = listOf("가리지", "안가려", "가리기꺼", "가리기풀")
private val MASK_ON = listOf("가려", "가리기켜", "숨겨", "안보이게")
private val SHARE = listOf("공유", "보내", "카톡", "전송")
