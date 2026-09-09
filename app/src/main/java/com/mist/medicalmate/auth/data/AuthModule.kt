package com.mist.medicalmate.auth.data

import com.mist.medicalmate.core.network.AccessTokenProvider
import com.mist.medicalmate.core.network.AuthFree
import com.mist.medicalmate.core.network.TokenRefresher
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

    /**
     * 토큰 재발급 전용 API.
     *
     * 인증을 붙이지 않는 경로로 만든다. 로그인과 재발급은 서버에서도 열려 있어 헤더가
     * 필요 없고, 재발급 호출이 `TokenAuthenticator`를 타면 401에서 자기를 다시 부른다.
     */
    @Provides
    @Singleton
    @AuthFree
    fun provideAuthFreeAuthApi(@AuthFree retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthRepositoryModule {
    @Binds
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository

    /** `core/network`가 `auth`를 직접 참조하지 않도록 인터페이스로 연결한다. */
    @Binds
    abstract fun bindAccessTokenProvider(impl: TokenStore): AccessTokenProvider

    @Binds
    abstract fun bindTokenRefresher(impl: DefaultTokenRefresher): TokenRefresher
}
