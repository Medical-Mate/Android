package com.mist.medicalmate.core.model

/**
 * 온보딩에 적은 복용약·기저질환·알러지.
 *
 * 증상 정리가 폰에서 질문 후보를 만들 때 카드와 함께 넣는다(#300). 값은 `profile` 도메인의 건강
 * 프로필에 있어서, `intake`가 그쪽을 직접 부르지 않도록 `core`에 인터페이스를 두고 Hilt가
 * 연결한다. [CurrentUserProvider]와 같은 방식이다.
 *
 * 목록이 비어 있으면 "없음"이 아니라 확인되지 않은 것이다. 온보딩이 둘을 가르지 못한다.
 */
fun interface PatientHealthProvider {
    /** 못 읽으면 null이다. 부르는 쪽은 비어 있는 것과 같게 다룬다. */
    suspend fun patientHealth(): PatientHealth?
}

data class PatientHealth(
    val medications: List<String> = emptyList(),
    val conditions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
)
