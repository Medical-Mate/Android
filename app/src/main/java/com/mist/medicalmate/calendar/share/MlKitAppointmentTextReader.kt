package com.mist.medicalmate.calendar.share

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.entityextraction.DateTimeEntity
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractor
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import jakarta.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlin.coroutines.resume

/**
 * ML Kit Entity Extraction으로 예약 문자를 읽는다(#270).
 *
 * 기기 안에서 돈다. 문자 내용이 서버로 나가지 않는다. 한국어 모델은 처음 쓸 때 한 번 내려받고,
 * 그때는 연결이 필요하다. 못 받으면 날짜를 비운 채 돌려준다 — 화면이 직접 고르라고 알린다.
 *
 * 날짜·시각만 추출기에 맡긴다. 병원 이름은 추출기가 다루는 종류가 아니어서 [hospitalIn]이 글
 * 모양으로 찾는다.
 *
 * 추출기는 부를 때마다 만들고 닫는다. 공유는 가끔 오는 일이라 모델을 메모리에 붙잡아 둘 이유가 없다.
 */
internal class MlKitAppointmentTextReader @Inject constructor() : AppointmentTextReader {
    override suspend fun read(text: String): SharedAppointment {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val (date, time) = pickSchedule(mentionsIn(text, now), now.toLocalDate())
        return SharedAppointment(hospital = hospitalIn(text), date = date, time = time)
    }

    private suspend fun mentionsIn(text: String, now: ZonedDateTime): List<DateTimeMention> {
        val extractor =
            EntityExtraction.getClient(EntityExtractorOptions.Builder(EntityExtractorOptions.KOREAN).build())
        try {
            if (extractor.downloadModelIfNeeded().outcome().isFailure) return emptyList()
            return annotate(extractor, text, now)
        } finally {
            extractor.close()
        }
    }

    private suspend fun annotate(extractor: EntityExtractor, text: String, now: ZonedDateTime): List<DateTimeMention> {
        // 기준 시각을 넘겨야 "내일"·"10월 15일"이 올해의 그 날로 풀린다.
        val params =
            EntityExtractionParams.Builder(text)
                .setEntityTypesFilter(setOf(Entity.TYPE_DATE_TIME))
                .setReferenceTime(now.toInstant().toEpochMilli())
                .setReferenceTimeZone(TimeZone.getTimeZone(now.zone))
                .build()
        val annotations = extractor.annotate(params).outcome().getOrNull().orEmpty()
        return annotations.flatMap { annotation ->
            annotation.entities.mapNotNull { entity ->
                entity.asDateTimeEntity()?.let { found ->
                    dateTimeMention(
                        start = annotation.start,
                        end = annotation.end,
                        timestampMillis = found.timestampMillis,
                        granularity = granularityOf(found.dateTimeGranularity),
                        zone = now.zone,
                    )
                }
            }
        }
    }
}

private fun granularityOf(value: Int): Granularity = when (value) {
    DateTimeEntity.GRANULARITY_DAY -> Granularity.DAY
    DateTimeEntity.GRANULARITY_HOUR,
    DateTimeEntity.GRANULARITY_MINUTE,
    DateTimeEntity.GRANULARITY_SECOND,
    -> Granularity.TIME
    else -> Granularity.COARSE
}

/**
 * [Task]가 끝나기를 기다려 결과를 값으로 받는다.
 *
 * 실패를 예외로 던지지 않는다. 모델을 못 받거나 추출이 실패해도 일정 추가는 빈 칸으로 열리면
 * 그만이라, 호출자가 `catch`를 둘 이유를 만들지 않는다.
 */
private suspend fun <T> Task<T>.outcome(): Result<T> = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val result =
            if (task.isSuccessful) {
                Result.success(task.result)
            } else {
                Result.failure(task.exception ?: IllegalStateException("cancelled"))
            }
        continuation.resume(result)
    }
}
