package com.mist.medicalmate.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * DESIGN.md 4.2 반경 7개.
 *
 * 같은 화면에서 반경 단계를 두 단계 이상 건너뛰지 않는다.
 *
 * 타입이 `RoundedCornerShape`인 이유는 Material3 `Shapes`가 `CornerBasedShape`를
 * 요구하기 때문이다. `Shape`로 두면 `Shapes` 생성자를 호출할 수 없다.
 *
 * 문서 8.1의 Button은 L/M/S에 16/14/12를 쓰는데 14는 4.2 Scale에 없는 값이다. 중간
 * 단계가 필요하면 [buttonM]처럼 용도를 밝혀 두고, 표에 추가할지는 디자인 트랙과 정한다.
 */
object MedicalMateRadius {
    /** Badge, Checkbox */
    val xs: RoundedCornerShape = RoundedCornerShape(8.dp)

    /** S Button, 원문 블록 */
    val sm: RoundedCornerShape = RoundedCornerShape(12.dp)

    /** L Button, Notice */
    val md: RoundedCornerShape = RoundedCornerShape(16.dp)

    /** Card, Callout */
    val lg: RoundedCornerShape = RoundedCornerShape(20.dp)

    /** Dialog */
    val xl: RoundedCornerShape = RoundedCornerShape(24.dp)

    /** Bottom Sheet 전체 반경. 실제 시트는 위쪽만 둥글어 [sheetTop]을 쓴다. */
    val xxl: RoundedCornerShape = RoundedCornerShape(28.dp)

    /** Chip, Avatar, pill */
    val full: RoundedCornerShape = CircleShape

    /** M Button 전용. 4.2 Scale에 없는 중간값이다. */
    val buttonM: RoundedCornerShape = RoundedCornerShape(14.dp)

    /** Bottom Sheet. 위쪽 두 각만 28. */
    val sheetTop: RoundedCornerShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

/**
 * Material3 슬롯에 얹은 반경.
 *
 * M3 컴포넌트가 내부에서 `MaterialTheme.shapes`를 읽는다. 비워 두면 M3 기본값이 남는다.
 * 슬롯이 5개라 7단계를 다 담지 못하므로 가까운 단계로 맞췄다. `radius/full`과 중간값은
 * [MedicalMateRadius]에만 있다.
 */
internal val MedicalMateShapes =
    Shapes(
        extraSmall = MedicalMateRadius.xs,
        small = MedicalMateRadius.sm,
        medium = MedicalMateRadius.md,
        large = MedicalMateRadius.lg,
        extraLarge = MedicalMateRadius.xl,
    )
