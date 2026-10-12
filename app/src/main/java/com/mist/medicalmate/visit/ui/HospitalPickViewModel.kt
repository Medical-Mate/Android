package com.mist.medicalmate.visit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.medicalmate.R
import com.mist.medicalmate.core.network.ApiResult
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceWords
import com.mist.medicalmate.visit.data.HospitalRepository
import com.mist.medicalmate.visit.data.HospitalSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 병원 찾기 상태 보유자.
 *
 * `GET /api/hospitals`를 부른다. 원천이 심평원이고 서버가 목록을 들고 있지 않아 앱도
 * 캐시하지 않는다. 개원·폐원이 계속 생기는 데이터다.
 *
 * **검색어가 멎은 뒤에 부른다.** 한 글자마다 부르면 심평원까지 왕복이 그만큼 간다. 부위
 * 검색은 앱 안에서 끝나서 그냥 돌렸지만 이쪽은 다르다. 앞선 요청은 취소한다. 늦게 온 답이
 * 새 검색어의 결과를 덮으면 화면과 검색어가 어긋난다.
 *
 * **기다리는 동안 화면에서 먼저 좁힌다.** 기기에서 재 보니 왕복이 1.3~3.3초다(중앙값 1.6초).
 * 그동안 목록이 그대로 있으면 친 글자가 아무 일도 안 하는 것처럼 보인다. 서버 규칙이 이름
 * 부분 일치라 앱에서도 같은 규칙으로 먼저 줄일 수 있고, 답이 오면 그것으로 바뀐다.
 *
 * **한 번 받은 검색어는 다시 부르지 않는다.** 지우고 다시 치는 일이 잦은데 그때마다 1.6초를
 * 기다릴 이유가 없다. 화면을 다시 열면 버린다. 개원·폐원이 계속 생기는 데이터라 오래 들고
 * 있을 값이 아니다.
 */
@HiltViewModel
class HospitalPickViewModel
@Inject
internal constructor(private val repository: HospitalRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HospitalPickUiState())
    val uiState: StateFlow<HospitalPickUiState> = mutableUiState.asStateFlow()

    private var searchJob: Job? = null

    /** 이번 화면에서 받아 둔 결과. 같은 검색어를 다시 치면 그대로 쓴다. */
    private val cache = mutableMapOf<String, HospitalSearchResult>()

    /**
     * 화면을 연다.
     *
     * **검색 전에는 보여줄 것이 없다.** 전에는 진료 후(1m)에 후보 네 곳을 먼저 보여줬는데
     * 그것이 픽스처였다. 서버 검색은 질의가 있어야 답이 오므로 1m도 1m-B와 같은 입력 전
     * 상태로 열린다. 시안과 어긋나는 지점이라 디자인 트랙에 올렸다(#155).
     */
    fun load(
        purpose: HospitalPickPurpose = HospitalPickPurpose.AFTER_VISIT,
        forExistingCard: Boolean = false,
        initialQuery: String? = null,
    ) {
        searchJob?.cancel()
        cache.clear()
        mutableUiState.value = HospitalPickUiState(purpose = purpose, forExistingCard = forExistingCard)
        initialQuery?.trim()?.takeIf { it.isNotEmpty() }?.let(::searchFirst)
    }

    /**
     * 들고 들어온 이름으로 바로 찾는다(#270). 일정 추가가 예약 문자에서 읽은 병원 이름이다.
     *
     * **못 찾으면 이름을 바꿔 한 번씩 더 찾는다.** 문자 속 이름과 심평원에 등록된 이름이 띄어쓰기나
     * 진료과에서 갈린다 — `고려튼튼 통증의학과`는 `고려튼튼통증의학과의원`으로, `서울OO병원 내과`는
     * `서울OO병원`으로 올라 있다. 서버가 부분 일치라 띄어쓰기를 뺀 이름과 마지막 낱말을 뗀 이름이면
     * 걸린다. 찾은 이름을 검색어 칸에 남겨 무엇으로 찾았는지가 보이게 한다.
     *
     * 입력 대기(300ms) 없이 바로 부른다. 사람이 치는 글이 아니다.
     */
    private fun searchFirst(query: String) {
        val candidates =
            listOf(query, query.replace(" ", ""), query.substringBeforeLast(' ', ""))
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
        mutableUiState.update { it.copy(query = query, searching = true, failed = false) }
        searchJob =
            viewModelScope.launch {
                var index = 0
                var result: ApiResult<HospitalSearchResult>
                do {
                    val candidate = candidates[index++]
                    result = repository.search(candidate)
                    if (result is ApiResult.Success) cache[candidate] = result.value
                } while (index < candidates.size && result is ApiResult.Success && result.value.hospitals.isEmpty())
                val found = candidates[index - 1]
                mutableUiState.update { it.copy(query = if (result.isHit()) found else query) }
                apply(result)
            }
    }

    /**
     * 검색어가 바뀌었다.
     *
     * 고른 병원이 결과에서 빠지면 선택을 지운다. 보이지 않는 것이 골라져 있으면 완료를
     * 눌렀을 때 무엇이 저장되는지 알 수 없다.
     */
    fun onQueryChange(query: String) {
        searchJob?.cancel()
        mutableUiState.update { it.copy(query = query) }

        val keyword = query.trim()
        if (keyword.isEmpty()) {
            mutableUiState.update {
                it.copy(results = emptyList(), selected = null, searching = false, total = 0, failed = false)
            }
            return
        }

        val cached = cache[keyword]
        if (cached != null) {
            apply(ApiResult.Success(cached))
            return
        }

        narrow(keyword)
        searchJob =
            viewModelScope.launch {
                delay(DEBOUNCE_MILLIS)
                mutableUiState.update { it.copy(searching = true, failed = false) }
                val result = repository.search(keyword)
                if (result is ApiResult.Success) cache[keyword] = result.value
                apply(result)
            }
    }

    /**
     * 답을 기다리는 동안 보이는 목록에서 먼저 좁힌다.
     *
     * 서버가 이름 부분 일치로 찾으므로 앱도 같은 규칙을 쓴다. 여기서 나오는 것은 서버 결과의
     * 부분집합이라 없던 병원이 나타나지 않는다.
     *
     * **좁혀서 비면 그대로 둔다.** 보이는 20곳에 없다고 전국에 없는 것이 아니다. 한글 조합
     * 중간 상태("서울ㅂ")도 여기서 비는데, 그때 목록을 지우면 글자마다 깜빡인다.
     */
    private fun narrow(keyword: String) {
        mutableUiState.update { state ->
            val narrowed = state.results.filter { it.name.contains(keyword, ignoreCase = true) }
            if (narrowed.isEmpty()) state else state.copy(results = narrowed, total = narrowed.size)
        }
    }

    /**
     * 줄을 골랐다.
     *
     * **이름이 아니라 그 줄을 고른다.** 같은 이름의 지점이 여럿 나온다(`태릉씨티치과의원`이 두 곳).
     * 이름으로 찾으면 어느 줄을 눌러도 첫 줄이 골라진다(#270).
     */
    fun onHospitalClick(hospital: Hospital) {
        mutableUiState.update { state -> state.copy(selected = hospital.takeIf { it in state.results }) }
    }

    /**
     * 결과를 화면에 넣는다.
     *
     * **0건이면 직전 결과를 남긴다.** 한글은 마지막 글자가 조합되는 동안 중간 상태가 되어
     * 한 글자마다 목록이 비었다 찼다 한다. 부위 검색과 같은 이유다.
     *
     * 못 닿은 것과 못 찾은 것을 가른다. 앞은 다시 시도할 일이고 뒤는 검색어를 바꿀 일이다.
     */
    private fun apply(result: ApiResult<HospitalSearchResult>) {
        mutableUiState.update { state ->
            when (result) {
                is ApiResult.Success ->
                    state.copy(
                        results = result.value.hospitals.ifEmpty { state.results },
                        selected = state.selected?.takeIf { it in result.value.hospitals },
                        total = result.value.total,
                        searching = false,
                    )

                is ApiResult.Rejected, is ApiResult.NetworkUnavailable ->
                    state.copy(searching = false, failed = true)
            }
        }
    }

    /**
     * 받아쓴 말(#282). "첫 번째"면 그 줄을 고르고 바로 마친다. 다시 고르려면 뒤로 와서 고른다.
     * 화면 이동이라 확인은 받지 않는다. 병원 이름이면 그것으로 다시 찾는다.
     *
     * @param submit 고른 병원으로 마친다. 목적마다 돌아가는 곳이 달라 라우트가 넘긴다.
     */
    fun onVoice(text: String, submit: (Hospital?) -> Unit): VoiceReply =
        when (val voice = parseHospitalPickVoice(text)) {
            null -> VoiceReply.NotMine
            is HospitalPickVoice.Pick -> {
                val results = mutableUiState.value.results
                val hospital = if (voice.ordinal ==
                    VoiceWords.LAST
                ) {
                    results.lastOrNull()
                } else {
                    results.getOrNull(voice.ordinal)
                }
                if (hospital == null) {
                    VoiceReply.Done(R.string.voice_hospital_no_row)
                } else {
                    onHospitalClick(hospital)
                    submit(hospital)
                    VoiceReply.Done(R.string.voice_hospital_picked, listOf(hospital.name))
                }
            }

            HospitalPickVoice.Submit -> {
                val selected = mutableUiState.value.selected
                if (selected == null) {
                    VoiceReply.Done(R.string.voice_hospital_no_row)
                } else {
                    submit(selected)
                    VoiceReply.Done(R.string.voice_hospital_picked, listOf(selected.name))
                }
            }

            is HospitalPickVoice.Search -> {
                onQueryChange(voice.query)
                VoiceReply.Done(R.string.voice_hospital_searching, listOf(voice.query))
            }
        }

    private fun ApiResult<HospitalSearchResult>.isHit(): Boolean =
        this is ApiResult.Success && value.hospitals.isNotEmpty()

    private companion object {
        /**
         * 검색어가 멎었다고 보는 시간.
         *
         * 한글은 한 글자에 두세 번 바뀐다. 조합이 끝나기를 기다리는 값이기도 하다.
         */
        const val DEBOUNCE_MILLIS = 300L
    }
}
