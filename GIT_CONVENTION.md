# Git Convention

백엔드 개발자, AI 엔지니어, 안드로이드 개발자가 하나의 GitHub Organization에서 각 담당 저장소를 운영하며 협업하기 위한 규칙입니다.

이 문서는 3인 소규모 팀이 바로 사용할 수 있도록 꼭 필요한 규칙만 정의합니다. Claude 등 AI 에이전트를 사용해 개발하는 상황을 전제로 하며, 검증 없이 코드가 쌓이는 것을 막는 데 초점을 둡니다.

> **이 사본에 대하여**
>
> 규칙 자체는 Organization 전체에 적용되지만, 이 파일은 `Medical-Mate/Android` 저장소에 있는 사본입니다. 8장은 Android 전용이고, 각 장의 "현황" 표기도 Android 저장소 기준입니다.
>
> `Backend`·`AI` 저장소도 같은 규칙을 따릅니다. 사본이 늘어나 서로 어긋나기 시작하면 org 공통 `.github` 저장소로 옮겨 단일 원본으로 관리합니다.

## 1. 저장소 구성

Organization 아래에 제품 영역별 저장소를 둡니다.

```text
Medical-Mate/
├── Backend    (private, main)
├── Android    (private, main)
├── AI         (private, 비어 있음)
└── Design     (private, 비어 있음)
```

- `Backend`: API, 서버 비즈니스 로직, 데이터베이스, 인증
- `AI`: 모델 호출, 프롬프트, RAG, 임베딩, 평가 및 추론 파이프라인
- `Android`: Android UI, 앱 로직, 로컬 저장소, 모바일 네트워크 연동
- `Design`: 디자인 산출물. 코드 저장소가 아니므로 이 문서의 커밋·PR 규칙은 선택 적용합니다.

`AI`와 `Design`은 아직 초기 커밋이 없습니다. 첫 커밋을 올릴 때 이 문서의 8장 Day 0 항목을 함께 적용합니다.

저장소는 **담당자가 아니라 독립적으로 개발·배포·버전 관리할 수 있는 제품 영역**을 기준으로 분리합니다. 담당자가 바뀌어도 저장소 구조는 유지될 수 있어야 합니다.

API 명세처럼 여러 저장소가 공유하는 파일은 처음에는 주 소유 저장소에서 관리합니다. 독립적인 버전 관리가 실제로 필요해지면 `api-spec` 또는 `shared` 저장소로 분리합니다.

---

## 2. 작업 진행 순서

모든 작업은 아래 순서를 따릅니다. Claude에게 작업을 맡길 때도 동일합니다.

```text
1. Issue 생성        (템플릿 준수)
2. 브랜치 생성        (태그 + 이슈번호)
3. 구현 → 로컬 검증 통과 → 커밋
4. push → PR 생성    (제목·본문 템플릿, Closes #N)
5. CI 확인           (실패 시 로그 확인 → 수정 → 재push, 통과까지 반복)
6. 리뷰 및 병합       (사람이 결정)
```

원칙:

- **Issue 없는 작업을 시작하지 않습니다.** 브랜치명과 커밋 메시지에 이슈번호가 들어가므로 1번을 건너뛰면 2·3번이 성립하지 않습니다.
- **3번의 로컬 검증은 생략할 수 없습니다.** CI가 있는 저장소에서 커밋은 "검증을 통과할 것이라는 주장"입니다. 검증 없이 커밋하면 `CI 실패 → 수정 커밋 → 또 실패`가 반복되며 이력이 지저분해집니다.
- **PR 생성은 작업의 끝이 아닙니다.** 5번까지가 작업자(또는 Claude)의 책임 범위입니다.
- **6번 병합은 항상 사람이 수행합니다.** Claude는 자신이 만든 PR을 병합하지 않습니다.

> **적용 시점**: 이 문서는 채택 시점 이후의 커밋·브랜치·PR에만 적용합니다. 이미 `main`에 올라간 커밋은 형식이 달라도 히스토리를 재작성하지 않습니다.

---

## 3. 브랜치 전략

각 저장소에서 단순한 GitHub Flow를 사용합니다.

### 기본 브랜치

- 각 저장소의 기본 브랜치는 `main`입니다.
- `main`은 항상 빌드·실행 가능한 상태로 유지합니다.
- `main`에 직접 push하지 않고 Pull Request(PR)를 통해 병합합니다.
- **브랜치 보호는 현재 설정할 수 없습니다.** Medical-Mate는 Free 플랜이고 저장소가 private이라 브랜치 보호 규칙과 ruleset이 모두 막혀 있습니다(API가 `403 Upgrade to GitHub Pro`를 반환). 즉 **"main 직접 push 금지"와 "승인 1명 필수"는 강제되지 않는 팀 약속입니다.**

  강제하려면 셋 중 하나가 필요합니다.

  | 방법 | 비용 | 비고 |
  | -- | -- | -- |
  | 저장소를 public으로 전환 | 무료 | 의료 도메인 코드라 현실적으로 어려움 |
  | Team 플랜으로 업그레이드 | 사용자당 유료 | private에서 보호 규칙·ruleset 사용 가능 |
  | 현행 유지 (약속으로만) | 무료 | 지금 상태. 아래 보완책을 함께 씁니다 |

  현행 유지 시 보완책:

  - PR 없이 `main`에 직접 push하지 않기로 명시적으로 합의합니다.
  - 저장소 설정에서 Squash 전용 + 머지 후 브랜치 자동 삭제를 켜 둡니다(`Android`는 적용 완료).
  - CI는 여전히 PR마다 돌므로, **머지 전에 체크가 초록인지 사람이 눈으로 확인**하는 것이 유일한 게이트입니다.

### 작업 브랜치

저장소가 이미 영역별로 분리되어 있으므로 브랜치명에 `backend`, `ai`, `android`를 반복하지 않습니다.

```text
<tag>/#<issue-number>
```

또는 내용을 함께 적습니다.

```text
<tag>/#<issue-number>-<short-description>
```

예시:

```text
feat/#12
feat/#12-google-login
fix/#31-empty-model-response
refactor/#44-network-layer
docs/#7-local-setup
hotfix/#58-payment-timeout
```

- 브랜치의 태그는 **커밋 태그를 소문자로 쓴 것**과 같습니다. `feat`, `fix`, `refactor`, `design`, `comment`, `style`, `chore`, `test`, `init`, `rename`, `remove`, `docs`, `hotfix`. (5장 태그 표와 1:1로 대응합니다.)
- 설명 부분은 영문 소문자와 하이픈(`-`)을 사용합니다.
- 브랜치명의 `#`는 git·bash·PowerShell에서 모두 정상 동작합니다(확인 완료). 다만 **URL에서는 `%23`으로 인코딩**되므로 브랜치 링크를 수동으로 만들 때 주의합니다. `#` 없이 `feat/12-google-login`으로 써도 무방하며, 팀에서 하나로 정해 통일합니다.
- 하나의 브랜치는 **하나의 Issue만** 다룹니다.
- 직군별로 장기간 유지되는 브랜치나 `develop` 브랜치는 두지 않습니다.
- 작업 브랜치는 짧게 유지하고 병합 후 삭제합니다.

저장소와 브랜치의 역할은 다릅니다. 저장소는 제품 영역을 나누고, 작업 브랜치는 해당 저장소 안에서 기능·수정 단위를 안전하게 분리합니다.

---

## 4. 커밋 메시지 규칙

```text
태그: 내용 #이슈번호
```

예시:

```text
Feat: 구글 로그인 기능 구현 #1
Fix: 토큰 갱신 실패 시 무한 재시도 수정 #23
Design: 로그인 화면 버튼 여백 조정 #31
Chore: Paparazzi 의존성 추가 #40
```

### 🚨 주의할 점

- **콜론(`:`) 뒤에만 space가 있음에 유의**
- **태그 첫 글자는 대문자**
- 이슈번호는 `#N` 형태로 문장 끝에 붙입니다.

### 작성 원칙

- 커밋 하나에는 **한 가지 논리적 변경만** 담습니다. CI가 실패했을 때 원인을 추적하기 쉬워집니다.
- 내용은 변경 결과를 짧고 구체적으로 작성합니다. `버그 수정`, `기능 구현` 같은 표현만으로는 무엇이 바뀌었는지 알 수 없습니다.
- 내용은 한글로 작성하는 것을 기본으로 하고, 하나의 PR 안에서는 언어를 통일합니다.
- 끝에 마침표를 붙이지 않습니다.
- 서로 독립적인 변경이라면 커밋을 나눕니다. 한 커밋에 여러 작업을 나열하지 않습니다.

상세한 배경이나 주의점은 빈 줄 뒤의 본문에 작성합니다.

```text
Fix: 중복 회원가입 요청 차단 #42

이미 존재하는 이메일로 가입 요청이 오면 거부하도록 변경.
기존에는 서버에서 409를 반환해도 클라이언트가 성공으로 처리하고 있었음.
```

호환성을 깨는 변경은 본문에 `BREAKING CHANGE:`로 명시하고 PR 본문 영향 범위에도 반드시 적습니다.

### 좋은 예시와 나쁜 예시

```text
# 좋은 예시
Feat: 생체 인증 로그인 추가 #12
Test: 생체 인증 실패 케이스 테스트 추가 #12
Refactor: 토큰 인터셉터 분리 #19
Chore: ktlint gradle 플러그인 설정 #5
Docs: 로컬 개발 환경 문서 갱신 #3

# 나쁜 예시
feat: 구글 로그인 구현 #1          -> 태그 첫 글자가 소문자
Feat : 구글 로그인 구현 #1          -> 콜론 앞에 space
Feat:구글 로그인 구현 #1            -> 콜론 뒤에 space 없음
Feat: 구글 로그인 구현              -> 이슈번호 누락
Fix: 버그 수정 #7                  -> 무엇이 바뀌었는지 알 수 없음
Feat: 로그인 추가하고 API 클라이언트 정리 #12  -> 독립적인 변경이 섞임
Chore: API 추가, 테스트 수정, README 갱신 #12  -> 기능 추가를 Chore로 표현
```

---

## 5. 태그 정의

| Type | Explanation |
| -- | -- |
| `Feat:` | 새로운 기능 추가 |
| `Fix:` | 버그 수정 |
| `Refactor:` | 리팩토링 |
| `Design:` | 사용자 UI 디자인 |
| `Comment:` | 필요한 주석 추가 및 변경 |
| `Style:` | 코드 포맷팅, 컨벤션 지키지 않았을 때 코드 수정 |
| `Chore:` | 빌드 테스크 업데이트, 패키지 매니저 설정할 경우(프로덕션 코드 변경 없음)<br>ex) gradle에 라이브러리 추가, gitignore 수정 |
| `Test:` | 테스트 코드 추가 및 수정 |
| `Init:` | 프로젝트 초기 생성 |
| `Rename:` | 파일 혹은 폴더명 수정하거나 옮기는 경우 |
| `Remove:` | 파일을 삭제하는 작업만 수행하는 경우 |
| `Docs:` | 문서 수정 |
| `!Hotfix:` | 급하게 치명적인 버그를 고쳐야 하는 경우 |

### 태그 선택 기준

- `backend`, `ai`, `android`는 이미 저장소로 구분되므로 태그로 사용하지 않습니다.
- **`Test:`** 는 CI 도입에 맞춰 추가한 태그입니다. 프로덕션 코드 변경 없이 테스트만 추가·수정할 때 사용합니다. 기능 구현과 그에 대한 테스트를 함께 작업했다면 `Feat:` 하나로 묶지 말고 커밋을 나누는 것을 권장합니다.
- **CI 설정 파일 변경(`.github/workflows/`)** 은 `Chore:` 를 사용합니다.
- `Design:` 은 UI의 시각적 변경, `Style:` 은 코드 포맷팅입니다. 혼동하지 않도록 주의합니다.
- 실제 기능 추가를 `Chore:` 로 축소해 표현하지 않습니다.

---

## 6. Issue 규칙

### Issue 템플릿

각 저장소에 `.github/ISSUE_TEMPLATE/issue_template.md`로 추가합니다.

```markdown
---
name: Issue
about: 작업 단위를 등록합니다
title: "[태그] 내용"
labels: ''
assignees: ''
---

### 목표
목표를 작성해주세요

### 체크리스트
- [ ] 
- [ ] 
- [ ] 
- [ ] 
```

제목 예시:

```text
[Feat] 구글 로그인 기능 구현
[Fix] 토큰 갱신 실패 시 무한 재시도
[Chore] CI 워크플로 초기 설정
```

- 제목의 태그는 커밋 태그와 동일한 목록을 사용합니다.
- 체크리스트는 실제로 검증 가능한 단위로 쪼갭니다. 그대로 커밋 단위가 되는 것이 이상적입니다.
- Issue 하나가 브랜치 하나, PR 하나에 대응합니다.

### 라벨 권장안

저장소 자체가 Backend, AI, Android 영역을 구분하므로 `area: backend` 같은 직군 라벨은 기본적으로 사용하지 않습니다.

**종류**

- `type: feature`
- `type: bug`
- `type: refactor`
- `type: docs`
- `type: chore`

**우선순위**

- `priority: high`
- `priority: normal`
- `priority: low`

**필요한 경우에만 사용하는 상태**

- `blocked`
- `help wanted`

저장소 안에 모듈이 많아졌을 때만 `area: login`, `area: network` 같은 모듈 라벨을 추가합니다. 담당자 표시는 라벨 대신 GitHub Assignee를 사용합니다.

---

## 7. Pull Request 규칙

### PR 제목

PR 제목도 커밋과 같은 형식을 사용합니다.

```text
태그: 내용 #이슈번호
```

예시:

```text
Feat: 구글 로그인 기능 구현 #1
Fix: 중복 문서 검색 방지 #23
Refactor: 토큰 갱신 흐름 단순화 #44
```

Squash and merge를 사용하고 저장소 설정이 `squash_merge_commit_title=PR_TITLE`이므로 **PR 제목이 그대로 `main`의 커밋 제목**이 됩니다(뒤에 `(#PR번호)`가 자동으로 붙습니다). 형식을 반드시 확인합니다.

### PR 본문

각 저장소에 다음 템플릿을 `.github/pull_request_template.md`로 추가합니다.

```markdown
## 변경 내용
- 무엇을 변경했는지 작성

## 변경 이유
- 왜 필요한지 작성

## 영향 범위
- 영향받는 모듈, API 또는 다른 저장소를 작성

## 확인 방법
- 실행한 검증 명령과 결과를 작성
- CI가 검증하지 못하는 부분은 수동 확인 절차를 작성

## 관련 작업
- 관련 Issue와 다른 저장소의 PR 링크 작성

## 체크리스트
- [ ] 커밋 전 diff를 직접 확인했다
- [ ] 관련 없는 변경이 포함되지 않았다
- [ ] 로컬 검증(빌드·린트·테스트)을 통과했다
- [ ] CI를 통과했다
- [ ] 검증을 우회하거나 테스트를 임의로 수정하지 않았다
- [ ] 민감정보가 diff에 포함되지 않았다
- [ ] 필요한 문서와 API 명세를 업데이트했다

Closes #이슈번호
```

- PR 하나에는 기능 하나 또는 버그 하나만 포함합니다.
- 리뷰어는 최소 1명으로 합니다.
- `Closes #N`을 본문에 넣어 병합 시 Issue가 자동으로 닫히게 합니다.
- 작성자는 리뷰 반영 후 CI 결과와 최종 diff를 다시 확인합니다.
- 다른 저장소에 영향을 주면 해당 저장소와 변경 내용을 본문에 명시합니다.

### 병합 방식

**Squash and merge만 사용합니다.** `Android` 저장소는 설정으로 강제되어 있고, 나머지 저장소도 동일하게 맞춥니다.

| 설정 | 값 |
| -- | -- |
| `allow_squash_merge` | `true` |
| `allow_merge_commit` | `false` |
| `allow_rebase_merge` | `false` |
| `squash_merge_commit_title` | `PR_TITLE` |
| `squash_merge_commit_message` | `PR_BODY` |
| `delete_branch_on_merge` | `true` |

- 작업 중 생긴 `fix typo`, `review 반영` 같은 중간 커밋은 히스토리에 남지 않습니다.
- `main`에서 커밋 하나가 PR 하나와 대응하므로 변경 추적과 되돌리기가 쉽습니다.
- 여러 독립 변경이 한 PR에 들어갔다면 squash로 숨기지 말고 PR을 나눕니다.
- 머지 후 원격 브랜치는 자동 삭제됩니다. 로컬은 `git checkout main && git pull --ff-only && git remote prune origin`으로 정리합니다.

> **⚠️ `PR_BODY` 설정의 부작용**: PR 본문이 **그대로 커밋 메시지 본문**이 됩니다. 아래 PR 템플릿의 체크리스트와 주석까지 전부 `main`의 커밋 메시지에 박힙니다. 머지 화면에서 본문을 다듬거나, 팀이 원하지 않으면 설정을 `BLANK`(제목만)으로 바꿉니다.
>
> ```bash
> gh api -X PATCH repos/Medical-Mate/<repo> -f squash_merge_commit_message=BLANK
> ```

rebase merge와 merge commit은 비활성화되어 있습니다. 대규모 마이그레이션처럼 개별 커밋 보존이 꼭 필요하면 그때 팀 합의로 설정을 일시 변경합니다.

---

## 8. CI 및 검증 계층 (Android)

AI 에이전트로 개발하면 코드 증가 속도가 사람이 직접 작성할 때보다 빠릅니다. 검증 장치가 없으면 부채도 같은 속도로 쌓입니다. **CI는 기능 개발 이후에 붙이는 것이 아니라 첫 커밋과 함께 존재해야 합니다.**

동시에, 처음부터 모든 계층을 깔면 파이프라인 디버깅에만 시간을 쓰게 됩니다. 계층을 하나씩 추가하고, 각 계층이 실제로 실패를 잡아내는지 확인한 뒤 다음으로 넘어갑니다.

### 검증 계층

| 계층 | 소요 | 실행 시점 | 내용 |
| -- | -- | -- | -- |
| **L0** | 수 초 | 매 편집 | `ktlintCheck`, `detekt`, 컴파일 |
| **L1** | 수십 초 | 매 반복 / 커밋 전 | JVM 단위 테스트 `testDebugUnitTest` |
| **L2** | 1~2분 | 커밋 전 / PR | 스크린샷 테스트 (Paparazzi 또는 Roborazzi) |
| **L3** | 수 분 | nightly / release | Maestro E2E (에뮬레이터) |

계층 설계의 전제:

- **로직을 JVM에서 검증할 수 있는 위치에 둡니다.** ViewModel, UseCase, Repository의 로직이 Activity/Fragment/Composable 안에 들어가 있으면 L1이 성립하지 않습니다. 아키텍처 규칙이 곧 검증 가능성 규칙입니다.
- **스크린샷 테스트가 Android 자동 검증의 핵심 해금 지점입니다.** Paparazzi/Roborazzi는 에뮬레이터 없이 JVM에서 Compose를 렌더링해 PNG diff를 냅니다. UI 오류를 텍스트 신호로 바꿔주므로 에이전트가 스스로 확인할 수 있습니다.
- **에뮬레이터 CI(L3)는 느리고 잘 깨집니다.** PR 필수 검사에서는 제외하고 nightly 또는 release 브랜치에만 겁니다.
- Gradle configuration cache와 build cache를 켜두면 L0/L1 체감 속도가 크게 달라집니다. `Android`는 `gradle.properties`에 `org.gradle.configuration-cache=true`가 이미 켜져 있습니다.
- `--offline`은 **로컬 전용**입니다. CI에서 쓰면 의존성을 받지 못해 실패합니다.

### 단일 진입점

저장소 루트에 `scripts/verify.sh`를 두고 모든 검증을 이 명령으로 통일합니다.

```bash
./scripts/verify.sh        # L0 + L1
./scripts/verify.sh --all  # L0 + L1 + L2
```

> **현황**: `scripts/verify.sh`는 **아직 어느 저장소에도 없습니다.** 만들기 전까지 `Android`에서는 아래 명령을 사용합니다(로컬에서 통과 확인 완료).
>
> ```bash
> ./gradlew --continue :app:ktlintCheck :app:detekt :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
> ```
>
> 자동 포맷이 필요하면 `./gradlew :app:ktlintFormat`을 먼저 돌립니다.

- 실패 시 Gradle 스택트레이스 전체를 그대로 출력하지 않고 **실패한 테스트명·파일·라인만 요약**해 출력합니다. 원본 출력은 AI 에이전트의 컨텍스트를 그대로 소모합니다.
- 로컬과 CI가 동일한 명령을 실행하게 합니다. "로컬에서는 됐는데 CI에서 안 된다"의 대부분은 명령이 다르거나 JDK·SDK 버전이 다른 경우입니다.

### CI 도입 시점

| 시점 | 추가할 것 | `Android` 현황 |
| -- | -- | -- |
| **Day 0** (프로젝트 생성 직후, 기능 코드 전) | `build` + `testDebugUnitTest` 워크플로, Gradle wrapper 검증, JDK 버전 고정, Gradle 캐시 | ✅ 완료 |
| Day 0 | 브랜치 보호 | ❌ **플랜 제약으로 불가** (3장 참고) |
| **첫 화면 완성 직후** | `ktlintCheck`, `detekt` (L0) | ✅ 완료 (앞당겨 적용) |
| **첫 ViewModel/UseCase 작성 후** | 단위 테스트 게이트 (L1) | ⚠️ 워크플로는 있으나 테스트 0개 |
| **화면 3~4개 누적 후** | 스크린샷 테스트 (L2) | 미도입 |
| **핵심 플로우 완성 후** | Maestro E2E, nightly 실행 (L3) | 미도입 |

`Android` 저장소의 Day 0 이행 내역:

- 워크플로 `.github/workflows/ci.yml` — PR에서 정적 분석 · 유닛테스트 · `assembleDebug`, `main` 푸시에서 릴리즈 AAB까지.
- **Gradle wrapper 검증** — `gradle/actions/setup-gradle@v6`의 `validate-wrappers`가 기본값 `true`라 별도 액션 없이 충족됩니다.
- **JDK 고정** — `gradle/gradle-daemon-jvm.properties`의 `toolchainVersion=21`, CI는 `actions/setup-java@v6` Temurin 21.
- **Gradle 캐시** — `gradle/actions/setup-gradle@v6`. `main`에서만 쓰기, PR은 읽기 전용.

- Day 0 워크플로는 테스트가 0개여도 올립니다. 목적은 검증이 아니라 파이프라인이 존재하는 것입니다.
- 린트를 나중에 붙이면 기존 파일 전체가 한꺼번에 에러를 뿜습니다. 파일이 열 개를 넘기기 전에 넣습니다.
- 골든 이미지(L2)는 UI가 흔들리는 초기에 넣으면 갱신 노동만 늘어납니다. 레이아웃이 굳은 뒤가 맞습니다.
- 커버리지 기준은 테스트가 실제로 쌓인 뒤에 겁니다. 그 전에 걸면 형식만 남습니다.

### 저장소에 함께 두어야 할 것

- `gradle/libs.versions.toml` (버전 카탈로그) — 의존성 버전의 단일 진실 소스. Compose는 API 변화가 빨라 에이전트가 옛 API를 자신 있게 사용하는 일이 흔합니다. 버전을 고정하고 항상 이 파일을 참조하게 합니다. ✅
- `.github/workflows/` — CI 워크플로. ✅
- `.editorconfig` — ktlint가 읽는 코드 스타일의 단일 진실 소스. Compose `@Composable` 함수의 PascalCase 예외가 여기 있습니다. ✅
- `config/detekt/detekt.yml` — detekt 기본 설정 위에 얹는 예외만 담습니다. ✅
- `CLAUDE.md` — 모듈 맵, 검증 명령, 아키텍처 규칙(각 레이어에서 사용 금지인 것 포함). ✅
- `.github/pull_request_template.md`, `.github/ISSUE_TEMPLATE/` — 6·7장의 템플릿. ✅
- `GIT_CONVENTION.md` — 이 문서. ✅
- `scripts/verify.sh` — 검증 단일 진입점. ❌ 미작성

---

## 9. Claude 사용 시 지켜야 할 규칙

Claude가 코드를 작성했더라도 최종 책임은 커밋 작성자에게 있습니다.

1. **작업 순서 준수**
   2장의 1~5단계를 순서대로 진행합니다. Issue 없이 브랜치를 만들거나, 검증 없이 커밋하거나, PR 생성 후 CI 확인을 생략하지 않습니다.

2. **검증 후 커밋**
   커밋 전에 `./scripts/verify.sh`(도입 전에는 8장의 Gradle 명령)를 실행하고 통과를 확인합니다. 실행하지 못했다면 PR 본문에 이유와 미확인 범위를 적습니다.

3. **검증 우회 금지**
   이것이 가장 중요한 규칙입니다. 검증이 실패하면 **코드를 고칩니다.** 다음 행위는 모두 금지입니다.
   - 통과시키기 위해 테스트 코드를 수정하거나 삭제
   - 스크린샷 골든 이미지를 확인 없이 갱신(`--record` 등)
   - `-x test`, `-x lintDebug`처럼 검사를 건너뛰는 태스크 제외 플래그 사용
   - 근거 없는 lint 무시 주석(`@Suppress`, `ktlint-disable`) 추가나 detekt baseline 생성
   - CI 워크플로에서 실패하는 검사를 제거

   테스트 파일이나 골든 이미지 변경이 실제로 필요하다면 그 사실과 이유를 명시하고 사람의 확인을 받습니다.

4. **CI 실패 대응**
   PR 생성 후 CI 결과를 확인합니다. 실패하면 로그를 읽고 원인을 수정한 뒤 다시 push하며, 통과할 때까지 반복합니다.

   ```bash
   gh pr checks --watch
   gh run view --log-failed
   ```

5. **병합 금지**
   Claude는 PR을 병합하지 않습니다. 병합 여부는 리뷰 후 사람이 결정합니다.

6. **커밋 전 diff 확인**
   변경된 모든 파일과 diff를 직접 확인합니다. 삭제, 대규모 포맷 변경, 설정 및 의존성 변경을 특히 주의합니다.

7. **관련 없는 변경 금지**
   요청 범위 밖의 리팩터링, 이름 변경, 포맷 수정, 주석 정리를 같은 커밋에 넣지 않습니다. 필요하면 별도 Issue와 커밋으로 분리합니다.

8. **작은 단위 커밋**
   한 번에 큰 작업을 맡겼더라도 논리적 변경 단위로 나눠 커밋합니다. 각 커밋은 가능하면 독립적으로 이해하고 되돌릴 수 있어야 합니다.

9. **자동 생성 메시지 검토**
   Claude가 제안한 커밋 메시지와 PR 설명을 그대로 사용하지 않습니다. 실제 diff와 일치하는지, 태그와 이슈번호 형식이 맞는지 확인합니다.

10. **현재 저장소 범위 준수**
    다른 저장소의 변경이 필요하면 자동으로 함께 수정하지 않습니다. 필요한 작업을 정리한 뒤 해당 저장소에서 별도 Issue와 PR로 처리합니다.

11. **민감정보 확인**
    API 키, 토큰, 개인정보, 로컬 설정 파일(`local.properties`, `google-services.json` 등)이 diff에 포함되지 않았는지 확인합니다.

### Claude 요청 예시

```text
이 Issue와 현재 저장소 범위만 수정해 줘.
관련 없는 리팩터링이나 포맷 변경은 하지 마.

구현 후 ./scripts/verify.sh 를 실행하고, 실패하면 코드를 고쳐서 통과시켜 줘.
테스트나 골든 이미지를 수정해서 통과시키지는 마. 그게 필요하면 먼저 알려 줘.

커밋 메시지는 `태그: 내용 #이슈번호` 형식으로.
PR 생성까지 하고, CI 결과를 확인해서 실패하면 고쳐서 다시 push해 줘.
병합은 하지 말고 내가 확인할 수 있게 남겨 둬.

마지막에 변경 파일, 검증 결과, 남은 위험을 정리해 줘.
```

---

## 10. 여러 저장소가 함께 변경되는 작업

하나의 기능 때문에 Backend, AI, Android 저장소가 함께 바뀌더라도 저장소마다 별도의 Issue, 브랜치, PR을 만듭니다.

```text
backend-repo: Feat: 스트리밍 응답 API 제공 #123
ai-repo:      Feat: 스트리밍 생성 지원 #45
android-repo: Feat: 스트리밍 메시지 렌더링 #67
```

협업 순서:

1. 대표 Issue 하나를 만들거나 GitHub Project에서 작업을 묶습니다.
2. 각 저장소에 필요한 Issue와 PR을 만들고 서로 링크합니다.
3. PR 본문에 의존 관계와 권장 병합 순서를 작성합니다.
4. API 명세 변경을 먼저 합의한 뒤 관련 작업을 진행합니다.
5. 호환되지 않는 변경은 동시에 배포하지 말고 이전 버전과의 호환 기간을 둡니다.

PR 본문에는 다음과 같이 관련 작업을 남깁니다.

```markdown
## 관련 저장소
- API 제공: organization/backend-repo#123
- AI 처리: organization/ai-repo#45
- Android 적용: organization/android-repo#67

## 병합 순서
1. backend-repo
2. ai-repo
3. android-repo
```

---

## 11. 긴급 hotfix 예외

운영 장애나 보안 문제처럼 즉시 조치해야 하는 경우에는 문제가 발생한 저장소의 `main`에서 hotfix 브랜치를 만듭니다.

```text
hotfix/#58-payment-timeout
hotfix/#59-model-fallback
```

- 브랜치에는 `hotfix/`를 사용하고, 커밋 태그는 `!Hotfix:` 를 사용합니다.
- 커밋 또는 PR 제목 예: `!Hotfix: 결제 요청 타임아웃 수정 #58`
- **대화형 bash에서는 `!`가 history expansion으로 해석**되어 `git commit -m "!Hotfix: ..."`가 `event not found`로 실패합니다. 작은따옴표를 쓰거나(`-m '!Hotfix: ...'`) 파일/에디터로 메시지를 작성합니다. PowerShell·GitHub 웹에서는 문제없습니다.
- 긴급 상황에서도 **L0/L1 검증과 diff 확인은 반드시 수행합니다.** 사전 리뷰는 생략할 수 있습니다.
- 리뷰를 생략했다면 병합 후 가능한 한 빨리 사후 리뷰를 받습니다.
- 원인과 재발 방지 조치를 Issue에 기록합니다.
- 긴급 수정에 리팩터링이나 기능 개선을 함께 넣지 않습니다.
- 여러 저장소에 긴급 수정이 필요하면 저장소별 hotfix PR을 만들고 적용 순서를 명시합니다.

---

## 12. 빠른 체크리스트

### 작업 시작 전

- [ ] Issue를 템플릿에 맞게 생성했는가?
- [ ] 브랜치명이 `<tag>/#<issue-number>` 형식인가?
- [ ] 브랜치가 Issue 하나만 다루는가?

### 커밋 전

- [ ] 한 가지 논리적 변경만 포함했는가?
- [ ] 태그 첫 글자가 대문자이고, 콜론 뒤에만 space가 있는가?
- [ ] 이슈번호(`#N`)를 넣었는가?
- [ ] 검증 명령을 실행해 통과했는가? (`verify.sh` 도입 전에는 8장의 Gradle 명령)
- [ ] 검증을 우회하지 않았는가? (테스트 수정, 골든 갱신, lint 무시 없음)
- [ ] 전체 diff를 직접 확인했는가?
- [ ] 관련 없는 변경과 민감정보가 없는가?

### PR 병합 전

- [ ] PR 제목이 `태그: 내용 #이슈번호` 형식인가?
- [ ] 본문에 변경 이유, 영향 범위, 확인 방법이 있는가?
- [ ] `Closes #N`을 넣었는가?
- [ ] CI를 통과했는가? (**보호 규칙이 없으므로 사람이 직접 확인**)
- [ ] 팀원 1명이 승인했는가? (강제되지 않는 약속. 리뷰 없이 머지하지 않기)
- [ ] 다른 저장소에 미치는 영향과 관련 PR을 표시했는가?
- [ ] 최종 diff와 squash 커밋 메시지를 확인했는가? (PR 본문이 그대로 커밋 본문이 됩니다)
