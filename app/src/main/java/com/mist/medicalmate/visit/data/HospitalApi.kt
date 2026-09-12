package com.mist.medicalmate.visit.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 병원 검색 API. `/v3/api-docs`의 `GET /api/hospitals`.
 *
 * 원천은 심평원 병원정보서비스이고 서버가 목록을 들고 있지 않다. 개원·폐원이 계속 생기는
 * 데이터라 복사해 두면 바로 낡는다는 것이 서버 쪽 설명이다. 그래서 앱도 캐시하지 않는다.
 *
 * **부분 일치다.** "서울"로 4천 건이 넘게 나온다.
 */
internal interface HospitalApi {
    @GET("api/hospitals")
    suspend fun hospitals(@Query("q") query: String, @Query("size") size: Int): HospitalSearchResponse
}

/**
 * @param url 홈페이지. **없는 병원이 많다.** 작은 의원은 대부분 비어 있고 null이 정상이다.
 */
@Serializable
internal data class HospitalResponse(val name: String, val url: String? = null)

/** @param totalCount 조건에 맞는 전체 건수. 받은 목록보다 클 수 있다. */
@Serializable
internal data class HospitalSearchResponse(
    val hospitals: List<HospitalResponse> = emptyList(),
    val totalCount: Int = 0,
)
