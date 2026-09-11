package com.mist.medicalmate.profile.data

import com.mist.medicalmate.core.model.CurrentUserProvider
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * 건강 프로필 읽기.
 *
 * 지금 쓰는 곳은 홈 헤더의 이름 한 글자다. 문답 시작을 막는 `canStartIntake`도 여기서
 * 오지만, 홈의 시작 버튼은 시안에서 언제나 눌리므로 아직 읽지 않는다.
 *
 * 저장은 신상정보 화면(1b)을 옮길 때 붙인다. 서버가 단계별 저장을 주지 않아 그 화면의
 * 임시저장을 어떻게 할지 먼저 정해야 한다(#137).
 */
@Singleton
class HealthProfileRepository
@Inject
internal constructor(private val api: HealthProfileApi, private val json: Json) :
    CurrentUserProvider {
    override suspend fun displayName(): ApiResult<String?> = when (val result = apiCall(json) { api.healthProfile() }) {
        is ApiResult.Success -> ApiResult.Success(result.value.name?.trim()?.takeIf { it.isNotEmpty() })
        is ApiResult.Rejected -> result
        is ApiResult.NetworkUnavailable -> result
    }
}
