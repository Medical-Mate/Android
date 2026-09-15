package com.mist.medicalmate.calendar.data

import com.mist.medicalmate.core.model.FollowUpScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object CalendarApiModule {
    @Provides
    @Singleton
    fun provideAppointmentApi(retrofit: Retrofit): AppointmentApi = retrofit.create(AppointmentApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CalendarRepositoryModule {
    @Binds
    abstract fun bindAppointmentRepository(impl: DefaultAppointmentRepository): AppointmentRepository

    /** `visit`이 기록을 저장한 뒤 재방문 일정을 만들 때 쓴다. 도메인이 서로 참조하지 않게 `core`로 받는다. */
    @Binds
    abstract fun bindFollowUpScheduler(impl: FollowUpAppointmentScheduler): FollowUpScheduler
}
