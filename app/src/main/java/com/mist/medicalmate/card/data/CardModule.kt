package com.mist.medicalmate.card.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object CardApiModule {
    @Provides
    @Singleton
    fun provideCardApi(retrofit: Retrofit): CardApi = retrofit.create(CardApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CardRepositoryModule {
    @Binds
    abstract fun bindCardRepository(impl: DefaultCardRepository): CardRepository
}
