package com.mist.medicalmate.core.time

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import java.time.Clock

/**
 * 날짜 계산의 기준 시계.
 *
 * "지난 진료 후 12일"과 D-day가 오늘이 며칠인지에 달려 있다. ViewModel이 `LocalDate.now()`를
 * 직접 부르면 그 계산을 JVM에서 검증할 수 없어서 주입으로 받는다.
 *
 * 기기 시간대를 쓴다. 서버가 날짜를 `yyyy-MM-dd`로 주고 D-day는 사용자가 있는 곳의
 * 자정을 기준으로 세는 것이 맞다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object ClockModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
