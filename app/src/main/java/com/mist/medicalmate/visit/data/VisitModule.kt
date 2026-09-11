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
internal object VisitApiModule {
    @Provides
    @Singleton
    fun provideVisitApi(retrofit: Retrofit): VisitApi = retrofit.create(VisitApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class VisitRepositoryModule {
    @Binds
    abstract fun bindVisitRepository(impl: DefaultVisitRepository): VisitRepository
}
