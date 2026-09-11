package com.mist.medicalmate.home.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object HomeApiModule {
    @Provides
    @Singleton
    fun provideHomeApi(retrofit: Retrofit): HomeApi = retrofit.create(HomeApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class HomeRepositoryModule {
    @Binds
    abstract fun bindHomeRepository(impl: DefaultHomeRepository): HomeRepository
}
