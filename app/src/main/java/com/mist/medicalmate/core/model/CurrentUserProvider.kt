package com.mist.medicalmate.core.model

import com.mist.medicalmate.core.network.ApiResult

/**
 * 로그인한 사람의 표시 이름을 준다.
 *
 * 홈 헤더의 아바타가 이름 첫 글자를 쓰는데 `GET /api/me/home` 응답에는 이름이 없다. 그
 * 값은 `profile` 도메인의 건강 프로필에 있다. 홈이 그쪽을 직접 부르면 도메인이 서로를
 * 참조하게 되므로 `core`에 인터페이스를 두고 Hilt가 연결한다. `AccessTokenProvider`와
 * 같은 방식이다.
 *
 * 실패를 값으로 돌려준다. 이름을 못 받은 것과 이름이 비어 있는 것은 다르다. 앞은 화면이
 * 다시 시도할 일이고 뒤는 아바타를 비워 둘 일이다.
 */
fun interface CurrentUserProvider {
    suspend fun displayName(): ApiResult<String?>
}
