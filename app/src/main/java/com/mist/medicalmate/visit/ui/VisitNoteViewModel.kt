package com.mist.medicalmate.visit.ui

import androidx.lifecycle.ViewModel
import com.mist.medicalmate.core.designsystem.component.MedicalMateVoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate

/**
 * 진료 후 메모 상태 보유자.
 *
 * 맨 위 카드가 **어떤 진료를 적는 것인지**를 말한다. 병원은 바로 앞 화면(1m)에서 고른 곳,
 * 카드 제목은 이 기록이 붙을 카드다. 둘 다 라우트를 타고 따라오므로 서버를 다시 부르지
 * 않는다.
 *
 * **메모를 나누는 일은 여기서 하지 않는다**(#183). "AI로 정리하기" 칩과 하단 저장하기가 둘 다
 * 1q-1로 보내고, `POST /api/visits/classify`는 결과를 그리는 그 화면이 부른다. 축 맵을 라우트에
 * 실어 나르지 않아도 된다.
 *
 * 원문을 지우지 않는다. 1q-1이 원문을 그대로 보여주므로 흐름 끝까지 남아 있어야 한다.
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
    fun load(clinic: String?, cardTitle: String?, visitedOn: LocalDate?) {
        val today = LocalDate.now(clock)
        mutableUiState.update {
            it.copy(
                // 흐름이 시작된 캘린더 일자다. 없으면 오늘로 둔다 — 그 경로가 지금은 없지만
                // 라우트가 선택값이라 열려 있다.
                visit =
                VisitHeadline(
                    visitedOn = visitedOn ?: today,
                    clinic = clinic,
                    cardTitle = cardTitle,
                    today = (visitedOn ?: today) == today,
                ),
            )
        }
    }

    fun onNoteChange(note: String) {
        mutableUiState.update { it.copy(note = note) }
    }

    /**
     * 우하단 마이크. 음성 패널을 열고 닫는다.
     *
     * **받아쓰기는 아직 없다.** 패널과 상태까지가 지금 범위이고, 실제 인식은 증상 문답과 함께
     * 붙인다. 그래서 듣는 중에서 글이 들어오지 않는다.
     */
    fun onVoiceClick() {
        mutableUiState.update { state ->
            val next =
                when (state.voice) {
                    null, MedicalMateVoiceState.IDLE -> MedicalMateVoiceState.LISTENING
                    else -> MedicalMateVoiceState.IDLE
                }
            state.copy(voice = next)
        }
    }

    /** 음성 패널의 "직접 입력할게요". 패널을 닫고 적던 글로 돌아간다. */
    fun onTypeInsteadClick() {
        mutableUiState.update { it.copy(voice = null) }
    }
}
