package com.mist.medicalmate.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.profile.data.OnboardingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 온보딩을 이미 마쳤는지 알려준다.
 *
 * 서버의 `onboardingRequired`만으로는 "가입하고 한 번"이 되지 않는다. 이유는
 * [OnboardingStore]에 적었다.
 *
 * 화면에 붙지 않고 앱 전체의 판단에 쓰이므로 `MainActivity`에서 Activity 스코프로 둔다.
 * 세션과 같은 성격이다.
 *
 * `null`은 아직 읽는 중이라는 뜻이다. false로 시작하면 온보딩을 마친 사람도 한 프레임
 * 동안 온보딩 대상이 되어 화면이 잠깐 스친다. 호출자는 `null`인 동안 스플래시를 유지한다.
 */
@HiltViewModel
class OnboardingGateViewModel
@Inject
constructor(private val onboardingStore: OnboardingStore) : ViewModel() {
    private val mutableCompleted = MutableStateFlow<Boolean?>(null)
    val completed: StateFlow<Boolean?> = mutableCompleted.asStateFlow()

    init {
        viewModelScope.launch {
            mutableCompleted.value = onboardingStore.completed.first()
        }
    }

    /**
     * 온보딩을 마쳤다고 남긴다.
     *
     * 기록이 저장되기 전에 화면을 옮기면 다시 온보딩으로 돌아갈 수 있으므로 메모리 값을
     * 먼저 올린다. 저장은 이어서 한다.
     */
    fun markCompleted() {
        mutableCompleted.value = true
        viewModelScope.launch { onboardingStore.markCompleted() }
    }
}
