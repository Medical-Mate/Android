# CLAUDE.md

Medical Mate Android 앱 저장소에서 AI 에이전트가 작업할 때 참고하는 문서입니다.
사람도 그대로 읽을 수 있게 작성합니다. 사실과 달라지면 즉시 갱신합니다.

---

## 1. 검증 명령

**커밋 전에 반드시 실행합니다.**

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

- **`testReleaseUnitTest`는 존재하지 않습니다.** 유닛 테스트는 debug 변형만 등록됩니다.
- `--offline`은 로컬 전용입니다. CI에서 쓰면 의존성을 받지 못해 실패합니다.
- 실패하면 **코드를 고칩니다.** 검증을 우회하는 방법은 6장을 참고하세요.

---

## 2. 기술 스택

| 항목 | 값 | 정의 위치 |
| -- | -- | -- |
| Gradle | 9.5.0 | `gradle/wrapper/gradle-wrapper.properties` |
| AGP | 9.3.2 | `gradle/libs.versions.toml` |
| Kotlin | 2.2.10 | `gradle/libs.versions.toml` |
| Compose BOM | 2026.02.01 | `gradle/libs.versions.toml` |
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
| DataStore Preferences | 1.2.1 | `gradle/libs.versions.toml` |

규칙:

- **의존성은 반드시 `gradle/libs.versions.toml`(버전 카탈로그)에 추가하고 `libs.*` 별칭으로 참조합니다.** 빌드 스크립트에 좌표를 직접 쓰지 않습니다.
- **`org.jetbrains.kotlin.android` 플러그인을 추가하지 마세요.** AGP 9의 내장 Kotlin 지원을 사용하며, 현재 `plugins` 블록에는 `com.android.application`과 `org.jetbrains.kotlin.plugin.compose`만 있습니다.
- `compileSdk`는 37 이상을 유지합니다. `androidx.core:core-ktx:1.19.0`이 API 37 이상을 요구하며, 낮추면 `checkDebugAarMetadata`에서 빌드가 실패합니다.
- **kotlinx-serialization-json은 1.9.0에 고정입니다.** 1.11.0은 `kotlin-stdlib` 2.3.20을 요구해 Kotlin 2.2.10 컴파일러가 읽지 못합니다. `kotlin`을 올릴 때 함께 재검토합니다. 최신 버전을 그대로 올리면 빌드가 깨집니다.
- **백엔드 base URL은 `BuildConfig.BACKEND_BASE_URL`로 주입합니다.** `local.properties`의 `BACKEND_BASE_URL` 또는 환경변수로 덮어쓸 수 있고, 기본값은 배포된 dev 서버입니다.
- **`java.time`을 쓰려면 core library desugaring이 켜져 있어야 합니다.** minSdk 24라서 끄면 `lintDebug`가 `NewApi` 오류로 막습니다(API 26 요구). `app/build.gradle.kts`의 `isCoreLibraryDesugaringEnabled = true`와 `coreLibraryDesugaring(libs.desugar.jdk.libs)`를 지우지 마세요. 일정·복용 알림·카드 작성일까지 날짜가 계속 나옵니다.
- **KSP 버전은 Kotlin 버전에 묶여 있습니다.** `kotlin`을 올리면 `ksp`도 함께 올려야 합니다. KSP 2.3.x는 Kotlin 2.3용이므로 지금 쓸 수 없습니다.
- **`android.disallowKotlinSourceSets=false`를 지우지 마세요.** KSP가 생성 소스를 `kotlin.sourceSets`로 등록하는데 AGP 9의 내장 Kotlin 지원이 이 DSL을 금지합니다. AGP가 직접 안내하는 억제 옵션이며, 없으면 `:app` 설정 단계에서 빌드가 실패합니다. KSP가 built-in Kotlin에 대응하면 제거합니다.
- **카카오 SDK는 Maven Central에 없습니다.** `settings.gradle.kts`의 `dependencyResolutionManagement`에 `https://devrepo.kakao.com/nexus/content/groups/public/`이 등록돼 있고 `includeGroup("com.kakao.sdk")`로 범위를 제한합니다. `repositoriesMode`가 `FAIL_ON_PROJECT_REPOS`라 모듈 빌드 스크립트에는 저장소를 넣을 수 없습니다.
- **네이티브 앱 키는 `local.properties`의 `KAKAO_NATIVE_APP_KEY`에서 읽습니다.** 없으면 환경변수를 보고, 그것도 없으면 빈 문자열로 빌드는 통과합니다. 실패는 런타임 카카오 API 호출에서만 납니다. CI는 환경변수 경로를 씁니다.
- 매니페스트에 `<queries>`를 직접 추가하지 마세요. `v2-common` AAR이 `com.kakao.talk`와 alpha·sandbox를 이미 선언하고 전이 병합됩니다. 손으로 넣으면 중복이고 alpha·sandbox가 빠집니다.
- `org.gradle.configuration-cache=true`가 켜져 있습니다. 빌드 스크립트에서 configuration cache와 호환되지 않는 패턴(태스크 실행 시점의 `Project` 접근 등)을 쓰지 않습니다.

---

## 3. 모듈 맵

단일 모듈입니다.

```text
MedicalMate/
├── app/                         유일한 모듈 (com.android.application)
│   └── src/
│       ├── main/java/com/mist/medicalmate/
│       │   ├── MainActivity.kt          진입점. 로그인·홈 임시 전환
│       │   ├── MedicalMateApplication.kt  @HiltAndroidApp, KakaoSdk.init
│       │   ├── core/
│       │   │   ├── designsystem/        Color.kt, Theme.kt, Type.kt
│       │   │   └── network/             Retrofit·OkHttp 설정, ApiResult
│       │   ├── auth/  data/ ui/         1o 로그인, 세션 복구
│       │   └── home/  ui/               1n 홈
│       ├── test/                        JVM 유닛 테스트
│       └── androidTest/                 계측 테스트 (CI에서 실행하지 않음)
├── config/detekt/detekt.yml     detekt 기본 설정 위에 얹는 예외
├── .editorconfig                ktlint가 읽는 코드 스타일
├── gradle/libs.versions.toml    의존성 버전 단일 진실 소스
└── .github/workflows/ci.yml     CI
```

- 패키지 루트: `com.mist.medicalmate`
**정해진 것**

- **UI는 Compose 단독입니다.** XML 뷰를 추가하지 않습니다. `res/values/themes.xml`은 Manifest용 테마로만 남습니다. 예외로 카카오 SDK가 `appcompat`과 `material` 뷰 라이브러리를 전이 의존으로 끌고 옵니다. SDK 액티비티가 그 테마를 쓰므로 exclude하면 런타임에 깨집니다.
- **패턴은 MVVM + 단방향 흐름입니다.** ViewModel이 상태를 노출하고 Composable이 소비합니다. MVI 라이브러리(Orbit, Mavericks)는 도입하지 않습니다.
- **UseCase 계층은 기본적으로 두지 않습니다.** `ui → Repository`가 기본이고, 여러 Repository를 조합하거나 실제 비즈니스 규칙이 있을 때만 UseCase를 만듭니다. 한 줄 위임만 하는 UseCase를 만들지 마세요.
- **DI는 Hilt입니다.** `@HiltAndroidApp`은 `MedicalMateApplication`, 화면 진입점은 `@AndroidEntryPoint`.
- **네트워크는 Retrofit + OkHttp + kotlinx.serialization입니다.** 설정은 `core/network/NetworkModule`.
- **API 실패는 예외가 아니라 값으로 다룹니다.** `apiCall()`이 `ApiResult`(`Success` / `Rejected` / `NetworkUnavailable`)를 돌려주고, Repository는 도메인 결과 타입으로 바꿉니다. ViewModel에 `try/catch`가 없습니다. 예외로 계층을 넘기면 호출자가 결국 `catch (e: Exception)`으로 뭉개게 되고, detekt의 `TooGenericExceptionCaught`·`SwallowedException`이 그걸 잡습니다. 설정을 완화하지 말고 결과 타입을 쓰세요.
- **직렬화 실패 같은 계약 위반은 잡지 않습니다.** `apiCall()`은 `HttpException`과 `IOException`만 잡습니다. 계약이 어긋난 것을 "다시 시도해주세요"로 감추면 원인이 묻힙니다.
- **`core`가 도메인을 참조하지 않습니다.** 인증 헤더를 붙이는 `AuthInterceptor`는 `core/network`에 있고 토큰이 필요하지만, `auth`의 `TokenStore`를 직접 쓰지 않고 `core/network/AccessTokenProvider` 인터페이스를 통해 받습니다. 구현 연결은 `auth/data/AuthModule`의 `@Binds`가 합니다. 반대로 두면 공용 계층이 특정 도메인에 묶입니다.
- **서버 JWT는 `TokenStore`(DataStore)에만 둡니다.** 카카오 토큰은 SDK가 자체 보관하고 서버도 저장하지 않으므로 앱이 따로 저장하지 않습니다. 같은 자격증명을 여러 곳에 두면 처리 범위만 늘어납니다.
- **`android:allowBackup="false"`를 되돌리지 마세요.** 토큰과 향후 로컬 캐시가 Google 클라우드 백업·기기 간 전송으로 나가는 것을 막습니다. refresh 토큰 TTL이 30일입니다.
- **ViewModel이 Activity 스코프라는 것을 전제하고 짜세요.** 화면 전환이 NavHost가 아니라 `MainActivity`의 `if` 분기라서 목적지별 스코프가 없습니다. `hiltViewModel()`로 가져온 ViewModel은 화면을 떠나도 살아 있고 상태가 남습니다. **완료·성공 같은 일회성 신호를 상태로 두면 화면이 다시 열릴 때 그 값이 다시 흘러나갑니다.** `LoginViewModel.onAuthenticationHandled()`처럼 소비 후 되돌리는 메서드를 두고, 호출자는 화면을 바꾸기 **전에** 소비 표시를 남기세요. 화면이 바뀌면 `LaunchedEffect`가 취소돼 뒤에 둔 코드가 실행되지 않습니다.
- **로직은 JVM에서 테스트 가능한 위치에 둡니다.** ViewModel·Repository의 로직이 Activity나 Composable 안에 들어가면 `testDebugUnitTest`로 검증할 수 없습니다.
- **ViewModel에 Android `Context`를 넣지 않습니다.** 카카오 SDK처럼 Activity Context를 요구하는 호출은 UI 계층(`LoginRoute` 같은 상태 있는 컴포저블)이 담당하고, ViewModel은 결과만 받습니다. 그래서 `KakaoLoginClient`는 Hilt로 주입하지 않고 컴포저블에서 `remember`로 만듭니다.
- **브랜드가 고정한 색상은 `colorScheme`에 넣지 않습니다.** 카카오 버튼 색(`KakaoContainer` 등)은 `core/designsystem/Color.kt`에 별도 상수로 둡니다. 카카오 디자인 가이드가 변경을 금지하므로 테마나 다크 모드에 따라 바뀌면 안 됩니다.

- **패키지는 기능(도메인) 우선입니다.** 최상위에 도메인 패키지를 두고 그 안에 `ui`와 `data`를 둡니다. 레이어를 최상위에 두지 않습니다.

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
  ├── intake/    ui/ data/    1l 1c 1m 1d 문답
  ├── card/      ui/ data/    1e 브리핑 카드
  ├── handoff/   ui/          1g 진료실 전달
  └── visit/     ui/ data/    1p 1q 사후 기록
  ```

  도메인 이름은 `Medical-Mate/Backend`의 패키지(`auth`, `profile`, `intake`, `card`, `handoff`, `visit`)와 맞췄습니다. 같은 단어가 양쪽에서 같은 것을 가리키게 하려는 목적입니다. `home`은 백엔드에 대응이 없습니다.

- **빈 패키지를 미리 만들지 마세요.** 위 구조는 규칙이지 골격이 아닙니다. 폴더는 해당 기능에 실제로 착수할 때 만듭니다. 한 도메인에 파일이 하나뿐이면 `ui`·`data` 하위 폴더도 만들지 않고 도메인 폴더에 바로 둡니다.

- **한 도메인이 다른 도메인을 직접 참조하지 않습니다.** 공유가 필요하면 `core`로 올립니다. 두 도메인이 같은 타입을 쓰면 그 타입은 `core/model`에 둡니다.

**아직 안 정해진 것**

- 네비게이션 라이브러리, 네트워크 클라이언트, 로컬 저장 방식은 미정입니다.

---

## 4. 코드 스타일

두 도구가 CI에서 강제됩니다. 로컬에서 통과시키고 커밋하세요.

**ktlint** — 설정은 `.editorconfig`에 있습니다.

- Jetpack Compose 관례에 따라 `@Composable` 함수는 PascalCase를 허용합니다(`ktlint_function_naming_ignore_when_annotated_with = Composable`).
- 와일드카드 import는 금지입니다. 자동 수정되지 않으므로 직접 풀어 써야 합니다.
- `*.kts` 빌드 스크립트도 검사 대상입니다.
- `max_line_length = 120`은 detekt `MaxLineLength` 기본값과 맞춘 값입니다. **둘을 다르게 두면 `ktlintFormat`이 줄을 붙이고 detekt가 그 줄을 잡는 무한 왕복이 생깁니다.** 한쪽만 바꾸지 마세요.
- 대부분의 위반은 `./gradlew :app:ktlintFormat`으로 해결됩니다.

**detekt** — `config/detekt/detekt.yml`. `buildUponDefaultConfig = true`이므로 이 파일에는 **기본값과 다른 부분만** 적습니다.

- `MagicNumber`는 `test`, `androidTest`, `core/designsystem`, 그리고 **`ui` 패키지 전체**에서 제외됩니다. 테마 색상 리터럴과 Compose 레이아웃의 `dp`·`sp` 값까지 잡으면 신호 대비 잡음이 너무 큽니다. `data`와 ViewModel의 계산 로직에서는 그대로 살아 있습니다.
- `UnusedPrivateMember`는 `@Preview`를 무시합니다. Preview 컴포저블은 코드가 아니라 IDE·툴링이 호출하므로 미사용이 아닙니다.
- `FunctionNaming`은 `@Composable`을 무시합니다.
- 보고서: `app/build/reports/detekt/detekt.html`, `detekt.sarif`

---

## 5. Git 컨벤션

전체 규칙은 [`GIT_CONVENTION.md`](GIT_CONVENTION.md)에 있습니다. 이 저장소에서 반드시 지킬 것만 요약합니다.

**커밋 메시지**

```text
태그: 내용 #이슈번호
```

- 태그 첫 글자는 대문자, 콜론 뒤에만 공백. 예: `Feat: 복약 알림 등록 화면 추가 #12`
- 태그: `Feat` `Fix` `Refactor` `Design` `Comment` `Style` `Chore` `Test` `Init` `Rename` `Remove` `Docs` `!Hotfix`
- CI 워크플로 변경은 `Chore:`를 씁니다.
- 커밋 하나에 논리적 변경 하나만 담습니다.
- 대화형 bash에서 `!Hotfix:`는 history expansion으로 실패합니다. 작은따옴표를 쓰세요.

**브랜치**

```text
<태그 소문자>/#<이슈번호>-<영문-설명>
```

예: `feat/#12-medication-reminder`, `chore/#3-project-templates`

**PR**

- 제목은 커밋과 같은 형식. 저장소가 **Squash 전용**이라 PR 제목이 그대로 `main`의 커밋 제목이 됩니다.
- **PR 본문도 그대로 커밋 메시지 본문이 됩니다**(`squash_merge_commit_message=PR_BODY`). 머지 전에 본문을 정리하세요.
- 본문에 `Closes #N`을 넣습니다.
- 브랜치는 `main` 하나입니다. `develop`을 만들지 않습니다.
- 머지 후 원격 브랜치는 자동 삭제됩니다.

**브랜치 보호가 없습니다.** Free 플랜 + private 저장소라 GitHub 보호 규칙을 켤 수 없습니다. `main` 직접 push 금지와 리뷰는 강제되지 않는 약속이므로, 머지 전에 CI가 초록인지 사람이 직접 확인해야 합니다.

---

## 6. 하지 말 것

1. **검증 우회.** 실패하면 코드를 고칩니다. 다음은 모두 금지입니다.
   - 통과시키려고 테스트를 수정하거나 삭제
   - `-x test`, `-x lintDebug` 같은 태스크 제외 플래그 사용
   - 근거 없는 `@Suppress`, `ktlint-disable` 추가, detekt baseline 생성
   - CI 워크플로에서 실패하는 검사를 제거

   테스트 변경이 실제로 필요하면 그 사실과 이유를 밝히고 사람의 확인을 받습니다.

2. **PR 병합.** 병합은 사람이 결정합니다.

3. **요청 범위 밖의 변경.** 관련 없는 리팩터링·이름 변경·포맷 수정을 같은 커밋에 넣지 않습니다.

4. **다른 저장소 수정.** `Medical-Mate/Backend`, `AI`, `Design`은 별도 Issue와 PR로 처리합니다.

5. **민감정보 커밋.** `local.properties`, `*.jks`, `*.keystore`, API 키는 `.gitignore`로 막혀 있습니다. 우회하지 마세요.

---

## 7. CI

`.github/workflows/ci.yml`. 공통 셋업은 `.github/actions/setup-android`(JDK 21 + Android SDK + Gradle 캐시)에 있습니다.

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
| 유닛 테스트 | `LoginViewModel` 13건, `SessionViewModel` 10건, `HomeViewModel` 6건, 템플릿 1개 |
| 아키텍처 패턴 | MVVM 확정. UseCase는 필요할 때만 |
| DI | Hilt 확정 |
| 패키지 구조 | 기능 우선 확정. 도메인명은 Backend와 일치 |
| 로그인 화면(1o) | 카카오 버튼만 구현. 서버 토큰 교환 미연동 |
| 홈 화면(1n) | 구조만. `HomeViewModel`이 픽스처를 노출. 서버 미연동 |
| 로그아웃 · 회원탈퇴 | 구현 완료. 홈 화면에 임시 진입점. 설정 화면 생기면 이동 |
| ViewModel 스코프 | 전부 Activity 스코프. 로그아웃 후에도 상태가 남는다. 네비게이션 도입 시 목적지별로 분리 |
| 계정 전환 시 이전 데이터 | `HomeViewModel`이 이전 계정 카드를 한 프레임 보여줄 수 있음. 지금은 픽스처라 무해 |
| 401 재발급 Authenticator | 미도입. 만료된 토큰으로 로그아웃·탈퇴하면 서버 호출이 401 |
| 토큰 암호화 | 미적용. DataStore 평문. 백업 차단으로 샌드박스 밖 유출만 막음 |
| 온보딩 필요 판단 | `refresh` 응답의 `onboardingRequired`는 서버가 항상 false. 프로필 조회로 옮겨야 함 |
| 화면 전환 | `MainActivity`의 임시 `if` 분기. 목적지가 늘면 NavHost로 교체 |
| Apple · 전화번호 로그인 | 백엔드 미지원. `User` 엔티티 식별자가 `kakaoId` 단독 |
| 카카오 말풍선 심볼 에셋 | 없음. 콘솔의 도구 > 리소스 다운로드에서 받아야 함 |
| 네비게이션 / 네트워크 / 로컬 저장 | 미정 |
| 릴리즈 서명 | 없음. `bundleRelease` 산출물은 미서명 |
| 스크린샷 테스트 (Paparazzi/Roborazzi) | 미도입 |
| E2E (Maestro) | 미도입 |
| `scripts/verify.sh` | 미작성. 1장의 Gradle 명령을 직접 사용 |
| org 공통 문서 위치 | `GIT_CONVENTION.md`가 저장소별 사본으로 존재. 어긋나면 `.github` 저장소로 통합 필요 |

`local.properties`는 `.gitignore` 대상입니다. 로컬에서는 Android SDK 경로가 필요하고, CI는 `ANDROID_HOME` 환경 변수를 사용합니다.
