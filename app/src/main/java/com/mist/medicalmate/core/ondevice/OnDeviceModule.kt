package com.mist.medicalmate.core.ondevice

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

/** 모델과 엔진은 하나만 둔다. 1GB 넘는 모델을 두 번 올릴 수 없다. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class OnDeviceModule {
    @Binds
    @Singleton
    abstract fun bindOnDeviceModel(impl: DefaultOnDeviceModel): OnDeviceModel

    @Binds
    abstract fun bindOnDeviceExtractor(impl: LlamaOnDeviceExtractor): OnDeviceExtractor
}
