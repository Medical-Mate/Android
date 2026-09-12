package com.mist.medicalmate.visit.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object HospitalApiModule {
    @Provides
    @Singleton
    fun provideHospitalApi(retrofit: Retrofit): HospitalApi = retrofit.create(HospitalApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class HospitalRepositoryModule {
    @Binds
    abstract fun bindHospitalRepository(impl: DefaultHospitalRepository): HospitalRepository
}
