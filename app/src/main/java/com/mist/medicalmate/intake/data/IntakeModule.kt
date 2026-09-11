package com.mist.medicalmate.intake.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object IntakeApiModule {
    @Provides
    @Singleton
    fun provideSessionApi(retrofit: Retrofit): SessionApi = retrofit.create(SessionApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class IntakeRepositoryModule {
    @Binds
    abstract fun bindSessionRepository(impl: DefaultSessionRepository): SessionRepository
}
