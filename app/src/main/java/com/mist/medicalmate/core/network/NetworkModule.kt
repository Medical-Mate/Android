package com.mist.medicalmate.core.network

import com.mist.medicalmate.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // 서버가 필드를 추가해도 앱이 깨지지 않게 한다.
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .addInterceptor(loggingInterceptor())
        // Render 무료 티어는 인스턴스가 잠들어 첫 요청이 수십 초 걸린다.
        .connectTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit
        .Builder()
        .baseUrl(BuildConfig.BACKEND_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /**
     * 본문을 절대 찍지 않는다. 증상·복용약·기저질환·알레르기가 요청 본문에 실리고,
     * 백엔드도 "예외 로깅에 요청 본문을 남기지 않는다"를 계약으로 두고 있다.
     * 릴리즈 빌드에서는 아예 끈다.
     */
    private fun loggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level =
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
    }

    private const val READ_TIMEOUT_SECONDS = 60L
}
