package com.mist.medicalmate.card.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateScreenPreviews
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.component.MedicalMateButton
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavBar
import com.mist.medicalmate.core.designsystem.component.MedicalMateNavLeading
import com.mist.medicalmate.core.designsystem.component.MedicalMateSurfaceStyle

/**
 * 와이어프레임 1f-1. Figma `404:1835`.
 *
 * 환자가 폰을 그대로 의사에게 건네는 화면이다. 그래서 읽는 것 말고는 아무것도 없다.
 * 수정 연필도, AI가 정리했다는 줄도 빼고 카드와 알러지, 질문만 남긴다. 건네받은 사람이
 * 환자의 카드를 고치는 일은 없어야 한다.
 *
 * 상단 왼쪽이 뒤로가 아니라 닫기다. 어디로 돌아가는 것이 아니라 이 화면을 끝내는 것이고,
 * 폰을 돌려받은 환자가 누른다.
 *
 * 맨 위 한 줄이 이 화면이 무엇인지 밝힌다. 폰을 건네받은 의사가 먼저 읽는 줄이다.
 */
@Composable
fun HandoffScreen(card: BriefCard, onCloseClick: () -> Unit, onDoneClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MedicalMateTheme.colors.bgSurface),
    ) {
        MedicalMateNavBar(
            title = stringResource(R.string.handoff_title),
            leading = MedicalMateNavLeading.CLOSE,
            onLeadingClick = onCloseClick,
            surface = MedicalMateSurfaceStyle.GLASS,
        )
        HandoffContent(card)
        Footer(onDoneClick = onDoneClick)
    }
}

@Composable
private fun ColumnScope.HandoffContent(card: BriefCard) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s12,
                bottom = MedicalMateSpace.s16,
            ),
        verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s14),
    ) {
        HandoffHeader()
        BriefCardBlock(card = card, showAiCaption = false)
        AllergyNotice(card.allergies)
        QuestionsCallout(card.questions)
    }
}

@Composable
private fun Footer(onDoneClick: () -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MedicalMateTheme.colors.bgSurface)
            .padding(
                start = MedicalMateSize.gutter,
                end = MedicalMateSize.gutter,
                top = MedicalMateSpace.s12,
                bottom = MedicalMateSpace.s8,
            ),
    ) {
        MedicalMateButton(
            onClick = onDoneClick,
            label = stringResource(R.string.handoff_done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@MedicalMateScreenPreviews
@Composable
private fun HandoffScreenPreview() {
    MedicalMateTheme {
        HandoffScreen(card = previewBriefCard, onCloseClick = {}, onDoneClick = {})
    }
}
