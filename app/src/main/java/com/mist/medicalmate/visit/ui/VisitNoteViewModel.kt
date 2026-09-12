package com.mist.medicalmate.visit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/**
 * 진료 후 메모 상태 보유자.
 *
 * 맨 위 카드가 **어떤 진료를 적는 것인지**를 말한다. 병원은 바로 앞 화면(1m)에서 고른 곳,
 * 카드 제목은 이 기록이 붙을 카드다. 둘 다 라우트를 타고 따라오므로 서버를 다시 부르지
 * 않는다.
 *
 * [onOrganizeClick]이 AI에게 문장을 다듬어 달라고 하는 자리다. 지금은 픽스처를 넣고
 * [VisitNoteUiState.organizing] 동안 입력을 잠근다. 다듬는 중에 환자가 계속 적으면 결과가
 * 덮어써서 방금 적은 문장이 사라진다.
 *
 * 원문을 지우지 않는다. 다듬은 결과가 마음에 들지 않을 수 있고, 1q-1이 원문을 그대로
 * 보여주므로 흐름 끝까지 남아 있어야 한다.
 */
@HiltViewModel
class VisitNoteViewModel
@Inject
constructor(private val clock: Clock) : ViewModel() {
    private val mutableUiState = MutableStateFlow(VisitNoteUiState(visit = VisitHeadline(LocalDate.now(clock))))
    val uiState: StateFlow<VisitNoteUiState> = mutableUiState.asStateFlow()

    /**
     * 무엇을 적는 진료인지 채운다.
     *
     * 적던 글은 건드리지 않는다. 화면이 다시 조합돼 이 호출이 한 번 더 오더라도 그 글은
     * 남아야 한다.
     */
    fun load(clinic: String?, cardTitle: String?) {
        mutableUiState.update {
            it.copy(visit = VisitHeadline(visitedOn = LocalDate.now(clock), clinic = clinic, cardTitle = cardTitle))
        }
    }

    fun onNoteChange(note: String) {
        mutableUiState.update { it.copy(note = note) }
    }

    fun onOrganizeClick() {
        if (mutableUiState.value.organizing) return
        mutableUiState.update { it.copy(organizing = true) }
        viewModelScope.launch {
            delay(ORGANIZE_DELAY_MILLIS)
            mutableUiState.update { it.copy(note = PREVIEW_VISIT_NOTE, organizing = false) }
        }
    }

    companion object {
        /** AI 응답을 기다리는 것처럼 보이게 두는 시간. 연동하면 사라진다. */
        private const val ORGANIZE_DELAY_MILLIS = 700L
    }
}
