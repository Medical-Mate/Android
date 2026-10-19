package com.mist.medicalmate.intake.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 3D 인체도에 띄울 모델(#330).
 *
 * 디자인 트랙이 새 모델 BX05(`body3d-bx05-20261015`)를 보냈다. 지금 모델과 정점 수 · 크기가 같은 몸체이고
 * 재질만 다르다. 그래서 부위 점([bodyMap3dAnchors])과 짚는 판정(`body_collision.bin`)을 함께 쓰고 그리는 파일만
 * 바꾼다. 둘을 같은 화면에서 번갈아 보려고 판 위에 debug 빌드에서만 보이는 토글을 둔다(사용자 결정, 시안에 없다).
 * 고르면 하나만 남기고 이 파일과 토글을 걷는다.
 */
internal enum class BodyMap3dModel(val asset: String) {
    CURRENT(BODY_3D_MODEL_ASSET),
    BX05(BODY_3D_BX05_MODEL_ASSET),
}

/**
 * 지금 고른 모델. 앱이 떠 있는 동안만 기억한다 — 비교하는 동안 화면을 오가도 같은 모델이 뜨면 충분하다.
 * release는 토글이 없어 늘 [BodyMap3dModel.CURRENT]다.
 */
internal object BodyMap3dModelChoice {
    var current by mutableStateOf(BodyMap3dModel.CURRENT)
}
