# 진료메이트 디자인 시스템 · 화면 구현 기준

> 상태: **Final v4.0 · 개발 기준 문서**
> 기준일: **2026-10-15** — 라이브러리 게시 직후 Figma Plugin API로 변수·스타일·컴포넌트·화면을 직접 읽어 정리했다. 손으로 옮긴 값이 아니다.
> 기준 Figma 파일: `OWKWtDt8lLQLmltq0ztT5O` (AX 해커톤 - 3차 프로젝트)
> - 디자인 시스템: 페이지 `디자인시스템 · 최종` [`291:636`](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=291-636) — 2026-10-15 게시본
> - 화면: 페이지 `UI / Wireframe / User-Flow / IA` → 섹션 `Wireframe 10-15` [`3596:11376`](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=3596-11376) — 와이어프레임 89장 + 첫 진입 모션 프로토타입 2장
> 대상: 환자용 Android 앱 · 기준 화면 **360×812** · 라이트 단일 테마
> 짝 문서: `진료메이트-PRD.md`(제품·흐름·AI 계약) · `design-system.md`(DS 운영 기록·Figma 보드 링크)

이 문서는 진료메이트 화면을 구현할 때 쓰는 디자인 계약이다. 토큰(색·글자·간격·반경·고도), 아이콘·로고, 게시된 컴포넌트, 그리고 `Wireframe 10-15` 화면의 골격과 패턴을 한곳에 모았다. 문서와 Figma가 다르면 Figma를 따른다. 문구·AI 동작·데이터 필드는 PRD가 정본이다.

진료메이트는 환자가 진료 전에 증상을 정리하고, 진료 후 들은 말을 기록하도록 돕는다. **AI는 진단하지 않고 환자가 말한 내용을 구조화한다.** 핵심 산출물은 환자가 확인하고 수정한 브리핑 카드 한 장이다.

---

## 1. 구현자가 먼저 지킬 규칙

1. 화면 코드에는 **시맨틱 토큰만** 쓴다(`bg/primary` · `fg/default` · `border/subtle`). `primary/500` 같은 원시 팔레트는 시맨틱 토큰을 정의하는 곳에서만 참조한다.
2. 색은 **변수로만** 제공된다. 같은 이름의 Paint Style 40개는 `_legacy/*`로 바뀌어 게시에서 숨겨졌다. 코드 토큰을 두 벌 만들지 않는다.
3. 사이징은 **폭 Fill · 높이 Hug**. 고정은 규격 자체가 의미인 것만: Button 높이 56/48/40 · Icon Button · Avatar · Date Cell · Toggle · Nav Bar · Tab Bar · Search Field · Number Badge.
4. DS 마스터 중 폭 350인 것(Card · KV Row · List Row · Notice 등)은 옛 390 규격의 기본값이다. 화면에서는 **콘텐츠 폭 320에 Fill**로 쓴다.
5. 쓰지 않는 것: 이름이 `.` · `.[구]` · `_`로 시작하는 컴포넌트·스타일, DS 페이지 `Archive · 참고 (앱 미사용)` 섹션 22종, `수정 중` 섹션 3종(미게시).
6. **유색 큰 면은 환자의 말에만**(P5). AI 발화는 면색 없이 "AI" 라벨로 구분한다(D4).
7. **색만으로 상태를 전달하지 않는다**(D11). 숫자·낱말·아이콘·굵기·채움 중 하나 이상을 겹친다.
8. 본문 기본은 `Body/L` 17px. 최소 터치 목표는 `size/touch-min` 48px.
9. AI가 정리한 결과는 항상 **환자 원문과 수정 진입점**을 함께 둔다. 진단·처방처럼 읽히는 문장을 쓰지 않는다.
10. 금지 시각 요소: 십자 · 알약 · 체온계 · 웃는 얼굴 캐릭터. `Icon/pill` · `Icon/heart-pulse`는 존재하지만 쓰지 않는다.

### 1.1 이름 변환

Figma 이름은 슬래시를 유지한다. 코드에서는 플랫폼 관례로 바꾼다.

| Figma | CSS | TypeScript/JSON | Android (Compose) |
|---|---|---|---|
| `bg/primary` | `--mm-bg-primary` | `color.bg.primary` | `MedicalMateTheme.colors.bgPrimary` |
| `fg/default` | `--mm-fg-default` | `color.fg.default` | `MedicalMateTheme.colors.fgDefault` |
| `radius/md` | `--mm-radius-md` | `radius.md` | `MedicalMateRadius.md` |
| `space/16` | `--mm-space-16` | `space[16]` | `MedicalMateSpace.s16` |
| `Body/L` | `.body-l` | `type.bodyL` | `MedicalMateTheme.typography.bodyL` |

색 hex를 컴포넌트 코드에 직접 복사하지 않는다. Figma 이름 ↔ 코드 이름 대응은 토큰 JSON에서 자동 생성하는 것을 권장한다.

---

## 2. 컬러

### 2.1 구조 (게시본 기준)

| 컬렉션 | 모드 | 게시 변수 | 비고 |
|---|---|---:|---|
| `진료메이트 / Palette` | Value | 39 | 원시 색. 화면에서 직접 쓰지 않음. 숨김 4개(`neutral/25` · `amber/500` · `green/500` · `red/500`)는 게시 안 됨 |
| `진료메이트 / Semantic` | Light | 44 | 화면에서 쓰는 유일한 색. 2026-10-15 3개 추가 |
| `진료메이트 / Scale` | Value | 39 | 반경 7 · 간격 12 · 크기 8 · 레이아웃 12 (4장) |
| `진료메이트 / Brand · 외부 규격` | Mode 1 | 0 | `kakao/*` 3개 모두 숨김. 값은 `Social Login Button` 안에만 있음 |

- 다크 모드 값은 없다. 다크 대응은 범위 밖이다.
- 기준 색상은 약 231°의 인디고 블루다. 의료 앱에서 흔한 210~215°의 파랑과 거리를 둔다.
- 위계는 색이 아니라 **형태(채움/테두리/글자)** 가 만든다.

### 2.2 Palette (원시 · 게시 39)

| 계열 | 값 |
|---|---|
| primary | 50 `#F2F4FE` · 100 `#E3E7FC` · 200 `#C7CFF8` · 300 `#A3AFF1` · 400 `#7484E3` · **500 `#5566D2`** · 600 `#3B4FC0` · 700 `#2E3E9E` · 800 `#242F79` · 900 `#1A2154` |
| neutral | 0 `#FFFFFF` · 50 `#F5F6FA` · 100 `#EDEFF5` · 200 `#DEE1EB` · 300 `#C6CAD8` · 400 `#989EB1` · 500 `#7C8397` · 600 `#585F73` · 700 `#3A4053` · 900 `#131722` |
| amber | 50 `#FFF4D6` · 100 `#FFE9B3` · 700 `#8A5A0B` |
| green | 50 `#E4F7ED` · 100 `#C6F0DC` · 700 `#0E7A4A` |
| red | 50 `#FFEDEB` · 100 `#FFD8D4` · 700 `#C4302B` |
| severity | 1 `#FFE3A8` · 2 `#FFC79B` · 3 `#FFA894` · 4 `#F58079` · 5 `#DC5A55` |
| severity tint | 1 `#FFF6E4` · 2 `#FFEFE4` · 3 `#FFE9E3` · 4 `#FDE4E2` · 5 `#F9DEDD` |

### 2.3 Semantic (44) — 화면은 이것만

#### Background (`FRAME_FILL` · `SHAPE_FILL`)

| 토큰 | → Palette | 값 | 쓰는 자리 |
|---|---|---|---|
| `bg/canvas` | neutral/50 | `#F5F6FA` | 회색 화면 바탕 토큰. **10-15 화면에서는 쓰지 않는다**(바탕은 모두 `bg/surface` 또는 `bg/primary`) |
| `bg/surface` | neutral/0 | `#FFFFFF` | 카드·시트 면, 10-15 화면 87장의 바탕 |
| `bg/subtle` | neutral/100 | `#EDEFF5` | 입력 필드 면 · 비활성 면 · 진행 표시 빈 칸 |
| `bg/primary` | primary/500 | `#5566D2` | Primary 버튼 · 선택 채움 · Card / Action 면 · 스플래시·등록 완료 화면 바탕 (흰 글자 4.98:1) |
| `bg/primary-pressed` | primary/600 | `#3B4FC0` | 눌림 (6.85:1) |
| `bg/primary-subtle` | primary/100 | `#E3E7FC` | **환자의 말**(Bubble/Patient) · Callout · 선택 칩 (잉크 14.49:1) |
| `bg/primary-faint` | primary/50 | `#F2F4FE` | 가장 옅은 브랜드 면 · Time Wheel 선택 띠 |
| `bg/primary-strong` | primary/800 | `#242F79` | 진한 정보 칩 면(`Chip` Type=Info). **2026-10-15 추가** |
| `bg/info` | primary/50 | `#F2F4FE` | Notice/Info |
| `bg/success` · `bg/warning` · `bg/danger` | green/amber/red 50 | `#E4F7ED` · `#FFF4D6` · `#FFEDEB` | Notice 톤 배경 |
| `bg/inverse` | neutral/900 | `#131722` | 반전 면 |
| `bg/inverse-soft` | neutral/700 | `#3A4053` | **Tooltip Bubble 면**(10-15 실측) |
| `bg/scrim` | neutral/900 | `#131722` | 모달 딤 — 불투명도 50%로 사용 |
| `bg/on-dark` | — | `#FFFFFF` 20% | 사진·primary 면 위 반투명 면: 배너 아바타 원, Card / Action 아이콘 원. **2026-10-15 추가** |
| `severity/1~5` | severity/1~5 | §2.2 | Severity Readout · Slider 판독 영역 전용 |
| `severity/1~5-tint` | severity tint | §2.2 | Severity Slider 판독 배경 전용 |

#### Foreground (`TEXT_FILL`)

| 토큰 | → Palette | 값 | 쓰는 자리 |
|---|---|---|---|
| `fg/default` | neutral/900 | `#131722` | 본문·주요 아이콘 (17.90:1) |
| `fg/subtle` | neutral/600 | `#585F73` | 2차 글자 · **플레이스홀더** · 캡션 (흰 6.36 · subtle 5.54 · primary-subtle 5.18 · warning 5.81) |
| `fg/muted` | neutral/500 | `#7C8397` | **글자 금지**(3.78). 비활성 아이콘·장식 획 전용 |
| `fg/disabled` | neutral/300 | `#C6CAD8` | 비활성 라벨 |
| `fg/on-primary` · `fg/on-inverse` | neutral/0 | `#FFFFFF` | 채움·반전 면 위 글자 |
| `fg/primary` · `fg/link` · `fg/info` | primary/700 | `#2E3E9E` | 강조 글자 · 링크 · 정보 (9.13 / bg/info 위 8.33) |
| `fg/success` · `fg/warning` · `fg/danger` | green/amber/red 700 | `#0E7A4A` · `#8A5A0B` · `#C4302B` | 상태 글자 (각 톤 배경 위 4.83 · 5.40 · 4.88) |

#### Border (`STROKE_COLOR`)

| 토큰 | → Palette | 값 | 쓰는 자리 |
|---|---|---|---|
| `border/subtle` | neutral/200 | `#DEE1EB` | 가장 옅은 구분선 · Divider |
| `border/default` | neutral/300 | `#C6CAD8` | 기본 경계선 |
| `border/strong` | neutral/500 | `#7C8397` | 비텍스트 3:1이 필요한 경계(3.78) · 미선택 체크박스 |
| `border/focus` | primary/500 | `#5566D2` | 2px 포커스 링 |
| `border/primary` | primary/500 | `#5566D2` | 선택된 컨테이너 |
| `border/on-dark` | — | `#FFFFFF` 32% | 사진 위 반투명 선: 배너 아바타 테두리 · 배너 링크 위 구분선. **2026-10-15 추가** |

### 2.4 Brand · 외부 규격

카카오 공식 규격(`#FEE500` 면 · `#000000` 85% 글자 · `#000000` 로고)은 `Social Login Button` Provider=Kakao 안에 고정돼 있다. 변수는 게시되지 않으므로 코드에서는 별도 브랜드 상수로 두고, 색·크기를 바꾸지 않는다. 주변 안내 문구는 일반 대비 기준을 지킨다.

### 2.5 색 사용 규칙

- 유색 큰 면은 환자의 말과 환자가 고른 것에만. AI 발화에는 면색을 쓰지 않는다.
- severity 색은 **판독 전용**. 고르는 동안 색이 판단을 유도하면 안 된다(Slider 트랙은 고른 뒤 판독 영역에만 tint).
- 상태 안내 기본 조합은 `bg/*` 50 + `fg/*` 700이다.
- 사진 위에는 `bg/on-dark` · `border/on-dark` · `fg/on-primary`(흰색)만 쓰고, 글자·아이콘에는 `Shadow/On Image` 효과를 붙인다(§5).

---

## 3. 타이포그래피 — 16스타일 · Pretendard

Figma 글꼴은 **Pretendard Variable**이다. 헤딩은 자간을 조이고 한글 본문은 0%를 유지한다. `Strong`은 크기를 바꾸지 않고 같은 크기의 SemiBold로 강조한다. 10-15 화면 글자의 97%가 아래 스타일에 연결돼 있다.

| 스타일 | 굵기 | 크기/행간 | 자간 | 용도 | Android `Type.kt` |
|---|---|---:|---:|---|---|
| `Display/M` | Bold | 30/40 | -2% | 스플래시 · 온보딩 히어로 · 홈 배너 제목 · 완료 화면 | `displayM` |
| `Heading/L` | Bold | 24/34 | -2% | 화면 질문 헤드라인(한 화면 한 질문) | `headingL` |
| `Heading/M` | SemiBold | 20/28 | -1.5% | 섹션 제목 · 바텀시트 제목 | `headingM` |
| `Heading/S` | SemiBold | 17/24 | -1% | 카드 제목 · 리스트 그룹 제목 · Time Wheel 선택 줄 | `headingS` |
| `Body/L` | Regular | 17/26 | 0 | **본문 기본** — 대기실에서 읽고 진료실에서 말할 문장 | `bodyL` |
| `Body/L Strong` | SemiBold | 17/26 | 0 | 본문 강조 · 선택지 라벨 | `bodyLStrong` |
| `Body/M` | Regular | 15/24 | 0 | 보조 본문 · 버블 | `bodyM` |
| `Body/M Strong` | SemiBold | 15/24 | 0 | 보조 본문 강조 | `bodyMStrong` |
| `Body/S` | Regular | 13/20 | 0 | 캡션 · 설명 | `bodyS` |
| `Body/S Strong` | SemiBold | 13/20 | 0 | 캡션 강조 · 표 헤더 | `bodySStrong` |
| `Label/L` | SemiBold | 15/20 | 0 | 높이 56 버튼 라벨 | `labelL` |
| `Label/M` | SemiBold | 13/18 | 0 | 작은 버튼 · Chip · Badge · Hint Row | `labelM` |
| `Label/S` | Medium | 11/16 | +2% | 탭 라벨 · 발화 주체(AI 라벨) · 태그 | `labelS` |
| `Label/XS` | Bold | 11/16 | 0 | 정보 칩(`Chip` Info) · Card / Resume 진행 숫자. **2026-10-15 추가** | (코드 미등록) |
| `Numeric/L` | Bold | 24/30 | -1.5% | 브리핑 카드 임상 숫자 · D-day | `numericL` |
| `Numeric/M` | SemiBold | 17/22 | -1% | 표·행 안의 숫자 | `numericM` |

- 한국어 줄바꿈은 낱말 단위(`word-break: keep-all`, Compose `LineBreak.Paragraph`)로 한다.
- OS 글자 확대를 허용한다. 130%까지 레이아웃이 깨지지 않게 높이는 Hug로 늘린다. 고정 높이 컨트롤(Button · Nav Bar · Tab Bar)은 최대 배율에서 별도 QA한다.
- `Card Thumbnail / Long Image` 안의 9.5 · 8.1px 글자는 축소 미리보기라 스타일 없이 둔다. 화면 글자로 쓰지 않는다.

---

## 4. 레이아웃 · 간격

### 4.1 화면 규격 (`Scale` 컬렉션 `layout/*`)

| 토큰 | 값 | 뜻 |
|---|---:|---|
| `layout/screen-w` · `screen-h` | 360 · 812 | 기준 화면. OS 상태바는 포함하지 않는다(화면 y=0이 Nav Bar 위끝) |
| `layout/gutter` | 20 | 좌우 여백 |
| `layout/content-w` | 320 | `screen-w − gutter×2` |
| `layout/nav-h` | 56 | 상단 Nav Bar |
| `layout/tabbar-h` | 79 | 하단 탭 = 여백 8 + 탭 46 + safe 24 (+선 1) |
| `layout/footer-h` | 92 | 하단 고정 CTA = 위 12 + 버튼 56 + safe 24 |
| `layout/footer-pad-top` | 12 | Footer 위 패딩 |
| `layout/safe-bottom` | 24 | Footer·Tab Bar 아래 시각 여백 |
| `layout/content-top` · `content-bottom` | 12 · 16 | Content 기본 위·아래 패딩 |
| `layout/block-gap` | 14 | Content 안 블록 간격 기본값 |

- 상태바·제스처 바 높이는 토큰으로 고정하지 않고 플랫폼 safe-area(insets) API를 쓴다. `layout/safe-bottom` 24는 Figma 안 시각 여백이며, 실제 기기 inset과 **중복 적용하지 않는다**(둘 중 큰 값).
- 기기 폭이 달라지면 거터 20을 유지하고 콘텐츠를 Fill한다. 320을 강제하지 않는다.

### 4.2 화면 골격 (10-15 89장 실측)

모든 화면은 세로 오토 레이아웃이고 바탕은 `bg/surface`(87장) 또는 `bg/primary`(스플래시 · 1b-4 등록 완료).

| 골격 | 구성 (위 → 아래) | 화면 |
|---|---|---|
| 흐름 단계 | Nav Bar 56 · Content · Footer 92 | 신상정보 · 증상 정리 · 브리핑 카드 · 진료 후 · 일정 추가 등 대부분 |
| 탭 루트 | Nav Bar 56 · Content · Tab Bar 79 | 캘린더 1r-1 · 기록 1j-1 · 1j-2 · 브리핑 카드 전체 1j-4 |
| 탭 루트 편집 | Nav Bar 56 · Content · Tab Bar 79 · Footer 92 | 1j-1-D · 1j-4-D 계열 |
| 홈 | Home Banner 400(첫 홈 334) · [Sheet + Tab Bar] — 시트가 배너 아래로 **28 겹침**(프레임 간격 −28) | 1n-1 · 1n-2 · 1n-1-* · 1n-1-M* |
| 상세(하단 없음) | Nav Bar 56 · Content | 1m-B 입력 전 · 1r-2 · 1r-2-A · 1j-3 · 1s-1 |
| 전면 | Content · Safe Bottom 24 | 스플래시 · 1b-4 · 로그인(Content 812) |
| 온보딩 | Top Bar 56(건너뛰기 + Onboarding Progress) · Content · Footer 92 | V2-00 ~ V2-03 |
| 오버레이 | 위 골격 + Overlay Scrim(화면 전체 높이) + Sheet 또는 Dialog | -S · -X · -DC · 1r-4-D/T |
| 떠 있는 요소 | 위 골격 + 절대 위치 요소 | FAB(캘린더) · Mate Card(헤이 메이트) · Tooltip Bubble(-T) |

- **고정**: Nav Bar(위) · Footer · Tab Bar(아래)는 스크롤해도 고정. 그 사이 Content만 스크롤한다. 홈은 탭바만 고정이고 배너와 시트가 함께 스크롤한다.
- **812를 넘는 화면**(스크롤): 홈 1002~1094 · 브리핑 카드 1e-1 1118~1186 · 1j-3-X 1301 · 1j-3-R 949 · 1r-2 계열 840~1170 · 1r-4-C 824~932 · 1s-1 1499 · 1c-D4 838 · 1p-T 836. 화면 높이는 콘텐츠가 정한다.
- 스크롤 끝 여백: 마지막 요소가 Footer/Tab Bar에 가리지 않도록 Content 아래 패딩(16~28)을 **스크롤 영역 안에** 둔다.

### 4.3 Content 패딩 (위/좌우/아래)

| 패딩 | 화면 수 | 화면 |
|---|---:|---|
| 12 / 20 / 16 | 43 | **기본** — 신상정보 · 증상 정리 · 브리핑 카드 · 진료 후 · 일자 상세 등 흐름 단계 대부분 |
| 8 / 20 / 16 | 15 | 첫 요소가 Section Header · Month Head(헤더가 위 여백을 가짐) — 탭 루트 1j-1 · 1j-2 · 1j-4 · 1r-1 계열, 1s-1 · 1s-3 |
| 8 / 20 / 24 | 9 | 일정 추가 1r-4 계열(Footer 120 위 여유) |
| 72 / 20 / 24 | 4 | 온보딩 V2-00~03 — 일러스트 가운데 배치 |
| 16 / 20 / 28 | 3 | 기록 상세 1j-3 계열(타임라인) |
| 16 / 20 / 16 | 2 | 스플래시 · 1b-4 등록 완료 |
| 24 / 20 / 24 | 1 | 로그인 |
| 20 / 20 / 40 | 1 | 1c-5 증상 정리 완료 |

### 4.4 블록 간격 (Content `itemSpacing`)

| 간격 | 화면 수 | 화면 |
|---:|---:|---|
| 14 | 31 | **기본**(`layout/block-gap`) — 증상 문답 · 추천 질문 · 브리핑 카드 · 캘린더 · 일자 상세 |
| 12 | 14 | 목록형 1j-1 · 1j-2 · 1j-4 계열 · 진료 후 기록 1q-1 계열 |
| 0 | 13 | 요소가 자체 여백을 가짐 — 일정 추가 1r-4 계열(명시적 Gap 프레임) · 기록 상세 1j-3 계열 · 로그인(여백 프레임) |
| 20 | 9 | 질문 화면 — 신상정보 1b · 통증 강도 1d · 병원 찾기 1m-B · 1m-12 · 1m |
| 24 | 4 | 온보딩 |
| 10 | 3 | 스플래시 · 1b-4 · 1c-5 (전면·완료) |
| 16 | 3 | 진료 후 메모 1p · 1p-T |
| 18 | 1 | 1s-2 건강 정보 수정 |

### 4.5 하단 고정 Footer

`bg/surface` 불투명 · 효과 없음 · 패딩 위 12 / 좌우 20 / 아래 24 · 버튼은 폭 Fill.

| 높이 | 구성 | 화면 |
|---:|---|---|
| 92 | 버튼 L 1개 | 기본(54장) |
| 120 | Hint Row 18 + 10 + 버튼 L | 1r-4 · 1r-4-S(예약 문자 안내) · 1s-3(알림 예시) |
| 158 | 버튼 L 2개 세로(간격 10) — 위 Primary · 아래 Outline | 1c-5 증상 정리 완료 |
| 148 | 버튼 L 2개 세로, 아래 패딩 12 | 1s-2 건강 정보 수정 |

- Primary는 화면당 하나. 두 버튼을 세로로 쌓을 때 위가 주 행동이다.
- 키보드가 올라온 입력 단계(1c-2 · 1c-D2/D3 등)는 Footer에 버튼이 없다.

### 4.6 Spacing · Radius · Size 스케일

**Spacing** (`space/*`, `WIDTH_HEIGHT` · `GAP`): 2 · 4 · 6 · 8 · 10 · 12 · 14 · 16 · 20 · 24 · 32 · 40

**Radius** (`radius/*`, `CORNER_RADIUS`)

| 토큰 | px | 대표 사용 |
|---|---:|---|
| `radius/xs` | 8 | Badge · Checkbox · 작은 칩 · Card Thumbnail |
| `radius/sm` | 12 | S 버튼 · 인라인 pill · 원문 블록 |
| `radius/md` | 16 | L 버튼 · Notice · 입력 필드 · List Row · Question Row · 메모 카드 · 내보내기 미리보기 |
| `radius/lg` | 20 | Card · Callout · 브리핑/기록 카드 · 캘린더 그리드 · Card / Action · Resume · Schedule |
| `radius/xl` | 24 | Dialog · Mate Card · Voice Input Mate |
| `radius/2xl` | 28 | Bottom Sheet 상단 · 홈 시트 상단 |
| `radius/full` | 999 | Chip · Avatar · Icon Button · Number Badge · Pill · 진행 마커 |

작은 요소일수록 작은 반경. 같은 화면에서 두 단계 이상 건너뛰지 않는다.

**Size** (`size/*`): touch-min 48 · icon sm 18 / md 20 / lg 24 · control sm 40 / md 48 / lg 56 · mic 88

**스케일 밖 값(코드 상수로 유지)**: Button M 반경 14 · Date Cell 반경 13 · Time Wheel 선택 띠 반경 14. 간격 3 · 5 · 13 · 18 등은 개별 화면 실측값이며 토큰화는 보류 상태다. Figma에서 반경 변수 연결은 화면 12% · 간격 1%뿐이라, **값이 스케일과 같으면 해당 토큰으로 구현**한다.

---

## 5. 고도 · 표면

테두리 대신 고도로 위계를 만든다. 그림자는 순수 검정이 아닌 **브랜드 틴트 `#1B255A`** 를 쓴다(사진 위 그림자만 `#131722`). 떠 있는 층은 화면에 둘까지, 3단계를 넘기지 않는다. 선은 목록 구분·선택·포커스에만 쓴다.

| 스타일 | 값 | 10-15에서 쓰는 곳 |
|---|---|---|
| `Elevation/Card` | `0 3 10 0 #1B255A 8%` + `0 1 2 0 #1B255A 5%` | 흰 카드 · Card / Resume · Card / Schedule · 선택 행 |
| `Elevation/Float` | `0 4 14 0 #1B255A 9%` + `0 1 3 0 #1B255A 6%` | 캘린더 FAB · 플로팅 요소 |
| `Elevation/Sheet` | `0 -4 24 0 #1B255A 10%` + `0 0 2 0 #1B255A 5%` | 홈 시트 · Bottom Sheet · Dialog |
| `Elevation/Primary` | `0 8 30 -18 #3B4FC0 28%` | primary 채움 카드 · 큰 버튼(Card / Action). **2026-10-15 추가** |
| `Shadow/On Image · Text` | `0 1 2 0 #131722 50%` + `0 2 16 0 #131722 40%` | Home Banner 사진 위 로고 · 제목 · 설명. **2026-10-15 추가** |
| `Shadow/On Image · Icon` | `0 1 3 0 #131722 50%` | Home Banner 사진 위 알림 아이콘. **2026-10-15 추가** |
| `Surface/Glass` | background blur 24 | Nav Bar · Tab Bar (콘텐츠 위 크롬 전용) |

- Glass는 콘텐츠 위에 떠 있는 크롬에만. 본문 Card · Button · Footer에는 쓰지 않는다. Glass에 테두리를 추가하지 않는다.
- 현재 10-15의 Nav Bar · Tab Bar 면은 `bg/surface` 100%라 blur가 보이지 않는 **불투명 상태**다(§13-2). 구현은 불투명을 기본으로 하고, 반투명 Glass를 쓸 경우 저사양·절전·블러 미지원 환경에 같은 레이아웃·대비의 불투명 폴백을 둔다(Android `RenderEffect.createBlurEffect`).

---

## 6. 아이콘

### 6.1 규격

- 이름 `Icon/{name}` · 24×24 격자 · 라이브 영역 20 · 획 1.75 · 라운드 캡/조인 · 단일 색
- 기본색 `fg/default`. 상태·문맥에 따라 시맨틱 foreground 토큰으로 바꾼다.
- 크기: 기본 24 · 인라인 20 · 좁은 자리 18. **16 이하로 줄이지 않는다.**
- 채움형 4개(`*-filled`)는 탭 활성 상태 전용. 본문·버튼에는 획형을 쓴다. 탭 활성은 색만 바꾸지 않고 채움형으로 전환한다.

### 6.2 게시 목록 (47)

| 분류 | 아이콘 |
|---|---|
| 음성 | `mic` · `mic-listening` · `mic-off` · `waveform` |
| 이동 | `chevron-left` · `chevron-right` · `chevron-up` · `chevron-down` · `arrow-right` · `arrow-up` · `close` · `plus` · `minus` · `more-horizontal` |
| 상태 | `info` · `check` · `check-circle` · `alert-circle` · `alert-triangle` · `spinner` |
| 탭 · 구조 | `home` · `note` · `calendar` · `user` |
| 탭 활성 | `home-filled` · `note-filled` · `calendar-filled` · `user-filled` |
| 임상 | `stethoscope` · `hospital` · `body-point` · (`pill` · `heart-pulse` 사용 금지) |
| 행동 | `edit` · `share` · `export` · `copy` · `trash` · `search` · `bell` · `clock` · `camera` · `chat` · `lock` |
| 빈 상태 | `empty-box` · `search-off` · `wifi-off` |

- `export`(2026-10-15 추가): 브리핑 카드 머리글의 "이미지로 내보내기". `share`는 예약 문자 공유 안내(Hint Row)에 쓴다.
- `mic-listening`은 듣는 중에만. chevron은 화면 이동, `arrow-right`는 다음 단계 진행.
- 라벨 없는 아이콘 버튼은 의미가 자명할 때만 허용하고 접근성 이름을 반드시 붙인다.
- 미게시: `Icon/briefing-cards`(State=Default/Active)는 `수정 중` 섹션의 작업물이다. 10-15 화면에서 쓰지 않는다.

---

## 7. 로고

로고는 맞물린 두 조각으로, 환자의 말과 진료를 잇는 역할을 표현한다.

| 컴포넌트 | 변형 | 규격 | 사용 |
|---|---|---|---|
| `Logo Symbol` | Style=Gradient / Flat / Subtle / Mark / Mono Light | 48×48, 최소 24 | Gradient는 앱 아이콘·스플래시만. UI 기본은 Flat. 공유 시트 그림 안 진료메이트 아이콘 |
| `Logo Lockup` | Style=Flat / Mark / Mono Light | 139×36 (심볼 36 + 워드마크 Bold 22, 간격 10) | 로그인 Flat · 스플래시 Mono Light(bg/primary 위) · 홈 배너 Mono Light 114×30(0.82배) + `Shadow/On Image · Text` |

- 보호 여백은 심볼 높이의 1/4. 비율을 고정하고 찌그러뜨리거나 회전하지 않는다. 가로 조합만 쓴다.
- `Mark` 마스터 안의 옛 파란 gradient(`#1769F7 → #0644C5`)는 새 브랜드 색처럼 재사용하지 않는다.

---

## 8. 컴포넌트

### 8.1 게시 현황 (2026-10-15)

| 묶음 | 수 | 위치 | 상태 |
|---|---:|---|---|
| 기본 컴포넌트 | 34 + `카카오 로고` | DS 페이지 `Design System` 섹션 | 게시 · 10-15에서 사용 |
| 아이콘 | 46 + `Icon/export` | `Design System` · `추가` 섹션 | 게시 |
| 추가 컴포넌트 | 20 | `추가 · 새 컴포넌트 · 시스템 (2026-10-15 게시 완료)` `3610:1849` | 게시 · 10-15에서 사용 |
| Archive | 22 | `Archive · 참고 (앱 미사용)` | 게시돼 있으나 **새 화면에 쓰지 않음**(Bottom Sheet · Bottom CTA Bar · Radio · Voice Input · Source Quote · Toast 원형 등) |
| 수정 중 | 3 | `수정 중` | 미게시 — `Icon/briefing-cards` · `Tab Bar / Briefing`(5탭) · `streamline-color:cards` |

- 속성 이름은 Figma 그대로 코드 enum으로 쓴다(§12.2).
- "10-15 사용"은 10-15 섹션 화면 안 **최상위 인스턴스 수 / 화면 수**다(다른 컴포넌트 안에 중첩된 것은 세지 않음, Z 모션 복제본 2장 포함).

### 8.2 Action

| 컴포넌트 | 속성 | 마스터 | 10-15 사용 | 규칙 |
|---|---|---|---|---|
| `Button` | Type=Primary/Tonal/Outline/Ghost/Danger · Size=L/M/S · State=Default/Pressed/Disabled · Show Leading Icon? · Show Trailing Icon? | L 56(r16) · M 48(r14) · S 40(r12), 좌우 패딩 20, 간격 6 | 65 / 57 | 높이 고정·폭 Fill. 브랜드 채움은 실제 행동에만, **Primary는 화면당 하나**. 라벨 텍스트 속성 없음(문구는 화면마다). Danger L은 편집 모드의 삭제 진입 |
| `Icon Button` | Style=Ghost/Tonal/Solid/Outline · Size=L/M/S | 48/40/32 · 아이콘 24/20/18 · r full | 97 / 32 | 뒤로·닫기·월 이동·공유처럼 의미가 자명할 때만. S도 터치 영역 48 확보. 캘린더 FAB = Solid L + `Elevation/Float` |
| `Chip` | Type=Select/Info · State=Default/Pressed/Selected | Select 40 높이 r20 · Info 24 높이 | 51 / 10 | Select: 짧은 증상·부위 선택. 선택은 tint + 테두리 + 글자 진하게(색만으로 X). **Info**(2026-10-15): 누르지 않는 정보 칩 — `bg/primary-strong` 면 + 흰 `Label/XS` |
| `Checkbox` | State=Unchecked/Checked/Disabled | 상자 24 r8 · 행 전체 hit | 36 / 13 | 편집 모드 다중 선택 · Card Pick · Question Row 안. 미선택 테두리 `border/strong` 1.5 |
| `Toggle` | State=Off/On | 52×32 | 1 / 1 (+ Setting Row 안) | 누르는 즉시 적용되는 설정에만. 라벨은 바깥(Setting Row), 행 전체가 hit |

### 8.3 Input

| 컴포넌트 | 속성 | 마스터 | 10-15 사용 | 규칙 |
|---|---|---|---|---|
| `Text Field` | State=Default/Focus/Filled/Error/Disabled | 필드 높이 56, 래퍼 높이 Hug | 9 / 9 | Default는 테두리 없는 `bg/subtle`. Focus/Filled는 surface + 테두리. Error는 색 + 행동 문구. 본문 Body/L |
| `Text Area` | State=Empty/Filled/Focus · Show Mic? | 최소 120 r16 | 3 / 3 | 진료 후 메모(1p) · 추가 질문. 글자 수 표시(0/300). 플레이스홀더 `fg/subtle` |
| `Search Field` | State=Empty/Filled · Value(글자) | 56 고정 r16 | 3 / 3 | 병원 찾기. 한 줄이 정상이라 56 고정 |
| `Severity Slider` | Level=1~5 · Show NRS? | 320×162 | 1 / 1 | 1d 통증 강도. 5단계 스냅, 드래그와 정지점 탭 모두. 고른 뒤에만 판독 영역에 tint |

### 8.4 Content

| 컴포넌트 | 속성 | 마스터 | 10-15 사용 | 규칙 |
|---|---|---|---|---|
| `Card` | Emphasis=Default/Brand/Quiet | r20 · 패딩 20 · 간격 6 · 최소 116 | 6 / 6 | Default = surface + `Elevation/Card`, Brand = `bg/primary-faint`(1r-2 일정), Quiet = `bg/subtle` |
| `KV Row` | Type=Default/Emphasis/Link/Editing | 행 54(값 2줄이면 80), 키 칸 고정 | 83 / 18 | 의사가 훑어보도록 값 열 정렬. **Emphasis는 카드당 하나**. Editing은 편집 모드 |
| `List Row` | Type=Default/Badge/Plain · Show Summary? · Show Select? | 80(요약 포함 104) r16 | 70 / 31 | 제목 + 메타. 상세가 있을 때만 chevron. 편집 모드에서 Show Select로 체크박스 슬롯 |
| `Section Header` | — | 62(위 24 · 아래 10 포함) | 102 / 51 | 헤더가 위 여백을 가진다 → 첫 요소면 Content 위 패딩 8 |
| `Divider` | — | 1 · `border/subtle` | 42 / 33 | 고밀도 목록·카드 안 구획에만. 상태를 선만으로 전달하지 않음 |
| `Badge` | Tone=Brand/Neutral/Success/Warning/Danger | 높이 26 r8 | 12 / 12 | 읽는 상태·속성. Chip과 구분하려고 pill을 쓰지 않음. 상태 이름을 글자로 |
| `Avatar` | Type=Patient/Doctor | 44 원형 | 3 / 3 | 홈 배너 · 내 정보. 배너 위에서는 `bg/on-dark` 면 + `border/on-dark` 테두리 |
| `Date Cell` | State=Default/Today/Selected/Disabled | 42×42 r13 | 150 / 5 | 7열 캘린더. 오늘 = 옅은 면, 선택 = 채움, 점 = 기록 있음 — 셋을 다른 형태로 구분 |
| `Severity Readout` | Level=1~5 | 높이 32 | 13 / 13 | 판독 전용. 단계 숫자 + 환자 낱말 + NRS 등가. **severity 색은 여기와 Slider 판독 영역에만** |
| `Bubble` | Sender=AI/Patient | 폭 280 · AI 72 · Patient 50 | 21 / 7 | AI = 면 없는 본문 + AI 라벨. Patient = `bg/primary-subtle` 면 + 꼬리, 오른쪽 정렬 |
| `Callout` | Editing? · Show Q4? | r20 · 패딩 16 · 간격 12 | 12 / 12 | 브리핑 카드 "묻고 싶은 것" 전용, 카드 안 최강 블록(D5). 질문 3개 기본, Show Q4로 4번째, Editing으로 편집 |
| `Progress Indicator` | Step=1/2/3/4 | 높이 34 | 12 / 12 | 단계 칸 + "n / 3" 글자. 10-15에서는 **3칸**(신상정보 1~3, 증상 정리 2~3). 채운 칸 수 = 현재 단계(§13-4) |
| `Onboarding Progress` | Step=1/4 · 2/4 · 3/4 · 4/4 | 72×24, 내부 62×6 | 4 / 4 | 온보딩 전용. 현재 20×6 pill `bg/primary`, 나머지 6×6 점 `bg/subtle`, 간격 8. 접근성 이름 "총 4단계 중 n단계", 마커는 탐색 버튼 아님 |

### 8.5 Feedback · Overlay

| 컴포넌트 | 속성 | 마스터 | 10-15 사용 | 규칙 |
|---|---|---|---|---|
| `Notice` | Tone=Info/Success/Warning/Danger | 높이 79~ r16 | 17 / 16 | 화면에 남는 안내. 제목은 톤 글자색, 본문 `fg/subtle`. **알러지 경고 · 휴대폰 알림 꺼짐은 Warning** |
| `Tooltip` | State=Idle/Active | 48×48(ⓘ 터치 영역) | 13 / 13 | 한 번만 필요한 설명은 접어 둔다. AI 정리 캡션 옆 ⓘ |
| `Tooltip Bubble` | Text(글자) | 180×36~52 · `bg/inverse-soft` 면 | 3 / 3 | Tooltip Active 때 절대 위치로 띄움(-T 화면) |
| `Dialog` | — | 320×226 r24 · `Elevation/Sheet` | 5 / 5 | 되돌릴 수 없는 확인에만. 가로 가운데(x 20), 버튼 왼쪽 취소 · 오른쪽 실행(Danger) |
| `Overlay Scrim` | — | 화면 전체 · `bg/scrim` 50% | 11 / 11 | 시트·다이얼로그 뒤. 기본 탭 닫기, 되돌릴 수 없는 선택 중에는 막음 |
| `Empty State` | Type=NoRecord/NoResult/Offline/MicDenied | 높이 286(Offline 310) | 8 / 8 | 사과가 아니라 다음 행동을 제시. Offline은 작성 내용이 남았음을 먼저 알림. MicDenied는 2026-10-15 이름 정리 |

**Bottom Sheet**는 게시 컴포넌트가 없다(원형은 Archive). 10-15 화면의 시트는 아래 규격으로 조립돼 있다.

| 항목 | 값 |
|---|---|
| 면 · 효과 | `bg/surface` · `Elevation/Sheet` · 상단 반경 28(`radius/2xl`), 하단 0 |
| 패딩 · 간격 | 위 12 · 좌우 20 · 아래 24 · 자식 간격 0(명시적 Gap 프레임 4~20) |
| 그래버 | 맨 위 320×4 줄 가운데 36×4 · r full |
| 제목 | `Heading/M` 20/28, 설명 `Body/M` |
| 버튼 | L 56 세로 쌓기, 간격 8 (예: 1e-1-S "진료 일정 잡기" · "아직 안 정했어요") |
| 높이 | 내용 Hug — 1e-1-S 250 · 1r-4-T 312 · 1r-1-S 330 · 1r-4-D 400 · 1r-4-S 434 · 1e-1-X 610 |

시트 위에 시트나 다이얼로그를 다시 띄우지 않는다.

### 8.6 Navigation

| 컴포넌트 | 속성 | 마스터 | 10-15 사용 | 규칙 |
|---|---|---|---|---|
| `Nav Bar` | Leading=Back/Close/None · Trailing=Action/None/Text · Action(글자) | 360×56, 좌우 슬롯 48 | 71 / 71 | 제목 가운데 고정. 오른쪽 주요 행동은 아이콘 대신 **글자**(편집 · 취소 · 확인). Close는 모달성 흐름(일정 추가 · 건강 정보 수정) |
| `Tab Bar` | Active=홈/기록/캘린더 | 79 높이, 화면 폭 Fill | 26 / 26 | **3탭: 캘린더 · 홈 · 기록**(홈 가운데). 1뎁스에서만. 활성 = 채움형 아이콘 + 색 + 라벨 굵기 |

Nav Bar 조합(10-15 실측):

| Leading / Trailing | 화면 |
|---|---|
| Back / None | 흐름 단계(신상정보 · 증상 정리 · 진료 후 메모 · 기록 상세 · 내 정보 · 알림 시간) |
| Back / Text | 편집 가능한 상세(브리핑 카드 · 진료 후 기록 · 일자 상세 · 병원 찾기). 글자는 상태에 따라 편집 → 취소 → 확인 |
| Close / None | 일정 추가 1r-4 계열 · 건강 정보 수정 1s-2 |
| None / None | 탭 루트(캘린더 · 기록 비어 있음) |
| None / Text | 기록 목록 1j-1("편집") |

### 8.7 Brand

| 컴포넌트 | 속성 | 마스터 | 규칙 |
|---|---|---|---|
| `Social Login Button` | Provider=Kakao/Naver/Apple/Google | 56 r16 · 간격 10 | 각 사 공식 규격. 색·크기 임의 변경 금지. 로고는 출시 전 공식 asset으로 교체 |
| `Social Login Stack` | — | 마스터 350×188(3사 세로, 간격 10) | 10-15 로그인에서는 **카카오 단독**(높이 56). 로그인 화면 하단 Actions 안 |
| `카카오 로고` | — | 20×20 | Social Login Button 안 |

### 8.8 홈 (2026-10-15 추가)

| 컴포넌트 | 속성 | 마스터 | 10-15 사용 | 규칙 |
|---|---|---|---|---|
| `Home Banner` | Banner=1 오늘의 한 줄 / 2 예약 문자 / 3 일정 알림 / 4 이미지 저장 / 5 헤이 메이트 · CTA=Link/None | 360×400 (CTA=None 334) | 11 / 11 | 홈 맨 위 사진 배너. 헤더 56(좌우 20): Logo Lockup Mono Light 114×30 · 알림 Icon Button · Avatar. 제목 `Display/M` · 설명 `Body/M` · 링크 `Body/M Strong` · 장 수 `Label/M`, 글자·아이콘에 `Shadow/On Image`. CTA=Link는 하단 Link / On Image, CTA=None은 첫 홈(1n-2). 단계별 사진 포함, 문구는 인스턴스에서 직접 |
| `Link / On Image` | — | 320×48 | Home Banner 안 | 배너 하단 텍스트 링크 + 장 수 "n / 5". 글자·화살표 `neutral/0`(흰색), 위 구분선 `border/on-dark` |
| `Button / Pill Arrow` | — | 178×52 r full | Home Banner 안 | 흰 알약 + 오른쪽 원 화살표. 10-15 와이어프레임에서 단독 사용 없음 |
| `Pager / Glass` | — | 49×28 r full | Home Banner 안 | 사진 위 장 번호 "n / 5" 반투명 알약. 10-15 와이어프레임에서 단독 사용 없음 |
| `Card / Action` | — | 320×90 r20 | 13 / 13 | "증상 정리 시작하기". `bg/primary` 면 + 아이콘 원(`bg/on-dark`) + `Elevation/Primary` |
| `Card / Resume` | — | 320×80 r20 | 9 / 9 | "이어서 하기". 흰 카드 + 진행 원(n/3, `Label/XS`) + 화살표. 멈춘 단계로 복귀 |
| `Card / Schedule` | — | 320×80 r20 | 11 / 11 | "다가오는 일정". 흰 카드 + 날짜 블록 + D-day `Badge` Brand |

### 8.9 헤이 메이트 · 음성 (2026-10-15 추가)

| 컴포넌트 | 속성 | 마스터 | 10-15 사용 | 규칙 |
|---|---|---|---|---|
| `Mate Card` | State=듣는 중 · 가이드 / 말하는 중 / 답 / 화면 이동 | 336 폭 r24 · 높이 253 / 80 / 84 / 80 | 4 / 4 | 음성 비서 떠 있는 카드. **딤 없이** 절대 위치 x 12, 탭바 위 12. Icon Button · Divider · 아이콘(note · edit · calendar · close) 포함 |
| `Voice Input Mate` | State=Listening | 320×188 r24 | 1 / 1 | 진료 후 메모 받아쓰기(1p-T). 누르면 바로 듣기 시작 — 파형 + "듣고 있어요" |
| `Waveform` | Size=L · State=말하는 중 | 72×72 | Voice Input Mate 안 | 채우기 없는 파형(메이트 톤). 움직임 축소 설정에서는 정지 + 상태 글자 유지 |

### 8.10 브리핑 카드 내보내기 (2026-10-15 추가)

| 컴포넌트 | 속성 | 마스터 | 10-15 사용 | 규칙 |
|---|---|---|---|---|
| `Export Preview / Long Image` | — | 320×300 r16 | 1 / 1 | 1e-1-X 시트 미리보기 영역. Card Thumbnail + `Chip` Info "긴 이미지 1장" |
| `Card Thumbnail / Long Image` | — | 160×250 r8 | Export Preview 안 | 긴 이미지 한 장의 모양을 보여 주는 축소 카드 |

### 8.11 온보딩 (2026-10-15 추가)

| 컴포넌트 | 속성 | 마스터 | 10-15 사용 | 규칙 |
|---|---|---|---|---|
| `Onboarding Illustration` | Step=1/4 · 2/4 · 3/4 · 4/4 | 이미지 원래 크기·비율: 326×326 · 342×228 · 340×340 · 307×307 | 4 / 4 | 온보딩 V2-00~03. 화면에서는 일러스트 칸 320×340 가운데에 둔다. 비율을 바꾸지 않는다 |

### 8.12 증상 문답 · 설정 · 일정 (2026-10-15 추가)

| 컴포넌트 | 속성 | 마스터 | 10-15 사용 | 규칙 |
|---|---|---|---|---|
| `Number Badge` | Tone=Primary/Subtle · Number(글자) | 22 원 | Question Row · Step Row 안 | Primary = 질문·단계 번호, Subtle = 빈 칸 번호 |
| `Question Row` | State=Default/Selected/Writing/Add/Focus · Question(글자) | 320×64 r16 | 21 / 5 | 1c-D~D4 추천 질문 고르기. Writing = 직접 적는 중, Add = 빈 칸 "질문 직접 추가", Focus = 빈 칸 누름. 선택은 체크 + 테두리 + 번호(색만으로 X) |
| `Setting Row` | Type=Toggle/Navigation/Value · Title · Description · Value(글자) · Show Value? | 284 폭(카드 패딩 6/18 안), Navigation·Value 최소 56 | 11 / 2 | 설정 카드 안 한 줄. 제목 Body/L, 설명 Body/S `fg/subtle`, 값 Body/M `fg/subtle`. 1s-1 · 1s-3 |
| `Time Wheel` | — | 284×240 · 5줄 | 1 / 1 | 오전/오후 · 시 · 분 휠. 선택 띠 `bg/primary-faint` r14, 선택 줄 `Heading/S`. 일정 추가 시간 휠과 같은 띠·글자 |
| `Hint Row` | Icon(교체) · Label(글자) | 320×18 | 3 / 3 | 하단 버튼 위 한 줄 안내. 아이콘 18 + `Label/M` `fg/primary`, 가운데. 1r-4(share) · 1s-3(bell) |
| `Step Row` | Step(글자) | 320×24 | 4 / 1 | 번호 + 한 줄 안내. 1r-4-S 예약 문자 방법 4단계 |
| `Illustration / Share Sheet` | — | 320×160 r16 | 1 / 1 | 문자 앱 공유 시트 그림. 받은 예약 문자 말풍선 + 공유 대상 4칸(진료메이트 로고만 실제, 나머지 회색 스켈레톤) |

### 8.13 통증 5단계 (Severity) — 단일 정의

Slider · Readout(코드의 Scale · Select 포함)이 같은 정의를 쓴다(Android `MedicalMateSeverity.kt`).

| 단계 | 라벨 | 서술 | NRS 등가 | base | tint |
|---|---|---|---|---|---|
| 1 | 조금 불편해요 | 신경 쓰이지만 하던 일은 계속할 수 있어요 | 1–2 | `severity/1` | `severity/1-tint` |
| 2 | 은근히 아파요 | 자꾸 생각나고 집중이 잘 안 돼요 | 3–4 | `severity/2` | `severity/2-tint` |
| 3 | 꽤 아파요 | 하던 일을 멈추게 될 때가 있어요 | 5–6 | `severity/3` | `severity/3-tint` |
| 4 | 많이 아파요 | 일상생활이 어렵고 참기 힘들어요 | 7–8 | `severity/4` | `severity/4-tint` |
| 5 | 견디기 힘들어요 | 잠도 못 자고 아무것도 못 하겠어요 | 9–10 | `severity/5` | `severity/5-tint` |

- 5단계 모두 잉크 글자 4.80:1 이상. 색은 순서를 보조할 뿐, 단계 숫자·낱말·상황 설명을 항상 함께 보인다.
- NRS 등가는 Readout 마스터 값이며 **임상 검증 전**이다. AI 계약상 값은 1~5 서열척도다.

---

## 9. 화면 — Wireframe 10-15

### 9.1 흐름

```
A 진입: 스플래시 → 로그인(카카오) → 온보딩 4장
 → B 신상정보: 복용약 → 기저질환 → 알러지(1/3~3/3) → 등록 완료
 → C 홈(배너 1~5 · 이어서 하기 · 최근 브리핑 카드 · 다가오는 일정)
    ├ D 증상 정리: [부위 짚기 1/3 — 앱의 3D 인체도, Figma 와이어프레임 없음]
    │   → 통증 강도(2/3) → AI 증상 문답(3/3) → 추천 질문 고르기 → 완료 → (병원 찾기)
    │   → E 브리핑 카드 → 일정 잡기 시트 · 이미지로 내보내기 · 편집 · 질문 추가
    ├ F 진료 후: 진료한 병원 확인 → 메모(직접·받아쓰기) → 자동 분류 결과 → 저장
    ├ G 캘린더: 월 → 일자 상세 → 일정 추가(예약 문자 공유 · 날짜 · 시간 · 할 일)
    ├ H 기록: 목록 → 기록 상세(타임라인 · 재방문 누적)
    ├ I 내 정보: 알림 설정 · 건강 정보 수정 · 알림 시간 (홈 헤더 아바타로 진입)
    └ J 헤이 메이트: 듣는 중 → 말하는 중 → 답 → 화면 이동
```

화면 연결 (10-15 섹션의 "흐름 ·" 메모 기준):

| 출발 | 행동 | 도착 |
|---|---|---|
| 홈 | "증상 정리 시작하기"(Card / Action) | D · 1d 통증 강도 |
| 홈 배너 1 | "진료 후 기록하기" | F · 1m-12 → 1q-1-DC 진료 후 기록 전체 |
| 홈 배너 1 | "카드 다시 보기" | E · 1e-1 진료 전 |
| 홈 배너 1 | "진료 일정 잡기" | G · 1r-4 일정 추가 |
| 홈 배너 2 | "방법 보기" | G · 1r-4-S 일정 추가 + 방법 안내 시트 |
| 홈 배너 3 | "일정 추가하기" | G · 1r-4 일정 추가 |
| 홈 배너 4 | "카드 저장하기" | E · 1e-1-X 이미지로 내보내기 시트 |
| 홈 배너 5 | "메이트에게 말해보기" | 1n-1-M — 지금 홈 위로 Mate Card |
| 브리핑 카드 최초 생성 | "저장하기" | 1e-1-S 일정 시트 |
| 1e-1-S 시트 | "진료 일정 잡기" / "아직 안 정했어요" | G · 1r-4 일정 추가(카드 담김) / 시트 닫힘 |
| 브리핑 카드 머리글 | 공유(`Icon/export`) | 1e-1-X 이미지로 내보내기 |
| 브리핑 카드 · 진료 전 | "진료 일정 보기" | G · 1r-2 일자 상세 |
| 브리핑 카드 · 진료 지남 | "진료 후 기록하기" | F · 1m-12 → 1p |
| 1n-1-M 듣는 중 | "다음 진료 언제야"를 받아 적음 | 1n-1-M2 말하는 중 |
| 1n-1-M2 | 내 일정에서 찾아 답함 | 1n-1-M3 답 → 화면 이동 |
| 1n-1-M3 | 캘린더로 옮김 | 1r-1-M4. 날짜를 말하면 그날 1r-2 일자 상세를 엶 |

### 9.2 공통 패턴

| 패턴 | 규칙 |
|---|---|
| 화면 이름 | `ID · 화면 · 상태`. 접미사: `-T` 툴팁 열림 · `-E` 편집 · `-E2` 편집 · 변경 있음 · `-DC` 삭제 확인 · `-D` 목록 편집(선택) · `-S` 시트 · `-X` 내보내기/펼침 · `-A` 추가 · `-M` 메이트 |
| 편집 모드 | Nav 오른쪽 글자: 보기 "편집" → 편집 진입 "취소" → 변경이 생기면 "확인". Footer는 Danger L "○○ 삭제". 목록은 행에 체크박스(Show Select), 버튼 문구에 선택 수("2건 삭제" · "2장 삭제") |
| 삭제 확인 | Overlay Scrim + Dialog(320×226). 되돌릴 수 없음을 알리고 취소(왼쪽) · 삭제(오른쪽 Danger) |
| 툴팁 | AI 정리 캡션 "AI가 … 정리했어요/나눴어요" 옆 ⓘ(`Tooltip` Idle). 누르면 `Tooltip Bubble`이 절대 위치로 뜬다 |
| AI 표시 | AI가 만든 결과 아래에 캡션 + ⓘ, 환자 원문(메모·발화)을 같은 화면에 둔다 |
| 진행 표시 | 신상정보 "내 정보 등록 n / 3", 증상 정리 "증상 정리 2 / 3 · 3 / 3"(1단계 부위 짚기는 앱 3D 화면) |
| 빈 상태 | `Empty State` NoRecord — 첫 홈 · 기록 비어 있음 · 병원 검색 결과 없음 · 일자 상세 진료 후 기록 없음 |
| 더미 데이터 | 김OO · OO세 여 · 복부 통증 · 3주 · 서울OO병원 내과 · 2026년 9월(09.12 · 09.18) |

### 9.3 화면 목록 (89장)

크기는 폭×높이. 하단 열의 버튼은 Type · Size: 라벨.

#### A · 진입 — 스플래시 → 로그인 → 온보딩 (6)

| 화면 | 크기 | 상단 | 하단 | 내용 |
|---|---|---|---|---|
| V2 · 스플래시 | 360×812 | — | Safe Bottom 24 | `bg/primary` 바탕, Logo Lockup Mono Light + 한 줄 "진료실에서 하고 싶은 말, 미리 정리해요" |
| V2 · 로그인 | 360×812 | — | — | 위아래 여백 234 사이 Hero(Logo Lockup Flat + 제목 + 설명, 간격 20) · Actions(Social Login Stack 카카오 + 약관) |
| V2-00 · 안심하고 시작해요 | 360×812 | Top Bar(건너뛰기 · 1/4) | Primary L: 다음 | Content 72/20/24, 간격 24: Copy(제목 + 설명) · 일러스트 칸 320×340 |
| V2-01 · 짚어요 | 360×812 | 〃 2/4 | Primary L: 다음 | 〃 |
| V2-02 · 정리해요 | 360×812 | 〃 3/4 | Primary L: 다음 | 〃 |
| V2-03 · 이어가요 | 360×812 | 〃 4/4 | Primary L: 시작하기 | 〃 |

#### B · 신상정보 — 복용약 → 기저질환 → 알러지 (4, 등록 완료 1b-4는 C 섹션)

| 화면 | 크기 | 상단 | 하단 | 내용 |
|---|---|---|---|---|
| 1b-1 · 복용 중인 약 | 360×812 | Back · "내 정보 등록" | Primary L: 다음 | Progress 1 / 3 · 질문 "지금 드시는 약이 있나요?" |
| 1b-2 · 기저질환 | 360×812 | 〃 | Primary L: 다음 | Progress 2 / 3 · "진단받은 병이 있나요?" |
| 1b-3 · 알러지 | 360×812 | 〃 | Primary L: 완료 | Progress 3 / 3 · "알러지가 있나요?" |
| 1b-3-T · 알러지 · 툴팁 열림 | 360×812 | 〃 | Primary L: 완료 | Tooltip Bubble 열림 |

#### C · 홈 — 허브 · 배너 1~5 · 브리핑 카드 전체 (14)

| 화면 | 크기 | 상단 | 하단 | 내용 |
|---|---|---|---|---|
| 1b-4 · 신상정보 완료 | 360×812 | — | Safe Bottom 24 | `bg/primary` 바탕 "내 정보가 등록되었어요" |
| 1n-2 · 홈 · 처음 (기록 없음) | 360×812 | Home Banner 1 · CTA=None(334) | Tab 홈 | 시트: Card / Action · Empty State NoRecord |
| 1n-1 · 홈 · 기록 있음 · 배너 1 진료 후 | 360×1002 | Home Banner 1 · Link | Tab 홈 | 시트: Card / Action · 최근 브리핑 카드 · 다가오는 일정 |
| 1n-1-4 · 홈 · 내일 진료 · 배너 1 | 360×1094 | 〃 | Tab 홈 | 시트: Card / Action · **Card / Resume** · 최근 브리핑 카드 · 다가오는 일정 |
| 1n-1-5 · 홈 · 카드 저장 · 일정 없음 · 배너 1 | 360×1094 | 〃 | Tab 홈 | 〃 |
| 1n-1-B2 · 홈 · 배너 2 예약 문자 | 360×1094 | Home Banner 2 | Tab 홈 | 〃 |
| 1n-1-B3 · 홈 · 배너 3 일정 알림 | 360×1094 | Home Banner 3 | Tab 홈 | 〃 |
| 1n-1-B4 · 홈 · 배너 4 카드 이미지 저장 | 360×1094 | Home Banner 4 | Tab 홈 | 〃 |
| 1n-1-B5 · 홈 · 배너 5 헤이 메이트 | 360×1094 | Home Banner 5 | Tab 홈 | 〃 |
| 1n-1-M · 홈 · 메이트 듣는 중 (배너 5에서) | 360×1094 | Home Banner 5 | Tab 홈 | + Mate Card 듣는 중 · 가이드(253) |
| 1j-4 · 브리핑 카드 전체 | 360×812 | Back · "브리핑 카드" · 편집 | Tab 기록 | 월별 Section Header + 카드 목록 |
| 1j-4-D · 브리핑 카드 · 편집 · 선택 없음 | 360×812 | 〃 · 취소 | Danger L: 삭제 · Tab | 체크박스 행 |
| 1j-4-D2 · 브리핑 카드 · 편집 · 2장 선택 | 360×812 | 〃 · 취소 | Danger L: 2장 삭제 · Tab | |
| 1j-4-DC · 브리핑 카드 · 삭제 확인 | 360×812 | 〃 · 취소 | Danger L: 2장 삭제 · Tab | + Scrim · Dialog |

#### D · 증상 정리 — 아픈 정도(2/3) → AI 문답(3/3) → 추천 질문 → 완료 → 병원 찾기 (11)

| 화면 | 크기 | 상단 | 하단 | 내용 |
|---|---|---|---|---|
| 1d · 통증 강도 (2/3) | 360×812 | Back · "증상 정리" | Primary L: 다음 | 간격 20: Progress 2 / 3 · 질문 "복부가 얼마나 아프세요?"(Heading/L) + 설명 · Severity Slider · Notice Info |
| 1c-2 · 증상 문답 · 직접 입력 (3/3) | 360×812 | 〃 | 입력 영역(버튼 없음) | Progress 3 / 3 · "짚은 부위" + Chip Selected · AI/Patient Bubble 교차(간격 14) |
| 1c-3 · 증상 문답 · 음성 대기 (3/3) | 360×812 | 〃 | 음성 입력 영역 | 〃 |
| 1c-D · 추천 질문 · 고르기 전 (3/3) | 360×812 | 〃 | Primary L: 다음 | AI Bubble · Questions(Question Row, 간격 8) |
| 1c-D1 · 추천 질문 · 2개 고름 (3/3) | 360×812 | 〃 | Primary L: 다음 | Selected 2 · Default · Add |
| 1c-D2 · 추천 질문 · 빈 칸 눌러 추가 (3/3) | 360×812 | 〃 | 키보드 | Question Row Focus |
| 1c-D3 · 추천 질문 · 직접 입력 중 (3/3) | 360×812 | 〃 | 키보드 | Question Row Writing |
| 1c-D4 · 추천 질문 · 3개 골라 마무리 (3/3) | 360×838 | 〃 | Primary L: 다음 | Selected 3 |
| 1c-5 · 증상 정리 완료 | 360×812 | 〃 | Footer 158: Primary L 브리핑 카드 만들기 · Outline L 진료받을 병원 먼저 찾기 | "증상 정리가 끝났어요" |
| 1m-B · 병원 찾기 · 진료 전 (입력 전) | 360×812 | Back · "병원 찾기" | — | 질문 + Search Field + Empty State |
| 1m-B · 병원 찾기 · 진료 전 | 360×812 | 〃 | Primary L: 브리핑 카드 만들기 | 검색 결과 선택 |

#### E · 브리핑 카드 → 일정 · 내보내기 · 진료실 전달 (12)

| 화면 | 크기 | 상단 | 하단 | 내용 |
|---|---|---|---|---|
| 1e-1 · 브리핑 카드 · 일정 없음 (진료 일정 잡기) | 360×1121 | Back · "브리핑 카드" · 편집 | Primary L: 저장하기 | 최초 생성 직후. 저장하기 → 1e-1-S 시트 |
| 1e-1 · 브리핑 카드 · 일정 없음 (진료 일정 잡기) | 360×1121 | 〃 | Primary L: 진료 일정 잡기 | 저장 후 일정 없음 |
| 1e-1-S · 브리핑 카드 · 저장 직후 일정 시트 | 360×1121 | 〃 | Primary L: 진료 일정 잡기 | + Scrim · 시트 250 "진료 날짜가 정해졌나요?" + 버튼 2 |
| 1e-1 · 브리핑 카드 · 진료 전 (진료 일정 보기) | 360×1121 | 〃 | Outline L: 진료 일정 보기 | |
| 1e-1 · 브리핑 카드 · 진료 지남 (진료 후 기록하기) | 360×1121 | 〃 | Tonal L: 진료 후 기록하기 | |
| 1e-1-X · 브리핑 카드 · 이미지로 내보내기 시트 | 360×812 | 〃 | — | + Scrim · 시트 610(Export Preview) |
| 1e-1-T · 브리핑 카드 · 툴팁 열림 | 360×1121 | 〃 | Primary L: 진료 일정 잡기 | Tooltip Bubble |
| 1e-1-E · 브리핑 카드 · 편집 | 360×1122 | 〃 · 취소 | Danger L: 브리핑 카드 삭제 | KV Row Editing · Callout Editing |
| 1e-1-E2 · 브리핑 카드 · 편집 · 변경 있음 | 360×1148 | 〃 · 확인 | 〃 | |
| 1e-1-A · 브리핑 카드 · 질문 추가 | 360×1186 | 〃 · 취소 | 〃 | Callout Show Q4 |
| 1e-1-A2 · 브리핑 카드 · 질문 적는 중 | 360×1182 | 〃 · 확인 | 〃 | |
| 1e-1-DC · 브리핑 카드 · 삭제 확인 | 360×1118 | 〃 · 확인 | 〃 | + Scrim · Dialog |

#### F · 진료 후 — 병원 선택 → 메모 → 기록 → 정리 (10)

| 화면 | 크기 | 상단 | 하단 | 내용 |
|---|---|---|---|---|
| 1m-12 · 진료한 병원 선택 | 360×812 | Back · "진료 후 기록" | Primary L: 이 병원이 맞아요 | "진료 받은 병원이 맞나요?" |
| 1m · 진료한 병원 선택 | 360×812 | Back · "병원 찾기" | Primary L: 완료 | "진료받은 병원을 찾아주세요" |
| 1p · 진료 후 메모 (2장: 짧은 메모 · 긴 메모, 둘 다 입력 중) | 360×812 | Back · "진료 후 기록" | Primary L: 저장하기 | 간격 16: 제목 + 병원·일자 카드(r16) · Text Area · "AI로 정리하기" Chip + ⓘ |
| 1p-T · 진료 후 메모 · 듣는 중 | 360×836 | 〃 | Primary L: 저장하기 | + Voice Input Mate Listening |
| 1q-1 · 자동 분류 결과 | 360×812 | Back · "진료 후 기록" · 편집 | Primary L: 저장하기 | Record Card(§9.4) |
| 1q-1-T · 진료 후 기록 · 툴팁 열림 | 360×812 | 〃 | 〃 | Tooltip Bubble |
| 1q-1-E · 진료 후 기록 · 편집 | 360×812 | 〃 · 취소 | Danger L: 진료 후 기록 삭제 | |
| 1q-1-E2 · 진료 후 기록 · 편집 · 변경 있음 | 360×812 | 〃 · 확인 | 〃 | |
| 1q-1-DC · 진료 후 기록 · 삭제 확인 | 360×812 | 〃 · 확인 | 〃 | + Scrim · Dialog |

#### G · 캘린더 · 일정 추가 (18)

| 화면 | 크기 | 상단 | 하단 | 내용 |
|---|---|---|---|---|
| 1r-1 · 캘린더 · 월 | 360×812 | "캘린더" | Tab 캘린더 · FAB | 월 이동 · 캘린더 그리드 · 그날 목록 |
| 1r-1 · 캘린더 · 일정 없을 때 | 360×812 | 〃 | 〃 | |
| 1r-1-S · 캘린더 · 카드만 있는 날 시트 | 360×812 | 〃 | 〃 | + Scrim · 시트 330 |
| 1r-2 · 캘린더 · 일자 상세 페이지 | 360×1078 | Back · "9월 12일 (금)" · 편집 | — | 이 날 일정(Card Brand) · 브리핑 카드(List Row) · 할 일 + Text Field · 진료 후 기록(Empty State) |
| 1r-2-E · 캘린더 · 일자 · 편집 | 360×1170 | 〃 · 취소 | Danger L: 일정 삭제 | |
| 1r-2-E2 · 캘린더 · 일자 · 편집 · 변경 있음 | 360×1116 | 〃 · 확인 | 〃 | |
| 1r-2-DC · 캘린더 · 일자 · 일정 삭제 확인 | 360×1116 | 〃 · 확인 | 〃 | + Scrim · Dialog |
| 1r-2-A · 캘린더 · 일자 · 진료 완료 | 360×912 | Back · "9월 18일 (금)" · 확인 | — | |
| 1r-2-A2 · 캘린더 · 일자 · 다음 일정 확정 | 360×840 | Back · "9월 12일 (금)" · 확인 | — | |
| 1r-4 · 일정 추가 · 병원 미등록 (예약 문자 안내) | 360×812 | Close · "일정 추가" | Footer 120: Hint Row(share) · Primary L 저장하기 | 병원 · 날짜·시간 필드 · 가져갈 브리핑 카드 · 할 일 |
| 1r-4-S · 일정 추가 · 예약 문자 방법 안내 시트 | 360×812 | 〃 | 〃 | + Scrim · 시트 434(Share Sheet 그림 + Step Row ×4) |
| 1r-4-B · 일정 추가 · 병원 자동 | 360×812 | 〃 | Primary L: 저장하기 | 예약 문자로 채워진 상태 |
| 1r-4-D · 일정 추가 · 날짜 선택 | 360×812 | 〃 | 〃 | + Scrim · 시트 400(달력) |
| 1r-4-T · 일정 추가 · 시간 선택 | 360×812 | 〃 | 〃 | + Scrim · 시트 312(시간 휠) |
| 1r-4-C · 할 일 목록 | 360×824 | Back · "일정 추가" | Primary L: 저장하기 | |
| 1r-4-C · 할 일 추가 눌렀을 때 | 360×876 | 〃 | 〃 | |
| 1r-4-C · 할 일 추가로 적는 중 (2장) | 360×876 · 932 | 〃 | 〃 | 입력 길이가 다른 두 장 |

#### H · 기록 (8)

| 화면 | 크기 | 상단 | 하단 | 내용 |
|---|---|---|---|---|
| 1j-1 · 기록 · 목록 | 360×812 | "기록" · 편집 | Tab 기록 | 간격 12: 월 Section Header + List Row Badge(요약 포함 104) |
| 1j-1-D · 기록 · 편집 · 선택 없음 | 360×812 | Back · 취소 | Danger L: 삭제 · Tab | |
| 1j-1-D2 · 기록 · 편집 · 2건 선택 | 360×812 | 〃 | Danger L: 2건 삭제 · Tab | |
| 1j-1-DC · 기록 · 삭제 확인 | 360×812 | 〃 | 〃 | + Scrim · Dialog |
| 1j-2 · 기록 · 비어 있음 | 360×812 | "기록" | Tab 기록 | Empty State "아직 진료 기록이 없어요" |
| 1j-3 · 기록 상세 | 360×820 | Back · "기록 상세" | — | Content 16/20/28, 간격 0: Head("서울OO병원 내과 · 09.12 진료") · Timeline(단계 간격 16, 세로선 2) |
| 1j-3-X · 기록 상세 · 카드 펼침 | 360×1301 | 〃 | — | 브리핑 카드 펼침 |
| 1j-3-R · 기록 상세 · 재방문 누적 | 360×949 | 〃 | — | 재방문 기록 쌓임 |

#### I · 내 정보 · 알림 (3)

| 화면 | 크기 | 상단 | 하단 | 내용 |
|---|---|---|---|---|
| 1s-1 · 내 정보 · 알림 설정 | 360×1499 | Back · "내 정보" | — | Profile(패딩 18, Avatar + 이름) · 건강 정보 카드 · 설정 카드(Setting Row) · Notice Warning "휴대폰 알림이 꺼져 있어요" · 설정 카드 ×3 |
| 1s-2 · 건강 정보 수정 | 360×812 | Close · "건강 정보 수정" | Footer 148: Primary L 저장하기 · Outline L 취소 | |
| 1s-3 · 알림 시간 | 360×812 | Back · "알림 시간" | Footer 120: Hint Row(bell) · Primary L 확인 | Section Header · 카드(Setting Row Value ×2) · Section Header · Wheel 카드(Time Wheel) |

#### J · 헤이 메이트 — 듣는 중 → 말하는 중 → 답 → 화면 이동 (3, 듣는 중 1n-1-M은 C 섹션)

| 화면 | 크기 | 상단 | 하단 | 내용 |
|---|---|---|---|---|
| 1n-1-M2 · 홈 · 메이트 말하는 중 | 360×1094 | Home Banner 5 | Tab 홈 | Mate Card 말하는 중(80) |
| 1n-1-M3 · 홈 · 메이트 답 | 360×1094 | 〃 | Tab 홈 | Mate Card 답(84) |
| 1r-1-M4 · 캘린더 · 메이트 화면 이동 | 360×812 | "캘린더" | Tab 캘린더 · FAB | Mate Card 화면 이동(80) — 메이트가 캘린더로 이동시킨 뒤 |

#### Z · 첫 진입 모션 — 프로토타입 전용 (2)

M-1n-2 처음 홈 · M-1n-1 홈(이어서 하기 1/3). 와이어프레임 원본이 아니며, Home Banner · Card / Resume를 이 복제본에서만 분리해 키프레임을 넣었다(§10).

### 9.4 핵심 화면 해부

#### 홈 (1n-1 계열)

- 프레임 세로 간격 **−28**: Home Banner(400) 아래로 흰 시트가 28 겹친다.
- Sheet: `bg/surface` · 상단 반경 28 · `Elevation/Sheet` · 패딩 28/20/20/20 · 간격 12.
- 시트 순서: `Card / Action`(증상 정리 시작하기) → `Card / Resume`(진행 중일 때만) → 최근 브리핑 카드(Section Header + 목록, 묶음 간격 12) → 다가오는 일정(Section Header + `Card / Schedule`).
- 첫 홈(1n-2): 배너 CTA=None(334), 시트에 Card / Action + Empty State NoRecord.
- Tab Bar는 화면 바닥에 고정, 배너와 시트가 함께 스크롤한다. 스크롤 끝에서 마지막 카드와 탭바 사이 20을 유지한다.
- 헤이 메이트: Mate Card를 딤 없이 x 12 · 탭바 위 12에 띄운다. 듣는 중 · 가이드 253 → 말하는 중 80 → 답 84 → 화면 이동 80.

#### 증상 정리 (1d · 1c · 1c-D)

- 모든 단계 위에 Progress Indicator(3칸). 1단계 부위 짚기는 앱의 3D 인체도라 Figma 와이어프레임이 없다.
- 1d: 질문 `Heading/L` + 설명 `Body/M` → Severity Slider → Notice Info(숫자는 의사가 읽는다는 안내). 블록 간격 20.
- 1c: 위에 "짚은 부위" + Chip Selected로 맥락을 고정하고, AI Bubble(면 없음) · Patient Bubble(`bg/primary-subtle`, 오른쪽)을 교차한다.
- 1c-D: AI가 추천한 질문을 Question Row로 고른다(번호 = Number Badge). 빈 칸(Add)을 누르면 Focus → Writing.

#### 브리핑 카드 (1e-1)

Content 12/20/16, 간격 14. 순서는 고정한다.

1. **Brief Card**(r20 · 패딩 18/18/6/18): 머리글(제목 "복부 통증 · 3주" + 메타, 오른쪽 공유 Icon Button Ghost + `Icon/export`) → Divider → KV Row ×5(그중 Emphasis 1) → Severity Readout → AI 캡션 "AI가 말씀하신 내용을 정리했어요" + ⓘ
2. **Notice Warning** — 알러지
3. **Callout** — 묻고 싶은 것(질문 3, Show Q4로 4번째)
4. Section Header + **Hospital Card**(r20, 안쪽 패딩 16, 간격 13)

하단 CTA는 상태로 바뀐다: 저장 전 Primary "저장하기" → 일정 없음 Primary "진료 일정 잡기" → 진료 전 Outline "진료 일정 보기" → 진료 지남 Tonal "진료 후 기록하기". 진료실에서는 이 화면을 그대로 보여 준다(별도 의사용 화면 없음).

#### 진료 후 (1p · 1q-1)

- 1p: 제목 2줄 "진료실에서 들은 말을 그대로 남겨두세요" → 병원·일자 카드(r16, 패딩 16) → Text Area → "AI로 정리하기" Chip + ⓘ. 받아쓰기는 Voice Input Mate(1p-T).
- 1q-1 **Record Card**(r20 · 패딩 18/18/6/18): 머리글("서울OO병원 내과 · 2026.09.18") → Divider → KV Row ×4(Emphasis 1, 값 2줄이면 80) → Divider → 진료 메모 원문(Quote, 패딩 12) → AI 캡션 "AI가 메모를 4가지로 나눴어요" + ⓘ. **원문은 그대로 보존**하고 분류 결과와 나란히 둔다.

#### 캘린더 · 일정 추가 (1r)

- 1r-1: Content 8/20/16, 간격 14 — Month Head(Icon Button Ghost L ‹ › + "2026년 9월", 48) → Calendar Grid(r20 · 패딩 12/8/12/8 · 간격 4: 요일 + 주 5줄 Date Cell + 범례) → Section Header → List Row. FAB(Icon Button Solid L)는 오른쪽 20 · 탭바 위 20.
- 1r-4: Content 8/20/24, 간격 0 + 명시적 Gap. 라벨 `Body/S` → 필드(높이 56 r16): 병원 · 날짜 · 시간(가로 2칸, 간격 10) → Section Header "가져갈 브리핑 카드" → Card Pick(체크박스 + 글자, r20 · 패딩 16/18) → "새 카드 만들기" → Section Header → "할 일 추가".

#### 기록 (1j)

- 1j-1: 월별 Section Header + List Row Badge(요약 포함 104), 간격 12. 편집 모드는 §9.2.
- 1j-3: Head(제목 + 메타, 아래 18) → Timeline(세로선 2 + 단계 간격 16). 재방문은 같은 타임라인에 쌓인다(1j-3-R).

#### 내 정보 (1s)

설정 카드(r20 · 패딩 6/18)에 Setting Row를 쌓는다. 경고는 카드 사이 Notice Warning. 알림 시간(1s-3)은 Setting Row Value ×2 카드 + Time Wheel 카드, 하단에 Hint Row로 예시("5일 진료라면 4일 · 5일 …").

---

## 10. 모션

| 대상 | 규칙 |
|---|---|
| 기본 곡선 | `cubic-bezier(0.22, 1, 0.36, 1)` (ease out). 선형·기본 ease-in-out을 쓰지 않는다 |
| 홈 첫 진입 (1.8초, 1회) | 배너 사진 투명 0 → 1(0~0.6초) + 108% → 100%(0~1.4초) · 헤더 0.15~0.55초 · 제목·설명·링크 14px 위로, 0.1초 간격 · 흰 시트 56px 위로(0.1~0.8초) · 시트 안 요소 12px 위로, 0.09초 간격(0.4초부터) · 진행 원 채움 0.75~1.55초, "1/3" 1.0~1.4초 |
| 홈 배너 자동 넘김 | 장마다 **4초 노출** → 0.5초 이동(위 곡선). 첫 이동 5.8초(진입 1.8 + 4). 5장 뒤 1장으로 이음매 없이 순환. 헤더(로고·알림·아바타)는 고정 |
| 진행 원 (Card / Resume) | 12시에서 시계 방향으로 n/3까지 채움(선 그리기) |
| 온보딩 진행 | Onboarding Progress Smart Animate 240ms `EASE_IN_AND_OUT`, 화면 전환과 동시에 Step 갱신 |
| Mate Card | 듣는 중 카드가 홈 상태 그대로 탭바 위 하단에서 올라온다. 상태 전환은 높이 변화(253 → 80 → 84)를 부드럽게. 딤 없음 |
| 움직임 축소 | OS 설정이 켜지면 진입 모션·배너 자동 넘김·파형·spinner를 멈추거나 즉시 전환하고 상태 글자는 유지한다 |

Figma 프로토타입: Z 섹션 시작점 "첫 진입 모션 · 처음 홈" · "첫 진입 모션 · 홈 (이어서 하기)". 탭바는 고정 레이어, 화면은 812 + 세로 스크롤.

---

## 11. 접근성 계약

| 항목 | 기준 |
|---|---|
| 텍스트 대비 | WCAG 2.1 AA 4.5:1(큰 글자 3:1). 토큰별 실측은 §2.3 |
| 비텍스트 대비 | 3:1(아이콘·경계·컨트롤 상태). 직접 계산한다 |
| 터치 영역 | 최소 48×48. Icon Button S(32) · Date Cell(42) · Tooltip ⓘ는 부모·그리드 hit area로 보완 |
| 색 단독 금지 | 선택·진행·통증 단계·상태는 색 + 형태·낱말 |
| 글자 배율 | 130%까지 레이아웃 유지(Hug). 고정 높이 컨트롤은 별도 QA |
| 플레이스홀더 | `fg/subtle`(6.36). `fg/muted`는 글자 금지 |
| 포커스 | `border/focus` 2px. 키보드·스위치 환경에서 제거하지 않음 |
| 아이콘 버튼 | 모두 접근성 이름. 진행 마커는 "총 n단계 중 m단계" 한 개 이름, 개별 마커는 숨김 |
| 통증 Slider | 드래그 + 정지점 탭 + 방향키 · Home/End |
| 음성 | 음성 입력 옆에 항상 텍스트 입력 대안. 마이크 거부 시 Empty State MicDenied |
| AI 결과 | 원문과 수정 진입점 유지. "AI가 정리했어요" 표시 |
| 사진 위 글자 | 흰 글자 + `Shadow/On Image`. 사진이 밝아도 읽히는지 배너 5장 모두 QA |
| 예외 | 카카오 버튼은 공식 규격(변경 불가). 주변 안내는 기준 통과 |

---

## 12. 개발 적용

### 12.1 Android 매핑 (`com.mist.medicalmate.core.designsystem`, 읽기 전용 확인)

| Figma | 코드 | 이번 게시로 추가할 것 |
|---|---|---|
| Palette | `Palette.kt` | — |
| Semantic | `MedicalMateColors.kt` · `BrandColor.kt` | `bgOnDark`(흰 20%) · `borderOnDark`(흰 32%) · `bgPrimaryStrong`(primary/800) |
| Typography | `Type.kt`(15스타일, 값 Figma와 동일) | `labelXS`(Bold 11/16, 자간 0) |
| Scale · Layout | `Dimens.kt` · `MedicalMateRadius.kt` · `MedicalMateSpace` | layout 값 360/320/79/92 확인(예외 상수 buttonM 14 · dateCell 13 유지) |
| Elevation | `Elevation.kt` · `MedicalMateSurfaceStyle.kt` | `elevationPrimary` · `shadowOnImageText` · `shadowOnImageIcon` |
| Severity | `MedicalMateSeverity.kt` | — |
| Icons · Logo | `MedicalMateIcons.kt` · `MedicalMateLogo.kt` | `export` |
| 컴포넌트 | `component/*.kt`(52종) | §8.8~8.12의 20종 |

### 12.2 컴포넌트 API 원칙

- Figma variant 이름을 코드 enum으로 쓴다. 예: `Button(type = Primary, size = L)`, `Chip(type = Info)`, `HomeBanner(banner = Reservation, cta = Link)`.
- Figma boolean → 선택 prop(`showLeadingIcon` · `showNrs` · `showSelect` · `showQ4` · `showValue`). 텍스트 속성 → 문자열 prop.
- `State`가 화면 상태와 겹치면 `visualState`처럼 의미를 분명히 하고 매핑표를 남긴다.
- 버튼 라벨과 접근성 이름을 분리한다.
- 같은 시각 요소를 화면마다 복제 구현하지 않는다. variant와 slot으로 확장한다. 시트·Footer·Nav는 공통 scaffold로 만든다.

### 12.3 CSS 토큰 (HTML 시안·웹 데모용)

```css
:root {
  /* Semantic — 화면은 이것만 */
  --mm-bg-canvas:#F5F6FA; --mm-bg-surface:#FFFFFF; --mm-bg-subtle:#EDEFF5;
  --mm-bg-primary:#5566D2; --mm-bg-primary-pressed:#3B4FC0; --mm-bg-primary-subtle:#E3E7FC;
  --mm-bg-primary-faint:#F2F4FE; --mm-bg-primary-strong:#242F79;
  --mm-bg-info:#F2F4FE; --mm-bg-success:#E4F7ED; --mm-bg-warning:#FFF4D6; --mm-bg-danger:#FFEDEB;
  --mm-bg-inverse:#131722; --mm-bg-inverse-soft:#3A4053; --mm-bg-scrim:rgba(19,23,34,.5);
  --mm-bg-on-dark:rgba(255,255,255,.2);
  --mm-fg-default:#131722; --mm-fg-subtle:#585F73; --mm-fg-muted:#7C8397; --mm-fg-disabled:#C6CAD8;
  --mm-fg-on-primary:#FFFFFF; --mm-fg-on-inverse:#FFFFFF;
  --mm-fg-primary:#2E3E9E; --mm-fg-link:#2E3E9E; --mm-fg-info:#2E3E9E;
  --mm-fg-success:#0E7A4A; --mm-fg-warning:#8A5A0B; --mm-fg-danger:#C4302B;
  --mm-border-subtle:#DEE1EB; --mm-border-default:#C6CAD8; --mm-border-strong:#7C8397;
  --mm-border-focus:#5566D2; --mm-border-primary:#5566D2; --mm-border-on-dark:rgba(255,255,255,.32);
  --mm-severity-1:#FFE3A8; --mm-severity-2:#FFC79B; --mm-severity-3:#FFA894; --mm-severity-4:#F58079; --mm-severity-5:#DC5A55;
  --mm-severity-1-tint:#FFF6E4; --mm-severity-2-tint:#FFEFE4; --mm-severity-3-tint:#FFE9E3; --mm-severity-4-tint:#FDE4E2; --mm-severity-5-tint:#F9DEDD;
  /* Shape */
  --mm-radius-xs:8px; --mm-radius-sm:12px; --mm-radius-md:16px; --mm-radius-lg:20px;
  --mm-radius-xl:24px; --mm-radius-2xl:28px; --mm-radius-full:999px;
  /* Elevation */
  --mm-elev-card:0 3px 10px rgba(27,37,90,.08), 0 1px 2px rgba(27,37,90,.05);
  --mm-elev-float:0 4px 14px rgba(27,37,90,.09), 0 1px 3px rgba(27,37,90,.06);
  --mm-elev-sheet:0 -4px 24px rgba(27,37,90,.10), 0 0 2px rgba(27,37,90,.05);
  --mm-elev-primary:0 8px 30px -18px rgba(59,79,192,.28);
  --mm-shadow-on-image-text:0 1px 2px rgba(19,23,34,.5), 0 2px 16px rgba(19,23,34,.4);
  --mm-shadow-on-image-icon:0 1px 3px rgba(19,23,34,.5);
  /* Layout */
  --mm-screen-w:360px; --mm-screen-h:812px; --mm-gutter:20px; --mm-content-w:320px;
  --mm-nav-h:56px; --mm-tabbar-h:79px; --mm-footer-h:92px; --mm-footer-pad-top:12px; --mm-safe-bottom:24px;
  --mm-content-top:12px; --mm-content-bottom:16px; --mm-block-gap:14px;
  --mm-touch-min:48px; --mm-control-sm:40px; --mm-control-md:48px; --mm-control-lg:56px; --mm-mic:88px;
  /* Type */
  --mm-font:'Pretendard Variable','Pretendard',-apple-system,'Apple SD Gothic Neo','Noto Sans KR',sans-serif;
}
.display-m{font:700 30px/40px var(--mm-font);letter-spacing:-.02em}
.heading-l{font:700 24px/34px var(--mm-font);letter-spacing:-.02em}
.heading-m{font:600 20px/28px var(--mm-font);letter-spacing:-.015em}
.heading-s{font:600 17px/24px var(--mm-font);letter-spacing:-.01em}
.body-l{font:400 17px/26px var(--mm-font)} .body-l-strong{font:600 17px/26px var(--mm-font)}
.body-m{font:400 15px/24px var(--mm-font)} .body-m-strong{font:600 15px/24px var(--mm-font)}
.body-s{font:400 13px/20px var(--mm-font)} .body-s-strong{font:600 13px/20px var(--mm-font)}
.label-l{font:600 15px/20px var(--mm-font)} .label-m{font:600 13px/18px var(--mm-font)}
.label-s{font:500 11px/16px var(--mm-font);letter-spacing:.02em}
.label-xs{font:700 11px/16px var(--mm-font)}
.numeric-l{font:700 24px/30px var(--mm-font);letter-spacing:-.015em}
.numeric-m{font:600 17px/22px var(--mm-font);letter-spacing:-.01em}
.ko{word-break:keep-all;overflow-wrap:anywhere;min-width:0}
```

웹 아티팩트처럼 Google Fonts만 허용되는 환경에서는 `Noto Sans KR`을 앞에 두고 Pretendard를 폴백으로 둔다.

### 12.4 구현 완료 체크리스트

- [ ] 모든 화면 색이 Semantic 토큰에서 왔는가? 새 토큰 3개(`bg/on-dark` · `border/on-dark` · `bg/primary-strong`)가 등록됐는가?
- [ ] 16 텍스트 스타일(`Label/XS` 포함)이 정확한 굵기·행간·자간으로 등록됐는가?
- [ ] 360 기준 `20 + 320 + 20`이 기기 폭 변화에도 유지되고, safe-area와 `safe-bottom`이 중복되지 않는가?
- [ ] Nav · Footer · Tab Bar가 고정되고 Content만 스크롤하는가? 홈 탭바 고정·시트 −28 겹침이 맞는가?
- [ ] 편집 모드(편집 → 취소 → 확인), Danger 삭제, Dialog 확인이 모든 편집 화면에 같은 규칙으로 동작하는가?
- [ ] 정상 · 눌림 · 비활성 · 포커스 · 오류 · 로딩 · 빈 상태가 필요한 화면에 있는가?
- [ ] 색을 빼도 선택 · 진행 · 통증 단계 · 활성 탭이 구분되는가?
- [ ] 환자 원문과 AI 정리 결과가 구분되고 수정 가능한가?
- [ ] 글자 130%에서 잘림이 없는가? (탭 라벨 · Nav "건너뛰기" · Step Row 우선 확인)
- [ ] 움직임 축소 설정에서 배너 자동 넘김·진입 모션이 멈추는가?
- [ ] Archive · `.[구]` · `_legacy` 요소가 섞이지 않았는가?

---

## 13. Figma 정합성 메모 (2026-10-15)

구현에는 아래처럼 대응하고, 디자인 파일 정리는 별도로 진행한다.

1. **DS 마스터 기본 폭 350 · 390**: Card · KV Row · List Row · Notice · Checkbox · Text Field 등은 350, Tab Bar · Overlay Scrim은 390×(79·844)로 남아 있다. 화면에서는 모두 Fill로 늘리거나 줄였다. 코드에는 고정 폭을 넣지 않는다.
2. **Nav Bar · Tab Bar Glass**: `Surface/Glass`(blur 24)가 연결돼 있지만 면이 `bg/surface` 100%라 사실상 불투명이다. 반투명 비율(예전 82~86%)은 아직 정하지 않았다. 구현 기본은 불투명.
3. **Tab Bar 변형 이름**: `Active=홈/기록/캘린더/Active4` — `Active4`는 옛 "내 정보" 탭의 흔적이다. 3탭만 구현한다.
4. **Progress Indicator**: 3칸 + "n / 3" 글자를 덮어써서 쓰며, 일부 화면은 변형 이름과 채운 칸 수가 다르다(1d는 `Step=3` 변형에 채움 2칸 · "2 / 3"). **채운 칸 수 = 현재 단계**로 구현한다.
5. **Overlay Scrim 색**: 화면 인스턴스의 면이 변수 없이 50% 직접 값이다. 코드에서는 `bg/scrim` 50%로 구현한다.
6. **Label/S 글꼴**: Figma 스타일은 `Pretendard` Regular 11/16, 코드 `Type.kt`는 Medium 11/16. 코드(Medium)를 기준으로 하고 Figma를 맞춘다.
7. **반경 · 간격 변수 연결률**: 반경은 화면 12%, 간격은 1%만 변수에 연결돼 있다. 값은 스케일과 일치하므로 값 기준으로 토큰에 매핑한다. 스케일 밖 값(§4.6)은 코드 상수.
8. **Time Wheel 바깥 줄 글자**: 설명상 `fg/muted` · `fg/disabled` 글자를 쓴다. 휠의 장식성 줄이라 예외로 두되, 선택 줄과 바로 위아래 줄은 `fg/subtle` 이상으로 구현한다.
9. **같은 이름 화면**: `1e-1 · 일정 없음`(최초 생성 "저장하기" / 저장 뒤 "진료 일정 잡기") · `1p`(짧은 메모 / 긴 메모) · `1r-4-C · 적는 중`(876 / 932) · `1m-B`(입력 전 / 결과)는 상태가 다른 두 장이다. 하단 CTA와 크기로 구분한다.
10. **Brand 변수 미게시**: `kakao/*` 3개가 게시에서 숨겨져 있다. Social Login Button 안 값이 정본이다.
11. **원본 없는 인스턴스**: 2026-10-15 정리로 삭제한 변형(Home Banner CTA=Pill 5개 · Waveform M 3개)의 인스턴스가 V3 · 브랜딩 페이지에 남아 있다. 10-15 화면에는 없다.
12. **Severity NRS 등가**: 임상 검증 전. 카드에 "참고" 표기 여부는 PRD §9.4 결정을 따른다.
13. **미게시 작업물**: `수정 중` 섹션의 `Tab Bar / Briefing`(5탭: 홈 · 캘린더 · 브리핑 · 메모 · 내 정보) · `Icon/briefing-cards`는 10-15에 반영되지 않았다. 현재 기준은 3탭이다.

---

## 14. 변경 관리

- 디자인 변경은 Palette가 아니라 Semantic 역할부터 검토한다.
- 토큰 이름을 바꾸면 Figma 변수, 코드 토큰, 이 문서를 같은 변경 단위로 갱신한다.
- 새 컴포넌트 · 스타일은 DS 페이지 `추가` 섹션의 맞는 묶음에 **컴포넌트 이름 그대로** 넣고 목록 표에 한 줄 추가한다. 라이브러리 게시는 디자인 담당이 한다.
- Variant를 추가할 때 사용 조건 · 금지 조건 · 크기 · 상태 전환 · 접근성 이름을 component description과 이 문서에 함께 적는다.
- Breaking change 금지. 기존 속성·변형 이름을 바꾸거나 지우지 않고 추가만 한다.
- PR 리뷰에서는 색 hex보다 Semantic 토큰 이름, 임의 간격보다 Scale 토큰 사용 여부를 먼저 본다.

### 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v4.0 | 2026-10-15 | 기준 파일을 `OWKWtDt8lLQLmltq0ztT5O`로, 기준 화면을 360×812 · 콘텐츠 320으로 교체. 2026-10-15 게시본 반영: Semantic 44(+`bg/on-dark` · `border/on-dark` · `bg/primary-strong`), 텍스트 스타일 16(+`Label/XS`), 효과 스타일 7(+`Elevation/Primary` · `Shadow/On Image` 2), 아이콘 47(+`export`), 추가 컴포넌트 20, `Chip` Type=Info, `Empty State` MicDenied. 색 Paint Style은 `_legacy`로 숨김. Tab Bar 3탭 · Nav Bar 속성 갱신. 10-15 화면 89장의 골격 · 패딩 · 간격 · Footer · 시트 · 편집 패턴 · 화면 목록 · 모션 추가. Toast · Bottom CTA Bar · Bottom Sheet · Voice Input · Radio · Segmented Control · Source Quote는 Archive로 이동한 상태를 반영 |
| v3 | 2026-09-10 | 구 파일 `cG6lz8nwzp75bfAXCnMqxx` 기준 · 390 규격 · 컴포넌트 40종 (이 문서로 대체) |
