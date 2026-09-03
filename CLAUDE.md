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

규칙:

- **의존성은 반드시 `gradle/libs.versions.toml`(버전 카탈로그)에 추가하고 `libs.*` 별칭으로 참조합니다.** 빌드 스크립트에 좌표를 직접 쓰지 않습니다.
- **`org.jetbrains.kotlin.android` 플러그인을 추가하지 마세요.** AGP 9의 내장 Kotlin 지원을 사용하며, 현재 `plugins` 블록에는 `com.android.application`과 `org.jetbrains.kotlin.plugin.compose`만 있습니다.
- `compileSdk`는 37 이상을 유지합니다. `androidx.core:core-ktx:1.19.0`이 API 37 이상을 요구하며, 낮추면 `checkDebugAarMetadata`에서 빌드가 실패합니다.
- `org.gradle.configuration-cache=true`가 켜져 있습니다. 빌드 스크립트에서 configuration cache와 호환되지 않는 패턴(태스크 실행 시점의 `Project` 접근 등)을 쓰지 않습니다.

---

## 3. 모듈 맵

단일 모듈입니다.

```text
MedicalMate/
├── app/                         유일한 모듈 (com.android.application)
│   └── src/
│       ├── main/java/com/mist/medicalmate/
│       │   ├── MainActivity.kt          진입점 (ComponentActivity + Compose)
│       │   └── ui/theme/                Color.kt, Theme.kt, Type.kt
│       ├── test/                        JVM 유닛 테스트
│       └── androidTest/                 계측 테스트 (CI에서 실행하지 않음)
├── config/detekt/detekt.yml     detekt 기본 설정 위에 얹는 예외
├── .editorconfig                ktlint가 읽는 코드 스타일
├── gradle/libs.versions.toml    의존성 버전 단일 진실 소스
└── .github/workflows/ci.yml     CI
```

- 패키지 루트: `com.mist.medicalmate`
- **아키텍처 레이어는 아직 정해지지 않았습니다.** 현재는 `MainActivity`와 테마뿐입니다. 레이어 구조(예: `ui` / `domain` / `data`)를 도입하는 시점에 이 문서를 함께 갱신합니다.
- 다만 **로직은 JVM에서 테스트 가능한 위치에 둡니다.** ViewModel·UseCase·Repository의 로직이 Activity나 Composable 안에 들어가면 `testDebugUnitTest`로 검증할 수 없습니다.

---

## 4. 코드 스타일

두 도구가 CI에서 강제됩니다. 로컬에서 통과시키고 커밋하세요.

**ktlint** — 설정은 `.editorconfig`에 있습니다.

- Jetpack Compose 관례에 따라 `@Composable` 함수는 PascalCase를 허용합니다(`ktlint_function_naming_ignore_when_annotated_with = Composable`).
- 와일드카드 import는 금지입니다. 자동 수정되지 않으므로 직접 풀어 써야 합니다.
- `*.kts` 빌드 스크립트도 검사 대상입니다.
- 대부분의 위반은 `./gradlew :app:ktlintFormat`으로 해결됩니다.

**detekt** — `config/detekt/detekt.yml`. `buildUponDefaultConfig = true`이므로 이 파일에는 **기본값과 다른 부분만** 적습니다.

- `MagicNumber`는 `test`, `androidTest`, `ui/theme`에서 제외됩니다. 테마 색상 리터럴 때문입니다.
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
| 유닛 테스트 | 템플릿 예제 1개뿐. 실질 커버리지 없음 |
| 아키텍처 레이어 | 미정 |
| 릴리즈 서명 | 없음. `bundleRelease` 산출물은 미서명 |
| 스크린샷 테스트 (Paparazzi/Roborazzi) | 미도입 |
| E2E (Maestro) | 미도입 |
| `scripts/verify.sh` | 미작성. 1장의 Gradle 명령을 직접 사용 |
| org 공통 문서 위치 | `GIT_CONVENTION.md`가 저장소별 사본으로 존재. 어긋나면 `.github` 저장소로 통합 필요 |

`local.properties`는 `.gitignore` 대상입니다. 로컬에서는 Android SDK 경로가 필요하고, CI는 `ANDROID_HOME` 환경 변수를 사용합니다.
