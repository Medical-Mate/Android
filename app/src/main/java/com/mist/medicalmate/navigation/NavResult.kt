package com.mist.medicalmate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * 화면이 앞 화면에 값을 돌려주는 방법.
 *
 * **엔트리를 갈아치우지 않는다.** 전에는 고른 값을 라우트에 담아
 * `navigate(X(value)) { popUpTo<X> { inclusive = true } }`로 돌아왔다. 그러면 그 목적지의
 * 엔트리가 pop되면서 ViewModel도 함께 정리되고, 화면이 들고 있던 나머지 입력이 전부
 * 사라진다. 일정 추가에서 날짜와 시간을 채운 뒤 병원을 고르러 갔다 오면 그 둘이 비어 있던
 * 것이 이것이다.
 *
 * 값을 앞 엔트리의 `SavedStateHandle`에 넣고 그냥 뒤로 간다. 앞 화면은 백스택에 그대로
 * 남아 있어서 ViewModel도 살아 있고, 돌아온 값만 더해진다.
 *
 * 라우트 인자로 받던 자리를 전부 이것으로 바꾸지는 않았다. 흐름이 앞으로 나아가면서 값을
 * 넘기는 경우(문답 → 카드)는 새 화면을 여는 것이 맞다. 여기 쓰는 것은 **골라서 돌아오는**
 * 경우다.
 */
internal object NavResult {
    /**
     * 병원 찾기(1m-B)가 돌려주는 병원 이름.
     *
     * 이름뿐이다. 주소도 함께 돌려주던 자리가 있었는데 서버가 이름만 준다(#155).
     */
    const val HOSPITAL_NAME = "result.hospitalName"
}

/**
 * 값을 앞 화면에 남기고 뒤로 간다.
 *
 * 앞 엔트리가 없으면(이 화면이 시작점이면) 넣을 곳이 없으므로 그냥 뒤로만 간다.
 */
internal fun NavController.popWithResult(vararg results: Pair<String, String?>) {
    previousBackStackEntry?.savedStateHandle?.let { handle ->
        results.forEach { (key, value) -> handle[key] = value }
    }
    popBackStack()
}

/**
 * 돌아온 값을 한 번만 읽는다.
 *
 * 읽고 지운다. 남겨 두면 그 화면을 다시 열 때 옛 값이 또 흘러나온다. 화면을 떠났다
 * 돌아오는 것과 값을 받은 것을 구별할 방법이 없기 때문이다.
 */
@Composable
internal fun NavBackStackEntry.ConsumeResult(key: String, onResult: (String?) -> Unit) {
    val handle = savedStateHandle
    val value by handle.getStateFlow<String?>(key, null).collectAsStateWithLifecycle()

    LaunchedEffect(value) {
        if (handle.contains(key)) {
            onResult(value)
            handle.remove<String>(key)
        }
    }
}
