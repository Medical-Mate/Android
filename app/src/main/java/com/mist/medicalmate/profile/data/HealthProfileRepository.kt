package com.mist.medicalmate.profile.data

import com.mist.medicalmate.core.model.CurrentUserProvider
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.network.apiCall
import com.mist.medicalmate.core.network.map
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * 건강 프로필 읽고 쓰기.
 *
 * 저장은 통째로 덮어쓴다. 서버가 부분 갱신을 주지 않고 여섯 필드를 모두 요구한다. 1b와
 * 1s-2가 묻는 것은 그중 셋뿐이라 [save]가 나머지를 [HealthProfile]에서 받아 함께 보낸다.
 * 읽은 값을 그대로 되돌려 보내는 자리라 화면이 들고 있을 값이 아니다.
 *
 * 홈이 쓰는 [CurrentUserProvider]를 함께 구현한다. 같은 응답에서 이름을 꺼내는 일이라
 * 호출을 두 벌로 두지 않는다.
 */
interface HealthProfileRepository : CurrentUserProvider {
    suspend fun profile(): ApiResult<HealthProfile>

    /**
     * 세 갈래를 한 번에 저장한다.
     *
     * 이름·출생연도·성별이 없으면 부르지 않고 null을 돌려준다. 서버가 셋을 필수로 두고
     * 있어 없는 채로 보내면 400이고, 그 셋은 카카오에서 채워져 오는 값이라 화면에서 받을
     * 자리가 없다. [ApiResult.Rejected]가 아닌 이유는 서버에 물어보지도 않았기 때문이다.
     */
    suspend fun save(base: HealthProfile, health: HealthEdit): ApiResult<HealthProfile>?
}

@Singleton
internal class DefaultHealthProfileRepository
@Inject
constructor(
    private val api: HealthProfileApi,
    private val json: Json,
) : HealthProfileRepository {
    override suspend fun displayName(): ApiResult<String?> =
        profile().map { it.name?.trim()?.takeIf(String::isNotEmpty) }

    override suspend fun profile(): ApiResult<HealthProfile> =
        apiCall(json) { api.healthProfile() }.map { it.toProfile() }

    override suspend fun save(base: HealthProfile, health: HealthEdit): ApiResult<HealthProfile>? {
        val name = base.name
        val birthYear = base.birthYear
        val sex = base.sex
        if (name == null || birthYear == null || sex == null) return null

        val request =
            HealthProfileRequest(
                name = name,
                birthYear = birthYear,
                birthMonthDay = base.birthMonthDay,
                sex = sex,
                medications = health.medications.toListField(),
                conditions = health.conditions.toListField(),
                allergies = health.allergies.toTextField(),
            )
        return apiCall(json) { api.save(request) }.map { it.toProfile() }
    }
}

/**
 * 서버에 있는 건강 프로필.
 *
 * [name]·[birthYear]·[sex]는 카카오에서 온다. 저장할 때 그대로 되돌려 보내야 해서 화면이
 * 쓰지 않아도 들고 있는다.
 */
data class HealthProfile(
    val name: String?,
    val birthYear: Int?,
    val birthMonthDay: String?,
    val sex: String?,
    val health: HealthEdit,
    val onboardingCompleted: Boolean,
    val canStartIntake: Boolean,
)

/** 화면이 고치는 세 갈래. */
data class HealthEdit(
    val medications: HealthField = HealthField(),
    val conditions: HealthField = HealthField(),
    val allergies: HealthField = HealthField(),
)

/**
 * 한 갈래의 답.
 *
 * **비어 있는 것을 "없다"로 보지 않는다.** 서버는 `NONE`("없다")과 `UNKNOWN`("모른다")을
 * 가르는데 화면에는 그 둘을 말할 칩이 없다. 아무것도 고르지 않고 넘어간 것이 "없어요"인지
 * "그냥 넘겼어요"인지 알 수 없어서 [UNKNOWN][HealthStatus.UNKNOWN]으로 보낸다. 알러지에서
 * "없음"과 "모름"은 처방이 달라지는 값이라 단정하면 안 된다.
 *
 * 시안에는 "잘 모르겠어요 · 없어요" 보조 버튼이 있는데 우리가 뺐다(#67). 그 판단을 다시
 * 봐야 한다. 다만 그 버튼을 그대로 넣어도 한 개에 두 뜻이 묶여 있어 `NONE`과 `UNKNOWN`이
 * 갈리지 않는다. 디자인 트랙에 올렸다(#150).
 */
data class HealthField(
    val items: List<String> = emptyList(),
    /**
     * 서버가 말한 상태. 읽어 온 값에는 그대로 들어 있고, 화면이 고친 값에는 [items]에서
     * 가늠한 것이 들어간다.
     *
     * 그래서 서버에 `NONE`으로 있던 갈래를 손대지 않고 저장하면 `UNKNOWN`으로 내려간다.
     * 화면에 "없어요"를 말할 자리가 없어서 생기는 일이고, 그 자리를 만드는 것이 #150의
     * 디자인 확인 대기 항목이다.
     */
    val status: HealthStatus = if (items.isEmpty()) HealthStatus.UNKNOWN else HealthStatus.KNOWN,
)

enum class HealthStatus { KNOWN, NONE, UNKNOWN }

private fun HealthField.toListField() = ListFieldRequest(
    status = status.name,
    items = items.map { it.take(ITEM_MAX_LENGTH) }.take(ITEM_MAX_COUNT),
)

/**
 * 알러지는 한 줄이다.
 *
 * 화면은 셋 다 칩으로 받는데 서버가 알러지만 문자열 하나를 받는다. 고른 것을 쉼표로 잇는다.
 */
private fun HealthField.toTextField() = TextFieldRequest(
    status = status.name,
    text = items.joinToString(", ").take(TEXT_MAX_LENGTH),
)

private fun ListFieldResponse?.toField() = HealthField(
    items = this?.items.orEmpty(),
    status = statusOf(this?.status, this?.items.orEmpty()),
)

/**
 * 알러지는 한 줄로 와서 다시 칩으로 나눈다. 저장할 때 쉼표로 이어 보낸 그대로다.
 */
private fun TextFieldResponse?.toField(): HealthField {
    val items = this?.text.orEmpty().split(",").map(String::trim).filter(String::isNotEmpty)
    return HealthField(items = items, status = statusOf(this?.status, items))
}

/** 서버가 상태를 말하지 않았으면 값이 있는지로 가늠한다. */
private fun statusOf(status: String?, items: List<String>): HealthStatus =
    HealthStatus.entries.firstOrNull { it.name == status }
        ?: if (items.isEmpty()) HealthStatus.UNKNOWN else HealthStatus.KNOWN

private fun HealthProfileResponse.toProfile() = HealthProfile(
    name = name?.trim()?.takeIf(String::isNotEmpty),
    birthYear = birthYear,
    birthMonthDay = birthMonthDay,
    sex = sex,
    health =
    HealthEdit(
        medications = medications.toField(),
        conditions = conditions.toField(),
        allergies = allergies.toField(),
    ),
    onboardingCompleted = onboardingCompleted,
    canStartIntake = canStartIntake,
)

/** 서버가 막는 길이. 넘겨 보내면 400이라 잘라서 보낸다. */
private const val ITEM_MAX_LENGTH = 50

private const val ITEM_MAX_COUNT = 20

private const val TEXT_MAX_LENGTH = 200
