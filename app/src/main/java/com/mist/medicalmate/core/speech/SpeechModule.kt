package com.mist.medicalmate.core.speech

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

/**
 * 음성 인식은 하나만 둔다.
 *
 * 인식기가 기기 자원을 잡고 있어서 화면마다 만들면 두 화면을 오갈 때 남는다. 화면은 한 번에
 * 하나만 듣는다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SpeechModule {
    @Binds
    @Singleton
    abstract fun bindSpeechToText(impl: MlKitSpeechToText): SpeechToText
}
