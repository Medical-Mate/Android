package com.mist.medicalmate.visit.data

import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.core.network.map
import com.mist.medicalmate.visit.ui.Hospital
import jakarta.inject.Inject
import kotlinx.serialization.json.Json

/** 병원 찾기. */
interface HospitalRepository {
    /**
     * 이름으로 찾는다.
     *
     * 빈 검색어로는 부르지 않는다. 서버가 `q`를 필수로 두고, 그 전에 부분 일치라 무엇이든
     * 받으면 수천 건이 온다.
     */
    suspend fun search(query: String): ApiResult<HospitalSearchResult>
}

/**
 * 찾은 결과.
 *
 * [total]은 조건에 맞는 전체 건수다. [hospitals]보다 클 수 있고, 그때는 검색어를 더 좁혀야
 * 한다는 뜻이다. 화면이 그것을 알려야 사용자가 목록 끝까지 훑다 포기하지 않는다.
 */
data class HospitalSearchResult(val hospitals: List<Hospital>, val total: Int) {
    /** 받은 것보다 더 있는지. */
    val truncated: Boolean get() = total > hospitals.size
}

internal class DefaultHospitalRepository
@Inject
constructor(private val api: HospitalApi, private val json: Json) :
    HospitalRepository {
    override suspend fun search(query: String): ApiResult<HospitalSearchResult> =
        apiCall(json) { api.hospitals(query = query.trim(), size = PAGE_SIZE) }
            .map { response ->
                HospitalSearchResult(
                    hospitals = response.hospitals.map { Hospital(name = it.name) },
                    total = response.totalCount,
                )
            }

    private companion object {
        /**
         * 한 번에 받는 수.
         *
         * 고르는 화면이라 훑을 수 있는 만큼만 받는다. 부분 일치라 "서울"이면 4천 건이 넘는데,
         * 그때 필요한 것은 더 받는 것이 아니라 검색어를 좁히는 것이다.
         */
        const val PAGE_SIZE = 20
    }
}
