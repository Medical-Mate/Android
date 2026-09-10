package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme

/** DESIGN.md의 `Doctor Card`의 `State` variant. */
enum class MedicalMateDoctorCardState {
    AVAILABLE,
    CLOSED,
}

/**
 * DESIGN.md의 `Doctor Card`.
 *
 * 닫힘 상태에서도 정보는 그대로 유지하고 가능 시각 줄만 가라앉힌다. 이름과 진료과를 함께
 * 흐리면 환자가 어느 병원을 봤는지 기억하지 못한다.
 *
 * [hours]만 색이 바뀌므로 상태를 색으로만 전달하지 않도록 [statusLabel]을 함께 받는다.
 * 문서의 D11이 상태를 색만으로 알리지 말라고 한다.
 */
@Composable
fun MedicalMateDoctorCard(
    name: String,
    specialty: String,
    hours: String,
    statusLabel: String,
    modifier: Modifier = Modifier,
    state: MedicalMateDoctorCardState = MedicalMateDoctorCardState.AVAILABLE,
    onClick: (() -> Unit)? = null,
) {
    val colors = MedicalMateTheme.colors
    val typography = MedicalMateTheme.typography
    val closed = state == MedicalMateDoctorCardState.CLOSED

    MedicalMateCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedicalMateAvatar(
                initial = name,
                type = MedicalMateAvatarType.DOCTOR,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MedicalMateSpace.s4),
            ) {
                Text(text = name, style = typography.headingS, color = colors.fgDefault)
                Text(text = specialty, style = typography.bodyM, color = colors.fgSubtle)
                Text(
                    text = hours,
                    style = typography.bodyS,
                    // 닫힘일 때 이 줄만 가라앉힌다.
                    color = if (closed) colors.fgMuted else colors.fgSuccess,
                )
            }
            MedicalMateBadge(
                label = statusLabel,
                tone =
                if (closed) {
                    MedicalMateBadgeTone.NEUTRAL
                } else {
                    MedicalMateBadgeTone.SUCCESS
                },
            )
        }
    }
}
