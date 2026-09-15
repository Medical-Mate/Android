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
| minSdk / targetSdk | 26 / 36 | `app/build.gradle.kts` |
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
│       │   │   └── network/               Retrofit·OkHttp 설정, ApiResult
│       │   ├── navigation/                단일 NavHost, 도메인별 그래프 등록, 세션 경계
│       │   ├── auth/  data/ ui/           1o 로그인, 1a-1 스플래시, 세션 복구
│       │   ├── profile/  data/ ui/        ONB 온보딩, 1b 신상정보, 1s 내 정보, 계정 동작
│       │   ├── intake/  ui/               1l 인체도, 1c·1d·1i 증상 정리
│       │   ├── card/  ui/                 1e 브리핑 카드, 1f 진료실 화면, 1j 기록
│       │   ├── calendar/  ui/             1r 캘린더
│       │   ├── visit/  ui/                1m 병원 찾기, 1p 메모, 1q 진료 후 기록
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
현재 사본은 3.0(2026-09-09)이고 저장소 루트에 있다.

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
└── visit/     ui/ data/    1m 1p 1q 사후 기록
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
| 유닛 테스트 | 모두 661건이다. `IntakeViewModel` 50건, `VisitRecordViewModel` 41건, `ScheduleAddViewModel` 36건, `BriefCardViewModel` 33건, `CalendarDayViewModel` 31건, `HomeRepository` 26건, `RecordDetailViewModel` 26건, `SessionViewModel` 23건, `VisitNoteViewModel` 21건, `CalendarViewModel` 19건, `HospitalPickViewModel` 19건, `RecordViewModel` 19건, `CardMapping` 18건, `MyProfileViewModel` 18건, `BodyMapGeometry` 17건, `HealthEditViewModel` 17건, `AppointmentRepository` 16건, `BriefCardListViewModel` 16건, `IntakeSessionActions` 16건, `BodyMap3dPick` 15건, `ProfileSetupViewModel` 14건, `LoginViewModel` 13건, `DesignToken` 12건, `RecordDetailFixtures` 12건, `BodyMap3dCamera` 11건, `BodyMapSearchState` 11건, `VectorParity` 11건, `BodyMapLayout` 10건, `BodyMap3dFrame` 8건, `BodyPartSearch` 8건, `BriefCardDelete` 8건, `BodyMap3dMesh` 7건, `HomeViewModel` 7건, `MedicalMateSeverity` 7건, `TokenAuthenticator` 7건, `VisitMapping` 7건, `VisitDetailViewModel` 6건, `FollowUpAppointmentScheduler` 5건, `HomeSchedule` 4건, `PretendardFont` 4건, `ProgressIndicator` 4건, `SessionRepository` 4건, `SegmentedControl` 3건, 템플릿 1개 |
| 네비게이션 테스트 | 없음. `NavHost`는 계측 테스트가 필요하고 CI가 androidTest를 실행하지 않음 |
| 아키텍처 패턴 | MVVM 확정. UseCase는 필요할 때만 |
| DI | Hilt 확정 |
| 패키지 구조 | 기능 우선 확정. 도메인명은 Backend와 일치 |
| 네비게이션 | Navigation Compose 2.10.0 확정. 단일 `NavHost` + 타입 세이프 라우트. 목적지 23개(1m-12 병원 확인 #246, 진료 후 기록 상세 #256). 그래프 등록은 도메인별 확장 함수로 나눠 `MedicalMateNavGraphs.kt`에 있고 `NavHost` 파일에는 그래프 본체와 세션 경계 처리만 남는다 |
| 화면 전환 | `MedicalMateNavHost`. `MainActivity`는 세션 확인 중 로딩만 담당. **전환은 `MedicalMateNavTransitions`가 들고 있다**(#214) — 가로로 밀고, 물러나는 화면은 폭의 1/4만 움직인다. navigation-compose의 기본값(0.7초 크로스페이드)을 그대로 두면 화면이 어느 쪽으로 갔는지가 남지 않는다. 가장자리를 쓸어 돌아가는 것도 같은 전환을 쓴다 — 돌아가는 길이 하나여야 하고, 기본값이 그 자리에 걸어 두는 축소(0.7)는 화면이 작아져 사라지는 것으로 읽힌다. 쓸어내는 동안에는 같은 전환이 손가락 위치만큼만 진행된다. 디자인 문서에 화면 전환 규격이 없어 값은 Material 표준을 따랐다 |
| ViewModel 스코프 | 화면 ViewModel은 목적지 스코프. `SessionViewModel`만 Activity 스코프 |
| 디자인 시스템 | DESIGN.md 3.0의 토큰·타이포·아이콘 반영 완료. 시맨틱 41개, 타이포 15종, 아이콘 46개(arrow-up 추가), 로고 4개, Pretendard 4무게. 3.0이 확정한 `layout/tabbar-h` 79와 Date Cell 42는 코드와 같다. Badge를 `Label/M`으로 적었던 2.0의 오기는 3.0에서 사라졌다 |
| 컴포넌트 | 52종 구현(`FAB` 추가). Figma 마스터 대응은 `COMPONENT_MAP.md`. `Severity Scale`은 Figma에서 마스터가 삭제돼 함께 지웠는데 3.0 문서에는 아직 남아 있다. 디자인 트랙 확인 필요. **`Voice Input`은 듣는 중에 파형이 원 안에 들어간다** — 원 위에 따로 띄우던 것을 마스터에 맞춰 고쳤다(#170). **`Section Header`의 제목은 `Heading/M` 20이고 액션은 가운데 맞춤이다**(#235). **`Nav Bar`는 바깥 8 · 슬롯 48 · 제목은 바의 가운데다**(#226) — 아이콘 왼쪽 끝이 화면에서 20에 온다. 제목을 좌우 슬롯 사이에 끼우면 텍스트 액션이 붙을 때 밀리므로, 좌우에 같은 값(액션 없으면 60, 있으면 88)을 비우고 그 안에서 가운데에 둔다. 우측 텍스트 액션은 버튼이 아니라 `Body/L Strong` 17 글자에 높이 48과 버튼 역할을 얹은 것이다. **상태 칩은 제목 바로 옆이다**(#227) — 마스터의 `Title Row`가 제목과 배지를 간격 6으로 묶는다. **`List Row`는 카드 모양으로 뜬다**(#227) — 면만 깔고 그림자가 없어 줄들이 바탕에 붙어 있었다. 마스터가 카드로 띄우고 구분선을 쓰지 말라고 적는다. 반경 16 · `Elevation/Card` · 좌 18 우 14다. **줄 제목은 `Heading/S`다**(#237) — `Body/L`로 두면 같은 17이라도 Regular라 제목이 아래 메타와 같은 무게로 읽힌다. 홈의 최근 브리핑 카드·다가오는 일정과 병원 찾기 결과가 이 한 줄을 함께 쓴다. **`Card`의 머리 라벨은 `Label/S`다**(#237) — 11 Medium에 자간 2다. 홈의 "오늘의 한 줄"·"이어서 하기", 일자 화면의 일정 카드, 내 정보의 로그인 수단 줄이 여기다. **목록 줄은 `Card`가 아니라 `List Row` 모양이다**(#229) — `MedicalMateCard`가 최소 높이 116과 여백 20을 강제하는데 시안의 줄은 104이고 편집에서는 80이다. 마스터 `List Row`(`335:1114`)의 반경 16 · 좌 18 우 14 · 위아래 16 · `Elevation/Card`를 `RecordList`가 직접 그린다. `Dialog`는 제목과 본문이 한 칸이고 사이가 4다. **불투명 `Bottom CTA Bar`에 `Elevation/Float`이 걸린다**(#230) — 마스터의 `Surface=Opaque`가 그 그림자를 달고 있고 `Glass`는 블러만 있다. 블러를 못 쓰는 환경에서 층을 만드는 것이 그림자다. **`Button`의 Ghost는 `fg/subtle`이다**(#249) — 마스터(`291:670`)가 세 크기 모두 그렇고 코드는 `fg/primary`였다. 면이 없는 버튼이 색까지 링크와 같으면 둘이 한 가지로 보인다. **`Notice`는 마스터(`292:668`) 구조 그대로다**(#243) — 흰 32 원형 배지에 20 아이콘, 왼 14 · 오른 16 · 위아래 14, 글 묶음 위 4에 제목·본문 사이 3. 아이콘을 맨몸으로 두고 여백 16에 최소 높이 79로 짜여 있던 것을 고쳤다. `BRAND` 톤은 마스터에 없다 — 기록 상세 예정 알림 인스턴스(`1076:4047`)가 `bg/primary` 면과 `fg/on-primary` 글자로 덮어 그린 것을 톤으로 올렸고, 마스터에 변이로 올려 달라고 디자인 트랙에 넘길 항목이다. **`Empty State`의 행동은 채움 없는 글자다**(#217) — 마스터는 Tonal 알약이고 설명도 그렇게 적는데, 시안의 인스턴스가 확인한 둘(`1j-2`·일자 화면) 모두 채움을 지우고 `Label/L`·`fg/link` 글자만 남겼다. 화면에 그려진 쪽을 따랐고 어느 쪽이 정본인지는 디자인 트랙 확인 필요 |
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
| 홈 화면(1n) | Figma 1n-1·1n-2 반영. `GET /api/me/home` 연동 완료(#137). 화면으로 돌아올 때마다 다시 읽는다. 읽지 않은 알림 표시는 늘 꺼 둔다 — 응답에 대응하는 값도 알림 목록 API도 없다. **오늘의 한 줄이 아홉 갈래다**(#235) — 디자인 트랙이 여덟 상태와 규칙을 확정했다. 차례가 곧 우선순위다(오늘 일정 · 기록이 빠진 지난 일정 · 다음 진료 · 지난 진료 · 카드만). 경과일과 남은 날은 이틀 이상일 때만 숫자로 적고 하루는 "어제"·"내일", 0일은 오늘 갈래다 — 앞날 진료에 음수가 찍히던 것이 이 규칙으로 사라졌다(#224). **기록이 빠진 지난 일정은 `pendingRecordOn`으로 온다**(#241 · Backend#123) — 그 달 일정을 따로 불러 앱이 찾던 것을 걷어냈다. 홈 진입마다 붙던 왕복과 달 경계 구멍이 함께 없어졌다. 서버가 일정 날짜로 남긴 기록이 있는지를 보고 14일까지만 거슬러 찾으며 취소한 일정은 세지 않는다. **오늘 일정은 시각이 지나도 `nextAppointment`에 하루 종일 남는다** — 서버가 날짜로만 견준다. 그래서 ①-a와 ①-b는 앱이 `scheduledTime`과 현재 시각을 견주어 가르고, 시간 미정이면 하루 종일 ①-a다. **"이어서 하기" 카드는 서버의 왕복 수가 아니라 증상 정리 네 단계로 적는다**(#176). `progressCurrent`/`progressTotal`은 문답 왕복이고 상한이 20이라 그대로 찍으면 "20단계 중 0단계"가 됐다. 부위를 짚어야 세션이 생기므로 1단계는 끝난 것이고 한 마디라도 답했으면 2단계다. 홈 응답에 강도도 질문도 없어 3·4단계는 가려낼 수 없고, 그 세션이 홈에 뜨는지 자체가 백엔드 확인 대기다. **카드도 일정도 없는 홈(`1n-2`)은 목록이 아니라 한 화면이다**(#215) — 빈 상태가 남은 높이를 전부 받아 그 안에서 가운데에 선다. 시안 인스턴스가 행동 버튼 자리를 꺼 두었다. 바로 위에 "증상 정리 시작하기"가 있어서 같은 곳으로 가는 버튼이 둘이 된다. **시작 버튼 아이콘은 청진기다**(#227) — 마이크를 달고 있었는데 말로 답하는 것은 증상 정리의 한 가지 방법일 뿐이다 |
| 진입 · 온보딩 | 스플래시와 온보딩 네 장(`V2-00`~`V2-03`) 구현 완료. **시안이 `A` 섹션을 통째로 교체했다.** `1a-1`·`1o`·`ONB-00`~`ONB-03` 프레임이 모두 삭제되고 `V2` 여섯 장이 들어왔으며 섹션 이름도 `A · 진입 — 스플래시 → 로그인 → 온보딩 v2 — 문구와 일러스트`가 됐다. 스플래시와 로그인은 `V2 · 스플래시`(`1320:4553`)·`V2 · 로그인`(`1320:4558`)이 문구와 구조까지 지금 구현과 같아 손대지 않았다. 온보딩 네 장은 문구·순서·그림이 전부 달라 다시 썼다(#131). 네 장이 한 목적지 안에서 넘어간다. 뒤로 갈 곳이 로그인이라 상단 바에 Nav Bar를 두지 않는다. **상단 바 오른쪽에 건너뛰기가 있다**(#213) — 누르면 소개를 끝내고 신상정보로 나간다. 마지막 장에도 있다. `Body/L Strong` 17인데 Ghost 버튼의 라벨이 15·13이라 버튼 컴포넌트를 쓰지 않고 글자에 48 터치 영역과 버튼 역할을 얹었다. 진행 표시는 상단 바의 세로 가운데다 — 건너뛰기가 들어오면서 시안이 y=26에서 가운데로 옮겼다. 그림은 네 장을 통째로 벡터로 내보냈다. v1과 달리 v2 일러스트에는 글자가 없어서 Compose로 나눠 그릴 이유가 없어졌고, 그때 만든 `OnboardingIllustrations`·`OnboardingIllustrationParts`·`img_onboarding_body`를 지웠다. 그림 폭은 자연 크기 352에서 멈춘다. 화면 폭을 채우게 두면 시안(360)보다 넓은 기기에서 함께 커져 304 칸을 넘는다. `widthIn`은 `fillMaxWidth` 앞에 둬야 걸린다. 스플래시는 최소 2초 노출, 세션 복구를 기다리는 상한은 6초. 상한을 넘기거나 연결이 없으면 `SessionUiState.RestoreFailed`로 로그인 화면에 보내고 카카오 버튼 위에 이유를 적는다. 시안에 없는 문구다. 상한을 넘기면 진행 중인 재발급 요청도 취소한다. **장이 밀려서 바뀌고 기기 뒤로가기로 되짚는다**(#227) — 앞으로 갈 때는 오른쪽에서, 되짚을 때는 왼쪽에서 들어온다. 상단 바와 하단 버튼은 그대로 둔다. 첫 장의 뒤로가기는 아무 일도 하지 않는다 — 그냥 두면 로그인 화면으로 나가는데 이미 로그인한 사람이라 로그아웃된 것으로 읽힌다 |
| 신상정보 입력(1b) | 1b-1~1b-3 화면과 1b-4 완료 모션 구현. **홈의 등록 완료 토스트는 걷어냈다**(#194) — IA가 `1n-2-S`를 제외로 옮기고 1b-4를 채택했다. 문구도 1b-4의 제목과 같아 같은 말이 두 화면에 이어 나왔다. 마지막 단계의 완료가 `PUT /api/me/health-profile`이고 성공해야 1b-4로 넘어간다. **단계별 저장이 없다.** 서버가 부분 갱신을 주지 않고 여섯 필드를 모두 요구해서 세 단계를 모아 한 번에 보낸다. 중간에 나가면 아무것도 남지 않는, 임시저장이 없는 유일한 흐름이다. 이름·출생연도·성별은 1b가 묻지 않고 카카오 값을 읽어 그대로 되돌려 보낸다 |
| 증상 정리(1l·1c·1d·1i) | 네 단계 화면 구현. AI 응답과 **음성 인식까지 연동됐다**(#190). 받아쓰기는 ML Kit GenAI이고 기기 안에서 돈다 — 오디오가 나가지 않는다. **말로 하는 대화가 한 바퀴 돈다**: 마이크를 누르면 바로 듣고(패널의 "말씀해 주세요"는 아직 못 들었다는 뜻이다), 말하는 글이 대화에 미리 서고, 다 말하고 누르면 그 자리에서 보내고, AI가 답하면 다시 듣는다. 문답이 끝나면 "다음"이라고 말해 넘어간다. **안 되는 기기에서는 마이크를 그리지 않는다** — Basic 모드가 API 31 이상이고 AICore를 못 쓰는 기기가 있다. 4단계(1i)의 목록은 `SessionResponse.questionCandidates`를 `rank` 차례로 채운다(#173). **3단계에서 강도를 저장하는 응답으로 받는다** — 서버가 모든 세션 응답에 후보를 싣고, 그 호출이 4단계 직전의 마지막 왕복이다. 환자가 한 번이라도 손댔으면 후보로 덮지 않는다 — 덮으면 지운 질문이 되살아난다. **상단 바 제목이 "기록"이고 진행 라벨이 "증상 정리"다** — 1c-2·1c-4·1c-5·1i 네 장이 그렇게 그려져 있다. **문답은 마디가 붙으면 끝으로 보낸다**(#176) — 키보드 높이도 조건에 넣는다. 마디가 늘지 않아도 보이는 높이가 줄면 마지막 마디가 입력창 뒤에 가린다. **이어서 하기는 나간 단계로 돌아간다** — 강도가 있으면 4단계, 문답만 끝났으면 3단계, 아니면 2단계다. 질문으로는 가릴 수 없다(적어 둔 것이 없어도 AI 후보가 그 자리에 온다). `IntakeStep`은 홈도 쓰므로 `core/model`에 있다. **답을 기다리는 점 셋이 움직이고 AI의 말이 설 왼쪽에 선다**(#228) — 면 없이 점만 둔다. 기기에서 애니메이션을 껐으면 움직이지 않는다. **강도 슬라이더는 마스터 값이다**(#228) — 트랙 10 · 정지점 6 · 손잡이 28에 링 4이고, 정지점은 채움 위면 흰색 밖이면 테두리 색이다. 끄는 동안에는 손가락을 따라가고 놓을 때 단계로 붙는다. **질문을 더하면 그 줄로 화면이 따라간다** — 화면이 열릴 때 AI 후보가 채워지는 것까지는 따라가지 않는다 |
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
| 브리핑 카드(1e·1f·1j-4) | 카드 읽기·전체 수정·진료실 화면과 카드 전체 목록(1j-4) 구현. 목록은 홈의 "전체 보기"에서 들어가고 편집으로 여러 장을 골라 지운다. 읽기·수정·확정이 `GET`·`PATCH`·`POST /api/cards/{id}`에 붙어 있고(#139) 삭제는 #157이다. 카드 본문은 `BriefCardBlock` 하나이고 1e-1·진료실 화면이 같이 쓴다(#167). **강도는 KV 줄이 아니라 눈금이다** — `severity` 축에서 숫자만 읽어 다섯 단계에 맞추고, 밖이면 그리지 않는다. **복용약·기저질환·알러지가 카드 응답에 실린다**(#181) — `patient.medications`·`conditions`·`allergies`이고 앞의 둘은 카드 줄, 알러지는 카드 밖 경고다. 전에는 화면이 `GET /api/me/health-profile`에서 읽어 얹어 보는 시점의 프로필이 찍혔는데 그 우회를 걷어냈다. 알러지만 한 줄로 와서 쉼표로 나눈다. **진료실 전달은 이 화면이다**(#194) — IA가 `1f`를 제외로 옮기고 1e-1을 그대로 건네기로 해서 전달 화면·목적지·API를 걷어냈다. **카드를 확정하는 자리가 없다** — `POST /api/cards/{id}/confirm`을 부르던 것이 그 버튼 하나였고, 버튼이 빠진 뒤로는 걷어내기 전에도 불리지 않았다. 어디서 확정할지는 미정이다. **병원은 라우트를 타고 들어온다** — 카드 응답에 없고 목록 응답과 홈 응답의 `clinicName`에만 있어서, 홈·브리핑 카드 전체에서 누른 줄이 들고 있던 값을 함께 넘긴다. **병원 찾기(1m-B)에서 고른 경우에는 주소도 함께 나른다**(#194) — 시안 1e-1의 병원 블록이 이름 아래에 주소를 그린다. 목록에서 연 카드는 `CardSummary`에 주소가 없어 이름뿐이다(Backend#101). 서버가 카드 응답에 담아 주면 나르던 코드를 지운다. **카드를 지우면 카드 목록으로 간다**(#194) — IA `1e-1-DC`가 그렇게 적는다. 카드를 만드는 것은 문답 쪽(`POST /api/sessions/{id}/card`)이다. **확정한 카드를 고치면 서버가 새 버전을 만든다**(Backend#114) — 응답의 `cardId`가 달라지므로 화면이 그 카드로 갈아타야 한다. 라우트가 든 옛 id로 다시 읽으면 고친 것이 사라진 것처럼 보이고, 옛 id로 다시 고치면 버전이 가지를 쳐서 그 편집이 어느 화면에도 나오지 않는다(#219). 그래도 남은 경로가 있으면 서버가 `CARD_ALREADY_EDITED`로 막고 `details.latestCardId`를 준다(Backend#117). 그 카드로 갈아타고 **한 번만** 다시 보낸다(#222). **이미 저장한 카드에는 하단 저장하기가 없다**(#229) — 다시 보는 자리인데 버튼이 서 있으면 저장이 안 된 것으로 읽힌다. 아직 저장하지 않은 카드에는 남긴다. 저장이 곧 확정이고 확정하지 않은 카드에는 진료 후 기록을 붙일 수 없다. 카드 안에서 병원만 바꾸러 1m-B로 갈 때는 CTA가 "브리핑 카드 만들기"가 아니라 "완료"다 — 계속 따라가면 어디서 멈출지 알 수 없고, 다른 데서 고치는 중이라면 덮어쓰기를 반복하게 된다 |
| 진료 후 기록(1m·1p·1q) | 세 화면 구현(1m·1p·1q-1). 캘린더 일자의 "진료 후 기록하기"에서 들어간다. **그 앞에 병원 확인(1m-12, `1576:8517`)이 선다**(#246) — 일정에 등록한 병원이 맞는지 묻고, 맞으면 바로 1p로, "다른 병원이에요"면 1m으로 간다. 병원은 일정을 추가할 때 이미 등록하는 값이라 아는 채로 다시 찾게 할 이유가 없다. 일정에 병원이 없을 때만 1m이 첫 화면이다. 주소는 일정 응답에 없어 카드에 남은 것(1m-B에서 고른 값)을 적고 없으면 이름만 적는다. 1q-1의 저장이 `POST /api/cards/{cardId}/visit`이고, 그 요청에 들어갈 값(병원 이름·붙일 카드·원문 메모)이 라우트를 타고 1m → 1p → 1q-1로 따라간다. 1p의 병원 블록(아이콘·병원 이름·무엇으로 받은 진료인지·구분선·언제인지)은 픽스처가 아니라 앞 화면에서 고른 값이다 — 카드 제목은 캘린더 일자에서 라우트를 타고 따라온다. `Hospital Card` 컴포넌트를 쓰지 않는다. 그쪽은 흰 면에 칩 pill이고 여기는 조용한 면에 평평한 글줄이라, 시안에서도 인스턴스가 아니라 프레임이다. **마이크를 누르면 증상 문답과 같은 음성 패널(`MedicalMateVoiceInput`)이 적던 글 아래에 선다**(#170). 마이크 버튼은 `Icon Button`이 아니라 `FAB`다 — 48 원형·브랜드 채움은 같고 그림자가 있어 본문 위에 뜬 층으로 읽힌다. 캘린더의 일정 추가도 시안에서는 같은 FAB인데 아직 텍스트 버튼이다. **받아쓰기가 붙었다**(#190). 증상 문답과 같은 `Dictation`을 쓰고, 말이 끊겼다 이어져도 앞말 뒤에 붙는다. **1q-1에 재방문 일정 등록 체크가 없다** — 시안에서 빠졌다. 재방문 날짜는 `follow_up` 축으로 실려 간다(#178). **저장이 되면 그 날짜로 재방문 일정이 만들어진다**(#245) — 서버는 기록에서 일정을 만들지 않는다(문서가 "환자가 보고 등록하는 흐름"으로 못 박았고 AI가 날짜를 잘못 뽑아도 조용히 생기지 않아야 한다고 적는다). 그 확인은 1q-1의 재방문 줄이 한다 — 날짜가 보이고 고칠 수 있어 저장이 곧 확인이다. `core/model/FollowUpScheduler`를 `calendar`가 구현하고 `visit`이 받는다. 같은 카드로 그 날 일정이 있으면 만들지 않고, 시각은 비우며, 출처가 `VISIT_FOLLOW_UP`이라 홈·일자 화면이 "재진"으로 적는다. 못 만들어도 저장은 성공이다. **일정에 카드가 걸려 있지 않으면 일자 화면에 "진료 후 기록하기"를 두지 않는다.** 일정 추가에서 **시간은 필수가 아니다**(#172) — 안 고르면 오전 9시로 저장한다. 서버 `scheduledAt`이 시각을 요구하고 미정을 실을 자리가 없다. 서버가 카드에 매달린 기록만 받아서, 카드 없이 들어가면 끝에서 저장이 아무 일도 하지 않았다. **AI 분류가 `POST /api/visits/classify`에 붙었다**(#183). 1p에서 적은 메모를 축 맵으로 나눠 받고 저장은 하지 않는다. **항목은 고정이 아니다** — 값이 있는 축만 줄로 서고 모르는 축도 뒤에 붙는다. 하나도 못 나눴을 때만 소견·검사·약·재방문 네 자리를 빈 채로 연다. 편집에서 줄을 더할 수 없어서고, 그 자리는 #184다. 다시 나눌 때는 직전 `labels`를 함께 보낸다 — 안 보내면 AI 모델 호출이 다시 나간다. **진료 날짜는 오늘이 아니라 흐름이 시작된 캘린더 일자다**(#178). 어제 진료를 오늘 적어도 그 일자 화면에 선다. 병원 검색은 `GET /api/hospitals`다(#155). 원천이 심평원이고 서버가 목록을 들고 있지 않아 앱도 캐시하지 않는다. **이름과 주소가 온다**(#187) — 우리가 요청해서 홈페이지 자리를 주소가 대신했다(Backend#80). 같은 이름의 다른 지점을 구별하는 값이라 결과 줄에 함께 적고, 심평원에 없는 곳은 비어 있어 그때는 줄을 그리지 않는다. **상류가 죽으면 빈 목록이 아니라 에러가 온다** — "검색 결과 없음"과 "지금 검색이 안 됨"을 가르던 코드가 그제야 제 일을 한다. 검색어가 멎은 뒤(300ms) 한 번 부르고 앞선 요청은 취소한다. **진료 전(1m-B)의 상단 바에는 뒤로가기만 있다**(#228) — 병원 없이 카드로 가던 건너뛰기를 걷어냈다. 시안에 그 자리가 없고, 나가는 길은 뒤로가기이며 1c-5의 "브리핑 카드 만들기"가 그대로 있다. 빈 상태는 병원 아이콘이고 남은 높이를 받아 가운데에 선다. 부분 일치라 받은 것보다 많으면 그 사실을 줄에 적는다. **1k 이번 진료 정리는 시안에서 사라졌다.** 유저플로우가 1q-1 저장에서 캘린더 일자로 바로 잇는다. #159에서 붙였다가 #165에서 화면·상태·문자열을 함께 걷어냈다. 저장하면 흐름이 시작된 일자로 돌아가고 그 화면이 다시 읽으면서 방금 남긴 기록이 "이 날 기록"으로 선다. **저장이 안 될 때 다시 눌러 될 일인지를 가려 적는다**(#220) — 연결이 없거나 서버가 재시도 여지를 준 경우만 "잠시 후 다시 시도해주세요"이고, 서버가 거절한 경우는 브리핑 카드를 먼저 저장해야 한다고 적는다. 한 문구로 두면 눌러도 되지 않는 버튼을 계속 누르게 된다 |
| 기록·캘린더(1j·1r) | G·H 섹션 대조 완료(#121). 기록 목록·비어 있음·상세와 상세의 두 상태(카드 펼침 1j-3-X · 재방문 누적 1j-3-R), 목록 편집과 여러 건 삭제(1j-1-D·D2·DC), 캘린더 월·일자와 카드만 있는 날 시트(1r-1-S), 일정 추가 여덟 장(1r-4 계열), 일자 편집과 일정 삭제(1r-2-E·E2·DC), 다녀온 날과 다음 일정(1r-2-A·A2)까지 구현. 기록 목록은 `GET /api/me/visits`를 달로 묶고, 상세는 `GET /api/visits/{id}`다. **상세 타임라인이 세 단계다**(#171). 예정 · 진료 후 기록 · 브리핑 카드 순으로 최신이 위다 — 시안 `1j-3`은 카드가 위이고 `1j-3-R`은 최신이 위인데 디자인 피드백이 최신 위를 멘탈 모델로 적어 둔 쪽을 따랐다. 기록 · 카드 · 일정 셋을 읽는다. 기록을 못 읽으면 실패이고 나머지 둘은 못 읽어도 그 단계만 빠진다. **재방문이 쌓이면 한 화면에 모두 선다**(#221) — 카드 하나에 기록 하나였던 제약이 풀리면서(Backend#119) 같은 문답에 재방문이 이어 붙는다. `GET /api/cards/{cardId}/visits`로 모은다. 목록을 `cardId`로 거르면 안 된다 — 재방문 전에 카드를 고치면 두 기록이 서로 다른 카드 행에 붙고 목록의 `cardId`는 최신 버전이라 묶을 열쇠가 못 된다(Backend#121). 모아 주는 목록에 축이 없어 기록마다 한 번씩 더 읽고, 못 읽은 것은 빼고 그린다. 머리글은 시안 그대로다(#243) — 제목이 병원이 아니라 브리핑 카드의 제목이고(카드 응답의 제목이 비면 목록이 든 제목, 그것도 없으면 병원), 둘째 줄은 병원이 앞이고 날짜가 뒤다. 한 번이면 `1j-3`의 `병원 · 09.12 진료`, 여러 번이면 `1j-3-R`의 `병원 · 09.12 초진 · 09.26 재방문`이다. 블록 제목은 시점 줄과 같은 말이다(`진료 후 기록` · `브리핑 카드`) — "진료에서 들은 것"·"진료 전에 정리한 것"으로 따로 이름 붙이던 것을 걷어냈다. 카드 단계의 시점 줄은 `09.04 작성`이다 — 시안은 뒤에 `09.12 진료실에서 보여줌`을 잇지만 그 날짜는 바로 위 진료 후 기록 단계가 이미 적어서 뺐다. 예정 알림은 "다음 진료가 예약돼 있어요" 아래 `9월 26일 (토) 오전 10:30`이다. 미리보기 픽스처(`RecordDetailFixtures.kt`)가 먼저 시안 문구였고 실제 매핑만 뒤처져 있었다. **카드 전체 보기는 높이가 이어지며 펼쳐지고 자리를 잡은 뒤 그 블록으로 화면을 옮긴다**(#243). `animateContentSize`가 내용을 자기 크기로 잘라서 `shadow`보다 안쪽에 두면 카드 좌우 그림자가 잘린다 — 그림자를 바깥에 둔다. **진료 후 기록 단계에 원문 인용이 없다** — 원문은 1q-1에서 확인하고 저장하는 값이다. 예정 단계는 그 카드로 잡힌 앞으로의 일정이 있을 때만 붙는다(재방문 날짜가 기록 응답에 없다). 카드 단계에 건강 정보가 함께 선다(#181). 서버가 카드에 박아 준 값이라 그때 먹던 약이 남는다. 일자 화면의 기록은 `GET /api/me/visits`를 그 날짜로 거른 것이고, **그 줄을 누르면 기록 상세(1j-3)가 아니라 진료 후 기록 상세로 간다**(#256) — `VisitDetailDestination`이 `GET /api/visits/{id}`로 읽어 1q-1과 같은 카드를 읽기 전용으로 그린다. 1j-3은 카드·기록·예정을 모아 보이는 자리라 기록 탭의 줄만 그리로 간다. 줄의 제목은 시안대로 `진료 후 기록`이고 메타는 상세를 따로 읽어 항목 값의 첫 줄을 `·`로 이은 것이다(못 읽으면 병원 이름). 일정 카드의 마지막 줄은 `복부 통증 브리핑 카드를 가져가요`다. 다음 일정은 `GET /api/me/appointments/upcoming`의 첫 줄이다(#163). 기록이 있는 날에만 다음 일정을 붙이고 그 날 자신의 일정은 뺀다. 진료가 끝난 날에는 진료 전 할 일을 두지 않는다. **할 일은 그 날 일정에 매달린다**(#187) — `AppointmentResponse.todos`이고 `POST`·`PATCH`로 보낸다. 편집 중에는 저장하지 않는다(취소가 실행 취소를 대신하는데 이미 보냈으면 되돌릴 것이 없다). 마칠 때 통째로 갈아끼운다 — 지운 줄이 남지 않으려면 화면에 있는 것을 전부 보내야 한다. 일정이 없는 날에는 할 일도 없다. 목록 삭제는 `DELETE /api/visits/{id}`다(#178) — 서버가 카드 삭제와 갈라 줘서 기록만 지운다. 한 번에 지우는 API가 없어 한 건씩 부르고, 일부가 실패해도 나머지는 계속 지운다. `1r-2-C`는 IA가 `1r-1-S로 대체`로 적어 시트 쪽으로 정해졌다(#189). **캘린더의 "기록 있음" 점은 카드를 쓴 날과 진료 후 기록을 남긴 날이다**(#251) — 기록을 남긴 날을 빼면 재방문하고 기록까지 남긴 날이 그 날 일정 때문에 "예정"으로 남는다. 기록이 예정보다 앞선다. **기록 상세의 예정은 가장 최근 기록보다 뒤의 일정이다**(#251) — 열어 본 기록 기준으로 찾으면 첫 기록에서 열 때 이미 다녀온 재방문 일정이 예정으로 선다. 잡아 둔 일정이 없으면 최신 기록의 재방문 날짜를 "재방문 예정이에요"로 세운다. **일자 화면은 그 날 기록을 전부 세운다**(#235 · #179) — 하루에 진료가 둘이면 기록도 둘이다. 카드와 일정은 초진에 묶이고 기록만 진료마다 쌓인다. **카드만 있는 날 시트는 그 카드로 만든 일정이 있으면 그리로 보낸다**(#235). **일정 추가는 열릴 때 날짜와 병원을 들고 간다**(#230) — 캘린더에서 고른 날(보고 있는 달일 때만), 일자 화면의 그 날, 다음 일정의 그 날이 각각 따라온다. 아무 날도 안 고른 채 들어오면 빈 채로 열고 그 화면에서 고른다(1r-4-D). 가져갈 카드를 고르면 그 카드의 병원이 채워진다(1r-4-B) — 이미 적힌 병원은 덮지 않는다. **시간 미정 일정은 눌러서 고친다**(#230) — 일자 화면의 카드를 누르면 일정 추가가 그 값으로 열리고 저장이 `PATCH`로 나간다. 새로 만들면 같은 일정이 둘이 된다. 서버에 일정 하나를 id로 읽는 경로가 없어 그 날의 목록에서 찾는다. 수정 요청에 병원 자리가 없어 병원은 보내지 않는다. **시간 휠은 눌러서도 고른다**(#230) — 굴리는 길만 두면 값이 둘뿐인 열에서 얼마나 움직여야 하는지가 안 보이고, `ModalBottomSheet`가 남은 스크롤을 받아 시트를 닫아 버린다. 그 남은 양은 휠에서 삼킨다. **일자 편집의 일정 삭제는 하단 고정이 아니라 본문 끝이다**(#230) — 고정하면 편집을 누르는 순간 마지막 요소를 자른다 |
| 하단 탭 | 캘린더·홈·기록 세 탭 연결 완료. 홈이 가운데다. 내 정보는 탭이 아니라 홈 헤더 아바타로 진입. 마스터에 네 번째 variant(`Active=Active4`, 아무 탭도 활성이 아닌 상태)가 있지만 쓰는 화면이 없어 열지 않았다(#194) |
| 내 정보(1s) | 두 화면 구현. 1s-1의 건강 요약과 1s-2가 `GET`·`PUT /api/me/health-profile`이다. 1s-2에서 고치고 돌아오면 1s-1이 다시 읽는다(`ON_RESUME`). 한 번만 읽으면 방금 고친 값 대신 들어올 때 읽은 값이 남는다. 프로필 줄(이름·생년·성별)도 같은 응답에서 온다. 이름은 서버가 마스킹해 줄 수 있어("김OO") 아바타 글자를 자르는 자리를 ViewModel에 뒀다. 로그인 수단은 읽지 않는다 — 서버의 식별자가 `kakaoId` 단독이라 카카오 하나뿐이고 그 줄은 고정이다. 값이 없으면 그 줄을 그리지 않는다. **설정 토글 셋 중 하나만 계정이다**(#187) — 진료 하루 전 알림이 `GET`·`PATCH /api/me/settings`이고, 받을지 말지가 기기 취향이 아니라 그 사람의 선택이라 계정에 붙는다(기기를 바꾸면 안 받겠다고 한 사람에게 다시 간다). 나머지 둘은 이 기기에서 어떻게 보일지의 문제라 `DataStore`다. 알림을 예약하는 것은 여전히 앱이고 그 값은 예약할지 말지를 정한다 |
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
| 디자인 트랙 | #189의 열한 건은 2026-09-15에 모두 확인이 끝났다 — 셋은 #235로 반영했고(1q-1 편집은 찾은 줄만 · 하루 두 진료는 기록만 쌓임 · 저장한 기록은 고치지 않음), 둘은 피그마 대조에서 풀렸고(인체도 프레임은 `제작 예정` · `1r-2-C`는 `1r-1-S`로 대체), 남은 여섯은 코드 그대로 확정이다. #208(기록 상세에 원문 없음)도 구현된 모양으로 확정됐다. **아직 넘겨야 할 것** — 코드가 마스터나 시안과 갈린 채 두고 있는 자리다. (1) `Notice`에 브랜드 채움 변이가 없다 — `1j-3` 예정 알림이 인스턴스 덮어쓰기로 그려져 있어 `BRAND` 톤으로 올렸다(#243). (2) `Badge`에 `Label/M` 13 크기 변이가 없다 — `1j-3` 머리 뱃지가 그 크기로 직접 그려져 있고 코드는 마스터(`Label/S` 11)를 따른다(#237). (3) `Empty State`의 행동이 마스터는 Tonal 알약, 인스턴스는 채움 없는 글자다(#217). (4) `Select Bar`·`Hospital Card`·`Card Pick` 마스터에 채움이 묶여 있지 않아 코드가 색을 정했다. (5) `Severity Scale`은 마스터가 지워졌는데 3.0 문서에 남아 있다. (6) 1m-12의 "다른 병원이에요"는 Ghost 버튼 여백 20을 물려야 시안 자리에 온다 — 마스터 Ghost에 여백 없는 변이가 없다(#246). (7) 기록 상세 카드 단계의 시점 줄에서 시안의 `09.12 진료실에서 보여줌`을 뺐다 — 바로 위 진료 후 기록 단계가 같은 날짜를 적어서다(#243). (8) 시간 휠의 분이 시안(00 · 30)과 달리 10분 단위다(#239). **백엔드에 알릴 것** — 진료 후 기록 저장 문서가 "일정은 만들지 않는다, 앱에서 따로 부르라"인데 앱이 1q-1에서 확인한 재방문 날짜로 저장 직후 일정을 만든다(#245). 일정 응답에 병원 주소가 없어 1m-12가 카드에 남은 주소로 대신한다(#246) |
| org 공통 문서 위치 | `GIT_CONVENTION.md`가 저장소별 사본으로 존재. 어긋나면 `.github` 저장소로 통합 필요 |

`local.properties`는 `.gitignore` 대상입니다. 로컬에서는 Android SDK 경로가 필요하고, CI는
`ANDROID_HOME` 환경 변수를 사용합니다.
