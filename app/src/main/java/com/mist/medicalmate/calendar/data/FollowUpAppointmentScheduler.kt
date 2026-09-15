package com.mist.medicalmate.calendar.data

import com.mist.medicalmate.core.model.FollowUpScheduler
import com.mist.medicalmate.core.network.ApiResult
import jakarta.inject.Inject
import java.time.LocalDate

/**
 * 재방문 날짜를 일정으로 만든다. [FollowUpScheduler]의 구현이다.
 *
 * **같은 카드로 그 날 일정이 이미 있으면 만들지 않는다.** 기록을 다시 저장하거나 같은 진료의
 * 재방문을 두 번 남기면 같은 일정이 둘이 된다. 취소한 일정은 세지 않는다 — 환자가 지운 것을
 * 되살리지는 않지만, 취소는 안 간 것이라 그 자리에 다시 잡을 수 있어야 한다.
 *
 * 시각은 비운다. 기록에서 나온 날짜는 "일주일 뒤"처럼 날짜까지이고, 시각은 예약하고 나서
 * 일자 화면에서 채운다(1r-2의 시간 미정 → 눌러서 고치기).
 *
 * 출처가 `VISIT_FOLLOW_UP`이다. 홈과 일자 화면이 이 값으로 "재진"을 적는다.
 */
internal class FollowUpAppointmentScheduler
@Inject
constructor(private val repository: AppointmentRepository) :
    FollowUpScheduler {
    override suspend fun schedule(clinic: String?, on: LocalDate, cardId: Long) {
        val clinicName = clinic?.takeIf { it.isNotBlank() } ?: return
        val existing = (repository.day(on) as? ApiResult.Success)?.value.orEmpty()
        val booked =
            existing.any { appointment ->
                appointment.status != AppointmentStatus.CANCELED && appointment.cards.any { it.id == cardId }
            }
        if (booked) return
        repository.create(
            NewAppointment(
                clinicName = clinicName,
                on = on,
                cardIds = listOf(cardId),
                origin = AppointmentOrigin.VISIT_FOLLOW_UP,
            ),
        )
    }
}
