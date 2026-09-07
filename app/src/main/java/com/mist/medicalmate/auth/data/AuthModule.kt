package com.mist.medicalmate.auth.data

import com.mist.medicalmate.core.network.AccessTokenProvider
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

    /** `core/network`가 `auth`를 직접 참조하지 않도록 인터페이스로 연결한다. */
    @Binds
    abstract fun bindAccessTokenProvider(impl: TokenStore): AccessTokenProvider
}
