package com.mist.medicalmate.auth.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object AuthApiModule {
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthRepositoryModule {
    @Binds
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository
}
