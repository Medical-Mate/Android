# CLAUDE.md

Medical Mate Android 앱 저장소에서 AI 에이전트가 작업할 때 참고하는 문서입니다.
사람도 그대로 읽을 수 있게 작성합니다. 사실과 달라지면 즉시 갱신합니다.

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
| minSdk / targetSdk | 24 / 36 | `app/build.gradle.kts` |
| ktlint / detekt | 14.2.0 / 1.23.8 | `gradle/libs.versions.toml` |
| KSP / Hilt | 2.2.10-2.0.2 / 2.60.1 | `gradle/libs.versions.toml` |
| 카카오 SDK | 2.25.0 | `gradle/libs.versions.toml` |
| desugar_jdk_libs | 2.1.5 | `gradle/libs.versions.toml` |
| Retrofit / OkHttp | 3.0.0 / 5.5.0 | `gradle/libs.versions.toml` |
| kotlinx.serialization | 1.9.0 | `gradle/libs.versions.toml` |
| Navigation Compose | 2.10.0 | `gradle/libs.versions.toml` |
| DataStore Preferences | 1.2.1 | `gradle/libs.versions.toml` |

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

**core library desugaring.** minSdk 24에서 `java.time`을 쓰려면 필요합니다. 끄면
`lintDebug`가 `NewApi`로 막습니다(API 26 요구). `app/build.gradle.kts`의
`isCoreLibraryDesugaringEnabled = true`와 `coreLibraryDesugaring(libs.desugar.jdk.libs)`를
지우지 마세요. 일정, 복용 알림, 카드 작성일에 계속 날짜가 나옵니다.

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
│       │   │   │   └── component/        디자인 시스템 컴포넌트 50종
│       │   │   └── network/               Retrofit·OkHttp 설정, ApiResult
│       │   ├── navigation/                단일 NavHost, 세션 경계 동기화, 하단 탭 이동
│       │   ├── auth/  data/ ui/           1o 로그인, 1a-1 스플래시, 세션 복구
│       │   ├── profile/  data/ ui/        1a-2 온보딩, 1b 신상정보, 1s 내 정보, 계정 동작
│       │   ├── intake/  ui/               1l 인체도, 1c·1d·1i 증상 정리
│       │   ├── card/  ui/                 1e 브리핑 카드, 1f 진료실 화면, 1j 기록
│       │   ├── calendar/  ui/             1r 캘린더
│       │   ├── visit/  ui/                1m 병원 찾기, 1p 메모, 1q 진료 후 기록, 1k 정리
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

값의 정본은 Figma다. `DESIGN.md`는 디자인 트랙이 보내주는 사본이고, 어긋나면 Figma가
우선한다. 실제로 `Elevation/Card`가 달랐다. 문서를 고치지 말고 디자인 트랙에 넘기세요.
현재 사본은 3.0(2026-09-09)이다. `DESIGN.md`는 아직 저장소에 커밋하지 않았다.

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
API 31부터입니다. minSdk 24라서 Android 7.0~11에는 블러가 걸리지 않습니다.

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
├── profile/   ui/ data/    1a 1b 온보딩
├── intake/    ui/ data/    1l 1c 1d 1i 문답
├── card/      ui/ data/    1e 브리핑 카드
├── handoff/   ui/          1g 진료실 전달
└── visit/     ui/ data/    1m 1p 1q 1k 사후 기록
```

도메인 이름은 `Medical-Mate/Backend`의 패키지(`auth`, `profile`, `intake`, `card`,
`handoff`, `visit`)와 맞췄습니다. 같은 단어가 양쪽에서 같은 것을 가리키게 하려는
목적입니다. `home`은 백엔드에 대응이 없습니다.

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
| 유닛 테스트 | `LoginViewModel` 13건, `SessionViewModel` 23건, `HomeViewModel` 9건, `IntakeViewModel` 27건, `BodyMapGeometry` 17건, `BodyMapLayout` 10건, `BriefCardViewModel` 8건, `RecordDetailViewModel` 10건, `MyProfileViewModel` 5건, `HealthEditViewModel` 11건, `HospitalPickViewModel` 10건, `VisitRecordViewModel` 11건, `VisitNoteViewModel` 6건, `CalendarViewModel` 7건, `ProfileSetupViewModel` 7건, `HomeSchedule` 4건, `TokenAuthenticator` 7건, 템플릿 1개 |
| 네비게이션 테스트 | 없음. `NavHost`는 계측 테스트가 필요하고 CI가 androidTest를 실행하지 않음 |
| 아키텍처 패턴 | MVVM 확정. UseCase는 필요할 때만 |
| DI | Hilt 확정 |
| 패키지 구조 | 기능 우선 확정. 도메인명은 Backend와 일치 |
| 네비게이션 | Navigation Compose 2.10.0 확정. 단일 `NavHost` + 타입 세이프 라우트. 목적지 18개 |
| 화면 전환 | `MedicalMateNavHost`. `MainActivity`는 세션 확인 중 로딩만 담당 |
| ViewModel 스코프 | 화면 ViewModel은 목적지 스코프. `SessionViewModel`만 Activity 스코프 |
| 디자인 시스템 | DESIGN.md 3.0의 토큰·타이포·아이콘 반영 완료. 시맨틱 41개, 타이포 15종, 아이콘 46개(arrow-up 추가), 로고 4개, Pretendard 4무게. 3.0이 확정한 `layout/tabbar-h` 79와 Date Cell 42는 코드와 같다. Badge를 `Label/M`으로 적었던 2.0의 오기는 3.0에서 사라졌다 |
| 컴포넌트 | 50종 구현. Figma 마스터 대응은 `COMPONENT_MAP.md`. `Severity Scale`은 Figma에서 마스터가 삭제돼 함께 지웠는데 3.0 문서에는 아직 남아 있다. 디자인 트랙 확인 필요 |
| 컴포넌트 v2 | Figma `06 · 추가`가 9종을 v2로 재등록. KV Row·Tab Bar·Nav Bar·Date Cell·Text Field·Segmented Control 반영 완료. Text Area의 Footer Row(카운터·마이크), Toast의 Timer, List Row의 Summary는 모두 기본 false라 미구현. Ghost→Outline은 구현된 화면에서 해당 지점이 여전히 텍스트형이라 적용 대상 없음(F·H 플로우에서 화면별로 확인) |
| 컴포넌트 3.0 신규 | 7종 구현(`Add Row` · `Todo Row` · `Picker Field` · `Card Pick` · `Hospital Card` · `Select Bar` · `Onboarding Progress`). 아직 어느 화면도 쓰지 않는다. Preview로만 확인했고 기기 확인은 화면에 붙을 때 한다. `Hospital Card`·`Card Pick`·`Select Bar` 마스터는 채움이 묶여 있지 않아 카드 둘은 `bg/surface`를 넣었고 `Select Bar`는 면 없이 뒀다. 디자인 트랙 확인 필요 |
| CRUD 규칙 | 3.0이 장으로 확정. 편집 상태는 `편집 → 취소 → 확인` 한 자리에서 이름만 바뀌고, 삭제는 세 갈래(개체=하단 Danger CTA+Dialog / 항목=행 × 확인 없음 / 목록=체크 다중선택+Dialog)다. 토스트와 스와이프는 쓰지 않는다. 미반영이고 #92가 이 규칙을 쓴다 |
| 반경 13 · 14 | 3.0이 둘 다 스케일 밖 값으로 잡고 `radius/sm` 12 · `radius/md` 16 통일안을 적었다. 코드의 `MedicalMateRadius.dateCell`(13)과 `buttonM`(14)이 그 값이다. Figma가 바뀌면 함께 바꾼다 |
| Elevation/Card | Figma가 2겹(y3 r10 8% + y1 r2 5%)으로 바뀜. 코드는 `Modifier.shadow` 한 겹 근사 |
| Body Map | 구현 완료(#51). `core/designsystem/component`가 아니라 `intake/ui`에 있다. 화면 하나에서만 쓰고 좌표표·이미지 9장이 딸려 있어서 디자인 시스템에 올리지 않았다 |
| 인체도 좌표 | `BodyMapGeometry.kt`는 디자인 트랙의 `humanmap_coords.json`(schema 3.0)에서 생성한 파일이다. **손으로 고치지 않는다.** 좌표가 바뀌면 새 파일로 다시 생성한다 |
| 인체도 표시 크기 | 48dp 조작 영역이 겹치지 않는 최소 크기를 좌표에서 계산한다(`BodyMapLayout.kt`). 전신 505dp, 확대 최대 505dp. 시안 값을 받아 적지 않은 이유는 좌표가 바뀌면 필요한 크기도 바뀌기 때문이다 |
| 인체도 미해결 | 좌표표의 `SUR:041 허리 가운데`가 천골 위치(신장 58%)에 있다. 허리 옆(62%)과 같은 높이로 올려달라고 디자인 트랙에 넘겼다. 데이터만 교체하면 되는 건이다 |
| Search Field | 구현 완료(1m 병원 찾기에서 사용). 2.0 문서에 항목이 없어 Figma 마스터를 따랐고, 3.0이 항목을 올렸다 |
| 로그인 화면(1o) | 카카오 버튼 + 서버 토큰 교환 구현 완료. 토큰 적용 완료 |
| 홈 화면(1n) | Figma 1n-1·1n-2 반영. `HomeViewModel`이 픽스처를 노출. 서버 미연동 |
| 진입 · 온보딩(1a) | 스플래시(1a-1)와 온보딩 인트로(1a-2) 구현 완료. 스플래시는 최소 2초 노출, 세션 복구를 기다리는 상한은 6초. 상한을 넘기거나 연결이 없으면 `SessionUiState.RestoreFailed`로 로그인 화면에 보내고 카카오 버튼 위에 이유를 적는다. 시안에 없는 문구다. 상한을 넘기면 진행 중인 재발급 요청도 취소한다 |
| 신상정보 입력(1b) | 1b-1~1b-3 화면과 1b-4 완료 모션, 홈 등록 완료 토스트까지 구현. **서버 저장 미연동** |
| 증상 정리(1l·1c·1d·1i) | 네 단계 화면 구현. AI 응답과 음성 인식 미연동 |
| 아픈 부위(1l) | 인체도 구현 완료. 앞뒤 전환 · 앵커 9개 · 확대 후 구역 25개 · 팔·다리 좌우 반전 · 전신·피부 칩 · 진료과 안내 · 목록 대안. 부위 id는 AI 트랙의 온톨로지(`ANC:*` · `SUR:*`)를 쓰고 이름과 진료과는 `BodyMapOntologyFixture.kt`가 픽스처로 들고 있다. 서버 연동 시 그 파일만 지운다 |
| 부위 선택 개수 | 한 곳만 고른다. 다중선택을 넣었다가 되돌렸다 |
| 고른 부위 표시 | 상태를 `focus`(확대한 앵커)와 `selection`(고른 부위)으로 나눈다. 하나로 두면 구역을 고르는 순간 화면이 앵커 단계로 돌아가 고른 점도 고른 줄도 보이지 않는다. 확대 화면의 제목과 좌우 반전도 `focus`를 봐야 맞는다 |
| 인체도 화면 배치 | 판이 505dp라 상태바·하단 버튼까지 빼면 판 위에 쓸 수 있는 높이가 131dp뿐이다. 앵커 화면에 설명 문구를 두지 않는 이유이고, 전신·피부 칩과 목록 전환을 한 줄에 합친 이유다. 이 둘을 판 아래에 두면 첫 화면에서 보이지 않고 다리 앵커가 하단 버튼 뒤로 들어간다 |
| 화면별 스크롤 | `StepContent`가 `scrollKey`를 받는다. 인체도의 세 화면이 한 스크롤 상태를 공유하면 아래로 내려 부위를 짚었을 때 확대 화면이 그 위치로 열려 제목이 잘린다 |
| 확대 애니메이션 | 짚은 점을 축으로 전신을 밀어내고 확대 이미지를 들여보낸다(`BodyMapTransition.kt`). 두 이미지가 다른 파일이라 이어 확대할 수 없어 그렇게 읽히도록 만든 것이다. 판 모양으로 잘라야 커지는 이미지가 위아래 버튼을 덮지 않는다. 기기에서 애니메이션을 끄면(`ANIMATOR_DURATION_SCALE` 0) 즉시 바뀐다 |
| 목록에서 고르기 | 고르는 줄은 `MedicalMateRadio`다. 라디오가 역할과 선택 상태를 시맨틱에 실어 스크린 리더가 "선택됨"을 읽는다. 앵커 줄은 하위 목록으로 들어가는 이동이라 `List Row`이고, 그 안에서 고른 부위 이름을 보조 텍스트에 적는다 |
| 진료과 안내 | 없다. 응답(`docs/examples/body-map.json`)이 부위별 진료과를 함께 주지만 화면에 넣지 않았고, 픽스처에도 담지 않았다. 넣게 되면 응답에서 다시 가져온다 |
| 부위 이름 조사 | 물음의 주격 조사를 문자열 리소스에 박지 않는다. 부위가 25가지라 `%1$s가`로 고정하면 "무릎가"가 된다. 문구는 리소스에, 조사는 `withSubjectParticle`이 계산해 `IntakeUiState.bodyPartSubject`로 넘긴다 |
| 브리핑 카드(1e·1f) | 카드 읽기·전체 수정·진료실 화면 구현. 카드 내용은 픽스처. AI 응답과 저장 미연동 |
| 진료 후 기록(1m·1p·1q·1k) | 네 화면 구현. 병원 검색·메모·자동 분류·정리. 내용은 픽스처이고 AI 분류와 저장은 미연동. 캘린더 일자의 "진료 후 기록하기"에서 들어간다 |
| 기록·캘린더(1j·1r) | 다섯 화면 구현. 목록에서 기록 상세(1j-3)로, 상세의 카드 열기에서 브리핑 카드로 이어짐. 목록·상세·일정은 픽스처. 할 일 체크와 일정 추가는 저장 미연동 |
| 하단 탭 | 기록·홈·캘린더 세 탭 연결 완료. 내 정보는 탭이 아니라 홈 헤더 아바타로 진입 |
| 내 정보(1s) | 두 화면 구현. 프로필·건강 요약·설정 토글·로그아웃. 내용은 픽스처이고 설정과 건강 정보 저장은 미연동 |
| 로그아웃 · 회원탈퇴 | 구현 완료. 1s-1 하단으로 옮김. 회원탈퇴는 시안에 자리가 없어 로그아웃 아래 텍스트로 뒀다 |
| 계정 전환 시 이전 데이터 | 해결. 로그아웃 시 홈 엔트리가 pop되면서 `HomeViewModel`도 정리됨 |
| 401 재발급 Authenticator | 도입 완료. `TokenAuthenticator`가 401을 받으면 refresh 토큰으로 재발급하고 원래 요청을 한 번 더 보낸다. 재발급 호출은 인터셉터·Authenticator가 없는 `@AuthFree` 클라이언트로 나간다. 재발급이 거절되면 토큰을 지우고 `TokenStore.hasSession`이 false를 흘려 `SessionViewModel`이 로그인 화면으로 보낸다 |
| 토큰 암호화 | 미적용. DataStore 평문. 백업 차단으로 샌드박스 밖 유출만 막음 |
| 온보딩 노출 판단 | 서버 `onboardingRequired`와 로컬 `OnboardingStore`를 함께 본다. `refresh` 응답은 항상 false이고, 프로필을 서버에 저장하기 전까지 로그인 응답은 계속 true라 한쪽만으로는 안 된다 |
| 탈퇴 후 재가입 | 온보딩 기록을 지우지 않아 온보딩이 건너뛰어진다. 서버 `onboardingCompleted`가 정본이 되면 사라지는 문제 |
| Apple · 전화번호 로그인 | 백엔드 미지원. `User` 엔티티 식별자가 `kakaoId` 단독 |
| 카카오 말풍선 심볼 에셋 | 없음. 콘솔의 도구 > 리소스 다운로드에서 받아야 함 |
| 디자인 캔버스 | 360dp 재단 완료(활성 화면 전부 360x812, 콘텐츠 320, 거터 20). 화면은 fill-width라 코드 영향은 토큰뿐이었다. 미착수 화면(1e-2·1f-2·1q-2)은 아직 390. `1l-1`~`1l-3`도 390이지만 인체도는 좌표에서 크기를 계산해서 재단과 무관하다 |
| 소셜 로그인 버튼 규격 | 2.0 문서는 radius 16, 카카오 가이드는 12. 색은 가이드, 크기는 문서를 따름. 3.0은 반경을 적지 않는다 |
| `mipmap-*` 래스터 아이콘 | 템플릿 그대로. API 24~25에서 쓰인다. Android Studio Image Asset으로 교체 필요 |
| 오픈소스 고지 화면 | 없음. Pretendard가 OFL이라 스토어 배포 시 필요 |
| 로컬 저장 | 미정. 문답 화면 착수 때 백엔드 API를 보고 결정 |
| 릴리즈 서명 | 하지 않기로 결정. `bundleRelease` 산출물은 미서명 |
| 스크린샷 테스트 (Paparazzi/Roborazzi) | 미도입 |
| E2E (Maestro) | 미도입 |
| `scripts/verify.sh` | 미작성. 1장의 Gradle 명령을 직접 사용 |
| org 공통 문서 위치 | `GIT_CONVENTION.md`가 저장소별 사본으로 존재. 어긋나면 `.github` 저장소로 통합 필요 |

`local.properties`는 `.gitignore` 대상입니다. 로컬에서는 Android SDK 경로가 필요하고, CI는
`ANDROID_HOME` 환경 변수를 사용합니다.
