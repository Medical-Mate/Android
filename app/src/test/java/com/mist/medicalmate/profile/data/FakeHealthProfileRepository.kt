package com.mist.medicalmate.profile.data

import com.mist.medicalmate.core.network.ApiResult
import java.io.IOException

/**
 * 건강 프로필 저장소 대역.
 *
 * 저장 요청은 [saved]에 남긴다. 화면이 고른 것을 어떤 모양으로 보내는지가 관심사다.
 * [base]는 저장할 때 그대로 되돌려 보내야 하는 값이라 함께 확인한다.
 */
internal class FakeHealthProfileRepository(
    private val read: ApiResult<HealthProfile> = ApiResult.Success(PROFILE),
    private val written: ApiResult<HealthProfile>? = ApiResult.Success(PROFILE),
) : HealthProfileRepository {
    var saveCount = 0
    var base: HealthProfile? = null
    var saved: HealthEdit? = null

    override suspend fun displayName(): ApiResult<String?> = ApiResult.Success(PROFILE.name)

    override suspend fun profile(): ApiResult<HealthProfile> = read

    override suspend fun save(base: HealthProfile, health: HealthEdit): ApiResult<HealthProfile>? {
        saveCount += 1
        this.base = base
        saved = health
        return written
    }

    companion object {
        val PROFILE =
            HealthProfile(
                name = "김OO",
                birthYear = 1994,
                birthMonthDay = "03-12",
                sex = "FEMALE",
                health = HealthEdit(),
                onboardingCompleted = false,
                canStartIntake = true,
            )

        /** 연결이 없는 경우. 저장이 나가지 않았는지 보는 데 쓴다. */
        val OFFLINE = ApiResult.NetworkUnavailable(IOException("offline"))
    }
}
