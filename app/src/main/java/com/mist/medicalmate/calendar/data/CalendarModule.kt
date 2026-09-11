package com.mist.medicalmate.calendar.data

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
}
