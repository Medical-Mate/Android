package com.mist.medicalmate.core.designsystem.component

/**
 * DESIGN.md 5절의 Glass surface 여부.
 *
 * 콘텐츠 위에 떠 있는 면(Bottom CTA Bar, Nav Bar, Tab Bar)이 공유한다. 알파는 면마다
 * 달라서(78 / 82 / 86) 각 컴포넌트가 `MedicalMateGlass`에서 자기 값을 가져간다.
 *
 * [GLASS]에는 테두리를 추가하지 않는다. 층 구분은 고도가 담당한다.
 *
 * 블러는 API 31부터이고 compose-ui 1.10.5에 뒤 배경을 블러하는 API가 없다. 자세한
 * 사정과 기본값을 [MedicalMateBottomCtaBar]에 적어 두었다.
 */
enum class MedicalMateSurfaceStyle {
    OPAQUE,
    GLASS,
}
