package com.mist.medicalmate.profile.data

import com.mist.medicalmate.core.model.CurrentUserProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object ProfileApiModule {
    @Provides
    @Singleton
    fun provideHealthProfileApi(retrofit: Retrofit): HealthProfileApi = retrofit.create(HealthProfileApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProfileRepositoryModule {
    /** 홈이 `profile`을 직접 참조하지 않도록 `core`의 인터페이스로 연결한다. */
    @Binds
    abstract fun bindCurrentUserProvider(impl: HealthProfileRepository): CurrentUserProvider
}
