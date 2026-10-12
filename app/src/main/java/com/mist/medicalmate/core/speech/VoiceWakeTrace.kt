package com.mist.medicalmate.core.speech

import android.util.Log
import com.mist.medicalmate.BuildConfig
import jakarta.inject.Inject

/**
 * 음성 호출이 무엇을 들었는지 남긴다(#278).
 *
 * 기기에서 "헤이 메이트"가 안 먹힐 때 어디서 막혔는지 가르려고 둔다. 듣기 조건이 빠져 감지기가
 * 안 돈 것인지, 돌았는데 못 들은 것인지, 다른 낱말로 받아쓴 것인지, 신뢰도가 모자란 것인지.
 *
 * **debug 빌드에서만 남긴다.** 받아쓴 말이 그대로 찍힌다. 진료 이야기가 logcat에 남으면 안
 * 되므로 release에서는 문장을 만들지도 않는다 — 그래서 문자열이 아니라 람다를 받는다.
 *
 * 인터페이스로 둔 것은 ViewModel 시험 때문이다. JVM에서 `android.util.Log`를 부르면 시험이
 * 깨진다.
 *
 * 보는 법: `adb logcat -s VoiceWake`
 */
fun interface VoiceWakeTrace {
    fun log(message: () -> String)
}

internal class LogcatVoiceWakeTrace
@Inject
constructor() : VoiceWakeTrace {
    override fun log(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message())
    }

    private companion object {
        const val TAG = "VoiceWake"
    }
}
