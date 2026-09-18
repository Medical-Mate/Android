# 진료메이트 (Medical Mate) — Android

진료실에서 하고 싶은 말을 미리 정리하는 앱입니다. 환자가 아픈 부위를 짚고 AI와 문답으로
증상을 정리하면 한 장의 **브리핑 카드**가 만들어지고, 그 카드를 진료실에서 의사에게
보여줍니다. 진료가 끝나면 들은 말을 메모로 남기고, AI가 소견·검사·약·재방문으로 나눠
기록해 둡니다. 재방문 날짜는 캘린더 일정으로 이어집니다.

원티드 공모전 출품작이고, 디자인 2 · AI · 백엔드 · 앱 네 트랙 중 앱은 1인이 맡았습니다.
2026-09-03에 착수한 3주 MVP입니다.

## 무엇이 되어 있나

| 흐름 | 화면 | 상태 |
| -- | -- | -- |
| 진입 | 스플래시 · 카카오 로그인 · 온보딩 4장 · 신상정보 3단계 | 구현. 서버 토큰 교환과 401 재발급까지 |
| 증상 정리 | 3D 인체도 부위 선택 → AI 문답 → 통증 강도 → 의사에게 물어볼 질문 | 구현. 음성 입력(기기 내 받아쓰기)까지 |
| 브리핑 카드 | 카드 읽기 · 전체 편집 · 진료받을 병원 지정 · 카드 목록 · 삭제 | 구현. 확정한 카드를 고치면 서버가 새 버전을 만드는 규칙까지 반영 |
| 진료 후 기록 | 병원 확인 → 메모(글·음성) → AI 분류 결과 확인 → 저장 | 구현. 저장하면 재방문 일정이 자동으로 잡힘 |
| 기록 | 월별 기록 목록 · 기록 상세 타임라인(예정 · 진료 후 기록 · 카드) · 여러 건 삭제 | 구현. 같은 카드에 재방문이 쌓이면 한 화면에 |
| 캘린더 | 월 화면 · 일자 화면 · 일정 추가/수정 · 진료 전 할 일 · 다음 일정 | 구현 |
| 홈 | 오늘의 한 줄 · 이어서 하기 · 최근 브리핑 카드 · 다가오는 일정 | 구현 |
| 내 정보 | 건강 정보 요약과 수정 · 알림 설정 · 로그아웃 · 회원탈퇴 | 구현 |

목적지 23개, 디자인 시스템 컴포넌트 52종, JVM 유닛 테스트 666건입니다. 디자인 시스템은
Figma 원본을 정본으로 두고 와이어프레임 최종본 23장을 마스터까지 펴서 대조했습니다.

AI 처리(문답 · 질문 추천 · 메모 분류)는 백엔드 API가 맡습니다. 기기 안에서 도는 것은
음성 받아쓰기(ML Kit GenAI)이고 오디오는 기기 밖으로 나가지 않습니다. 발화 원문을 폰
안에서 처리하는 온디바이스 추출은 엔진과 모델 검증까지 와 있고 아직 화면에 붙지
않았습니다(`feat/#142-uses-native-library`).

## 기술 스택

- Kotlin 2.2 · Jetpack Compose(BOM 2026.03) · Material 3 위에 자체 디자인 시스템
- Hilt · Navigation Compose(타입 세이프 라우트, 단일 NavHost) · MVVM 단방향 흐름
- Retrofit 3 · OkHttp 5 · kotlinx.serialization · DataStore
- Filament(3D 인체도, Draco glTF) · ML Kit GenAI 음성 인식 · 카카오 SDK
- Gradle 9.5 · AGP 9.3 · minSdk 26 / targetSdk 36
- ktlint · detekt · Android Lint · JUnit — CI에서 모두 강제

## 구조

단일 모듈이고 패키지는 기능 우선입니다. 도메인 이름은 백엔드 패키지와 맞췄습니다.

```text
com.mist.medicalmate/
├── core/          designsystem(토큰 · 컴포넌트) · network(Retrofit · ApiResult) · model(도메인 공유 타입)
├── navigation/    단일 NavHost, 도메인별 그래프 등록, 세션 경계
├── auth/          로그인 · 세션 복구
├── profile/       온보딩 · 신상정보 · 내 정보
├── intake/        인체도 · 증상 문답 · 강도 · 질문
├── card/          브리핑 카드 · 기록 목록/상세
├── calendar/      캘린더 · 일정 · 진료 전 할 일
├── visit/         병원 찾기 · 진료 후 메모 · 기록 분류/상세
└── home/          홈
```

API 실패는 예외가 아니라 `ApiResult` 값으로 다루고, ViewModel에 `try/catch`가 없습니다.
서버 JWT는 `TokenStore`(DataStore)에만 두고 `allowBackup`은 꺼져 있습니다.

## 빌드와 검증

Android Studio에서 열거나, 커밋 전에 아래 한 줄을 돌립니다.

```bash
./gradlew --continue :app:ktlintCheck :app:detekt :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

`local.properties`에 `KAKAO_NATIVE_APP_KEY`를 두면 카카오 로그인이 동작합니다. 없어도
빌드는 됩니다. 백엔드 주소는 `BACKEND_BASE_URL`로 덮어쓸 수 있고 기본값은 dev 서버입니다.

## 함께 읽을 문서

- [`CLAUDE.md`](CLAUDE.md) — 작업 규칙과 현재 상태. 화면마다 왜 그렇게 만들었는지가 표로 정리되어 있습니다.
- [`PORTFOLIO.md`](PORTFOLIO.md) — 기술 판단 30건의 상황 · 선택지 · 근거.
- [`DESIGN.md`](DESIGN.md) · [`COMPONENT_MAP.md`](COMPONENT_MAP.md) — 디자인 시스템 사본과 Figma 마스터 대응표.
- [`GIT_CONVENTION.md`](GIT_CONVENTION.md) — 커밋 · 브랜치 · PR 규칙.

함께 쓰는 저장소: [`Medical-Mate/Backend`](https://github.com/Medical-Mate/Backend) ·
[`Medical-Mate/AI`](https://github.com/Medical-Mate/AI) · [`Medical-Mate/Design`](https://github.com/Medical-Mate/Design).
