package com.mist.medicalmate.card.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateBottomSheet
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateButtonType
import com.mist.medicalmate.core.designsystem.component.MedicalMateExportPreview
import com.mist.medicalmate.core.designsystem.component.MedicalMateSwitch
import com.mist.medicalmate.core.designsystem.component.MedicalMateToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 카드 머리의 내보내기를 누르면 뜨는 시트. Figma 1e-1-X `3663:7463`(#306 · #314).
 *
 * 그래버 아래 14 · 제목 · 4 · 설명 · 16 · 미리보기(300) · 8 · 가리기 줄 · 8 · `이미지 저장`(Primary) · 8 ·
 * `공유하기`(Outline)다. 시트 본체의 자식 간격 12를 받지 않도록 한 묶음으로 둔다.
 *
 * **이름·나이를 가릴지 고른다.** 켜면 내보내는 이미지의 환자 줄에서 이름은 성만 남고 나이가 빠진다. 화면의
 * 카드는 그대로이고 미리보기의 환자 줄만 함께 바뀐다. 저장과 공유에 함께 걸린다.
 *
 * 가리기 줄은 `Toggle` 마스터가 라벨을 밖에 두라고 해서 직접 짠다 — `Body/M` 글자와 위아래 8이다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BriefCardExportSheet(
    export: BriefCardExport,
    card: BriefCard?,
    onSave: (ImageBitmap) -> Unit,
    onShare: (ImageBitmap) -> Unit,
) {
    if (!export.opened || card == null) return
    // 시트가 떠 있는 동안은 시트가 말을 받는다(#334). 전에는 열 수만 있어 "뒤로"가 카드 화면째 빠졌다.
    BriefCardExportVoice(export = export, onSave = onSave, onShare = onShare)
    MedicalMateBottomSheet(onDismissRequest = export::close, grabberGap = MedicalMateSpace.s14) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHead(
                title = stringResource(R.string.brief_card_export_title),
                body = stringResource(R.string.brief_card_export_body),
            )
            Spacer(modifier = Modifier.height(MedicalMateSpace.s16))
            MedicalMateExportPreview(
                title = card.title,
                patientLine = if (export.maskPatient) card.maskedPatientLine else card.patientLine,
                chipLabel = stringResource(R.string.brief_card_export_chip),
            )
            Spacer(modifier = Modifier.height(MedicalMateSpace.s8))
            MaskRow(checked = export.maskPatient, onCheckedChange = { export.maskPatient = it })
            Spacer(modifier = Modifier.height(MedicalMateSpace.s8))
            MedicalMateButton(
                onClick = { export.send(onSave) },
                label = stringResource(R.string.brief_card_export_save),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(MedicalMateSpace.s8))
            MedicalMateButton(
                onClick = { export.send(onShare) },
                label = stringResource(R.string.brief_card_export_share),
                modifier = Modifier.fillMaxWidth(),
                type = MedicalMateButtonType.OUTLINE,
            )
        }
    }
}

/** 10-15 시트 머리. `Heading/M` 제목 아래 4 띄워 `Body/M` `fg/subtle` 설명이다. 1e-1-S도 같다. */
@Composable
internal fun SheetHead(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4)) {
        Text(text = title, style = MedicalMateTheme.typography.headingM, color = MedicalMateTheme.colors.fgDefault)
        Text(text = body, style = MedicalMateTheme.typography.bodyM, color = MedicalMateTheme.colors.fgSubtle)
    }
}

/** 이름·나이 가리기 줄. 줄 전체가 스위치다. */
@Composable
private fun MaskRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = MedicalMateSpace.s8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.brief_card_export_mask),
            style = MedicalMateTheme.typography.bodyM,
            color = MedicalMateTheme.colors.fgDefault,
        )
        MedicalMateSwitch(checked = checked)
    }
}

/**
 * 내보내기 시트의 상태와 카드를 찍는 일(#306).
 *
 * 가리기를 켜고 내보내면 그 한 프레임만 카드의 환자 줄을 가린 줄로 그리고 찍는다. 화면의 카드를 따로 그려 두지
 * 않는 것은, 찍는 층이 스크롤 안쪽 내용 전체를 이미 담고 있어서 같은 카드를 한 벌 더 그리면 둘이 어긋날 자리가
 * 생기기 때문이다. 시트가 스크림으로 화면을 덮고 있어 바뀌는 한 프레임은 눈에 띄지 않는다.
 */
@Stable
internal class BriefCardExport(private val layer: GraphicsLayer, private val scope: CoroutineScope) {
    /** 시트가 열려 있는지. */
    var opened by mutableStateOf(false)
        private set

    /** 내보낼 때 이름·나이를 가릴지. 처음에는 가린다 — 카드를 남에게 보이는 자리라 덜 내보내는 쪽이 기본이다. */
    var maskPatient by mutableStateOf(true)

    /** 찍는 동안만 켠다. 카드가 이 값으로 환자 줄을 고른다. */
    private var capturingMasked by mutableStateOf(false)

    /** 찍는 동안인지. 카드 머리의 내보내기 버튼이 이미지에 찍히지 않게 그 한 프레임만 감춘다(#314). */
    private var capturing by mutableStateOf(false)

    /** 카드 본문이 그려 넣을 층과, 지금 가린 줄로 그리는 중인지. */
    val capture: BriefCardCapture get() = BriefCardCapture(layer, capturingMasked, capturing)

    fun open() {
        opened = true
    }

    fun close() {
        opened = false
    }

    /** 카드를 찍어 [to]로 보내고 시트를 닫는다. */
    fun send(to: (ImageBitmap) -> Unit) {
        scope.launch {
            capturingMasked = maskPatient
            capturing = true
            // 바꾼 줄이 그려져 층에 다시 담길 때까지 두 프레임을 기다린다.
            withFrameNanos {}
            withFrameNanos {}
            val image = layer.toImageBitmap()
            capturingMasked = false
            capturing = false
            opened = false
            to(image)
        }
    }
}

/** 내보낼 때 찍을 층과, 지금 가린 줄로 그리는 중인지. */
internal class BriefCardCapture(val layer: GraphicsLayer, val maskPatient: Boolean, val capturing: Boolean = false)

@Composable
internal fun rememberBriefCardExport(layer: GraphicsLayer): BriefCardExport {
    val scope = rememberCoroutineScope()
    return remember(layer) { BriefCardExport(layer, scope) }
}

/**
 * 저장했는지 알리는 글(#306). 카드 본문 칸의 아래에 선다 — 하단 버튼이 있으면 그 바로 위, 없으면 화면 아래에서
 * 20 위다. 글이 없으면 그리지 않는다.
 */
@Composable
internal fun BoxScope.BriefCardExportToast(message: String?) {
    message ?: return
    MedicalMateToast(
        message = message,
        modifier =
        Modifier
            .align(Alignment.BottomCenter)
            .padding(start = MedicalMateSize.gutter, end = MedicalMateSize.gutter, bottom = MedicalMateSpace.s20),
    )
}
