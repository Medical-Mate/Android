# CLAUDE.md

Medical Mate Android 앱 저장소에서 AI 에이전트가 작업할 때 참고하는 문서입니다.
사람도 그대로 읽을 수 있게 작성합니다. 사실과 달라지면 즉시 갱신합니다.

---

## 0. 디자인 기준 (Figma)

**화면과 컴포넌트는 아래 Figma 파일을 보고 그대로 구현합니다.** 2026-10-15에 디자인
작업이 새 파일 `AX 해커톤 - 3차 프로젝트`(파일 키 `OWKWtDt8lLQLmltq0ztT5O`)로 옮겨졌습니다.
이전 파일(`AX 해커톤`, 파일 키 `cG6lz8nwzp75bfAXCnMqxx`)은 더 이상 기준이 아니므로 열지
않습니다. `COMPONENT_MAP.md` 안의 링크는 아직 이전 파일을 가리킵니다.

| 용도 | 노드 | 링크 |
| -- | -- | -- |
| 디자인 시스템과 컴포넌트 전체 | `291:636` | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=291-636) |
| 그중 새로 추가된 컴포넌트와 시스템 | `3610:1849` | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=3610-1849) |
| UI 전체 | `296:3042` | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=296-3042) |
| 그중 반영해야 할 새 UI가 적용된 페이지 | `3523:11713` | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=3523-11713) |
| 모든 것이 반영된 와이어프레임 | `3596:11376` | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=3596-11376) |

### 디자인 트랙이 넘겨준 문서

둘 다 새 파일을 Figma Plugin API로 직접 읽어 정리한 사본입니다(2026-10-15 받음). 손으로
고치지 않고, 어긋나면 Figma가 우선합니다.

| 문서 | 판 | 볼 때 |
| -- | -- | -- |
| [`DESIGN.md`](DESIGN.md) | v4.0 · 2026-10-15 | **화면을 구현할 때.** 토큰(색 44 · 글자 16 · 고도 7 · 반경 · 간격), 게시 컴포넌트의 속성과 규칙, `Wireframe 10-15` 89장의 골격 · Content 패딩 · 블록 간격 · Footer · 시트 규격, 화면 연결, 모션, 접근성, 코드에 아직 없는 것(12장 Android 매핑), Figma 정합성 메모 |
| [`DESIGN-SYSTEM.md`](DESIGN-SYSTEM.md) | v3.0 · 2026-10-10 (10-15 추가분 포함) | **Figma에서 원본을 찾을 때.** 문서 보드와 컴포넌트 34종의 노드 링크, 2026-10-15 게시 목록(추가 섹션 `3610:1849`의 묶음별 컴포넌트와 쓰는 화면) |

**Figma는 위 표의 다섯 링크에서 엽니다.** 컴포넌트는 두 갈래입니다. 기존에 쓰던 것은
디자인 시스템 페이지(`291:636`)의 `Design System` 섹션에, 이번에 새로 추가된 것은
`3610:1849`에 있습니다. 두 문서 안의 노드 링크는 같은 파일 안에서 원본을 빨리 찾는 보조
수단이고, 기준은 위 다섯 링크입니다.

보는 차례는 Figma → `DESIGN.md` → `DESIGN-SYSTEM.md`입니다. 두 문서가 다르면 날짜가 늦은
`DESIGN.md`를 따르고, 그래도 모호하면 Figma를 엽니다. 문서가 짝으로 적은 `진료메이트-PRD.md`는
저장소에 없습니다.

### 지켜야 할 것

1. **디자인을 반영하거나 기능 추가 지시를 받으면 먼저 위 링크에서 해당 화면을 엽니다.**
   화면 흐름과 최종 모양은 와이어프레임(`3596:11376`)을, 이번에 바뀐 부분은 새 UI
   페이지(`3523:11713`)를 봅니다.
2. **Figma에 있는 컴포넌트와 디자인 시스템 값만 씁니다.** 새로 추가된 것(`3610:1849`)을
   포함해 마스터가 있으면 그 마스터를 코드의 같은 컴포넌트로 옮기고, 토큰은
   `MedicalMateTheme`·`MedicalMateSpace`·`MedicalMateSize`·`MedicalMateRadius`에 있는 값을
   씁니다. 시안에 새 토큰이나 컴포넌트가 있으면 코드에 먼저 추가한 뒤 화면에서 씁니다.
   **디자인 시스템 페이지의 `Archive · 참고 (앱 미사용)` 섹션은 보지 않습니다.** 앱에서 쓰지
   않는 예전 컴포넌트를 모아 둔 곳이라 이름이 같은 마스터가 있어도 거기서 가져오지 않습니다.
   같은 페이지의 `수정 중` 섹션(미게시 작업물)과 이름이 `.` · `.[구]` · `_`로 시작하는
   컴포넌트·스타일도 쓰지 않습니다(`DESIGN.md`의 구현자 규칙).
3. **임의로 값을 넣거나 디자인을 만들지 않습니다.** 색·크기·간격·문구·아이콘·애니메이션을
   Figma와 똑같이 구현합니다. 보기 좋게 다듬거나, 비슷한 컴포넌트로 대신하거나, 시안에 없는
   자리를 채우지 않습니다.
4. **Figma에 없거나 모호하면 구현을 멈추고 묻습니다.** 상태(빈 화면·오류·로딩)가 그려져
   있지 않거나, 마스터와 인스턴스가 다르거나, 값을 읽을 수 없으면 추측하지 않고 사람에게
   확인합니다. 묻지 못한 채 넘어가야 했다면 그 사실을 PR 본문에 적습니다.
5. **Figma를 열 수 없으면 작업하지 않습니다.** Figma MCP는 인증이 필요합니다. 열리지 않으면
   예전 기억이나 문서만 보고 구현하지 말고 인증을 요청합니다.

8장 표에 있는 "시안에 없다", "디자인 트랙 확인 필요"로 남은 자리는 이 규칙 이전에 정한
것입니다. 해당 화면을 새 파일로 다시 작업할 때 Figma 기준으로 맞춥니다.

---

## 1. 검증 명령

커밋 전에 실행합니다.

```bash
./gradlew --continue :app:ktlintCheck :app:detekt :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

| 목적 | 명령 |
| -- | -- |
| 코드 스타일 자동 수정 | `./gradlew :app:ktlintFormat` |
| 스타일 검사만 | `./gradlew :app:ktlintCheck` |
| 정적 분석만 | `./gradlew :app:detekt` |
| Android Lint | `./gradlew :app:lintDebug` |
| 유닛 테스트 | `./gradlew :app:testDebugUnitTest` |
| 디버그 APK | `./gradlew :app:assembleDebug` |
| 릴리즈 번들 (미서명) | `./gradlew :app:bundleRelease` |

주의:

- `testReleaseUnitTest`는 존재하지 않습니다. 유닛 테스트는 debug 변형만 등록됩니다.
- `--offline`은 로컬 전용입니다. CI에서 쓰면 의존성을 받지 못해 실패합니다.
- 검사가 실패하면 코드를 고칩니다. 우회 금지 항목은 6장에 있습니다.

---

## 2. 기술 스택

| 항목 | 값 | 정의 위치 |
| -- | -- | -- |
| Gradle | 9.5.0 | `gradle/wrapper/gradle-wrapper.properties` |
| AGP | 9.3.2 | `gradle/libs.versions.toml` |
| Kotlin | 2.2.10 | `gradle/libs.versions.toml` |
| Compose BOM | 2026.03.00 | `gradle/libs.versions.toml` |
| Gradle 데몬 JDK | 21 | `gradle/gradle-daemon-jvm.properties` |
| Java 소스 호환성 | 11 | `app/build.gradle.kts` |
| compileSdk | 37 | `app/build.gradle.kts` |
| minSdk / targetSdk | 26 / 36 | `app/build.gradle.kts` |
| ktlint / detekt | 14.2.0 / 1.23.8 | `gradle/libs.versions.toml` |
| KSP / Hilt | 2.2.10-2.0.2 / 2.60.1 | `gradle/libs.versions.toml` |
| 카카오 SDK | 2.25.0 | `gradle/libs.versions.toml` |
| ML Kit Entity Extraction | 16.0.0-beta6 | `gradle/libs.versions.toml` |
| Vosk (호출어, 영어 소형 모델) | 0.3.75 | `gradle/libs.versions.toml` |
| desugar_jdk_libs | 2.1.5 | `gradle/libs.versions.toml` |
| Retrofit / OkHttp | 3.0.0 / 5.5.0 | `gradle/libs.versions.toml` |
| kotlinx.serialization | 1.9.0 | `gradle/libs.versions.toml` |
| Navigation Compose | 2.10.0 | `gradle/libs.versions.toml` |
| DataStore Preferences | 1.2.1 | `gradle/libs.versions.toml` |
| WorkManager | 2.12.0 | `gradle/libs.versions.toml` |

### 의존성 추가

의존성은 `gradle/libs.versions.toml`(버전 카탈로그)에 추가하고 `libs.*` 별칭으로
참조합니다. 빌드 스크립트에 좌표를 직접 쓰지 않습니다.

`org.jetbrains.kotlin.android` 플러그인은 추가하지 않습니다. AGP 9의 내장 Kotlin 지원을
쓰고 있고, `plugins` 블록에는 `com.android.application`과
`org.jetbrains.kotlin.plugin.compose`만 있습니다.

### 버전을 고정한 것

`compileSdk`는 37 이상을 유지합니다. `androidx.core:core-ktx:1.19.0`이 API 37 이상을
요구해서, 낮추면 `checkDebugAarMetadata`에서 빌드가 실패합니다.

kotlinx-serialization-json은 1.9.0에 고정입니다. 1.11.0은 `kotlin-stdlib` 2.3.20을
요구해 Kotlin 2.2.10 컴파일러가 읽지 못합니다. `kotlin`을 올릴 때 함께 재검토합니다.

KSP 버전은 Kotlin 버전에 묶여 있습니다. `kotlin`을 올리면 `ksp`도 함께 올립니다.
KSP 2.3.x는 Kotlin 2.3용이라 지금 쓸 수 없습니다.

Compose BOM은 navigation-compose가 요구하는 Compose 버전과 맞춥니다. navigation-compose
2.10.0이 `ui`·`runtime`·`animation` 1.10.5를 요구하는데 BOM 2026.02.01은 1.10.4를
고정합니다. 그 세 개만 1.10.5로 올라가고 `foundation`·`material3`는 1.10.4에 남는 혼합
상태가 되므로 BOM을 2026.03.00으로 올렸습니다. navigation을 올릴 때 BOM을 함께 확인하고,
결과는 `./gradlew :app:dependencies --configuration debugRuntimeClasspath`로 봅니다.

### 지우면 빌드가 깨지는 설정

**`android.disallowKotlinSourceSets=false`.** KSP가 생성 소스를 `kotlin.sourceSets`로
등록하는데 AGP 9의 내장 Kotlin 지원이 이 DSL을 금지합니다. AGP가 오류 메시지에서 직접
안내하는 억제 옵션이고, 없으면 `:app` 설정 단계에서 실패합니다. KSP가 built-in Kotlin에
대응하면 제거합니다.

**core library desugaring.** minSdk 24에서 `java.time`을 쓰려고 켰습니다. minSdk가
26이 되면서(#190) 그 이유는 사라졌습니다 — `java.time`이 API 26부터 있습니다. 그래도
`app/build.gradle.kts`의 `isCoreLibraryDesugaringEnabled = true`와
`coreLibraryDesugaring(libs.desugar.jdk.libs)`를 그냥 지우지 마세요. desugaring이
`java.time` 말고도 여러 API를 채워 주고, 무엇이 걸려 있는지 보지 않고 끄면 런타임에만
드러납니다. 끄려면 따로 확인하고 합니다.

### 카카오 SDK

Maven Central에 없습니다. `settings.gradle.kts`의 `dependencyResolutionManagement`에
`https://devrepo.kakao.com/nexus/content/groups/public/`을 등록하고
`includeGroup("com.kakao.sdk")`로 범위를 제한했습니다. `repositoriesMode`가
`FAIL_ON_PROJECT_REPOS`라 모듈 빌드 스크립트에는 저장소를 넣을 수 없습니다.

네이티브 앱 키는 `local.properties`의 `KAKAO_NATIVE_APP_KEY`에서 읽습니다. 없으면
환경변수를 보고, 그것도 없으면 빈 문자열로 빌드는 통과합니다. 실패는 런타임 카카오 API
호출에서만 납니다. CI는 환경변수 경로를 씁니다.

매니페스트에 `<queries>`를 직접 추가하지 마세요. `v2-common` AAR이 `com.kakao.talk`와
alpha·sandbox를 이미 선언하고 전이 병합됩니다. 손으로 넣으면 중복이고 alpha·sandbox가
빠집니다.

### 기타

백엔드 base URL은 `BuildConfig.BACKEND_BASE_URL`로 주입합니다. `local.properties`의
`BACKEND_BASE_URL`이나 환경변수로 덮어쓸 수 있고, 기본값은 배포된 dev 서버입니다.

`org.gradle.configuration-cache=true`가 켜져 있습니다. 빌드 스크립트에서 configuration
cache와 호환되지 않는 패턴(태스크 실행 시점의 `Project` 접근 등)을 쓰지 않습니다.

---

## 3. 모듈 맵

단일 모듈입니다. 패키지 루트는 `com.mist.medicalmate`입니다.

```text
MedicalMate/
├── app/                         유일한 모듈 (com.android.application)
│   └── src/
│       ├── main/java/com/mist/medicalmate/
│       │   ├── MainActivity.kt            진입점. 세션 확인 중 로딩
│       │   ├── MedicalMateApplication.kt  @HiltAndroidApp, KakaoSdk.init
│       │   ├── core/
│       │   │   ├── designsystem/          토큰, 타이포, 아이콘, 로고
│       │   │   │   └── component/        디자인 시스템 컴포넌트 51종
│       │   │   ├── network/               Retrofit·OkHttp 설정, ApiResult
│       │   │   ├── ondevice/              폰 안 모델(Qwen3.5-2B) 엔진·모델 받기·메모 라벨
│       │   │   └── voice/                 화면별 음성 명령(VoiceTarget)과 공용 말 풀이
│       │   ├── navigation/                단일 NavHost, 도메인별 그래프 등록, 세션 경계
│       │   ├── auth/  data/ ui/           1o 로그인, 1a-1 스플래시, 세션 복구
│       │   ├── profile/  data/ ui/        ONB 온보딩, 1b 신상정보, 1s 내 정보, 계정 동작
│       │   ├── intake/  ui/               1l 인체도, 1c·1d·1i 증상 정리
│       │   ├── card/  ui/                 1e 브리핑 카드, 1f 진료실 화면, 1j 기록
│       │   ├── calendar/  data/ ui/ reminder/ share/  1r 캘린더, 진료 하루 전 알림, 예약 문자 공유
│       │   ├── visit/  ui/                1m 병원 찾기, 1p 메모, 1q 진료 후 기록
│       │   ├── voice/  ui/                "헤이 메이트" 호출과 음성 명령
│       │   └── home/  ui/                 1n 홈
│       ├── test/                          JVM 유닛 테스트
│       └── androidTest/                   계측 테스트 (CI에서 실행하지 않음)
├── config/detekt/detekt.yml     detekt 기본 설정 위에 얹는 예외
├── .editorconfig                ktlint가 읽는 코드 스타일
├── gradle/libs.versions.toml    의존성 버전 단일 진실 소스
└── .github/workflows/ci.yml     CI
```

### 화면과 패턴

UI는 Compose 단독입니다. XML 뷰를 추가하지 않고, `res/values/themes.xml`은 Manifest용
테마로만 남깁니다. 예외로 카카오 SDK가 `appcompat`과 `material` 뷰 라이브러리를 전이
의존으로 끌고 옵니다. SDK 액티비티가 그 테마를 쓰므로 exclude하면 런타임에 깨집니다.

패턴은 MVVM에 단방향 흐름입니다. ViewModel이 상태를 노출하고 Composable이 소비합니다.
MVI 라이브러리(Orbit, Mavericks)는 도입하지 않습니다.

UseCase 계층은 기본적으로 두지 않습니다. `ui → Repository`가 기본이고, 여러 Repository를
조합하거나 실제 비즈니스 규칙이 있을 때만 UseCase를 만듭니다. 한 줄 위임만 하는 UseCase는
만들지 마세요.

DI는 Hilt입니다. `@HiltAndroidApp`은 `MedicalMateApplication`, 화면 진입점은
`@AndroidEntryPoint`.

브랜드가 고정한 색상은 `colorScheme`에 넣지 않습니다. 카카오 버튼 색(`KakaoContainer` 등)은
`core/designsystem/Color.kt`에 별도 상수로 둡니다. 카카오 디자인 가이드가 변경을 금지하므로
테마나 다크 모드에 따라 바뀌면 안 됩니다.

### 디자인 시스템

값의 정본은 Figma다. 기준 파일과 링크는 0장에 있다. `DESIGN.md`는 디자인 트랙이
보내주는 사본이고, 어긋나면 Figma가 우선한다. 실제로 `Elevation/Card`가 달랐다. 문서를
고치지 말고 디자인 트랙에 넘기세요. 현재 사본은 4.0(2026-10-15)이고 새 Figma 파일에서
뽑았다. Figma 보드와 컴포넌트 노드 링크는 `DESIGN-SYSTEM.md`에 있다. 둘 다 저장소 루트에 있다.

**주석에서 문서를 절 번호로 인용하지 마세요.** 문서가 개정되면 목차가 바뀐다. 3.0에서
컴포넌트 장이 8에서 7로 내려가고 고도가 5에서 4.2로 들어가면서 200건 가까운 인용이
엉뚱한 곳을 가리켰다. 장 이름(`문서의 접근성 기준`), 컴포넌트 이름, 3.0이 부여한 원칙
id(`P1`~`P5`·`D4`·`D5`·`D11`), 또는 `COMPONENT_MAP.md`의 Figma 노드 id로 가리킵니다.

**화면 코드에서 `MaterialTheme`을 직접 읽지 마세요.** 색과 타이포는
`MedicalMateTheme.colors`, `MedicalMateTheme.typography`로 읽습니다. `colorScheme`과
`typography` 슬롯에도 같은 값이 들어 있지만 그쪽은 M3 컴포넌트가 내부에서 참조하려고
채운 것입니다. 화면이 M3 이름을 쓰면 `bg/canvas`나 `fg/subtle`처럼 대응하는 역할이 없는
토큰을 못 쓰고, 디자인 시스템과 이름이 갈립니다.

**원시 팔레트를 화면에서 쓰지 마세요.** `Palette.kt`는 `internal`이고 시맨틱 토큰을
정의하는 곳에서만 참조합니다. 브랜드가 고정한 색(`KakaoContainer` 등)은 `BrandColor.kt`에
따로 있습니다. 카카오 디자인 가이드가 변경을 금지하므로 테마나 다크 모드에 따라 바뀌면
안 됩니다.

**간격과 크기는 `MedicalMateSpace`, `MedicalMateSize`, `MedicalMateRadius`를 씁니다.**
임의의 dp를 쓰지 마세요. 새 값이 필요하면 하드코딩하기 전에 토큰을 추가할지 검토합니다.
이 셋은 화면 폭이나 테마에 따라 달라지지 않아서 `CompositionLocal`이 아니라 오브젝트입니다.

**컴포넌트가 Figma 어느 마스터에서 왔는지는 `COMPONENT_MAP.md`에 있습니다.** 노드 id까지
적혀 있어 원본과 대조할 때 씁니다. 생성한 표라서 손으로 고치지 말고 그 문서에 적힌 절차로
다시 만듭니다. Code Connect로 Figma에 표시하려면 Dev 시트가 필요해서 지금은 문서로만
둡니다.

**아이콘과 로고는 `MedicalMateIcons`, `MedicalMateLogo`로 참조합니다.** `R.drawable`을
직접 쓰지 않습니다. drawable 파일은 Figma에서 내보낸 것이라 손으로 고치지 말고 원본에서
다시 내보냅니다.

**다크 모드는 지원 대상이 아닙니다.** 디자인 시스템에 다크 값이 없습니다. `values-night`를
만들거나 `isSystemInDarkTheme()`으로 분기하지 마세요. dynamic color도 쓰지 않습니다.
Android 12 이상에서 사용자 월페이퍼 색이 브랜드 컬러를 덮습니다.

**Glass 표면에는 Opaque 대안을 함께 두세요.** 블러가 `RenderEffect`에 의존하고 그것이
API 31부터입니다. minSdk 26이라 Android 8.0~11에는 블러가 걸리지 않습니다.

### 네트워크와 오류 처리

Retrofit + OkHttp + kotlinx.serialization을 씁니다. 설정은 `core/network/NetworkModule`에
있습니다.

API 실패는 예외가 아니라 값으로 다룹니다. `apiCall()`이
`ApiResult`(`Success` / `Rejected` / `NetworkUnavailable`)를 돌려주고 Repository가 도메인
결과 타입으로 바꿉니다. 그래서 ViewModel에 `try/catch`가 없습니다. 예외로 계층을 넘기면
호출자가 무엇이 날아올지 모르니 `catch (e: Exception)`을 쓰게 되고, detekt의
`TooGenericExceptionCaught`·`SwallowedException`이 그 지점을 잡습니다. 설정을 완화하는
대신 결과 타입을 쓰세요.

토큰 재발급은 인증을 붙이지 않는 경로로 나갑니다. `@AuthFree`로 표시한 `OkHttpClient`와
`Retrofit`이 따로 있고 인터셉터도 Authenticator도 달지 않습니다. 같은 클라이언트를 쓰면
재발급 호출이 만료된 헤더를 달고 나가고, 그 401이 다시 재발급을 부릅니다. 조립 순서도
막힙니다. `OkHttpClient`가 `TokenAuthenticator`를 받고 그것이 재발급 API를 받는데 그 API를
같은 `Retrofit`에서 만들면 순환입니다.

직렬화 실패 같은 계약 위반은 잡지 않습니다. `apiCall()`은 `HttpException`과 `IOException`만
잡습니다. 서버와 클라이언트의 계약이 어긋난 것을 "다시 시도해주세요"로 감추면 원인을 찾을
수 없게 됩니다.

### 의존 방향

`core`는 도메인을 참조하지 않습니다. 인증 헤더를 붙이는 `AuthInterceptor`가
`core/network`에 있고 토큰이 필요하지만, `auth`의 `TokenStore`를 직접 쓰지 않고
`core/network/AccessTokenProvider` 인터페이스로 받습니다. 구현 연결은
`auth/data/AuthModule`의 `@Binds`가 합니다. 반대로 두면 다른 도메인이 `core/network`를
쓸 때마다 `auth`가 따라옵니다.

한 도메인은 다른 도메인을 직접 참조하지 않습니다. 공유가 필요하면 `core`로 올리고, 두
도메인이 같은 타입을 쓰면 그 타입은 `core/model`에 둡니다. 예외는 `navigation`입니다.
조합 루트라서 모든 기능을 참조해도 되는 유일한 패키지입니다.

### 네비게이션

네비게이션은 Navigation Compose이고 그래프는 하나입니다. `navigation/MedicalMateNavHost`가
유일한 `NavHost`입니다. 라우트는 `@Serializable data object`나 `data class`로 정의하는
타입 세이프 방식을 쓰고 문자열 경로를 쓰지 않습니다.

**그래프를 로그인용과 본문용으로 쪼개지 마세요.** `NavHost`는 컴포지션을 떠날 때 아무것도
정리하지 않습니다(소스의 `onDispose {}`가 비어 있습니다). 목적지별 `ViewModelStore`가
Activity의 스토어에 얹혀 있어서, 세션 상태로 `NavHost` 자체를 갈아치우면 로그아웃할 때마다
ViewModel이 정리되지 않고 쌓입니다. 백스택에서 pop될 때만 확실히 정리되므로 로그인·로그아웃
경계는 `popUpTo(graph.id) { inclusive = true }`로 넘어갑니다.

화면 ViewModel은 목적지 스코프입니다. `composable<T> { }` 안에서 `hiltViewModel()`을
부르면 그 back stack entry에 묶입니다. 로그아웃으로 엔트리가 pop되면 ViewModel도 사라지므로
다음 계정으로 로그인해도 이전 계정 데이터가 남지 않습니다. `SessionViewModel`은 예외로
`MainActivity`에서 Activity 스코프로 둡니다. 세션은 특정 화면에 속한 상태가 아닙니다.
`VoiceCommandViewModel`(#273)도 같은 이유로 Activity 스코프입니다. 호출어는 어느 화면에서든
듣고, 화면을 옮겨도 듣기가 끊기면 안 됩니다.

라우트 타입과 그래프 등록은 기능 패키지에 둡니다. `auth/ui/LoginDestination.kt`처럼 라우트
`data object`와 `NavGraphBuilder` 확장 함수를 같은 파일에 두고, `navigation`은 그 확장
함수만 호출합니다. 이렇게 두면 `navigation`이 화면 컴포저블의 파라미터까지 알 필요가
없습니다.

### 상태와 테스트 가능성

일회성 신호를 상태로 두면 화면이 다시 열릴 때 그 값이 다시 흘러나갑니다. 목적지 스코프가
대부분을 막아주지만 pop 없이 화면이 다시 조합되는 경로는 남아 있습니다.
`LoginViewModel.onAuthenticationHandled()`처럼 소비 후 되돌리는 메서드를 두고, 호출자는
화면을 바꾸기 **전에** 소비 표시를 남기세요. 화면이 바뀌면 `LaunchedEffect`가 취소돼 뒤에
둔 코드는 실행되지 않습니다.

로직은 JVM에서 테스트 가능한 위치에 둡니다. ViewModel과 Repository의 로직이 Activity나
Composable 안에 들어가면 `testDebugUnitTest`로 검증할 수 없습니다.

ViewModel에 Android `Context`를 넣지 않습니다. 카카오 SDK처럼 Activity Context를 요구하는
호출은 UI 계층(`LoginRoute` 같은 상태 있는 컴포저블)이 담당하고 ViewModel은 결과만
받습니다. 그래서 `KakaoLoginClient`는 Hilt로 주입하지 않고 컴포저블에서 `remember`로
만듭니다.

### 토큰과 보안

서버 JWT는 `TokenStore`(DataStore)에만 둡니다. 카카오 토큰은 SDK가 자체 보관하고 서버도
저장하지 않으므로 앱이 따로 저장하지 않습니다. 같은 자격증명을 여러 곳에 두면 로그아웃할
때 지워야 할 곳이 늘어납니다.

**`android:allowBackup="false"`를 되돌리지 마세요.** 토큰과 향후 로컬 캐시가 Google 클라우드
백업이나 기기 간 전송으로 나가는 것을 막습니다. refresh 토큰 TTL이 30일입니다.

### 패키지 구조

패키지는 기능(도메인) 우선입니다. 최상위에 도메인 패키지를 두고 그 안에 `ui`와 `data`를
둡니다. 레이어를 최상위에 두지 않습니다.

```text
com.mist.medicalmate/
├── core/
│   ├── designsystem/   테마, 공용 Composable
│   ├── network/        Retrofit, 인터셉터
│   └── model/          여러 도메인이 공유하는 타입
├── navigation/
├── auth/      ui/ data/    1o 로그인
├── home/      ui/ data/    1n 홈
├── profile/   ui/ data/    ONB 온보딩, 1b 신상정보, 1s 내 정보
├── intake/    ui/ data/    1l 1c 1d 1i 문답
├── card/      ui/ data/    1e 브리핑 카드, 1j 기록
├── calendar/  ui/           1r 캘린더
├── handoff/   ui/           1f 진료실 전달 (앱에는 없음. 백엔드 패키지 대응)
├── visit/     ui/ data/    1m 1p 1q 사후 기록
└── voice/     ui/           "헤이 메이트" 음성 명령 (화면 번호 없음)
```

도메인 이름은 `Medical-Mate/Backend`의 패키지(`auth`, `profile`, `intake`, `card`,
`handoff`, `visit`)와 맞췄습니다. 같은 단어가 양쪽에서 같은 것을 가리키게 하려는
목적입니다. `home`과 `calendar`는 백엔드에 대응이 없습니다.

`handoff`는 만들지 않습니다. IA가 진료실 전달을 제외·보류로 옮기면서 `1f`에 별도 화면을
두지 않고 브리핑 카드(`1e-1`)를 그대로 건네기로 했습니다. `card/ui/HandoffScreen.kt`와
전달 API도 함께 걷어냈습니다(#194). 위 구조도의 `handoff` 줄은 백엔드 패키지 이름과의
대응으로만 남습니다.

빈 패키지를 미리 만들지 마세요. 위 구조는 규칙이고, 미리 세워둘 골격이 아닙니다. 폴더는
해당 기능에 실제로 착수할 때 만듭니다. 한 도메인에 파일이 하나뿐이면 `ui`·`data` 하위
폴더도 만들지 않고 도메인 폴더에 바로 둡니다.

### 아직 안 정해진 것

로컬 저장 방식이 미정입니다. 문답 중간 저장을 서버가 들고 있으면 앱에서 할 일이 없어서,
문답 화면(1l~1d)에 착수할 때 백엔드 API를 보고 정합니다.

---

## 4. 코드 스타일

두 도구가 CI에서 강제됩니다. 로컬에서 통과시키고 커밋하세요.

### ktlint

설정은 `.editorconfig`에 있습니다.

- Jetpack Compose 관례에 따라 `@Composable` 함수는 PascalCase를 허용합니다
  (`ktlint_function_naming_ignore_when_annotated_with = Composable`).
- 와일드카드 import는 금지입니다. 자동 수정되지 않으므로 직접 풀어 써야 합니다.
- `*.kts` 빌드 스크립트도 검사 대상입니다.
- 대부분의 위반은 `./gradlew :app:ktlintFormat`으로 해결됩니다.

`max_line_length = 120`은 detekt `MaxLineLength` 기본값과 맞춘 값입니다. 한쪽만 바꾸지
마세요. 둘이 다르면 `ktlintFormat`이 줄을 붙이고 detekt가 그 줄을 잡는 왕복이 생깁니다.

### detekt

설정은 `config/detekt/detekt.yml`입니다. `buildUponDefaultConfig = true`이므로 이 파일에는
기본값과 다른 부분만 적습니다.

- `MagicNumber`는 `test`, `androidTest`, `core/designsystem`, 그리고 `ui` 패키지 전체에서
  제외됩니다. 테마 색상 리터럴과 Compose 레이아웃의 `dp`·`sp` 값까지 잡으면 잡음이 너무
  많아 경고 전체를 무시하게 됩니다. `data`와 ViewModel의 계산 로직에서는 그대로 살아
  있습니다.
- `UnusedPrivateMember`는 `@Preview`를 무시합니다. Preview 컴포저블은 IDE와 툴링이
  호출하므로 미사용이 아닙니다.
- `FunctionNaming`은 `@Composable`을 무시합니다.

보고서는 `app/build/reports/detekt/detekt.html`과 `detekt.sarif`에 나옵니다.

---

## 5. Git 컨벤션

전체 규칙은 [`GIT_CONVENTION.md`](GIT_CONVENTION.md)에 있습니다. 이 저장소에서 반드시
지킬 것만 요약합니다.

### 커밋 메시지

```text
태그: 내용 #이슈번호
```

- 태그 첫 글자는 대문자, 콜론 뒤에만 공백. 예: `Feat: 복약 알림 등록 화면 추가 #12`
- 태그: `Feat` `Fix` `Refactor` `Design` `Comment` `Style` `Chore` `Test` `Init` `Rename` `Remove` `Docs` `!Hotfix`
- CI 워크플로 변경은 `Chore:`를 씁니다.
- 커밋 하나에 논리적 변경 하나만 담습니다.
- 대화형 bash에서 `!Hotfix:`는 history expansion으로 실패합니다. 작은따옴표를 쓰세요.

### 브랜치

```text
<태그 소문자>/#<이슈번호>-<영문-설명>
```

예: `feat/#12-medication-reminder`, `chore/#3-project-templates`

브랜치는 `main` 하나입니다. `develop`을 만들지 않습니다. 머지 후 원격 브랜치는 자동
삭제됩니다.

### PR

제목은 커밋과 같은 형식입니다. 저장소가 Squash 전용이라 PR 제목이 그대로 `main`의 커밋
제목이 되고, PR 본문도 그대로 커밋 메시지 본문이 됩니다
(`squash_merge_commit_message=PR_BODY`). 머지 전에 본문을 정리하세요. 본문에는
`Closes #N`을 넣습니다.

브랜치 보호는 켤 수 없습니다. Free 플랜에 private 저장소라 GitHub 보호 규칙이 막혀
있습니다. `main` 직접 push 금지와 리뷰는 강제되지 않는 약속이므로, 머지 전에 CI가 초록인지
사람이 직접 확인해야 합니다.

---

## 6. 하지 말 것

1. **검증 우회.** 실패하면 코드를 고칩니다. 다음은 모두 금지입니다.
   - 통과시키려고 테스트를 수정하거나 삭제
   - `-x test`, `-x lintDebug` 같은 태스크 제외 플래그 사용
   - 근거 없는 `@Suppress`, `ktlint-disable` 추가, detekt baseline 생성
   - CI 워크플로에서 실패하는 검사를 제거

   테스트 변경이 실제로 필요하면 그 사실과 이유를 밝히고 사람의 확인을 받습니다.

2. **PR 병합.** 병합은 사람이 결정합니다.

3. **요청 범위 밖의 변경.** 관련 없는 리팩터링·이름 변경·포맷 수정을 같은 커밋에 넣지
   않습니다.

4. **다른 저장소 수정.** `Medical-Mate/Backend`, `AI`, `Design`은 별도 Issue와 PR로
   처리합니다.

5. **민감정보 커밋.** `local.properties`, `*.jks`, `*.keystore`, API 키는 `.gitignore`로
   막혀 있습니다. 우회하지 마세요.

---

## 7. CI

`.github/workflows/ci.yml`에 있습니다. 공통 셋업은
`.github/actions/setup-android`(JDK 21 + Android SDK + Gradle 캐시)입니다.

| 잡 | 실행 | 시점 |
| -- | -- | -- |
| Static analysis | `ktlintCheck` + `detekt` + `lintDebug` | PR, main |
| Unit tests | `testDebugUnitTest` | PR, main |
| Assemble debug | `assembleDebug` | PR, main |
| Release bundle | `bundleRelease` | main 푸시만 |

- 캐시는 `main`에서만 쓰고 PR에서는 읽기만 합니다.
- PR에 새 커밋이 오면 이전 실행은 취소됩니다. main 실행은 취소하지 않습니다.
- 실패 확인: `gh pr checks --watch`, `gh run view --log-failed`

---

## 8. 현재 상태와 미정 사항

| 항목 | 상태 |
| -- | -- |
| 유닛 테스트 | 모두 1038건이다. `IntakeViewModel` 55건, `IntakeVoiceSilence` 8건, `IntakeQuestion` 9건, `VisitRecordViewModel` 41건, `ScheduleAddViewModel` 37건, `AppointmentText` 16건, `ScheduleAddSharedText` 6건, `SharedAppointmentLink` 3건, `HospitalNameChar` 4건, `SpacedHospitalName` 4건, `InvisibleSpace` 2건, `RealMessage` 2건, `HospitalSample` 5건, `TimeNear` 5건, `TruncatedDateSpan` 2건, `ScheduleAddHospitalSearch` 4건, `HospitalPickInitialQuery` 4건, `HospitalPickSameName` 1건, `ScheduleAddRevisit` 4건, `BriefCardViewModel` 34건, `BriefCardSchedule` 9건, `CalendarDayViewModel` 37건, `SessionViewModel` 27건, `HomeRepository` 26건, `RecordDetailViewModel` 26건, `VisitNoteViewModel` 23건, `MyProfileViewModel` 35건, `ReminderTimeViewModel` 5건, `VoiceCommandViewModel` 40건, `VoiceTab` 2건, `VoiceSuggestionPhrase` 5건, `VoiceScroll` 2건, `VoicePanelText` 2건, `ScheduleAddVoice` 43건, `IntakeVoice` 26건, `BriefCardVoice` 5건, `VoiceDate` 4건, `VisitVoice` 7건, `CalendarDayVoice` 1건, `VoiceHandoff` 1건, `HospitalPickVoice` 20건, `VoiceWords` 11건, `VoiceCommand` 10건, `WakeWord` 11건, `AppointmentRepository` 20건, `CalendarViewModel` 21건, `HospitalPickViewModel` 19건, `RecordViewModel` 19건, `CardMapping` 19건, `BodyMapGeometry` 17건, `HealthEditViewModel` 17건, `BriefCardListViewModel` 16건, `IntakeSessionActions` 17건, `BodyMap3dPick` 15건, `ProfileSetupViewModel` 14건, `LoginViewModel` 13건, `DesignToken` 12건, `RecordDetailFixtures` 12건, `BodyMap3dCamera` 11건, `BodyMapSearchState` 11건, `VectorParity` 18건, `VisitClassifyOnDevice` 6건, `SessionOnDevice` 16건, `BodyMapLayout` 10건, `BodyMap3dFrame` 8건, `BodyPartSearch` 8건, `BriefCardDelete` 8건, `VisitReminderPlan` 8건, `BodyMap3dMesh` 7건, `HomeViewModel` 12건, `MedicalMateSeverity` 7건, `TokenAuthenticator` 7건, `VisitMapping` 7건, `VisitDetailViewModel` 6건, `DefaultVisitReminders` 7건, `FollowUpAppointmentScheduler` 5건, `HomeSchedule` 4건, `PretendardFont` 4건, `ProgressIndicator` 4건, `SessionRepository` 4건, `SegmentedControl` 3건, `Dday` 1건, 템플릿 1개 |
| 네비게이션 테스트 | 없음. `NavHost`는 계측 테스트가 필요하고 CI가 androidTest를 실행하지 않음 |
| 아키텍처 패턴 | MVVM 확정. UseCase는 필요할 때만 |
| DI | Hilt 확정 |
| 패키지 구조 | 기능 우선 확정. 도메인명은 Backend와 일치 |
| 네비게이션 | Navigation Compose 2.10.0 확정. 단일 `NavHost` + 타입 세이프 라우트. 목적지 23개(1m-12 병원 확인 #246, 진료 후 기록 상세 #256). 그래프 등록은 도메인별 확장 함수로 나눠 `MedicalMateNavGraphs.kt`에 있고 `NavHost` 파일에는 그래프 본체와 세션 경계 처리만 남는다 |
| 화면 전환 | `MedicalMateNavHost`. `MainActivity`는 세션 확인 중 로딩만 담당. **전환은 `MedicalMateNavTransitions`가 들고 있다**(#214) — 가로로 밀고, 물러나는 화면은 폭의 1/4만 움직인다. navigation-compose의 기본값(0.7초 크로스페이드)을 그대로 두면 화면이 어느 쪽으로 갔는지가 남지 않는다. 가장자리를 쓸어 돌아가는 것도 같은 전환을 쓴다 — 돌아가는 길이 하나여야 하고, 기본값이 그 자리에 걸어 두는 축소(0.7)는 화면이 작아져 사라지는 것으로 읽힌다. 쓸어내는 동안에는 같은 전환이 손가락 위치만큼만 진행된다. 디자인 문서에 화면 전환 규격이 없어 값은 Material 표준을 따랐다 |
| ViewModel 스코프 | 화면 ViewModel은 목적지 스코프. `SessionViewModel`과 `VoiceCommandViewModel`(#273)만 Activity 스코프 |
| 디자인 시스템 | **DESIGN.md 4.0(2026-10-15)에서 더해진 것 가운데 코드에 들어온 것** — 색 `bg/on-dark` · `border/on-dark` · `bg/primary-strong`, 글자 `Label/XS`, 효과 `Elevation/Primary` · `Shadow/On Image` 둘(`MedicalMateEffects`, `Modifier.dropShadow`로 Figma 값을 그대로 옮긴다), 컴포넌트 `Number Badge` · `Question Row`(#310) · `Home Banner` · `Link / On Image` · `Card / Action` · `Card / Resume` · `Card / Schedule`(#312)과 그림자 없는 `Footer`. #312 둘째 묶음으로 `Hint Row` · `Step Row` · `Illustration / Share Sheet`와 `Elevation/Float` 두 겹(`MedicalMateEffects.floatWide`·`floatTight`)이 들어왔고, `Bottom Sheet` 그래버를 4.0 규격(36×4 · `border/subtle`)으로 맞췄다. 그래버 아래 간격은 시트마다 달라 `grabberGap`으로 받는다. 아직 없는 것은 아이콘 `export`와 나머지 추가 컴포넌트다(DESIGN.md의 Android 매핑 표). 아래는 3.0 기준으로 반영했던 내용이다. DESIGN.md 3.0의 토큰·타이포·아이콘 반영 완료. 시맨틱 41개, 타이포 15종, 아이콘 46개(arrow-up 추가), 로고 4개, Pretendard 4무게. 3.0이 확정한 `layout/tabbar-h` 79와 Date Cell 42는 코드와 같다. Badge를 `Label/M`으로 적었던 2.0의 오기는 3.0에서 사라졌다 |
| 컴포넌트 | 52종 구현(`FAB` 추가). Figma 마스터 대응은 `COMPONENT_MAP.md`. `Severity Scale`은 Figma에서 마스터가 삭제돼 함께 지웠는데 3.0 문서에는 아직 남아 있다. 디자인 트랙 확인 필요. **받아쓰기는 `Voice Input · Mate`(`3539:5647`)다**(#318) — 예전 `Voice Input`(마이크 88 원)은 Archive로 옮겨져 걷어냈고 `MedicalMateVoiceState`의 상태와 문구만 남았다. 마스터는 `Listening` 하나라 다른 상태도 같은 카드에 글만 바꾼다. 진료 후 메모와 증상 문답이 함께 쓴다. 증상 문답은 1c-2 음성 입력(`3663:7181`)대로 카드가 입력칸 위에 서고 둘 사이가 28이다(시안이 카드를 Content 아래 16에 띄우고 Footer가 위 12를 둔다). 받아쓴 말은 입력칸에 적지 않고(사용자 결정) 입력칸은 음성을 켜기 전에 적은 글을 보인다. 글은 "말씀해 주세요" · "듣고 있어요"로 고정이다. 파형은 디자인 트랙이 넘긴 `MateWaveform`이고 받아쓰는 글이 새로 들어올 때만 움직이고 들리지 않으면 시안 모양 그대로 멈춰 있다(ML Kit이 음량을 주지 않는다. 디자인 트랙 문서의 듣는 중 숨쉬기는 사용자 결정으로 뺐다). **`Section Header`의 제목은 `Heading/M` 20이고 액션은 가운데 맞춤이다**(#235). **`Nav Bar`는 바깥 8 · 슬롯 48 · 제목은 바의 가운데다**(#226) — 아이콘 왼쪽 끝이 화면에서 20에 온다. 제목을 좌우 슬롯 사이에 끼우면 텍스트 액션이 붙을 때 밀리므로, 좌우에 같은 값(액션 없으면 60, 있으면 88)을 비우고 그 안에서 가운데에 둔다. 우측 텍스트 액션은 버튼이 아니라 `Body/L Strong` 17 글자에 높이 48과 버튼 역할을 얹은 것이다. **보조 텍스트 액션 자리가 하나 더 있다**(#275) — 브리핑 카드의 `공유`가 `편집` 왼쪽에 같은 모양으로 선다. 마스터에 없는 자리라 디자인 트랙 확인 필요. 둘이 서면 제목 좌우를 120씩 비운다. **상태 칩은 제목 바로 옆이다**(#227) — 마스터의 `Title Row`가 제목과 배지를 간격 6으로 묶는다. **`List Row`는 카드 모양으로 뜬다**(#227) — 면만 깔고 그림자가 없어 줄들이 바탕에 붙어 있었다. 마스터가 카드로 띄우고 구분선을 쓰지 말라고 적는다. 반경 16 · `Elevation/Card` · 좌 18 우 14다. **줄 제목은 `Heading/S`다**(#237) — `Body/L`로 두면 같은 17이라도 Regular라 제목이 아래 메타와 같은 무게로 읽힌다. 홈의 최근 브리핑 카드·다가오는 일정과 병원 찾기 결과가 이 한 줄을 함께 쓴다. **제목은 한 줄까지이고 넘치면 말줄임이다**(#294) — 배지가 늘 제 크기로 제목 바로 옆에 선다. 제목이 폭을 먼저 가져가면 긴 병원 이름에 D-day 배지가 눌려 "D"와 "-0"으로 꺾였다. 배지 글자는 어디서든 꺾이지 않는다. 남은 날은 `ddayLabel`이 적고 오늘은 "D-0"이 아니라 "D-day"다. **`Card`의 머리 라벨은 `Label/S`다**(#237) — 11 Medium에 자간 2다. 홈의 "오늘의 한 줄"·"이어서 하기", 일자 화면의 일정 카드, 내 정보의 로그인 수단 줄이 여기다. **목록 줄은 `Card`가 아니라 `List Row` 모양이다**(#229) — `MedicalMateCard`가 최소 높이 116과 여백 20을 강제하는데 시안의 줄은 104이고 편집에서는 80이다. 마스터 `List Row`(`335:1114`)의 반경 16 · 좌 18 우 14 · 위아래 16 · `Elevation/Card`를 `RecordList`가 직접 그린다. `Dialog`는 제목과 본문이 한 칸이고 사이가 4다. **불투명 `Bottom CTA Bar`에 `Elevation/Float`이 걸린다**(#230) — 마스터의 `Surface=Opaque`가 그 그림자를 달고 있고 `Glass`는 블러만 있다. 블러를 못 쓰는 환경에서 층을 만드는 것이 그림자다. **`Button`의 Ghost는 `fg/subtle`이다**(#249) — 마스터(`291:670`)가 세 크기 모두 그렇고 코드는 `fg/primary`였다. 면이 없는 버튼이 색까지 링크와 같으면 둘이 한 가지로 보인다. **`Notice`는 마스터(`292:668`) 구조 그대로다**(#243) — 흰 32 원형 배지에 20 아이콘, 왼 14 · 오른 16 · 위아래 14, 글 묶음 위 4에 제목·본문 사이 3. 아이콘을 맨몸으로 두고 여백 16에 최소 높이 79로 짜여 있던 것을 고쳤다. `BRAND` 톤은 마스터에 없다 — 기록 상세 예정 알림 인스턴스(`1076:4047`)가 `bg/primary` 면과 `fg/on-primary` 글자로 덮어 그린 것을 톤으로 올렸고, 마스터에 변이로 올려 달라고 디자인 트랙에 넘길 항목이다. **`Empty State`의 행동은 채움 없는 글자다**(#217) — 마스터는 Tonal 알약이고 설명도 그렇게 적는데, 시안의 인스턴스가 확인한 둘(`1j-2`·일자 화면) 모두 채움을 지우고 `Label/L`·`fg/link` 글자만 남겼다. 화면에 그려진 쪽을 따랐고 어느 쪽이 정본인지는 디자인 트랙 확인 필요 |
| 컴포넌트 v2 | Figma `06 · 추가`가 9종을 v2로 재등록. KV Row·Tab Bar·Nav Bar·Date Cell·Text Field·Segmented Control 반영 완료. Text Area의 Footer Row(카운터·마이크)와 Toast의 Timer는 기본 false라 미구현. List Row의 Summary는 시안 `1j-1`의 줄이 그 변이인 것이 대조에서 확인됐다(#237) — 제목 · 메타 · 요약 세 줄에 높이 104이고 `RecordList`가 그 모양으로 직접 그린다. Ghost→Outline은 F·H 플로우를 대조한 결과 해당 지점이 여전히 텍스트형이라 적용 대상이 없다 |
| 컴포넌트 3.0 신규 | 7종 모두 화면에 붙었다. `Hospital Card`는 1e-1(1k는 시안에서 사라졌다), `Picker Field`·`Card Pick`은 1r-4, `Add Row`는 1r-4, `Todo Row`는 1r-2·1r-4, `Select Bar`는 1j-4, `Onboarding Progress`는 ONB다. `Select Bar`의 면색은 마스터에 채움이 없었는데 `1j-4-D2` 인스턴스가 `#F2F4FE`로 칠해져 있어 `bg/primary-faint`로 넣었다. `Hospital Card`·`Card Pick` 마스터도 채움이 묶여 있지 않아 `bg/surface`를 넣었다. 디자인 트랙 확인 필요 |
| CRUD 규칙 | 3.0이 장으로 확정. 편집 상태는 `편집 → 취소 → 확인` 한 자리에서 이름만 바뀌고, 삭제는 세 갈래(개체=하단 Danger CTA+Dialog / 항목=행 × 확인 없음 / 목록=체크 다중선택+Dialog)다. 토스트와 스와이프는 쓰지 않는다. 여섯 화면에 들어갔다. 1e-1·1q-1·1r-2가 편집 상태와 개체 삭제를, 1j-1·1j-4가 목록 삭제를, 1r-4가 항목 삭제를 쓴다 |
| 반경 13 · 14 | 3.0이 둘 다 스케일 밖 값으로 잡고 `radius/sm` 12 · `radius/md` 16 통일안을 적었다. 코드의 `MedicalMateRadius.dateCell`(13)과 `buttonM`(14)이 그 값이다. Figma가 바뀌면 함께 바꾼다 |
| Elevation/Card | Figma가 2겹(y3 r10 8% + y1 r2 5%)으로 바뀜. 코드는 `Modifier.shadow` 한 겹 근사 |
| Body Map | 구현 완료(#51). `core/designsystem/component`가 아니라 `intake/ui`에 있다. 화면 하나에서만 쓰고 좌표표·이미지 9장이 딸려 있어서 디자인 시스템에 올리지 않았다 |
| 인체도 좌표 | `BodyMapGeometry.kt`는 디자인 트랙의 `humanmap_coords.json`(schema 3.0)에서 생성한 파일이다. **손으로 고치지 않는다.** 좌표가 바뀌면 새 파일로 다시 생성한다 |
| 인체도 표시 크기 | 48dp 조작 영역이 겹치지 않는 최소 크기를 좌표에서 계산한다(`BodyMapLayout.kt`). 전신 505dp, 확대 최대 505dp. 시안 값을 받아 적지 않은 이유는 좌표가 바뀌면 필요한 크기도 바뀌기 때문이다 |
| 인체도 미해결 | 좌표표의 `SUR:041 허리 가운데`가 천골 위치(신장 58%)에 있다. 허리 옆(62%)과 같은 높이로 올려달라고 디자인 트랙에 넘겼다. 데이터만 교체하면 되는 건이다 |
| Search Field | 구현 완료. 1m 병원 찾기와 인체도 목록(#153)에서 쓴다. 2.0 문서에 항목이 없어 Figma 마스터를 따랐고, 3.0이 항목을 올렸다 |
| 로그인 화면(1o) | 카카오 버튼 + 서버 토큰 교환 구현 완료. 토큰 적용 완료 |
| 홈 화면(1n) | **10-15 시안으로 바꿨다**(#312). 맨 위 사진 배너(`Home Banner`, 400 · 처음 홈은 334) 아래로 흰 시트가 28 겹쳐 올라오고(위 반경 28 · `Elevation/Sheet` 두 겹) 탭바만 고정된다. 시트는 `Card / Action`(증상 정리 시작하기) · `Card / Resume`(이어서 하기, 진행 원 n/3) · 최근 브리핑 카드 두 장(카드 한 장 안에 줄 둘 · `Divider`, 배지는 1j-4와 같은 작성 중 · 진료 전 · 진료 완료 — 사용자 결정) · `Card / Schedule`(다가오는 일정, 날짜 블록 + D-day) 차례다. **배너는 다섯 장이고 4초마다 0.5초에 걸쳐 넘어가며 끝에서 첫 장으로 이어진다**(문서의 모션 규칙). 손으로 넘기는 동안은 세지 않고, 기기에서 애니메이션을 껐으면 저절로 넘기지 않는다. 헤더(로고 · 알림 · 아바타)는 고정이다. **첫 장은 상황에 따라 바뀐다**(사용자 결정) — 오늘 카드가 걸린 진료(이미 기록했으면 제외)면 "진료 잘 다녀오셨나요?"와 "진료 후 기록하기"(1m-12 → 1p), 내일이면 "내일 진료 전에 카드 한 번 볼까요?"와 "카드 다시 보기", 가장 최근 카드가 진료 전이고 그 카드로 잡은 일정이 없으면 "카드는 준비됐어요"와 "진료 일정 잡기"(그 카드가 골라진 일정 추가), 카드도 일정도 진료도 없으면 "처음 오셨네요"(넘김 없는 한 장 · 1n-2)다. 어디에도 안 맞으면 2~5장 가운데 하나를 무작위로 첫 장에 두고, 화면이 살아 있는 동안은 같은 장을 둔다. 일정 없는 카드인지는 홈 응답에 없어 그 카드를 `GET /api/cards/{id}`로 한 번 읽는다(`home` 도메인의 `HomeApi`에 따로 적었다). 병원 없는 오늘 일정은 "진료실에서 들은 말을"이다(사용자 결정). 2~5장 링크는 예약 문자 "방법 보기"(일정 추가), 일정 알림 "일정 추가하기", 카드 이미지 저장 "카드 저장하기"(가장 최근 카드의 1e-1-X 시트, 카드가 없으면 카드 목록 — 사용자 결정), 헤이 메이트 "메이트에게 말해보기"다. "방법 보기"는 일정 추가를 예약 문자 안내 시트(1r-4-S)가 뜬 채로 연다. "메이트에게 말해보기"는 마이크 권한을 받고 호출어 없이 Mate Card를 띄운다 — 호출어 설정과 상관없이 받아쓰기로 듣는다(`VoiceTargets.summon`). **배너 사진은 Figma가 이미지 레이어에 건 필터(밝기 · 색온도)까지 적용된 내보내기를 3배로 받은 것이다** — 원본 사진은 훨씬 밝아 그대로 쓰면 흰 글자가 읽히지 않는다. 사진 위 글자는 `Shadow/On Image · Text` 두 겹을 글자를 두 번 겹쳐 그려 낸다(`TextStyle`이 그림자를 한 겹만 받는다). 화면으로 돌아올 때마다 다시 읽는다. 읽지 않은 알림 표시는 늘 꺼 둔다 — 응답에 대응하는 값도 알림 목록 API도 없다. 홈 첫 진입 모션(Z 섹션, 1.8초)은 아직 넣지 않았다 |
| 진입 · 온보딩 | 스플래시와 온보딩 네 장(`V2-00`~`V2-03`) 구현 완료. **시안이 `A` 섹션을 통째로 교체했다.** `1a-1`·`1o`·`ONB-00`~`ONB-03` 프레임이 모두 삭제되고 `V2` 여섯 장이 들어왔으며 섹션 이름도 `A · 진입 — 스플래시 → 로그인 → 온보딩 v2 — 문구와 일러스트`가 됐다. 스플래시와 로그인은 `V2 · 스플래시`(`1320:4553`)·`V2 · 로그인`(`1320:4558`)이 문구와 구조까지 지금 구현과 같아 손대지 않았다. 온보딩 네 장은 문구·순서·그림이 전부 달라 다시 썼다(#131). 네 장이 한 목적지 안에서 넘어간다. 뒤로 갈 곳이 로그인이라 상단 바에 Nav Bar를 두지 않는다. **상단 바 오른쪽에 건너뛰기가 있다**(#213) — 누르면 소개를 끝내고 신상정보로 나간다. 마지막 장에도 있다. `Body/L Strong` 17인데 Ghost 버튼의 라벨이 15·13이라 버튼 컴포넌트를 쓰지 않고 글자에 48 터치 영역과 버튼 역할을 얹었다. 진행 표시는 상단 바의 세로 가운데다 — 건너뛰기가 들어오면서 시안이 y=26에서 가운데로 옮겼다. **그림은 10-15의 `Onboarding Illustration`(`3610:10270`) 래스터다**(#318) — 높이 340 칸 가운데에 단계마다 원래 크기(326 · 342×228 · 340 · 307) 그대로 두고 비율을 바꾸지 않는다. 2/4 · 3/4는 콘텐츠 320보다 넓어 거터로 조금 나가며 `requiredSize`로 줄이지 않는다. 그림은 3배 크기 무손실 WebP로 `drawable-nodpi`에 있고 예전 벡터는 걷어냈다. 글은 `Display/M` 제목 · `Body/L` 설명이고 Content가 위 72 · 사이 24다. 스플래시는 최소 2초 노출, 세션 복구를 기다리는 상한은 6초. 상한을 넘기거나 연결이 없으면 `SessionUiState.RestoreFailed`로 로그인 화면에 보내고 카카오 버튼 위에 이유를 적는다. 시안에 없는 문구다. 상한을 넘기면 진행 중인 재발급 요청도 취소한다. **장이 밀려서 바뀌고 기기 뒤로가기로 되짚는다**(#227) — 앞으로 갈 때는 오른쪽에서, 되짚을 때는 왼쪽에서 들어온다. 상단 바와 하단 버튼은 그대로 둔다. 첫 장의 뒤로가기는 아무 일도 하지 않는다 — 그냥 두면 로그인 화면으로 나가는데 이미 로그인한 사람이라 로그아웃된 것으로 읽힌다 |
| 신상정보 입력(1b) | 1b-1~1b-3 화면과 1b-4 완료 모션 구현. **홈의 등록 완료 토스트는 걷어냈다**(#194) — IA가 `1n-2-S`를 제외로 옮기고 1b-4를 채택했다. 문구도 1b-4의 제목과 같아 같은 말이 두 화면에 이어 나왔다. 마지막 단계의 완료가 `PUT /api/me/health-profile`이고 성공해야 1b-4로 넘어간다. **단계별 저장이 없다.** 서버가 부분 갱신을 주지 않고 여섯 필드를 모두 요구해서 세 단계를 모아 한 번에 보낸다. 중간에 나가면 아무것도 남지 않는, 임시저장이 없는 유일한 흐름이다. 이름·출생연도·성별은 1b가 묻지 않고 카카오 값을 읽어 그대로 되돌려 보낸다 |
| 증상 정리(부위·1d·1c) | **세 단계다**(#310 · Backend#169). 1단계 부위(앱의 3D 인체도, 시안 없음) → 2단계 통증 강도(1d) → 3단계 문답과 추천 질문 고르기(1c-2 · 1c-3 · 1c-D~D4)이고 진행 표시가 `n / 3`이다. **세션은 강도를 고르고 다음을 누를 때 연다** — 부위와 강도를 `POST /api/sessions`에 함께 보낸다. 강도로 돌아가 다시 고르면 세션을 새로 열지 않고 `PUT /severity`로 그 값만 고친다. 부위를 바꿔 다시 오면 그 세션은 버리고 새로 연다. **추천 질문은 문답이 끝난 턴에 실려 온다**(`TurnResponse.questionCandidates`). 온디바이스 세션은 서버가 만들지 않아 저장소가 그 턴에서 세션을 한 번 읽고 폰 후보를 보내 채운다. 문답이 끝나면 같은 대화 아래에 AI 말 · `Question Row` 줄 · 맨 아래 빈 칸("질문 직접 추가")이 서고 하단은 다음 버튼만 남는다. 줄을 누르면 체크가 바뀌고, 빈 칸을 누르면 하단이 문답과 같은 입력칸이 되어 적는 동안 그 줄이 "작성 중…"이다. 보내면 체크된 채 맨 아래에 붙는다. 하나라도 담으면 아래 AI 말이 몇 개를 담는지 적고, 둘째 줄은 직접 적은 질문이 있으면 "진료실에서 순서대로 물어보세요", 없으면 "빈 칸에 적어주세요"다(사용자 결정). 체크한 줄만 화면 차례로 `PUT /questions`에 간다. **다섯 개가 차면 더 체크되지 않고 빈 칸이 사라진다**(사용자 결정, 시안 없음). 체크 상자는 `Checkbox` 마스터 그대로 체크 표시가 16이다. 하단은 10-15 시안의 그림자 없는 `Footer`(`MedicalMateFooter`)이고 상단 내비는 불투명이다. 아래는 4단계 때 정리한 내용이고 단계 번호는 옛 것이다. AI 응답과 **음성 인식까지 연동됐다**(#190). 받아쓰기는 ML Kit GenAI이고 기기 안에서 돈다 — 오디오가 나가지 않는다. **말로 하는 대화가 한 바퀴 돈다**: 마이크를 누르면 바로 듣고(패널의 "말씀해 주세요"는 아직 못 들었다는 뜻이다), 말하는 글이 대화에 미리 서고, **말이 1초 끊기면 누르지 않아도 그 자리에서 보내고**(#318, 사용자 결정 — 받아쓴 글이 들어올 때마다 처음부터 다시 센다. 다 말하고 눌러도 보낸다), AI가 답하면 다시 듣는다. **듣기 시작하고 5초 동안 아무 말이 없으면 패널을 닫는다**(#318). **패널이 떠 있을 때 기기 뒤로 가기는 받아쓰기만 끈다** — 화면을 나가지 않는다. 입력칸이나 그 안의 마이크를 눌러도 끈다. 끄면 입력칸은 음성을 켜기 전에 적은 글로 돌아간다. 문답이 끝나면 "다음"이라고 말해 넘어간다. **안 되는 기기에서는 마이크를 그리지 않는다** — Basic 모드가 API 31 이상이고 AICore를 못 쓰는 기기가 있다. 4단계(1i)의 목록은 `SessionResponse.questionCandidates`를 `rank` 차례로 채운다(#173). **3단계에서 강도를 저장하는 응답으로 받는다** — 서버가 모든 세션 응답에 후보를 싣고, 그 호출이 4단계 직전의 마지막 왕복이다. 환자가 한 번이라도 손댔으면 후보로 덮지 않는다 — 덮으면 지운 질문이 되살아난다. **질문은 5개까지다**(#264 · Backend#132) — 상한이 `core/model/MAX_BRIEF_CARD_QUESTIONS`에 있고 4단계와 카드 편집(1e-1-E)이 함께 쓴다. 서버가 그 수를 넘기면 `PUT /questions`를 거절하는데 앱이 결과를 버려서, 후보 3개에 하나를 더 적으면 세션 질문이 빈 채로 남아 카드에 질문이 하나도 뜨지 않았다. 4단계는 차면 추가 버튼이 꺼지고 필드 아래에 그 사실을 적으며 구역 머리가 `N / 5개`다. 카드 편집은 차면 `질문 추가` 줄을 감춘다. 서버도 같은 날 5로 올렸다(Backend#132 반영, 2026-09-16 기기 확인). **첫 질문은 서버 것이다**(#259) — 세션을 만드는 `POST /api/sessions` 응답의 `messages`에 AI 첫 질문이 실려 오고 앱은 그것을 첫 마디로 세운다. 오기 전에는 답을 기다리는 점 셋만 둔다. 앱이 만든 문장(`intakeOpeningLine`)은 세션을 못 만들었을 때만 쓴다. 전에는 앱 문장을 먼저 세워서 첫 답을 보낸 뒤 대화가 서버 것으로 갈아 끼워질 때 첫 질문이 바뀌어 보였다. **상단 바 제목이 "기록"이고 진행 라벨이 "증상 정리"다** — 1c-2·1c-4·1c-5·1i 네 장이 그렇게 그려져 있다. **문답은 마디가 붙으면 끝으로 보낸다**(#176) — 키보드 높이도 조건에 넣는다. 마디가 늘지 않아도 보이는 높이가 줄면 마지막 마디가 입력창 뒤에 가린다. **이어서 하기는 늘 3단계로 돌아간다**(#310) — 세션은 강도를 고른 뒤에 생기기 때문이다. 문답이 끝났으면 추천 줄을 세우고 저장해 둔 것만 체크하며, 추천에 없는 저장 질문은 직접 적은 줄로 붙인다. `IntakeStep`은 홈도 쓰므로 `core/model`에 있다. **답을 기다리는 점 셋이 움직이고 AI의 말이 설 왼쪽에 선다**(#228) — 면 없이 점만 둔다. 기기에서 애니메이션을 껐으면 움직이지 않는다. **강도 슬라이더는 마스터 값이다**(#228) — 트랙 10 · 정지점 6 · 손잡이 28에 링 4이고, 정지점은 채움 위면 흰색 밖이면 테두리 색이다. 끄는 동안에는 손가락을 따라가고 놓을 때 단계로 붙는다. **질문을 더하면 그 줄로 화면이 따라간다** — 화면이 열릴 때 AI 후보가 채워지는 것까지는 따라가지 않는다 |
| 아픈 부위(1l) | **3D로 확정됐다**(#235). 부위 선택이 3D로 열리고 2D로 넘어가는 길을 두지 않는다. 2D 코드와 이미지는 지우지 않는다 — 되돌릴 판단이 남아 있고 좌표·온톨로지·검색이 그쪽에 묶여 있다. 목록에서 고르기는 3D 줄로 옮겼다. 전신·피부 칩은 다시 누르면 풀린다. 인체도 구현 완료. **시안의 Wireframe 섹션에 이 화면이 없다. 지워진 것이 아니라 아직 안 그린 것이다** — IA가 `1l | 1단계 · 부위 선택 (제작 예정)`으로 적고 수정사항 표도 "인체도 페이지 구현 필요 → 일단 대기"다. `1l-1`~`1l-3`과 `1c · 증상 문답 · 아픈부위 선택`(`1074:7302`) 프레임은 섹션 바깥 캔버스에 그대로 있다. **지금 구현이 이 화면의 유일한 기준이므로 지우지 않는다.** IA가 적은 갈래(앞뒤 · 좌우 · 전신·피부 칩 · 부위 확대 · 목록에서 고르기 · 인체도로 되돌아가기)가 구현과 같다. 앞뒤 전환 · 앵커 9개 · 확대 후 구역 25개 · 팔·다리 좌우 반전 · 전신·피부 칩 · 목록 대안까지 들어 있다. 부위 id는 AI 트랙의 온톨로지(`ANC:*` · `SUR:*`)를 쓴다. 이름과 별칭은 `BodyMapOntology.kt`가 들고 있고 `docs/examples/body-map.json`에서 **생성한 파일이라 손으로 고치지 않는다**(좌표 파일과 같은 규칙). 표시 문구의 출처를 이 파일 하나로 둔다 — 좌표 파일에도 이름이 들어 있는 판본이 있어 출처가 둘이면 어긋난다. 진료과는 응답에 있지만 보여줄 화면이 없어 담지 않았다 |
| 3D 인체도 | 2D 인체도 옆에 시험용으로 붙어 있다(#211). 화면 위 버튼으로 갈아 끼우고 고른 값은 양쪽이 같다. Filament(`filament-android` + `gltfio-android`)로 Draco 압축 glTF를 `TextureView`에 그린다 — `SurfaceView`는 창 뒤로 합성돼 아무것도 보이지 않는다. Filament의 JobSystem이 받아들이지 않은 스레드를 거부해서 엔진 호출을 모두 메인 스레드에서 한다. 짚는 것은 그리는 모델이 아니라 줄여 둔 충돌 메시(`body_collision.bin`)로 판정한다 — Möller–Trumbore 반직선 교차이고, 줄인 것이 판정을 바꾸지 않는다는 확인은 구역 점 46개를 앞뒤에서 겨냥한 92번이 원본과 같은 구역을 고르는 것으로 했다. **2D와 같은 두 단계다** — 큰 부위를 짚으면 그 부위로 카메라가 가고 거기서 구역을 고른다. 자리는 부위 크기에서 계산하되 0.55보다 가까이 가지 않는다(가슴·배·목·허리가 살갗에 붙어 어디인지가 사라진다). 조작 중에는 판이 화면에 다 들어오도록 스크롤을 붙인다. 판 우측 상단의 되돌리기로 전신으로 돌아온다 |
| 3D 인체도 자료 | `BodyMap3dGeometry.kt`는 디자인·AI 트랙이 준 `anchors3d.json`에서 생성한 파일이고 충돌 메시도 모델에서 생성한다. 좌표 파일과 같은 규칙으로 **손으로 고치지 않는다**. 구역·좌우는 2D와 같은 집합이어야 한다 — 한쪽에만 있으면 3D로 고른 선택이 2D 화면에서 켜지지 않는다. 시험이 그 일치를 본다 |
| 부위 선택 개수 | 한 곳만 고른다. 다중선택을 넣었다가 되돌렸다 |
| 고른 부위 표시 | 상태를 `focus`(확대한 앵커)와 `selection`(고른 부위)으로 나눈다. 하나로 두면 구역을 고르는 순간 화면이 앵커 단계로 돌아가 고른 점도 고른 줄도 보이지 않는다. 확대 화면의 제목과 좌우 반전도 `focus`를 봐야 맞는다 |
| 인체도 화면 배치 | 판이 505dp라 상태바·하단 버튼까지 빼면 판 위에 쓸 수 있는 높이가 131dp뿐이다. 앵커 화면에 설명 문구를 두지 않는 이유이고, 전신·피부 칩과 목록 전환을 한 줄에 합친 이유다. 이 둘을 판 아래에 두면 첫 화면에서 보이지 않고 다리 앵커가 하단 버튼 뒤로 들어간다 |
| 화면별 스크롤 | `StepContent`가 `scrollKey`를 받는다. 인체도의 세 화면이 한 스크롤 상태를 공유하면 아래로 내려 부위를 짚었을 때 확대 화면이 그 위치로 열려 제목이 잘린다 |
| 확대 애니메이션 | 짚은 점을 축으로 전신을 밀어내고 확대 이미지를 들여보낸다(`BodyMapTransition.kt`). 두 이미지가 다른 파일이라 이어 확대할 수 없어 그렇게 읽히도록 만든 것이다. 판 모양으로 잘라야 커지는 이미지가 위아래 버튼을 덮지 않는다. 기기에서 애니메이션을 끄면(`ANIMATOR_DURATION_SCALE` 0) 즉시 바뀐다 |
| 목록에서 고르기 | 고르는 줄은 `MedicalMateRadio`다. 라디오가 역할과 선택 상태를 시맨틱에 실어 스크린 리더가 "선택됨"을 읽는다. 앵커 줄은 하위 목록으로 들어가는 이동이라 `List Row`이고, 그 안에서 고른 부위 이름을 보조 텍스트에 적는다. **검색이 붙어 있다**(#153). 이름과 별칭 122개로 찾고 서버를 부르지 않는다 — 같은 규칙의 엔드포인트가 있지만 한 글자마다 왕복하게 된다. 규칙은 AI 트랙의 `docs/android-body-search.md`이고 검증 벡터 165케이스를 순서까지 대조한다. 결과 줄은 목록과 같은 모양이고 고르는 것은 한 곳이다. 0건이면 직전 결과를 남긴다 — 한글 조합 중간 상태가 0건이라 그대로 그리면 깜빡인다. **한/영 자판 오타 복원은 서버만 한다.** 두벌식 오토마타를 옮기는 비용이 커서 AI 트랙과 빼기로 정했고, 그래서 앱과 서버 결과가 그 지점에서 갈린다 |
| 진료과 안내 | 없다. 응답(`docs/examples/body-map.json`)이 부위별 진료과를 함께 주지만 화면에 넣지 않았고, 픽스처에도 담지 않았다. 넣게 되면 응답에서 다시 가져온다 |
| 부위 이름 조사 | 물음의 주격 조사를 문자열 리소스에 박지 않는다. 부위가 25가지라 `%1$s가`로 고정하면 "무릎가"가 된다. 문구는 리소스에, 조사는 `withSubjectParticle`이 계산해 `IntakeUiState.bodyPartSubject`로 넘긴다 |
| 브리핑 카드(1e·1f·1j-4) | 카드 읽기·전체 수정·진료실 화면과 카드 전체 목록(1j-4) 구현. 목록은 홈의 "전체 보기"에서 들어가고 편집으로 여러 장을 골라 지운다. 읽기·수정·확정이 `GET`·`PATCH`·`POST /api/cards/{id}`에 붙어 있고(#139) 삭제는 #157이다. 카드 본문은 `BriefCardBlock` 하나이고 1e-1·진료실 화면이 같이 쓴다(#167). **강도는 KV 줄이 아니라 눈금이다** — `severity` 축에서 숫자만 읽어 다섯 단계에 맞추고, 밖이면 그리지 않는다. **복용약·기저질환·알러지가 카드 응답에 실린다**(#181) — `patient.medications`·`conditions`·`allergies`이고 앞의 둘은 카드 줄, 알러지는 카드 밖 경고다. 전에는 화면이 `GET /api/me/health-profile`에서 읽어 얹어 보는 시점의 프로필이 찍혔는데 그 우회를 걷어냈다. 알러지만 한 줄로 와서 쉼표로 나눈다. **진료실 전달은 이 화면이다**(#194) — IA가 `1f`를 제외로 옮기고 1e-1을 그대로 건네기로 해서 전달 화면·목적지·API를 걷어냈다. **카드를 확정하는 자리가 없다** — `POST /api/cards/{id}/confirm`을 부르던 것이 그 버튼 하나였고, 버튼이 빠진 뒤로는 걷어내기 전에도 불리지 않았다. 어디서 확정할지는 미정이다. **병원은 라우트를 타고 들어온다** — 카드 응답에 없고 목록 응답과 홈 응답의 `clinicName`에만 있어서, 홈·브리핑 카드 전체에서 누른 줄이 들고 있던 값을 함께 넘긴다. **병원 찾기(1m-B)에서 고른 경우에는 주소도 함께 나른다**(#194) — 시안 1e-1의 병원 블록이 이름 아래에 주소를 그린다. 목록에서 연 카드는 `CardSummary`에 주소가 없어 이름뿐이다(Backend#101). 서버가 카드 응답에 담아 주면 나르던 코드를 지운다. **내보내기가 카드 본문 전체를 한 장의 PNG로 찍어 공유 시트로 보낸다**(#275) — 스크롤로 가려진 아래까지 담으려고 스크롤 안쪽 내용을 `GraphicsLayer`에 함께 그려 두고(`recordInto`) 누를 때 이미지로 뽑는다. 면색을 함께 담아 받는 앱이 바탕을 검게 그리지 않는다. 하단 버튼은 담기지 않는다. 앱 캐시(`cache/shared/brief_card.png`)에 덮어쓰고 `FileProvider`로 내주므로 저장소 권한이 없고 갤러리에 남지 않는다. 편집 중에는 감춘다. 카드에 복용약·기저질환·알러지가 들어 있지만 사람이 고른 앱으로만 나간다. 캡처와 공유는 `Context`가 필요해 JVM 유닛 테스트가 없다 — 기기에서 확인한다. **내보내기는 카드 머리에 있다**(#314) — 10-15 시안이 상단 바의 `공유` 글자를 걷고 카드 제목 줄 끝에 `export` 아이콘(Icon Button S)을 두었다. 이미지로 찍는 그 한 프레임에는 그 버튼을 감춘다. **누르면 내보내기 시트가 뜬다**(#306) — `이미지 저장`과 `공유하기` 두 버튼과 `이름·나이 가리기` 토글이다. 가리기는 처음에 켜져 있고 저장과 공유에 함께 걸린다. 켜면 찍는 그 한 프레임만 환자 줄을 가린 줄(`maskedPatientLine`, 성만 남기고 나이를 뺀다)로 그리고 찍는다 — 같은 카드를 한 벌 더 그리면 둘이 어긋날 자리가 생긴다. 저장은 `Pictures/진료메이트` 앨범에 새 파일로 남는다(`MediaStore`, Android 10부터 권한 없음). 9 이하는 `WRITE_EXTERNAL_STORAGE`(`maxSdkVersion=28`)를 그때 묻는다. 결과는 카드 칸 아래의 `Toast`가 2초 알린다. 시트는 1e-1-X(`3663:7463`) 그대로다(#314) — 제목 · 설명 · 미리보기(`Export Preview / Long Image`, 축소 카드에 "긴 이미지 1장" Info 칩) · 가리기 줄(`Body/M` + 스위치) · Primary `이미지 저장` · Outline `공유하기`다. 미리보기의 제목과 환자 줄은 그 카드 값이고 가리기를 켜면 함께 바뀐다. **카드를 지우면 카드 목록으로 간다**(#194) — IA `1e-1-DC`가 그렇게 적는다. 카드를 만드는 것은 문답 쪽(`POST /api/sessions/{id}/card`)이다. **확정한 카드를 고치면 서버가 새 버전을 만든다**(Backend#114) — 응답의 `cardId`가 달라지므로 화면이 그 카드로 갈아타야 한다. 라우트가 든 옛 id로 다시 읽으면 고친 것이 사라진 것처럼 보이고, 옛 id로 다시 고치면 버전이 가지를 쳐서 그 편집이 어느 화면에도 나오지 않는다(#219). 그래도 남은 경로가 있으면 서버가 `CARD_ALREADY_EDITED`로 막고 `details.latestCardId`를 준다(Backend#117). 그 카드로 갈아타고 **한 번만** 다시 보낸다(#222). **하단 버튼은 그 카드의 일정으로 갈린다**(#314, 시안 1e-1 네 장) — 저장하지 않았으면 `저장하기`, 저장했고 그 카드로 잡은 일정이 없으면 `진료 일정 잡기`(일정 추가), 앞날 일정이면 Outline `진료 일정 보기`(일자 화면), 당일이거나 지났으면 Tonal `진료 후 기록하기`(1m-12)다. 일정은 카드 응답의 `appointment`(안 지난 것 중 가장 가까운 것, 다 지났으면 가장 최근 것)이고 화면에 돌아올 때마다 다시 읽는다. 하단은 그림자 없는 `Footer`다. **처음 저장하면 화면에 남고 1e-1-S 시트가 뜬다** — "진료 날짜가 정해졌나요?"에 `진료 일정 잡기`와 Ghost `아직 안 정했어요`다. 그 카드로 잡은 일정이 이미 있으면 띄우지 않는다. 전에는 저장하기가 곧 홈으로 나가기였다(#229에서 저장한 카드의 하단을 없앴던 것도 이 버튼 셋으로 바뀌었다). 증상 정리에서 막 만든 카드를 저장한 뒤의 뒤로 가기는 홈이다 — 아래에 끝난 문답이 쌓여 있다. 저장이 곧 확정이고 확정하지 않은 카드에는 진료 후 기록을 붙일 수 없다. 카드 안에서 병원만 바꾸러 1m-B로 갈 때는 CTA가 "브리핑 카드 만들기"가 아니라 "완료"다 — 계속 따라가면 어디서 멈출지 알 수 없고, 다른 데서 고치는 중이라면 덮어쓰기를 반복하게 된다 |
| 진료 후 기록(1m·1p·1q) | 세 화면 구현(1m·1p·1q-1). 캘린더 일자의 "진료 후 기록하기"에서 들어간다. **그 앞에 병원 확인(1m-12, `1576:8517`)이 선다**(#246) — 일정에 등록한 병원이 맞는지 묻고, 맞으면 바로 1p로, "다른 병원이에요"면 1m으로 간다. 병원은 일정을 추가할 때 이미 등록하는 값이라 아는 채로 다시 찾게 할 이유가 없다. 일정에 병원이 없을 때만 1m이 첫 화면이다. 주소는 일정 응답에 없어 카드에 남은 것(1m-B에서 고른 값)을 적고 없으면 이름만 적는다. 1q-1의 저장이 `POST /api/cards/{cardId}/visit`이고, 그 요청에 들어갈 값(병원 이름·붙일 카드·원문 메모)이 라우트를 타고 1m → 1p → 1q-1로 따라간다. 1p의 병원 블록(아이콘·병원 이름·무엇으로 받은 진료인지·구분선·언제인지)은 픽스처가 아니라 앞 화면에서 고른 값이다 — 카드 제목은 캘린더 일자에서 라우트를 타고 따라온다. `Hospital Card` 컴포넌트를 쓰지 않는다. 그쪽은 흰 면에 칩 pill이고 여기는 조용한 면에 평평한 글줄이라, 시안에서도 인스턴스가 아니라 프레임이다. **받아쓰는 중에는 적던 글 아래에 `Voice Input · Mate` 카드가 선다**(#318, 1p-T) — 파형과 "듣고 있어요"이고 그동안 마이크 FAB는 감춘다. 카드의 글은 시안대로 "선생님이 뭐라고 하셨나요?" · "듣고 있어요"로 고정이고 카드는 눌러도 아무 일이 없다(사용자 결정). 적는 칸을 누르면 받아쓰기를 끝내고 손으로 적는다. 5초 동안 받아쓴 글이 없으면 패널을 닫고 적힌 글은 남긴다(#318). 적는 칸 아래의 "AI로 정리하기" 칩과 툴팁은 10-15 시안에서 빠져 걷어냈다. 하단은 그림자 없는 `Footer`다. 마이크 버튼은 `Icon Button`이 아니라 `FAB`다 — 48 원형·브랜드 채움은 같고 그림자가 있어 본문 위에 뜬 층으로 읽힌다. 캘린더의 일정 추가도 시안에서는 같은 FAB인데 아직 텍스트 버튼이다. **받아쓰기가 붙었다**(#190). 증상 문답과 같은 `Dictation`을 쓰고, 말이 끊겼다 이어져도 앞말 뒤에 붙는다. **1q-1에 재방문 일정 등록 체크가 없다** — 시안에서 빠졌다. 재방문 날짜는 `follow_up` 축으로 실려 간다(#178). **저장이 되면 그 날짜로 재방문 일정이 만들어진다**(#245) — 서버는 기록에서 일정을 만들지 않는다(문서가 "환자가 보고 등록하는 흐름"으로 못 박았고 AI가 날짜를 잘못 뽑아도 조용히 생기지 않아야 한다고 적는다). 그 확인은 1q-1의 재방문 줄이 한다 — 날짜가 보이고 고칠 수 있어 저장이 곧 확인이다. `core/model/FollowUpScheduler`를 `calendar`가 구현하고 `visit`이 받는다. 같은 카드로 그 날 일정이 있으면 만들지 않고, 시각은 비우며, 출처가 `VISIT_FOLLOW_UP`이라 홈·일자 화면이 "재진"으로 적는다. 못 만들어도 저장은 성공이다. **일정에 카드가 걸려 있지 않으면 일자 화면에 "진료 후 기록하기"를 두지 않는다.** **아직 오지 않은 날에도 두지 않는다**(#302) — 기록 날짜가 그 일자라서 앞날에 적으면 다녀오지 않은 진료가 기록으로 남고, 캘린더가 그 날을 "기록 있음"으로 찍어 일정을 지워도 점이 남았다(기기에서 15일 일정을 지운 뒤 일정 응답에서는 빠지고 기록 응답에는 남는 것으로 확인). 말로 해도 진료가 끝난 뒤에 하라고만 한다. 오늘은 된다. **다녀왔는지는 그 날 기록 전부가 아니라 그 날 일정의 카드로 남긴 기록으로 가린다**(#302) — 서버에서 기록은 일정이 아니라 카드에 붙어 일정을 지워도 남는다. 같은 날 다른 카드로 새 일정을 잡으면 남은 기록 때문에 새 일정이 "진료 완료"로 서고 기록을 남길 수 없었다. 카드를 고치면 id가 바뀌므로 `GET /api/cards/{cardId}/visits`로 버전을 묶어 본다. 다른 카드의 기록은 그 일정의 진료가 아니라서 "이 날 기록"에도 세우지 않는다 — 그 일정으로 아직 적지 않았으면 "아직 진료 전이에요"와 기록하기가 선다. 일정 없이 기록만 남은 날은 그 기록을 모두 세운다. **일정을 지우면 그 일정의 기록(화면의 "이 날 기록")도 앱이 함께 지운다** 일정을 먼저 지우고 기록을 한 건씩 지운다. 기록부터 지웠다가 일정이 안 지워지면 진료 기록만 잃는다. 홈의 `pendingRecordOn`은 서버가 날짜로만 보므로 같은 경우에 이 일정을 기록한 것으로 본다 — 백엔드에 알릴 것. 일정 추가에서 **시간은 필수가 아니다**(#172) — 안 고르면 오전 9시로 저장한다. **새 일정은 가져갈 카드가 하나 이상 있어야 한다**(#312, 사용자 결정) — 오늘 진료 배너와 진료 후 기록이 카드에 붙어서다. 시안 1r-4대로 병원 · 날짜 · 카드가 차기 전까지 저장하기가 비활성이고, 말로 저장하라고 하면 빠진 칸을 알리고 카드를 묻는다. 고치러 연 일정은 카드를 보내지 않으므로 묻지 않는다. 시안 1r-4-B는 날짜가 빈 채로 버튼이 켜져 있지만 서버가 날짜를 요구해 날짜도 필수로 두었다. 하단은 그림자 없는 `Footer`이고 병원이 빈 동안 그 위에 예약 문자 `Hint Row`가 선다. 누르면 1r-4-S 안내 시트(그림 · 네 단계, 버튼 없음)가 뜬다. 상단 왼쪽은 시안 세 장이 모두 그리는 ‹다(DESIGN.md의 Nav Bar 조합 표는 Close로 적는다). 서버 `scheduledAt`이 시각을 요구하고 미정을 실을 자리가 없다. 서버가 카드에 매달린 기록만 받아서, 카드 없이 들어가면 끝에서 저장이 아무 일도 하지 않았다. **AI 분류가 `POST /api/visits/classify`에 붙었다**(#183). 1p에서 적은 메모를 축 맵으로 나눠 받고 저장은 하지 않는다. **항목은 고정이 아니다** — 값이 있는 축만 줄로 서고 모르는 축도 뒤에 붙는다. 하나도 못 나눴을 때만 소견·검사·약·재방문 네 자리를 빈 채로 연다. 편집에서 줄을 더할 수 없어서고, 그 자리는 #184다. 다시 나눌 때는 직전 `labels`를 함께 보낸다 — 안 보내면 AI 모델 호출이 다시 나간다. **진료 날짜는 오늘이 아니라 흐름이 시작된 캘린더 일자다**(#178). 어제 진료를 오늘 적어도 그 일자 화면에 선다. 병원 검색은 `GET /api/hospitals`다(#155). 원천이 심평원이고 서버가 목록을 들고 있지 않아 앱도 캐시하지 않는다. **이름과 주소가 온다**(#187) — 우리가 요청해서 홈페이지 자리를 주소가 대신했다(Backend#80). 같은 이름의 다른 지점을 구별하는 값이라 결과 줄에 함께 적고, 심평원에 없는 곳은 비어 있어 그때는 줄을 그리지 않는다. **상류가 죽으면 빈 목록이 아니라 에러가 온다** — "검색 결과 없음"과 "지금 검색이 안 됨"을 가르던 코드가 그제야 제 일을 한다. 검색어가 멎은 뒤(300ms) 한 번 부르고 앞선 요청은 취소한다. **진료 전(1m-B)의 상단 바에는 뒤로가기만 있다**(#228) — 병원 없이 카드로 가던 건너뛰기를 걷어냈다. 시안에 그 자리가 없고, 나가는 길은 뒤로가기이며 1c-5의 "브리핑 카드 만들기"가 그대로 있다. 빈 상태는 병원 아이콘이고 남은 높이를 받아 가운데에 선다. 부분 일치라 받은 것보다 많으면 그 사실을 줄에 적는다. **1k 이번 진료 정리는 시안에서 사라졌다.** 유저플로우가 1q-1 저장에서 캘린더 일자로 바로 잇는다. #159에서 붙였다가 #165에서 화면·상태·문자열을 함께 걷어냈다. 저장하면 흐름이 시작된 일자로 돌아가고 그 화면이 다시 읽으면서 방금 남긴 기록이 "이 날 기록"으로 선다. **저장이 안 될 때 다시 눌러 될 일인지를 가려 적는다**(#220) — 연결이 없거나 서버가 재시도 여지를 준 경우만 "잠시 후 다시 시도해주세요"이고, 서버가 거절한 경우는 브리핑 카드를 먼저 저장해야 한다고 적는다. 한 문구로 두면 눌러도 되지 않는 버튼을 계속 누르게 된다 |
| 기록·캘린더(1j·1r) | G·H 섹션 대조 완료(#121). 기록 목록·비어 있음·상세와 상세의 두 상태(카드 펼침 1j-3-X · 재방문 누적 1j-3-R), 목록 편집과 여러 건 삭제(1j-1-D·D2·DC), 캘린더 월·일자와 카드만 있는 날 시트(1r-1-S), 일정 추가 여덟 장(1r-4 계열), 일자 편집과 일정 삭제(1r-2-E·E2·DC), 다녀온 날과 다음 일정(1r-2-A·A2)까지 구현. 기록 목록은 `GET /api/me/visits`를 달로 묶고, 상세는 `GET /api/visits/{id}`다. **상세 타임라인이 세 단계다**(#171). 예정 · 진료 후 기록 · 브리핑 카드 순으로 최신이 위다 — 시안 `1j-3`은 카드가 위이고 `1j-3-R`은 최신이 위인데 디자인 피드백이 최신 위를 멘탈 모델로 적어 둔 쪽을 따랐다. 기록 · 카드 · 일정 셋을 읽는다. 기록을 못 읽으면 실패이고 나머지 둘은 못 읽어도 그 단계만 빠진다. **재방문이 쌓이면 한 화면에 모두 선다**(#221) — 카드 하나에 기록 하나였던 제약이 풀리면서(Backend#119) 같은 문답에 재방문이 이어 붙는다. `GET /api/cards/{cardId}/visits`로 모은다. 목록을 `cardId`로 거르면 안 된다 — 재방문 전에 카드를 고치면 두 기록이 서로 다른 카드 행에 붙고 목록의 `cardId`는 최신 버전이라 묶을 열쇠가 못 된다(Backend#121). 모아 주는 목록에 축이 없어 기록마다 한 번씩 더 읽고, 못 읽은 것은 빼고 그린다. 머리글은 시안 그대로다(#243) — 제목이 병원이 아니라 브리핑 카드의 제목이고(카드 응답의 제목이 비면 목록이 든 제목, 그것도 없으면 병원), 둘째 줄은 병원이 앞이고 날짜가 뒤다. 한 번이면 `1j-3`의 `병원 · 09.12 진료`, 여러 번이면 `1j-3-R`의 `병원 · 09.12 초진 · 09.26 재방문`이다. 블록 제목은 시점 줄과 같은 말이다(`진료 후 기록` · `브리핑 카드`) — "진료에서 들은 것"·"진료 전에 정리한 것"으로 따로 이름 붙이던 것을 걷어냈다. 카드 단계의 시점 줄은 `09.04 작성`이다 — 시안은 뒤에 `09.12 진료실에서 보여줌`을 잇지만 그 날짜는 바로 위 진료 후 기록 단계가 이미 적어서 뺐다. 예정 알림은 "다음 진료가 예약돼 있어요" 아래 `9월 26일 (토) 오전 10:30`이다. 미리보기 픽스처(`RecordDetailFixtures.kt`)가 먼저 시안 문구였고 실제 매핑만 뒤처져 있었다. **카드 전체 보기는 높이가 이어지며 펼쳐지고 자리를 잡은 뒤 그 블록으로 화면을 옮긴다**(#243). `animateContentSize`가 내용을 자기 크기로 잘라서 `shadow`보다 안쪽에 두면 카드 좌우 그림자가 잘린다 — 그림자를 바깥에 둔다. **진료 후 기록 단계에 원문 인용이 없다** — 원문은 1q-1에서 확인하고 저장하는 값이다. 예정 단계는 그 카드로 잡힌 앞으로의 일정이 있을 때만 붙는다(재방문 날짜가 기록 응답에 없다). 카드 단계에 건강 정보가 함께 선다(#181). 서버가 카드에 박아 준 값이라 그때 먹던 약이 남는다. 일자 화면의 기록은 `GET /api/me/visits`를 그 날짜로 거른 것이고, **그 줄을 누르면 기록 상세(1j-3)가 아니라 진료 후 기록 상세로 간다**(#256) — `VisitDetailDestination`이 `GET /api/visits/{id}`로 읽어 1q-1과 같은 카드를 읽기 전용으로 그린다. 1j-3은 카드·기록·예정을 모아 보이는 자리라 기록 탭의 줄만 그리로 간다. 줄의 제목은 시안대로 `진료 후 기록`이고 메타는 상세를 따로 읽어 **소견·약·재방문 셋만** 그 차례로 값의 첫 줄을 `·`로 이은 것이다(#262 — 검사와 AI가 늘린 축은 넣지 않는다. 셋이 다 없거나 못 읽으면 병원 이름). 일정 카드의 마지막 줄은 `복부 통증 브리핑 카드를 가져가요`다. 다음 일정은 `GET /api/me/appointments/upcoming`의 첫 줄이다(#163). 기록이 있는 날에만 다음 일정을 붙이고 그 날 자신의 일정은 뺀다. 진료가 끝난 날에는 진료 전 할 일을 두지 않는다. **할 일은 그 날 일정에 매달린다**(#187) — `AppointmentResponse.todos`이고 `POST`·`PATCH`로 보낸다. 편집 중에는 저장하지 않는다(취소가 실행 취소를 대신하는데 이미 보냈으면 되돌릴 것이 없다). 마칠 때 통째로 갈아끼운다 — 지운 줄이 남지 않으려면 화면에 있는 것을 전부 보내야 한다. 일정이 없는 날에는 할 일도 없다. 목록 삭제는 `DELETE /api/visits/{id}`다(#178) — 서버가 카드 삭제와 갈라 줘서 기록만 지운다. 한 번에 지우는 API가 없어 한 건씩 부르고, 일부가 실패해도 나머지는 계속 지운다. `1r-2-C`는 IA가 `1r-1-S로 대체`로 적어 시트 쪽으로 정해졌다(#189). **그 시트는 날을 눌렀을 때만 뜬다**(#302) — 달을 다시 읽을 때 새로 띄우면, 시트에서 카드나 일정으로 갔다가 돌아올 때마다 다시 떠서 지나온 화면으로 또 보내려는 것처럼 보였다. 시트의 버튼은 시트를 닫고 옮기고, 달을 다시 읽을 때는 떠 있던 시트만 새 값으로 맞춘다. **캘린더의 "기록 있음" 점은 카드를 쓴 날과 진료 후 기록을 남긴 날이다**(#251) — 기록을 남긴 날을 빼면 재방문하고 기록까지 남긴 날이 그 날 일정 때문에 "예정"으로 남는다. 기록이 예정보다 앞선다. **기록 상세의 예정은 가장 최근 기록보다 뒤의 일정이다**(#251) — 열어 본 기록 기준으로 찾으면 첫 기록에서 열 때 이미 다녀온 재방문 일정이 예정으로 선다. 잡아 둔 일정이 없으면 최신 기록의 재방문 날짜를 "재방문 예정이에요"로 세운다. **진료를 마친 카드로 잡은 일정은 재진이다**(#272) — 일자 화면과 홈은 일정의 `origin`만 보고 초진·재진을 가르는데, 일정 추가로 직접 만든 일정은 `MANUAL`이라 같은 카드로 진료 후 기록을 남긴 뒤 다시 잡아도 "초진"으로 떴다. 기록 상세는 같은 진료를 재방문으로 적는다. 그래서 고른 카드가 `visited`면 `VISIT_FOLLOW_UP`으로 저장한다. 이미 저장된 일정과 고치기(`PATCH`, 출처 자리가 없다)는 그대로다. 확정한 카드를 고치면 버전이 갈라져 앱이 정확히 가리기 어려워, 서버가 일정에 초진·재진을 담아 주는 것이 정확하다 — 백엔드에 알릴 것. **일자 화면은 그 날 기록을 전부 세운다**(#235 · #179) — 하루에 진료가 둘이면 기록도 둘이다. 카드와 일정은 초진에 묶이고 기록만 진료마다 쌓인다. **카드만 있는 날 시트는 그 카드로 만든 일정이 있으면 그리로 보낸다**(#235). **일정 추가는 열릴 때 날짜와 병원을 들고 간다**(#230) — 캘린더에서 고른 날(보고 있는 달일 때만), 일자 화면의 그 날, 다음 일정의 그 날이 각각 따라온다. 아무 날도 안 고른 채 들어오면 빈 채로 열고 그 화면에서 고른다(1r-4-D). 가져갈 카드를 고르면 그 카드의 병원이 채워진다(1r-4-B) — 이미 적힌 병원은 덮지 않는다. **시간 미정 일정은 눌러서 고친다**(#230) — 일자 화면의 카드를 누르면 일정 추가가 그 값으로 열리고 저장이 `PATCH`로 나간다. 새로 만들면 같은 일정이 둘이 된다. 서버에 일정 하나를 id로 읽는 경로가 없어 그 날의 목록에서 찾는다. 수정 요청에 병원 자리가 없어 병원은 보내지 않는다. **시간 휠은 눌러서도 고른다**(#230) — 굴리는 길만 두면 값이 둘뿐인 열에서 얼마나 움직여야 하는지가 안 보이고, `ModalBottomSheet`가 남은 스크롤을 받아 시트를 닫아 버린다. 그 남은 양은 휠에서 삼킨다. **일자 편집의 일정 삭제는 하단 고정이 아니라 본문 끝이다**(#230) — 고정하면 편집을 누르는 순간 마지막 요소를 자른다 |
| 하단 탭 | 캘린더·홈·기록 세 탭 연결 완료. 홈이 가운데다. 내 정보는 탭이 아니라 홈 헤더 아바타로 진입. 마스터에 네 번째 variant(`Active=Active4`, 아무 탭도 활성이 아닌 상태)가 있지만 쓰는 화면이 없어 열지 않았다(#194) |
| 내 정보(1s) | **10-15 시안 1s-1 · 1s-3으로 바꿨다**(#316). 프로필 카드 아래로 건강 정보 · 알림 · 메이트 · 계정 · 앱 정보가 덩이 사이 14로 서고, 줄은 `Setting Row`(Toggle · Navigation · Value), 카드는 반경 20 · `Elevation/Card` 두 겹 · 좌우 18 · 위아래 6(건강 정보는 4)이다. 1s-1의 건강 요약과 1s-2가 `GET`·`PUT /api/me/health-profile`이고 1s-2 · 1s-3에서 돌아오면 다시 읽는다(`ON_RESUME`). 프로필 줄(이름·생년·성별)도 같은 응답에서 온다. 이름은 서버가 마스킹해 줄 수 있어("김OO") 아바타 글자를 자르는 자리를 ViewModel에 뒀다. 로그인 수단은 카카오 하나뿐이라 그 줄은 고정이다. **알림은 계정 값이다**(Backend#176) — `GET`·`PATCH /api/me/settings`의 진료 전 · 진료 후 기록 토글과 시간 미정 일정의 알림 시각이다. PATCH는 바꾼 필드만 보낸다. 토글 설명과 `알림 시간` 줄의 값이 그 시각을 적는다("미정이면 전날 오후 7시" · "오후 7:00"). `알림 시간`이 1s-3이다 — 위 카드는 시각을 정한 일정의 규칙(24시간 전 · 2시간 뒤)을 읽기만 하게 보이고, 아래 `Time Wheel`(다섯 줄 240 · 띠 48 반경 14)이 시간 미정 일정의 시각이다. 서버에는 전날 · 당일 두 칸이지만 휠 하나가 둘을 함께 정한다(사용자 결정). 분은 시안의 00 · 30이 아니라 10분 단위다(사용자 결정). `확인`을 눌러야 저장되고 하단 `Hint Row`(bell)가 고른 시각으로 예를 든다. 휴대폰 알림 권한이 꺼져 있으면 알림 카드 아래에 Warning `Notice`가 선다. **메이트 카드는 헤이 메이트로 부르기와 휴대폰에서 기록 정리다** — 뒤의 것은 시안에 없고 온디바이스를 확정할 때까지 시험용으로 둘째 줄에 둔다(사용자 결정). 둘 다 이 기기에서 켤 수 있을 때만 선다. 헤이 메이트 토글은 기기 값이다. 예전의 "브리핑 카드 자동 저장" · "진료실 화면 밝기 최대"는 시안에서 빠졌고 어디서도 값을 읽지 않아 `LocalSettingsStore`와 함께 걷어냈다(사용자 결정). 계정 카드는 로그아웃 · 회원탈퇴 두 줄이다. **이용약관 · 개인정보 처리방침은 자리만 있다** — 출시 준비 때 붙인다(사용자 결정). 앱 버전은 `BuildConfig.VERSION_NAME`이다. 프로필 카드의 아바타는 시안 인스턴스가 56에 반경 22로 그렸지만 마스터 `Avatar`(44 원형)를 키운 흔적이라 원형으로 뒀다 |
| 진료 알림 | 구현(#268 · #316). WorkManager로 로컬 알림을 띄운다. **진료 전 알림과 진료 후 기록 알림 둘이다**(#316, Backend#176) — 시각을 정한 일정은 진료 24시간 전과 2시간 뒤, 시간 미정이면 전날과 당일 정한 시각(기본 오후 7시)이다. 꺼 둔 쪽은 걸지 않는다. 전에는 전날 저녁 8시 하나뿐이었다. **알림 문구는 디자인 트랙이 넘긴 네 가지다** — 진료 전은 시각이 있으면 "내일 오전 10:30 진료예요", 미정이면 "내일 진료가 있어요"이고 본문이 "서울OO병원 내과 · 일정 확인하기"다. 진료 후 기록은 시각이 있으면 "진료 잘 다녀오셨어요?", 미정이면 "오늘 진료 다녀오셨어요?"이고 본문이 "… · 들은 말 남기기"다. 본문 앞은 일정의 병원과 진료과이고 없으면 할 일만 적는다. **debug 빌드는 네 알림을 바로 띄울 수 있다** — `adb shell am broadcast -n com.mist.medicalmate/.calendar.reminder.ReminderPreviewReceiver`가 앱의 일꾼에 견본 값을 넣어 돌린다(`src/debug`, release에는 없다). 2026-10-15 갤럭시 S24 울트라에서 네 문구가 시안대로 뜨는 것을 확인했다. 일정 하나에 알림이 둘이라 알림 번호를 종류마다 달리한다. 예약에 날짜가 빠져 있어 알림을 눌러도 앱만 열리던 것을 #316에서 고쳤다. **다가오는 일정 전체를 다시 읽어 통째로 갈아끼운다** — 다른 기기에서 고친 일정은 무엇이 바뀌었는지 이 기기에 오지 않는다. 맞추는 자리는 일정 Repository의 쓰기 셋(생성·수정·삭제)과 로그인, 토글 저장이다. 화면마다 부르지 않는다. **못 읽으면 걸린 예약을 그대로 둔다**(토글을 못 읽어도 같다). 로그아웃과 재발급 거절은 모두 `SignedOut`으로 모이고 그 자리에서 지운다. 예약은 `calendar/reminder`, 토글은 `profile`, 세션은 `auth`라 `core/model`의 `VisitReminders`·`VisitReminderSetting`으로 잇는다. 예약 쪽이 일정 Repository를 읽고 Repository가 예약을 부르는 순환은 `dagger.Lazy`로 끊었다. **권한(`POST_NOTIFICATIONS`)은 일정을 저장할 때와 토글을 켤 때 묻는다** — 결과를 기다리지 않고, 뜰 때 다시 본다. **알림을 누르면 그 일정의 일자 화면(1r-2)이 열린다.** 알림이 앱의 시작 인텐트에 날짜와 일정 id(`VisitReminderLink`)를 싣고 `MainActivity`가 꺼내 그래프에 넘긴다. 홈 위에 얹으므로 뒤로 가면 홈이다. 열려 있던 앱이면 `CLEAR_TOP`·`SINGLE_TOP`으로 같은 Activity가 받는다. 홈에 갈 수 있는 세션이 아니면(로그아웃·온보딩 중) 링크를 버린다 — 들고 있다가 다음 로그인 뒤에 옮기면 다른 계정의 날이 열린다. 다시 만들어진 Activity와 최근 앱에서 연 경우는 처음 인텐트를 다시 읽지 않는다 |
| 예약 문자 공유 | 구현(#270). 문자 앱의 공유 메뉴에 앱 이름("진료메이트")으로 뜨고, 받은 글로 일정 추가(1r-4)가 채워진 채 열린다. **캘린더 일정 추가 요청(`ACTION_INSERT`)도 받는다** — 갤럭시 메시지가 날짜를 알아보면 띄우는 "일정 추가"가 이 길이다. 보낸 앱이 읽은 날짜·시각이 숫자로 와서 그대로 쓰고, 제목·장소·설명은 병원을 찾는 데만 쓴다. 보낸 앱이 본문을 어느 키에 싣는지 정해져 있지 않아 글로 된 값은 모두 모아 찾는다. **갤럭시 메시지의 "일정 추가"는 문자 본문을 싣지 않는다**(2026-10-08 기기 확인) — 날짜·시각만 오고 병원은 채워지지 않는다. 병원까지 채우려면 공유로 들어와야 한다. 하루 종일 일정은 시각을 버리고 날짜를 UTC로 읽는다. 넘겨받은 날짜는 글을 읽기 전에 채운다 — 읽은 결과가 빈 칸만 채우고 안내도 날짜 칸이 찼는지로 고르기 때문이다. 다른 앱의 "캘린더에 추가"에도 진료메이트가 함께 뜬다. 진료 알림과 같은 길로 들어온다 — `MainActivity`가 인텐트에서 꺼내 그래프가 홈 위에 얹고, 홈에 갈 수 있는 세션이 아니면 버린다(`PendingLinkSync`). **날짜·시각은 ML Kit Entity Extraction이 기기 안에서 읽는다.** 글이 서버로 나가지 않는다. 한국어 모델은 처음 쓸 때 내려받아서 그때는 연결이 필요하고, 못 받으면 날짜를 비운 채 연다. 처음 나온 앞날을 고르고, 시각은 그 날짜 바로 뒤에 붙어 있을 때만 받는다 — "변경은 오후 6시까지"의 6시가 진료 시각으로 들어가지 않게 한다. **추출기가 시각을 빠뜨리면 글에서 직접 읽는다**(`timeNear`) — 기기에서 `09월08일 10시00분`의 날짜만 나온 일이 있었다. 날짜 자리 안과 바로 뒤(공백·쉼표·요일만 건너뜀)에서 `10시00분`·`오후 2시`·`2시 반`·`17:58`을 읽고, 오전·오후 없이 1시~7시면 오후로 본다. **추출기는 날짜 자리를 `10월19일`이 아니라 `10월19`까지만 잡기도 한다**(2026-10-12 기기 로그) — 남은 `일`이 날짜와 시각 사이를 막아 시각을 놓쳤으므로 그 한 글자도 건너뛴다. **병원 이름은 추출기가 다루지 않아 글 모양으로 찾는다** — 머리의 `[OO병원]`을 먼저 보고 없으면 본문에서 `병원·의원·치과…`로 끝나는 말을 찾는다. **과 이름으로 끝나는 의원(`튼튼정형외과`·`밝은안과`)도 병원으로 읽는다** — 동네 의원 문자가 대개 그 꼴이다. 과 이름만 홀로 선 것(`정형외과 진료 예약`)은 건너뛴다. 이름 안의 가림 글자(`서울○○병원`)와 전각 글자(`ＯＯ병원`)도 받는다. `&`·`·`·`-`는 말 사이에 더 자주 와서 받지 않는다. **띄어 쓴 이름(`강남 세브란스 병원`)은 붙여 쓴 이름을 못 찾았을 때만 본다** — 띄어쓰기를 받으면 `예약하신 병원`·`해당 병원`이 이름으로 잡힌다. 앞말은 두 낱말까지이고 숫자·가리키는 말·안내 문구·`~님`·본문의 꾸밈 끝말(`~신`·`~운`)에서 멈춘다. 머리(`[밝은 안과]`)에서는 꾸밈 끝말도 이름으로 받는다. 줄바꿈 없는 공백(U+00A0)·전각 공백은 띄어쓰기로, 폭 없는 문자는 지우고 읽는다 — `\s`가 그것을 공백으로 보지 않는다. 규칙은 `calendar/share/HospitalName.kt`에 있다. 진료과는 목록으로 적어 두어서 **빠진 과로 끝나는 의원은 놓친다** — 기기에서 `-고려튼튼 통증의학과-`를 놓친 것이 `통증의학과`가 빠져서였다. 그래서 병원 검색(`GET /api/demo/hospitals`, 인증 없음)에서 이름 2,746개를 표본으로 받아 `[이름]`·`-이름-`·본문 세 자리에 넣어 돌렸다(2026-10-12). 그 결과로 옛 과 이름(`신경정신과`·`방사선과`·`마취과`)과 심평원 진료과목의 치과·한방 세부 과, 이름 안의 괄호(`센(SEN)이비인후과의원`), 띄어 쓴 `치과의원`·`한의원`, `암센터`·`건강센터` 꼴을 더했다(`센터`만으로는 받지 않는다 — `고객센터`). **감싼 머리는 대괄호만이 아니다** — `<…>`·`『…』`·`(…)`와 같은 기호로 두른 `-…-`·`★…★`도 머리로 읽어 꾸밈 끝말 이름(`-파밀리에 치과의원-`)을 받는다. 남은 놓침은 대부분 법인 이름이 떨어지는 것(`(의) 열린의료재단 강동열린의원` → `강동열린의원`)과 본문에 맨몸으로 쓴 꾸밈 끝말 이름(`행복한 치과의원`)이다. 진료과가 바로 뒤에 붙으면 함께 든다. Gemini Nano로 읽는 것은 지원 기기가 좁아 넣지 않았다. **병원 이름을 읽으면 그 이름으로 찾은 병원 찾기(1m-B)가 바로 열린다** — 같은 이름의 다른 지점이 있어 문자 속 이름만으로는 어느 곳인지 모른다. 사람이 주소를 보고 고르면 일정 추가로 돌아온다. 그냥 나오면 문자 속 이름이 남는다. 병원 칸을 다시 눌러도 적힌 이름으로 검색된 채 열린다(`HospitalPickDestination.query`). 문자 속 이름과 심평원 등록 이름이 띄어쓰기·진료과에서 갈려(`고려튼튼 통증의학과` ↔ `고려튼튼통증의학과의원`, `서울OO병원 내과` ↔ `서울OO병원`) 0건이면 띄어쓰기를 뺀 이름, 마지막 낱말을 뗀 이름 순으로 다시 찾고 찾은 이름을 검색어 칸에 남긴다. **병원 찾기는 이름이 아니라 그 줄을 고른다** — 이름으로 찾으면 같은 이름의 두 번째 지점을 눌러도 첫 줄이 골라졌다. 다만 일정에는 주소 자리가 없어 고른 지점의 주소는 일정에 남지 않는다. **비어 있는 칸만 채운다** — 읽는 동안 사람이 고른 값이 나중의 뜻이다. 칸 위의 `Notice`가 채웠으니 확인하라는 것이나 날짜를 못 찾았다는 것을 적는다. 시안에 없는 자리라 디자인 트랙 확인이 필요하다. **추출기는 기기에서만 확인된다** — 유닛 테스트는 고르는 규칙만 보고 추출기 자체는 JVM에서 돌지 않는다. 2026-10-12에 갤럭시에서 실제 예약 문자(`2026-10-15 17:58` · `-고려튼튼 통증의학과-`)를 공유해 병원·날짜·시각이 모두 채워지는 것을 확인했다. 같은 날 `태릉씨티치과` · `10월19일 10시00분` 문자도 시각까지 채워졌다(`timeNear` 보강 뒤) |
| 음성 호출(#273·#278) | 앱을 쓰는 동안 "헤이 메이트"라고 부르면 음성 패널이 뜨고, 말한 대로 화면을 옮긴다(홈·캘린더·기록·내 정보·뒤로·증상 정리·브리핑 카드·일정 추가). 저장·삭제처럼 되돌릴 수 없는 동작은 넣지 않았다. **두 단계로 듣는다** — 호출어는 Vosk 영어 소형 모델(`vosk-model-small-en-us-0.15`), 명령은 기존 받아쓰기(ML Kit)다. 한 마이크를 나눠 쓰므로 감지기를 다 거둔 뒤 받아쓰기를 열고, 다른 화면이 받아쓰는 동안(`SpeechToText.active`)에는 감지기가 쉰다. **그래서 받아쓰기는 화면이 가려지면 멈춘다**(#304) — 증상 문답·진료 후 메모의 ViewModel은 다음 화면으로 가도 백스택에 남아, 켠 채 옮기면 받아쓰기가 이어지고 그동안 "헤이 메이트"가 듣히지 않았다. 두 화면이 `ON_STOP`에서 멈춘다. 증상 정리는 네 단계가 한 목적지라 문답 단계를 떠날 때(다음·이전)도 멈추고, AI 답이 늦게 와도 문답 단계가 아니거나 화면이 가려져 있으면 다시 듣지 않는다. 받아쓰기를 새로 붙이는 화면도 가려질 때 멈춰야 한다. **호출어가 영어인 이유** — 처음에는 한국어 모델(`vosk-model-small-ko-0.22`)로 "메이트야"를 들었다. 그 모델은 `[unk]`을 몰라 문법으로 좁힐 수 없었고(좁히면 모든 말이 호출이 된다), 전체를 받아쓰면 작은 언어 모델이 외래어 "메이트"를 거의 몰라 `네이트`·`레이트`·`메인`으로 끌려갔다. 갤럭시에서 세 번 재서 35%·33%·38%만 켜졌다. 판정선을 내리고 비슷한 낱말을 받아 줘도 나아지지 않았다. 영어 모델은 문법을 `["hey mate", "[unk]"]`로 좁힐 수 있고, 같은 갤럭시에서 "헤이 메이트" 8번이 모두 켜졌다. **판정 규칙** — `hey` 바로 뒤에 `mate`(신뢰도 0.6 이상)가 와야 하고, 그 뒤에 신뢰도 0.8 이상의 `[unk]`이 붙으면 호출이 아니다. PC 시험에서 "헤어 메이크업 했어"가 `hey mate [unk] [unk]`로 켜져서 둔 규칙이다. 그래서 부르고 잠깐 쉬어야 한다(발화가 끝났다고 보는 0.5초 쉼). "Hey Kate"는 켜지지만 영어 문장이라 두었다. 대화 중에 잘못 켜지는지는 측정이 끝나지 않았다 — 갤럭시 측정에서 대화 사이에 `[unk] hey mate`로 켜진 두 번이 부른 것인지 가리지 못했다. **무게** — 압축 41MB, 풀면 68MB, PC에서 메모리 150~180MB, 갤럭시에서 올리는 데 0.3초. 내 정보의 토글로 켠 사람만 받는다. 켤 때의 확인 창은 걷어냈다(#316, 사용자 결정) — 마이크 권한만 묻고 바로 받는다. **꺼도 모델은 남긴다**(#280) — 한국어 모델(264MB) 때는 끄면 지웠는데, 68MB가 된 지금은 껐다 켤 때마다 41MB를 다시 받는 쪽이 더 불편하다. 공간은 꺼 둔 채 모델이 있을 때만 보이는 "음성 모델 지우기"로 돌려받는다. 켜져 있을 때 지우면 듣던 것이 끊겨 그때는 버튼을 두지 않는다. 한국어 모델을 받아 둔 기기는 그 폴더를 지우고, 켜 둔 채 모델이 없으면 다시 묻지 않고 받는다. `totalMem` 5GiB 이상이면서 받아쓰기를 쓸 수 있는 기기에서만 토글을 그린다 — 한국어 모델(480~590MB) 때 정한 선이라 기기에서 재 본 뒤 낮춘다. 앱이 `RESUMED`일 때만 모델을 올리고 마이크를 잡으며, 뒤로 가면 내린다. 홈에 갈 수 있는 세션이고 마이크 권한이 있을 때만 듣는다. 모델은 alphacephei.com에서 받아 SHA-256을 맞춰 보고 `noBackupFilesDir`에 푼다 — **출시 전에 Play Asset Delivery나 우리 저장소로 옮길지 정해야 한다.** `libvosk.so`·`libjnidispatch.so`(JNA 5.18.1)는 arm64가 16KB 정렬이라 Play 요건을 통과하고 APK에 약 10MB를 더한다. 토글·확인 창·패널 문구는 시안에 없다. **부르면 대화가 이어진다**(#282) — 명령 하나를 처리해도 막대를 닫지 않고 다음 말을 듣는다. 받아쓴 말은 먼저 지금 화면이 받는다. 화면은 `VoiceTargetEffect`로 "이 화면에서 알아듣는 말"을 등록하고(`core/voice`), 모르는 말이면 `VoiceReply.NotMine`을 돌려 전역 명령(홈·캘린더·뒤로 등)으로 넘긴다. **화면이 모든 말을 받아 가면 전역 명령이 막히므로** 자기 말이 아니면 반드시 넘긴다. 새 동작을 만들지 않고 버튼과 같은 일을 말로 부른다. 저장처럼 되돌리기 어려운 일은 `VoiceReply.Confirm`으로 "할까요?"를 띄우고 다음 말이 "네"일 때만 하며, 하고 나면 막대를 닫는다. 12초 동안 들린 것이 없거나 "그만"이면 닫는다. 막대는 화면을 가리지 않도록 스크림 없이 아래에 뜬다(처음의 `Bottom Sheet`를 걷어냈다). **막대는 10-15의 `Mate Card`다**(#312, `3532:5543`) — 좌우 12, 탭바가 있는 화면은 탭바 위 12, 없는 화면은 바닥 위 12에 뜬다. 반경 24 · `Elevation/Float` 두 겹이고 아랫줄은 파형(막대 다섯, 말이 들리면 커진다) · 글 · 닫기다. 추천 말은 `Guide`로 위에 서고 `Divider`로 나뉜다. 말하는 중에는 들리는 글이 제목 자리에 선다. 마스터의 `답` 상태(물은 것에 답하는 본문)와 가이드의 "다음 진료 언제야 · 일정 찾기"는 그 말을 알아듣는 명령이 없어 쓰지 않았다 — 공통 예시의 셋째 줄은 지금 알아듣는 "10월 20일 일정 보여 줘"다. 말하는 중 글의 끝 글자를 `fg/muted`로 흐리는 것도 받아쓰기가 확정 전 글자를 따로 주지 않아 넣지 않았다. **막대 뒤에 가린 내용은 굴려서 올려 본다**(#284) — 화면은 줄이지 않는다. 막대가 뜰 때마다 화면 전체가 들썩여서 줄이는 방식은 되돌렸다. 대신 막대 높이(`LocalVoicePanelInset`)를 그래프에 내려 주고, 스크롤 끝에 그만큼 빈자리를 붙인다(`verticalScroll(...)` 바로 뒤의 `voicePanelSpace()`, 목록은 `contentPadding` 아래에 `voicePanelInset()`). 맨 아래 내용도 막대 위로 올라온다. 새 화면도 이것을 붙인다. 화면 아래에 고정된 버튼은 가리지만 말로 누른다. 일정 추가에서 바뀐 칸을 옮길 때도 실제 막대 높이만큼 위로 둔다. 대화의 응답 규칙은 `VoiceConversation`이다. **되물어 흐름을 잇는다** — 실행하거나 골라야 하는 일이 생기면 막대에 질문을 띄워 사용자가 다시 말하게 한다. 막대는 윗줄이 방금 한 일, 아랫줄이 물음이다(`VoicePanel.question`). **물음은 말하는 동안에도 남는다**(#284) — 들리는 글은 물음 아래에 따로 적는다. 물음과 윗줄 모두 문장마다 줄을 바꿔 적는다(`sentencePerLine`, #288) — "어디가 아프세요? 이 중에서 말씀해 주세요"가 한 줄로 이어지면 무엇을 묻고 무엇을 하라는지가 섞인다. 폭을 넘는 문장은 글꼴의 어절 단위 줄바꿈이 맡는다. 목록을 보며 답하는 중에 목록이 사라지면 안 된다. 못 알아들어도 묻던 것을 남기고, "다시 보여 줘"·"뭐였지"·"못 봤어"면 화면에 넘기지 않고 띄운 것을 그대로 다시 둔다(`VoiceWords.isRepeat`). 문답에서 그 말이 답으로 나가지 않는 것도 이 때문이다. 화면은 답에 물음을 붙이거나(`VoiceReply.Done.question`), 사용자가 말하지 않았을 때 먼저 말을 건다(`VoiceTargets.ask`·`rememberVoicePrompt`). 먼저 건 말도 12초를 다시 센다 — 물어 놓고 바로 닫히면 답할 틈이 없다. 막대가 닫혀 있으면 버려진다. **일정 추가(1r-4)** — "10월 20일 오후 3시 고려튼튼통증의학과"를 말하면 예약 문자 공유(#270)의 읽기로 칸을 채우되 말한 값은 덮어쓴다(사람이 나중에 고른 값이라서). 병원을 말하면 그 이름으로 병원 찾기가 열린다. **화면의 칸과 버튼을 모두 말로 부른다** — 병원("병원 바꿀래"는 지금 이름으로 찾기), 날짜·시간("날짜 고를래"·"시간 고를래"는 시트, "시간 미정"은 비우기, 시트가 열린 채 말하면 채우고 닫기. **시각만 말하면 날짜는 그대로다** — 추출기가 날짜 없는 시각을 오늘로 돌려줄 수 있어 말에 날짜 표현이 없으면 그 날짜를 버린다. 추출기가 시각을 못 읽으면 말에서 바로 읽고, 받아쓰기가 우리말 수로 적은 "세 시"·"두 시 반"·"열한 시"·"정오"도 숫자로 바꿔 읽는다. "한 시간"·"적어 두시면"은 바꾸지 않는다), 가져갈 카드(고르기 "두 번째 카드"·"두통 카드", 빼기 "카드 빼 줘", 새로 "새 카드 만들래"), 할 일(더하기 "할 일 추가, …", 체크 "첫 번째 할 일 완료"·"약 챙기기 할 일 했어", 풀기 "완료 취소", 고치기 "두 번째 할 일 고쳐, …", 지우기 "… 지워 줘"), 나가기("나갈래", 적은 것이 있으면 확인), 저장(확인 뒤). 할 일과 카드는 그 낱말이 있을 때만이고 먼저 본다. 화면 조작은 그 말로 시작할 때만이다. **한 뜻에 여러 말을 받는다** — 사람은 정해 둔 말로 말하지 않는다. 할 일은 "할 일"·"준비물"·"챙길 거"·"체크리스트"로, 카드는 "카드"·"브리핑"으로 가리키고, 동작마다 말이 여럿이다(지우기 "지워"·"없애"·"삭제"·"빼", 체크 "완료"·"했어"·"챙겼어", 풀기 "아직 안 했어"·"체크 풀어", 나가기 "그냥 나갈게"·"작성 취소"·"다음에 할래", 병원 찾기 "병원 다시 고를래"·"다른 병원으로 할래", 저장 "일정 잡아 줘"·"이걸로 할게"·"마무리해 줘"). 더하라는 말("추가"·"넣어")이 있으면 더하기가 먼저다 — "할 일 추가, 약 빼먹지 않기"가 "빼"로 지우기가 되지 않는다. 고칠 글은 동사 뒤("고쳐, 약 챙기기")와 앞("약 챙기기로 바꿔 줘") 둘 다 읽는다. 날짜나 시각이 들어 있으면 시트를 여는 말보다 채우는 말이 먼저다("날짜 20일로 바꿔 줘"). 공용 말(네·아니요·그만·저장·몇 번째)은 `core/voice/VoiceWords`에 있고 "첫째"·"맨 위에 거"·"끝에 거"·"넵"·"하지 마"도 받는다. 일상 말에 흔한 "어"·"하나"는 받지 않는다 — 엉뚱한 말이 확인이나 선택으로 읽히는 쪽이 못 알아듣는 쪽보다 나쁘다. 규칙은 `ScheduleAddVoice.kt`이고 조작은 `ScheduleAddVoiceActions`가 든다. ****말로 바뀐 자리를 보인다** — 칸이 채워지거나 카드·할 일이 바뀌면 그 자리로 화면을 옮기고 1.8초 동안 `border/focus` 테를 두른다(`ScheduleAddFocus` · `VoiceFocusFrame`). 이 장치는 `core/voice/VoiceFocus.kt`에 있고 증상 정리도 쓴다(#284) — 부위를 고르거나 큰 부위로 들어가거나 좌우를 바꾸면 3D 인체도 판, 강도를 고르면 슬라이더, 질문을 넣거나 고치면 그 줄, 지우면 질문 목록 머리다. 화면마다 자리 종류(`ScheduleAddFocusTarget`·`IntakeFocusTarget`)만 정한다. 아래에 뜬 막대가 덮지 않게 아래를 넉넉히 두고 옮긴다. 후보가 여럿이라 되물을 때는 그 후보들에, 빠진 칸 때문에 저장하지 못할 때는 그 칸에, 지운 할 일은 남지 않아 할 일 구역 머리에 두른다. 말로 연 병원 찾기에서 돌아오면 병원 칸이다. 화면이 마지막으로 보인 차례(`seq`)를 저장해 두어 다른 화면에 다녀와도 지난 것이 다시 번쩍이지 않는다. 테는 시안에 없다. 처리하고 나면 빠진 칸을 이어서 묻는다** — 병원이 없으면 "어느 병원에 가세요?", 날짜가 없으면 "언제 가세요?", 다 찼으면 "다 됐으면 '저장'이라고 해 주세요"다(`nextQuestion`). 말로 들어왔을 때와 병원 찾기에서 돌아왔을 때도 묻는다. 병원 찾기로 떠나는 말에는 묻지 않는다 — 그쪽이 묻는다. 카드나 할 일이 여럿 걸리면 멋대로 고르지 않고 후보를 적어 몇 번째인지 되묻는다(`matchVoice`). 화면을 떠나는 길(저장·나가기·병원 찾기·새 카드)은 라우트가 `ScheduleAddVoiceRoutes`로 버튼과 같은 것을 넘긴다. 어느 화면에서든 "… 일정 추가해 줘"에 날짜가 들어 있으면 그 말을 들고 일정 추가가 열린다. **병원 찾기(1m · 1m-B)** — **말로 찾은 병원은 하나로 좁혀지면 알아서 고른다**(`decideVoicePick`). 이름이 같다는 것은 띄어쓰기와 끝의 "의원"·"병원"을 뺀 것이다("고려튼튼통증의학과" = "고려튼튼통증의학과의원"). 서버가 부분 일치라 결과 수만 보면 늘 여럿이어서, 이름이 같은 곳이 하나면 그곳, 여럿이면 그곳들만 놓고 "이름이 같은 병원이 N곳이에요 · 몇 번째인가요? 동네나 지점 이름으로 말씀하셔도 돼요", 같은 곳이 없는데 결과가 하나면 그곳, 없으면 이름을 다시 묻는다. 동네·지점 이름("하계동에 있는 거"·"강남점")은 주소와 이름에서 찾되 모든 후보에 있는 낱말은 버린다(`byArea`). 여럿이 남으면 그곳들로 좁혀 다시 묻는다. 자동 선택은 말로 연 경우만이다(`ScheduleAddDestination.voice` → `HospitalPickDestination.voicePick`). 예약 문자(#270)로 연 것과 "병원 바꿀래"는 지금처럼 사람이 고른다. "첫 번째"·"2번"·"마지막"이면 그 줄을 고르고 바로 마친다. "OO 찾아 줘"·"OO 보여 줘"·"이거 말고 OO"이거나 병원 이름처럼 끝나는 말이면 다시 찾는다. "여기로 할게"·"그걸로"는 골라 둔 병원으로 마친다. 그 밖의 말은 받지 않는다. 막대·안내 문구는 시안에 없다. **막대의 말은 사람이 옆에서 하듯 짧게 쓴다**(#288에서 전체를 다시 봤다) — 화면을 옮긴 뒤에는 어디로 갔는지 적는다("뒤로"에 "열었어요"라고 하지 않는다). "네"를 기다리다 다른 말이 들리면 묻던 것을 그대로 다시 세우고 "네"나 "아니요"로 답하라고 한다. 후보가 여럿이면 윗줄에 몇 개인지, 아랫줄에 후보를 한 줄에 하나씩 적는다. 안내 끝은 "…이요"로 자르지 않고 "…라고 해 주세요"까지 쓴다. "할 일을 더했어요: 약 챙기기"처럼 쌍점 뒤에 값을 늘어놓지 않고 "약 챙기기, 할 일에 넣었어요"로 쓴다. 끝마다 "이어서 말씀해 주세요"를 붙이지 않는다. 물음은 아랫줄 하나로 충분하다. **증상 정리부터 브리핑 카드 저장까지(#284)** — 어디서든 "오른쪽 어깨가 아파"라고 하면 증상 정리가 열리고 그 부위를 고른다(`IntakeDestination.spoken`). **"브리핑 카드 만들어 줘"·"카드 만들자"·"새 카드"도 증상 정리다** — 카드는 증상 정리를 마쳐야 생겨서 카드 목록으로 가면 만들 길이 없다. 보거나 여는 말("카드 보여 줘")은 그대로 카드 목록이다. 1단계는 목록의 부위 검색(이름·별칭 122개)을 그대로 쓴다. 좌우는 검색어에서 빼고 따로 읽는다. 큰 부위만 말하면("다리가 아파요") 그 부위로 들어가 어디인지 묻고, 좌우가 갈리는데 말하지 않았으면 어느 쪽인지 묻는다. 그 뒤에는 "왼쪽"만 말해도 고른다. **"반대"·"반대편"·"다른 쪽"이면 좌우를 바꾼다** — 고른 부위가 있으면 그 반대쪽을 고르고 보는 쪽도 옮기며, 고르기 전이면 보고 있는 팔·다리만 옮긴다. 좌우가 없는 곳이면 바꾸지 않는다고 알린다. **부위를 물을 때 고를 수 있는 곳을 함께 읽어 준다** — 처음에는 큰 부위 아홉, 큰 부위로 들어가면 그 안의 상세 부위다. 막대 아랫줄에 한 줄에 하나씩 `·`를 붙여 적는다 — 한 줄에 늘어놓으면 아홉 개가 이어져 읽기 어렵다. 그래서 막대의 물음은 줄 수를 자르지 않는다(말하는 중의 글만 세 줄까지다). 인체도만 봐서는 팔 안에 어디를 짚을 수 있는지 알 수 없고, 사람은 부위 이름을 모를 수 있다. 손으로 큰 부위를 짚어 들어가도 같은 목록을 읽고, "세 번째"·"마지막 거"로도 고르며, "잘 모르겠어요"면 다시 읽는다. 읽는 차례는 온톨로지 차례다. 화면에 이름을 띄우는 것은 하지 않았다 — 막대 안내만으로 정했다. 명치·아랫배도 좌우 점이 따로 있어 묻는다. 고르면 그 부위로 들어가 고른 자리가 보인다. **2단계는 막대가 문답을 맡는다** — AI의 질문이 막대 윗줄에 서고 말한 것은 모두 답으로 보낸다(서버에는 음성으로 적힌다). 명령은 "다음"과 "뒤로"뿐이다. 문답이 끝나기 전의 "다음"은 넘어가지 않고 아직 묻는 중이라고 한다. "다음 주에 다시 오래요" 같은 답이 걸리지 않게 "다음"은 짧은 말일 때만 본다. 문답 화면의 마이크 음성 모드는 그대로 있고, 그쪽이 듣는 동안에는 호출어 감지기가 쉰다. 3단계는 들어설 때 화면의 다섯 낱말을 고를 말로 한 줄에 하나씩 읽어 주고 점수로 말해도 된다고 적는다. 어떻게 말할지 모르면 다시 읽는다. "많이 아파요"·"견디기 힘들어요" 같은 낱말, "7점"(NRS를 다섯 단계로), "4단계"를 받는다. 4단계는 묻는 말("약 계속 먹어도 되나요")을 그대로 질문으로 넣는다. 지우기·고치기는 "질문"이라는 말이 있을 때만이다. 1c-5는 "카드 만들어 줘"·"병원부터 찾을래", 1e-1은 "저장"을 확인을 받고 한다. "뒤로"는 첫 단계가 아니면 단계를 되짚고, 첫 단계에서는 전역 "뒤로"로 넘겨 증상 정리를 나간다. 규칙은 `BodyVoicePick.kt`·`IntakeQuestionVoice.kt`·`IntakeVoiceWords.kt`, 동작은 `IntakeVoiceCommands`다. **어느 화면이든 말로 굴린다**(#284) — "아래로"·"내려 줘"·"위로"·"맨 아래로"·"끝까지"·"맨 위로". 화면마다 받지 않고 대화가 먼저 본다(`parseVoiceScroll`). 화면은 `rememberScrollState()`·`rememberLazyListState()` 자리에 `rememberVoiceScrollState()`·`rememberVoiceLazyListState()`를 쓰면 등록되고, 맨 나중에 등록한 것이 지금 화면이다. 새 화면도 이것을 쓴다. 시간 휠 같은 선택기는 등록하지 않는다. 한 번에 보이는 높이의 70%를 굴린다. 말머리에서만 보고 "로"나 동사까지 붙어야 한다 — "맨 아래 거"(목록의 마지막)·"배 위로 아파요"가 걸리지 않는다. 굴려도 막대의 물음은 남는다. **말로 카드와 날짜를 찾아 연다**(#286) — 브리핑 카드 목록에서 "두 번째"·"두통 카드"·"9월 4일 카드"·"어제 쓴 거"·"고려튼튼 거"로 골라 상세를 연다. 단서(몇 번째·제목이나 병원의 낱말·작성일이나 달)를 함께 말하면 모두 맞는 것만 남기고, 여럿이면 후보를 한 줄에 하나씩 띄워 몇 번째인지 묻는다. 그 다음 "두 번째"는 후보 안에서 센다(`BriefCardVoice.kt`). 카드를 가리키지 않은 말이 아무 카드에도 없으면 전역 명령으로 넘긴다("홈으로 가"). 캘린더에서는 "20일"·"내일"·"다음 주 화요일"이면 그 날의 일자 화면을, "12월"·"다음 달"이면 그 달로 넘긴다. 날짜 풀이는 진료 후 기록의 재방문 날짜도 쓰므로 `core/voice/VoiceDate.kt`에 있다. 짧은 말이라 기기 추출기를 부르지 않고 직접 읽는다. **다른 화면에서도 된다** — "10월 20일 일정 보여줘"·"오른쪽 어깨 브리핑 카드 열어 줘"는 전역 명령이 캘린더·카드 목록을 열면서 그 말을 넘기고(`VoiceTargets.handOff`), 열린 화면이 처음 등록할 때 받아 날짜나 카드를 연다. 넘긴 말은 3초 안에 한 번만 준다. 카드 목록은 받기 전이면 받을 때까지 기다린다. 서버에 새로 묻는 것은 없다. **진료 후 기록을 말로 한다**(#288) — 어디서든 "진료 후 기록할래"·"어제 진료 기록 남길래"면 말한 날(없으면 오늘)의 일자 화면을 열고 그 말을 넘긴다. 일자 화면은 그 날 일정에 카드가 걸려 있으면 기록을 시작하고(버튼과 같은 조건), 이미 남긴 기록이 있으면 "기록 보여 줘"로 열라고 한다. "카드 보여 줘"는 가져간 카드를 연다(`DayVoice.kt`). 병원 확인(1m-12)은 "네, 맞아요"·"다른 병원이에요". **메모(1p)는 막대가 받아 적는다** — 증상 문답과 같은 방식이다. 짧은 명령("다 했어"·"정리해 줘"·"방금 거 지워"·"다 지워"·"뒤로")만 명령이고 그 밖의 말은 메모 뒤에 이어 붙인다. "다음 주에 다시 오래요"가 넘어가기가 되지 않게 명령은 10자 이하일 때만 본다. 다 지우기는 확인을 받는다. 기록 확인(1q-1)은 "저장"만 말로 한다(확인을 받는다, `VisitRecordVoice.kt`). **완성된 결과물은 말로 편집하거나 지우지 않는다**(#296) — 1q-1은 AI가 나눈 결과를 보여 주는 자리이고 고치려면 `편집`으로 들어가는데, 처음에는 말로 그 진입 없이 항목을 바꾸고 지우고 재방문 날짜를 바꿀 수 있었다. 이제 고치거나 지우라는 말은 알아듣되 "화면에서 직접 해 주세요"라고만 한다. 이미 잡은 일정을 고치러 연 일정 추가 화면도 말을 받지 않는다(저장이 곧 수정이다). **작성 중에 잘못 넣은 것은 말로 고치고 지운다** — 일정 추가의 할 일, 증상 정리의 질문, 진료 후 메모가 그렇다. 브리핑 카드·일자 화면·카드 목록·진료 후 기록 상세는 처음부터 말로 편집이나 삭제에 들어가는 길이 없다. **지금 화면에서 할 수 있는 말을 추천한다**(#292) — 부를 때와 못 알아들었을 때 막대 위에 "이렇게 말해도 돼요"와 함께 아이콘 · 따옴표로 감싼 말 · 하는 일을 한 줄씩 세우고, 구분선 아래에 막대가 선다. 무엇이든 알아들으면 걷는다. **세 줄까지만 보인다**(`MAX_SUGGESTIONS`) — 더 늘면 막대가 화면 아래를 너무 가리고 고르기도 어렵다. 화면마다 예시도 세 개 이하로 둔다. 화면은 `VoiceTargetEffect(suggestions)`로 자기 예시를 등록하고 증상 정리는 단계마다 다르다(`IntakeUiState.voiceSuggestions`). 등록이 없는 화면(홈·기록·내 정보)은 공통 예시(`GLOBAL_SUGGESTIONS`)다. **예시는 실제로 알아듣는 말만 쓴다** — 예시대로 말했는데 못 알아들으면 예시가 거짓말이 된다. 문구가 리소스라 JVM에서 읽지 못해 같은 문장을 `VoiceSuggestionPhraseTest`에 적어 말 풀이에 넣어 본다. 예시를 바꾸면 그 문장도 바꾼다. 공통 예시의 "진료 끝났어"를 위해 진료 후 기록 명령에 "진료 끝"·"진료 다녀"를 더했다. 막대의 첫 말은 "무엇을 도와드릴까요?"로 줄였다 — 예시는 목록이 보여 준다. 형식만 맞췄고 시안은 없다. **말로 연 화면은 손으로 들어갈 때와 같은 스택에 선다**(#298) — 처음에는 지금 화면 위에 그냥 쌓아서, 일정 추가나 병원 찾기 같은 깊은 화면에서 "배가 아파"라고 하면 그 위에 증상 정리가 쌓이고 끝나고 뒤로 가면 엉뚱한 화면이 나왔다. 이제 그 화면에 손으로 들어가는 탭의 첫 화면으로 돌아간 뒤 그 위에 연다(`voiceTabOf`). 증상 정리·브리핑 카드 목록·내 정보는 홈 위에, 일정 추가·일자 화면은 캘린더 위에 선다. 탭의 첫 화면으로 갈 때는 그 탭에 저장해 둔 화면을 되살리지 않고, 떠나는 탭의 화면은 탭을 누를 때처럼 저장해 둔다 — 쓰던 일정 추가는 캘린더 탭을 누르면 돌아온다. 같은 화면이 이미 맨 위에 있으면 쌓지 않는다. 날짜를 말한 "캘린더"도 저장된 화면을 되살리지 않는다(예전에 열어 둔 일자 화면이 되살아나 날짜를 여는 말을 잘못 받았다). 첫 화면의 "뒤로"는 옮기지 않고 "더 돌아갈 화면이 없어요"라고 한다(`VoiceTargets.canGoBack`). 스택 자체는 `NavHost`라 JVM 시험이 없다 — 어느 탭에 서는지만 시험으로 보고 나머지는 기기에서 본다. **debug 빌드는 무엇을 들었는지 남긴다**(#278) — `adb logcat -s VoiceWake`로 듣기 조건 여섯의 값, 모델 받기·올리기, 말하는 중의 글, 발화가 끝났을 때의 낱말·신뢰도와 판정, 명령 받아쓰기가 찍힌다. 받아쓴 말이 그대로 남으므로 release에서는 문장을 만들지도 않는다(`VoiceWakeTrace`가 람다를 받는다) |
| 온디바이스 모델(#289 · #300) | **진료 후 메모 분류와 진료 전 문답 추출 · 질문 후보가 폰에서 돈다.** **문답(#300, Backend#157)** — 폰 모델이 있으면 세션을 `profile: ONDEVICE`로 연다(첫 자유 발화 없이 첫 축 질문부터). 응답의 `askingAxis: {key, label}`로 폰 프롬프트 첫 줄(`질문한 항목: {label} (axis={key}). 이 항목만 답한다.`)을 만들어 답마다 뽑고 `extraction`·`extractionMeta`로 함께 보낸다. 묻는 축은 저장소가 세션마다 마지막 응답 것을 들고 있고, 이어서 하기는 `GET`이 같은 값을 다시 준다. 폰이 못 뽑은 턴은 extraction 없이 보내 서버가 뽑는다. **온디바이스 세션은 서버가 질문 후보를 만들지 않는다** — 강도를 저장한 응답이 끝났는데 후보도 적은 질문도 없으면, 폰이 `cardAxes`·강도·건강 정보(`core/model/PatientHealthProvider`)로 후보를 만들어 `POST /api/sessions/{id}/question-candidates`로 보내고 서버가 검증·보충한 후보를 쓴다. 폰 모델이 없으면 빈 배열을 보내 템플릿으로 채운다. 질문 후보 프롬프트는 파일이 아니라 벡터의 예시 대화(`questions-small4.messages.json`)이고 카드 줄은 AI 쪽 `questions_user`와 같다 — 빈 칸은 "(안 답함)", 빈 건강 정보는 "(확인 안 됨)"이다. **세션 시작이 부위를 `siteCodes`(배열)로 보내고 있었는데 백엔드에 그 필드가 없어 조용히 버려졌다** — `siteNodeId` 하나와 `side`로 고쳤다. 좌우는 AI 온톨로지에서 `left_right`인 21곳에만 보낸다(나머지에 보내면 400). **메모(#289)** 묶음은 AI 트랙의 v2다(Qwen3.5-2B ssmout-q8 · memo-small-v5, AI#35). 내 정보의 "휴대폰에서 기록 정리"를 켜면 모델(1.24GB)을 받고, 다 받은 뒤부터 1p의 정리가 세 번에 나뉜다 — 서버가 모델 없이 문장만 나누고(`classify: false`), 폰이 문장마다 라벨을 붙이고, 그 라벨을 `labelsMeta`·`splitVersion`과 함께 보내 서버가 조립한다. **어디서 막혀도 서버 분류로 넘어간다**(모델 없음 · 엔진 실패 · 출력이 문법을 어김 · 조립 409). 지금까지의 서버 분류가 그 길이고 그대로 남는다. 토글은 NPU(HTP0)가 잡히고 `totalMem` 6GiB 이상인 기기에서만 그린다. CPU로 돌면 열 배 가까이 느리고 발열로 무너진다(AI 실측). 꺼도 모델은 남고, 꺼 둔 동안 "정리 모델 지우기"가 선다(음성 호출 #280과 같은 규칙). 모델은 받는 곳(Hugging Face, 커밋 고정)에서 이어 받고 SHA-256을 맞춘 뒤에야 `no_backup/ondevice/`에 제 이름으로 둔다. **엔진은 C API를 직접 부른다**(`ondevice.cpp`) — `n_seq_max = 1`(2B의 `ssm_out` 행렬곱이 시퀀스 차원을 가지면 Hexagon이 받지 않아 graph splits가 3에서 39가 된다), greedy로 고르되 가장 높은 토큰 하나만 먼저 문법에 대어 보고 어긋날 때만 어휘 전체(약 24만)를 거른다(llama-server와 같은 방식 — 매 토큰 전체를 거르면 같은 기기에서 추출 p50이 3.0초에서 6.0초가 됐다), 시스템 프롬프트를 처리한 상태를 떠 두었다가 되돌려 쓴다(2B는 DeltaNet 상태라 뒤쪽만 지울 수 없다). 프롬프트는 앱이 chatml에 생각 끄기 블록까지 붙여 만들고(`ChatPrompt`), AI 쪽 렌더 전문과 글자까지 같은지를 JVM 시험이 본다. 글은 JNI 문자열이 아니라 UTF-8 바이트로 넘긴다 — 변형 UTF-8이 이모지를 쪼갠다. 모델은 쓸 때 올리고 1분 쉬면 내린다. 프롬프트·문법 파일은 `.gitattributes`로 줄 끝을 고정했다 — 윈도우 `autocrlf`가 CRLF로 바꾸면 다른 프롬프트가 된다. **진료 전 문답 추출은 붙이지 않았다** — 폰 프롬프트에 "지금 묻는 축"이 들어가야 하는데 백엔드 응답에 그 값이 없다. 질문 후보·재방문 표현도 백엔드에 폰 결과를 실을 자리가 아직 없다. **기기 일치 확인은 `OnDeviceParityTest`(androidTest)다** — 메모 벡터 34개와 진료 전 추출 79개를 앱이 부르는 길(C API)로 돌려 PC 결과와 견주고, AI 쪽 실측 스크립트와 같은 모양의 jsonl을 `/sdcard/Android/data/com.mist.medicalmate/files/device-app-*.jsonl`에 남긴다. 갤럭시 S24 울트라(2026-10-14): 메모 PC와 같은 출력 32/34 · p50 2.5초, 추출 60/79 · 앱 흐름 p50 3.0초, 못 읽은 출력 0. 모델이 앱 폴더에 있어야 돈다. 토글·진행 줄·지우기 버튼은 시안에 없다. 켤 때의 확인 창은 걷어냈다(#316, 사용자 결정) |
| 로그아웃 · 회원탈퇴 | 구현 완료. 1s-1 하단으로 옮김. 회원탈퇴는 시안에 자리가 없어 로그아웃 아래 텍스트로 뒀다 |
| 계정 전환 시 이전 데이터 | 해결. 로그아웃 시 홈 엔트리가 pop되면서 `HomeViewModel`도 정리됨 |
| 401 재발급 Authenticator | 도입 완료. `TokenAuthenticator`가 401을 받으면 refresh 토큰으로 재발급하고 원래 요청을 한 번 더 보낸다. 재발급 호출은 인터셉터·Authenticator가 없는 `@AuthFree` 클라이언트로 나간다. 재발급이 거절되면 토큰을 지우고 `TokenStore.hasSession`이 false를 흘려 `SessionViewModel`이 로그인 화면으로 보낸다 |
| 토큰 암호화 | 미적용. DataStore 평문. 백업 차단으로 샌드박스 밖 유출만 막음 |
| 온보딩 노출 판단 | 서버 `onboardingRequired`와 로컬 `OnboardingStore`를 함께 본다. `refresh` 응답은 항상 false이고, 프로필을 서버에 저장하기 전까지 로그인 응답은 계속 true라 한쪽만으로는 안 된다 |
| 탈퇴 후 재가입 | 온보딩 기록을 지우지 않아 온보딩이 건너뛰어진다. 서버 `onboardingCompleted`가 정본이 되면 사라지는 문제 |
| Apple · 전화번호 로그인 | 백엔드 미지원. `User` 엔티티 식별자가 `kakaoId` 단독 |
| 카카오 말풍선 심볼 | 적용 완료. 디자인 시스템 `Social Login Button / Provider=Kakao`(`383:1290`) 안에 들어 있었다. 콘솔에서 받아야 한다고 적어둔 것이 틀렸다. 버튼에서 20x18.67로 놓이고 tint는 `KakaoLabel`이다. 네이버·Apple·Google은 아직 에셋이 없어 라벨만 둔다 |
| 디자인 캔버스 | 360dp 재단 완료(활성 화면 전부 360x812, 콘텐츠 320, 거터 20). 화면은 fill-width라 코드 영향은 토큰뿐이었다. `1e-2`·`1q-2`·`1f-1`·`1a-2`·`1l-*`·`1a-1`·`1o`·`ONB-00`~`ONB-03`은 시안에서 삭제됐다(`1f-1`은 흐름 주석에만 남아 있고 IA가 1e-1로 대체했다) |
| 소셜 로그인 버튼 규격 | 2.0 문서는 radius 16, 카카오 가이드는 12. 색은 가이드, 크기는 문서를 따름. 3.0은 반경을 적지 않는다 |
| `mipmap-*` 래스터 아이콘 | 템플릿 그대로. minSdk가 26이 되면서 시스템이 고르는 것은 `mipmap-anydpi-v26`의 어댑티브 아이콘이고, 래스터는 그것을 읽지 않는 런처의 대비로만 남는다. Android Studio Image Asset으로 교체 필요 |
| 오픈소스 고지 화면 | 없음. Pretendard가 OFL이라 스토어 배포 시 필요 |
| 로컬 저장 | 미정. 문답 화면 착수 때 백엔드 API를 보고 결정 |
| 릴리즈 서명 | 하지 않기로 결정. `bundleRelease` 산출물은 미서명 |
| 스크린샷 테스트 (Paparazzi/Roborazzi) | 미도입 |
| E2E (Maestro) | 미도입 |
| `scripts/verify.sh` | 미작성. 1장의 Gradle 명령을 직접 사용 |
| 타이포 검수 | 와이어프레임 최종본 여덟 장(1n-1 · 1j-1 · 1j-3 · 1m · 1p · 1s-1 · 1r-2 · 1c-2)을 마스터까지 펴서 대조했다(#237). 열두 자리가 어긋나 있었고 모두 마스터 쪽으로 맞췄다. 같은 크기의 다른 스타일이 가장 많이 틀렸다 — `Body/L`과 `Heading/S`가 둘 다 17, `Label/M`과 `Body/S Strong`이 둘 다 13이라 화면에서는 무게와 자간으로만 갈린다. 1p 병원 블록은 이름이 `Body/L Strong`, "오늘 진료"가 `Body/S Strong`이다 — `Heading/S`·`Label/M`이 아니다. 1j-3의 머리 뱃지는 시안이 `Label/M`으로 직접 그렸지만 `Badge` 마스터(`311:834`)에 그 크기 변이가 없어 마스터(`Label/S`)를 그대로 뒀다. 디자인 트랙 확인 필요 |
| 디자인 트랙 | #189의 열한 건은 2026-09-15에 모두 확인이 끝났다 — 셋은 #235로 반영했고(1q-1 편집은 찾은 줄만 · 하루 두 진료는 기록만 쌓임 · 저장한 기록은 고치지 않음), 둘은 피그마 대조에서 풀렸고(인체도 프레임은 `제작 예정` · `1r-2-C`는 `1r-1-S`로 대체), 남은 여섯은 코드 그대로 확정이다. #208(기록 상세에 원문 없음)도 구현된 모양으로 확정됐다. **아직 넘겨야 할 것** — 코드가 마스터나 시안과 갈린 채 두고 있는 자리다. (1) `Notice`에 브랜드 채움 변이가 없다 — `1j-3` 예정 알림이 인스턴스 덮어쓰기로 그려져 있어 `BRAND` 톤으로 올렸다(#243). (2) `Badge`에 `Label/M` 13 크기 변이가 없다 — `1j-3` 머리 뱃지가 그 크기로 직접 그려져 있고 코드는 마스터(`Label/S` 11)를 따른다(#237). (3) `Empty State`의 행동이 마스터는 Tonal 알약, 인스턴스는 채움 없는 글자다(#217). (4) `Select Bar`·`Hospital Card`·`Card Pick` 마스터에 채움이 묶여 있지 않아 코드가 색을 정했다. (5) `Severity Scale`은 마스터가 지워졌는데 3.0 문서에 남아 있다. (6) 1m-12의 "다른 병원이에요"는 Ghost 버튼 여백 20을 물려야 시안 자리에 온다 — 마스터 Ghost에 여백 없는 변이가 없다(#246). (7) 기록 상세 카드 단계의 시점 줄에서 시안의 `09.12 진료실에서 보여줌`을 뺐다 — 바로 위 진료 후 기록 단계가 같은 날짜를 적어서다(#243). (8) 시간 휠의 분이 시안(00 · 30)과 달리 10분 단위다(#239). (9) 음성 호출(#273)의 내 정보 토글·확인 창·"음성 모델 지우기" 버튼(#280)·음성 명령 막대(#282)와 그 안내 문구는 시안에 자리가 없다. 막대는 스크림 없이 화면 아래에 뜬다(#282). **백엔드에 알릴 것** — 진료 후 기록 저장 문서가 "일정은 만들지 않는다, 앱에서 따로 부르라"인데 앱이 1q-1에서 확인한 재방문 날짜로 저장 직후 일정을 만든다(#245). 일정 응답에 병원 주소가 없어 1m-12가 카드에 남은 주소로 대신한다(#246) |
| org 공통 문서 위치 | `GIT_CONVENTION.md`가 저장소별 사본으로 존재. 어긋나면 `.github` 저장소로 통합 필요 |

`local.properties`는 `.gitignore` 대상입니다. 로컬에서는 Android SDK 경로가 필요하고, CI는
`ANDROID_HOME` 환경 변수를 사용합니다.
