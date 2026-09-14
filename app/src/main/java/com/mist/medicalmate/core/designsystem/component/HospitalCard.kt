package com.mist.medicalmate.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mist.medicalmate.core.designsystem.MedicalMateElevation
import com.mist.medicalmate.core.designsystem.MedicalMateIcons
import com.mist.medicalmate.core.designsystem.MedicalMateRadius
import com.mist.medicalmate.core.designsystem.MedicalMateSize
import com.mist.medicalmate.core.designsystem.MedicalMateSpace
import com.mist.medicalmate.core.designsystem.MedicalMateTheme
import com.mist.medicalmate.core.designsystem.ShadowTint

/**
 * 병원 카드에 붙는 날짜 칩. 지난 진료는 [PAST], 예정된 재방문은 [PLANNED]다.
 *
 * 지난 것과 예정된 것을 색으로만 나누지 않는다. 칩의 글자가 "진료"와 "재방문"으로 다르다.
 */
enum class MedicalMateHospitalChipTone {
    PAST,
    PLANNED,
}

/** 병원 카드 아래에 놓는 날짜 칩 하나. */
data class MedicalMateHospitalChip(val label: String, val tone: MedicalMateHospitalChipTone)

/**
 * Figma `Hospital Card`(`1129:9195`) 320x141.
 *
 * 병원 하나를 이름 · 주소 · 날짜 칩으로 보여준다. 진료 후 기록(1k)과 병원 찾기(1m-B)에서
 * 쓴다.
 *
 * [MedicalMateCard]를 쓰지 않는다. 그쪽은 여백 20에 한 덩어리인데, 이 카드는 위(정보)와
 * 아래(칩)를 구분선으로 나누고 여백도 18/14/18로 다르다.
 *
 * 칩은 여기 안에 있다. [MedicalMateChip]은 높이 40에 누를 수 있는 컨트롤이고, 이 칩은
 * 높이 28에 읽기만 하는 표시라 규격이 다르다.
 *
 * 아이콘 상자는 48에 반경 14다. 반경 14는 문서가 스케일 밖 값으로 잡고 `radius/md` 16
 * 통일안을 낸 값이다. Figma가 아직 14라서 마스터를 따랐다.
 *
 * 면색은 `bg/surface`다. 마스터에는 채움이 묶여 있지 않은데, 그림자와 반경만 있고 면이
 * 없으면 카드로 보이지 않는다. 디자인 트랙에 확인을 넘겼다.
 */
@Composable
fun MedicalMateHospitalCard(
    name: String,
    address: String? = null,
    modifier: Modifier = Modifier,
    chips: List<MedicalMateHospitalChip> = emptyList(),
) {
    val colors = MedicalMateTheme.colors
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = MedicalMateElevation.card,
                shape = MedicalMateRadius.lg,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            )
            .clip(MedicalMateRadius.lg)
            .background(colors.bgSurface),
    ) {
        Info(name = name, address = address)
        if (chips.isNotEmpty()) {
            MedicalMateDivider(modifier = Modifier.padding(horizontal = EdgePadding))
            Chips(chips = chips)
        }
    }
}

/**
 * 아이콘 상자와 이름·주소. 마스터의 `Info` 묶음이다.
 *
 * **주소가 없으면 그 줄을 그리지 않는다.** 마스터에는 두 줄이 있는데 병원 검색이 서버로
 * 옮겨지면서 주소를 받을 수 없게 됐다(#155). 빈 줄을 남기면 이름 아래가 비어 카드가 잘린
 * 것처럼 보인다. 그 자리를 무엇으로 할지는 디자인 트랙 확인 대기다.
 */
@Composable
private fun Info(name: String, address: String?) {
    val colors = MedicalMateTheme.colors
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(start = EdgePadding, end = EdgePadding, top = EdgePadding, bottom = MedicalMateSpace.s14),
        horizontalArrangement = Arrangement.spacedBy(IconGap),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
            Modifier
                .size(MedicalMateSize.controlMd)
                .background(color = colors.bgPrimaryFaint, shape = IconBoxShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(MedicalMateIcons.Hospital),
                contentDescription = null,
                tint = colors.fgPrimary,
                modifier = Modifier.size(MedicalMateSize.iconLg),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(TitleGap)) {
            // 마스터가 `Body/L Strong`이다. `Heading/S`는 같은 17이지만 자간이 -1이라
            // 병원 이름이 좁아 보인다.
            Text(text = name, style = MedicalMateTheme.typography.bodyLStrong, color = colors.fgDefault)
            if (!address.isNullOrBlank()) {
                Text(text = address, style = MedicalMateTheme.typography.bodyS, color = colors.fgSubtle)
            }
        }
    }
}

/** 날짜 칩 줄. 마스터의 `Chips` 묶음이다. */
@Composable
private fun Chips(chips: List<MedicalMateHospitalChip>) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(
                start = EdgePadding,
                end = EdgePadding,
                top = MedicalMateSpace.s14,
                bottom = EdgePadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(MedicalMateSpace.s8),
    ) {
        chips.forEach { chip -> DateChip(chip = chip) }
    }
}

@Composable
private fun DateChip(chip: MedicalMateHospitalChip) {
    val colors = MedicalMateTheme.colors
    val container =
        when (chip.tone) {
            MedicalMateHospitalChipTone.PAST -> colors.bgSubtle
            MedicalMateHospitalChipTone.PLANNED -> colors.bgPrimarySubtle
        }
    val content =
        when (chip.tone) {
            MedicalMateHospitalChipTone.PAST -> colors.fgSubtle
            MedicalMateHospitalChipTone.PLANNED -> colors.fgPrimary
        }
    Text(
        text = chip.label,
        style = MedicalMateTheme.typography.labelM,
        color = content,
        modifier =
        Modifier
            .background(color = container, shape = MedicalMateRadius.full)
            .padding(horizontal = MedicalMateSpace.s10, vertical = ChipVerticalPadding),
    )
}

/** 마스터의 카드 안쪽 여백 18. 위·좌우·아래 끝에 같은 값이 쓰인다. */
private val EdgePadding = 18.dp

/** 마스터의 아이콘 상자와 글자 사이 13. 스케일에 없는 값이라 디자인 트랙에 넘겼다. */
private val IconGap = 13.dp

/** 마스터의 이름·주소 사이 3. 문서가 `space/4` 통일안을 낸 값이다. */
private val TitleGap = 3.dp

/** 마스터의 아이콘 상자 반경 14. 문서가 `radius/md` 16 통일안을 낸 값이다. */
private val IconBoxShape = RoundedCornerShape(14.dp)

/** 마스터의 칩 세로 여백 5. 문서가 `space/6` 통일안을 낸 값이다. */
private val ChipVerticalPadding = 5.dp
