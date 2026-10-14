package com.mist.medicalmate.core.ondevice

/**
 * `ondevice.cpp`의 JNI 함수들.
 *
 * 엔진([OnDeviceEngine])에서 떼어 둔다. 엔진이 하는 일(언제 열고, 언제 올리고 내리는지)과
 * C 쪽에 무엇을 넘기는지는 따로 읽히는 편이 낫다. 함수 이름이 C 쪽 심볼과 묶여 있어 이 객체의
 * 이름과 자리를 바꾸면 `ondevice.cpp`도 함께 바꿔야 한다.
 *
 * 글은 UTF-8 바이트로 주고받는다. JNI 문자열은 이모지를 쪼개 토크나이저와 출력을 깨뜨린다.
 */
internal object OnDeviceNative {
    @JvmStatic
    external fun nativeOpen(nativeLibDir: String, htpDir: String)

    @JvmStatic
    external fun nativeVersion(): String

    @JvmStatic
    external fun nativeDevices(): String

    /** 모델을 올린다. 실패하면 0이다. */
    @JvmStatic
    external fun nativeLoad(modelPath: String): Long

    /** 한 번 생성한다. 실패하면 null이다. */
    @JvmStatic
    external fun nativeGenerate(
        handle: Long,
        prefix: ByteArray,
        suffix: ByteArray,
        grammar: ByteArray,
        maxTokens: Int,
    ): ByteArray?

    @JvmStatic
    external fun nativeFree(handle: Long)
}
