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
    fun provideOkHttpClient(authInterceptor: AuthInterceptor, tokenAuthenticator: TokenAuthenticator): OkHttpClient =
        baseClientBuilder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()

    /**
     * 인증을 붙이지 않는 클라이언트. 토큰 재발급이 쓴다.
     *
     * 인터셉터도 Authenticator도 없다. 이유는 [AuthFree]에 적었다.
     */
    @Provides
    @Singleton
    @AuthFree
    fun provideAuthFreeOkHttpClient(): OkHttpClient = baseClientBuilder().build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    @AuthFree
    fun provideAuthFreeRetrofit(@AuthFree client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit
        .Builder()
        .baseUrl(BuildConfig.BACKEND_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private fun baseClientBuilder(): OkHttpClient.Builder = OkHttpClient
        .Builder()
        .addInterceptor(loggingInterceptor())
        // Render 무료 티어는 인스턴스가 잠들어 첫 요청의 **응답**이 수십 초 걸린다.
        // 연결 자체는 그와 무관하게 빨리 되거나 안 된다. 연결을 60초 기다리면 비행기
        // 모드나 잘못된 주소 같은 경우에 앱이 그만큼 멈춘 것처럼 보인다.
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)

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

    /** 응답을 기다리는 시간. 잠든 인스턴스가 깨는 데 걸리는 시간을 감당해야 한다. */
    private const val READ_TIMEOUT_SECONDS = 60L

    /** 연결을 기다리는 시간. 연결은 되거나 안 되거나이고 오래 걸릴 이유가 없다. */
    private const val CONNECT_TIMEOUT_SECONDS = 10L
}
