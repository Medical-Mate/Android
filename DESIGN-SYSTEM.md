# 진료메이트 디자인 시스템 — Claude Code 작업용 (v3.0)

**기준일** 2026-10-10 · **정본** Figma `OWKWtDt8lLQLmltq0ztT5O` → 페이지 `디자인시스템 · 최종`(291:636) · 섹션 `Design System`(1864:1443)
**값 출처** Figma 변수·스타일을 Plugin API로 직접 덤프한 값이다. 손으로 옮긴 값이 아니다.
**짝 문서** `진료메이트-PRD.md`

---

## 0. 세 가지 규칙 (00 · 시작하기 보드)

1. **화면에는 시맨틱만 쓴다.** `bg/primary` · `fg/default` · `border/subtle`처럼 역할 이름이 든 것만. `primary/500` 같은 원시 팔레트를 직접 집지 않는다.
2. **변수와 스타일은 같은 것이다.** 디자이너는 스타일 패널, 개발자는 변수에서 같은 이름을 읽는다. (색 페인트 스타일은 게시에서 숨겼다 — 색은 **변수로만** 제공)
3. **높이는 내용이 정하고(HUG) 폭은 화면이 채운다(FILL).** 고정은 규격이 곧 의미인 것만 — Icon Button · Avatar · Date Cell · Nav Bar · 버튼 높이 56/48/40 · Search Field.

추가 규칙
- 이름이 `.`(마침표) 또는 `.[구]`로 시작하는 컴포넌트는 옛 버전이다(파일 안 221개). 게시 안 됨. **새 화면에 쓰지 않는다.**
- 새 토큰·컴포넌트·스타일 등록은 **명준님 확인 선행**. 먼저 기존 34종에서 찾는다.
- Breaking change 금지. 추가만 허용.

### 문서 보드
| 보드 | 노드 | 내용 | 링크 |
|---|---|---|---|
| 00 · 시작하기 | `361:1290` | 5분 안내 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=361-1290) |
| 01 · Color | `300:694` | 팔레트 43 · 시맨틱 41 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=300-694) |
| 02 · Typography | `301:694` | 15스타일 · 본문 Body/L(17) | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=301-694) |
| 03 · Icon | `308:815` | 46개 · 획 1.75 · 최소 16 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=308-815) |
| 04 · Layout & Spacing | `1864:1444` | 360×812 · 여백 20 · 실측 스케일 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=1864-1444) |
| 05 · Shape & Elevation | `324:909` | 반경 7 · 고도 3 · 글래스 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=324-909) |
| 06 · Components | `337:1115` | 실사용 34종 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=337-1115) |
| 07 · Logo | `347:1212` | 심볼 5 · 락업 3 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=347-1212) |
| Icon set (마스터) | `296:4148` | | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=296-4148) |
| Archive · 참고 | `1864:1446` | 미사용 15 · v2 9 · 변경 이력 · 로고 후보 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=1864-1446) |

---

## 1. 색 — 2계층 (Palette → Semantic)

컬렉션: `진료메이트 / Palette (Value)` · `진료메이트 / Semantic (Light)` · `진료메이트 / Brand · 외부 규격`
단색 체계. 기준 색상 231°(인디고) — 흔한 파랑(210~215°)을 피했다. **위계는 색이 아니라 형태(채움/테두리/텍스트)가 만든다.**

### 1.1 Palette (원시 · 43) — 화면에서 직접 쓰지 않는다
| 계열 | 값 |
|---|---|
| primary | 50 `#F2F4FE` · 100 `#E3E7FC` · 200 `#C7CFF8` · 300 `#A3AFF1` · 400 `#7484E3` · **500 `#5566D2`** · 600 `#3B4FC0` · 700 `#2E3E9E` · 800 `#242F79` · 900 `#1A2154` |
| neutral | 0 `#FFFFFF` · 25 `#FAFBFD` · 50 `#F5F6FA` · 100 `#EDEFF5` · 200 `#DEE1EB` · 300 `#C6CAD8` · 400 `#989EB1` · 500 `#7C8397` · 600 `#585F73` · 700 `#3A4053` · 900 `#131722` |
| amber | 50 `#FFF4D6` · 100 `#FFE9B3` · 500 `#E0A227` · 700 `#8A5A0B` |
| green | 50 `#E4F7ED` · 100 `#C6F0DC` · 500 `#12A05F` · 700 `#0E7A4A` |
| red | 50 `#FFEDEB` · 100 `#FFD8D4` · 500 `#E5504A` · 700 `#C4302B` |
| severity | 1 `#FFE3A8` · 2 `#FFC79B` · 3 `#FFA894` · 4 `#F58079` · 5 `#DC5A55` |
| severity tint | 1 `#FFF6E4` · 2 `#FFEFE4` · 3 `#FFE9E3` · 4 `#FDE4E2` · 5 `#F9DEDD` |

### 1.2 Semantic (41) — 화면은 이것만
| 토큰 | → Palette | 값 | 쓰는 자리 |
|---|---|---|---|
| `bg/canvas` | neutral/50 | `#F5F6FA` | 화면 바탕 |
| `bg/surface` | neutral/0 | `#FFFFFF` | 카드·시트 면 |
| `bg/subtle` | neutral/100 | `#EDEFF5` | 입력 필드 면 |
| `bg/primary` | primary/500 | `#5566D2` | Primary 버튼·선택 채움 (흰 글자 4.98) |
| `bg/primary-pressed` | primary/600 | `#3B4FC0` | 눌림 |
| `bg/primary-subtle` | primary/100 | `#E3E7FC` | **환자의 말(Bubble/Patient)** · Callout · 선택 칩 |
| `bg/primary-faint` | primary/50 | `#F2F4FE` | 안내 카드(오늘의 한 줄) |
| `bg/info` | primary/50 | `#F2F4FE` | Notice/Info |
| `bg/success` · `bg/warning` · `bg/danger` | green/amber/red 50 | | Notice 톤 · 삭제 CTA 면 |
| `bg/inverse` · `bg/inverse-soft` · `bg/scrim` | neutral/900 · 700 · 900 | | 토스트·스크림 |
| `fg/default` | neutral/900 | `#131722` | 본문 (17.90) |
| `fg/subtle` | neutral/600 | `#585F73` | 보조 문구·**플레이스홀더** (6.36) |
| `fg/muted` | neutral/500 | `#7C8397` | **글자 금지** (3.78) — 아이콘·비활성 경계 전용 |
| `fg/disabled` | neutral/300 | | 비활성 |
| `fg/on-primary` · `fg/on-inverse` | neutral/0 | | 채움 위 글자 |
| `fg/primary` · `fg/link` · `fg/info` | primary/700 | `#2E3E9E` | 강조 글자·링크 (9.13) |
| `fg/success` · `fg/warning` · `fg/danger` | green/amber/red 700 | | 상태 글자 (warning 5.40) |
| `border/subtle` · `default` · `strong` | neutral/200 · 300 · 500 | | 목록 구분·컨트롤 경계 (strong 3.78 = 비텍스트 기준 통과) |
| `border/focus` · `border/primary` | primary/500 | | 포커스·선택 |
| `severity/1~5` · `severity/1~5-tint` | severity/* | | **Severity Readout·Slider 판독 영역에만** |
| `bg/on-dark` | (값) 흰 20% | `#FFFFFF33` | 사진·primary 면 위 반투명 면 — 배너 아바타 원, Card/Action 아이콘 원. 2026-10-15 추가 · 게시 |
| `border/on-dark` | (값) 흰 32% | `#FFFFFF52` | 사진 위 반투명 선 — 배너 아바타 테두리, 링크 위 구분선. 2026-10-15 추가 · 게시 |
| `bg/primary-strong` | primary/800 | `#242F79` | 진한 정보 칩 면(Chip Type=Info). 2026-10-15 추가 · 게시 |

### 1.3 Brand · 외부 규격
`kakao/bg #FEE500` · `kakao/text #000000@85%` · `kakao/logo #000000` — 카카오 공식 규격. 변경 금지.
> 고아 컬렉션 `Variable collection`(kakao/logo 1개)이 남아 있다. 정리 대상 후보 — 삭제 전 확인.

### 1.4 색 규칙
- **유색 큰 면은 환자의 말에만**(P5). AI 발화는 면색 없음 + "AI" 라벨(D4).
- severity 색은 **판독 전용**. 고르는 동안 색이 판단을 유도하면 안 된다.
- 색만으로 상태를 구분하지 않는다(D11) — 숫자·낱말·아이콘을 겹친다.
- 다크 모드 변수 모드는 **없다**(Light 단일). 다크 대응은 범위 밖.

---

## 2. 타이포그래피 — 15스타일 · Pretendard

| 스타일 | 굵기 | 크기/행간 | 자간 | 용도 |
|---|---|---|---|---|
| `Display/M` | Bold | 30/40 | -2% | 스플래시·완료 |
| `Heading/L` | Bold | 24/34 | -2% | 화면 질문 헤드라인 |
| `Heading/M` | SemiBold | 20/28 | -1.5% | 섹션 제목·카드 제목 |
| `Heading/S` | SemiBold | 17/24 | -1% | 카드 내 소제목 |
| `Body/L` · `Body/L Strong` | Regular · SemiBold | 17/26 | 0 | **본문 기본** |
| `Body/M` · `Body/M Strong` | Regular · SemiBold | 15/24 | 0 | 보조 본문 |
| `Body/S` · `Body/S Strong` | Regular · SemiBold | 13/20 | 0 | 메타·캡션 |
| `Label/L` | SemiBold | 15/20 | 0 | 버튼 L |
| `Label/M` | SemiBold | 13/18 | 0 | 칩·배지 |
| `Label/S` | Medium | 11/16 | +2% | 아주 작은 라벨(AI 라벨 등) |
| `Label/XS` | Bold | 11/16 | 0 | 작은 칩·진행 원 숫자(Chip Info, Card/Resume). 2026-10-15 추가 · 게시 |
| `Numeric/L` | Bold | 24/30 | -1.5% | 큰 숫자(D-day) |
| `Numeric/M` | SemiBold | 17/22 | -1% | 숫자 판독 |

- **본문 기본은 Body/L(17).** 근거: 진료실에서 소리 내어 읽을 문장. 연령 근거 아님.
- 헤딩은 자간을 조이고 본문은 0. 한글 본문 자간을 조이면 판독이 떨어진다.
- 한국어 줄바꿈: `word-break: keep-all` (§PRD 10).
- 텍스트 필드 행간 26이 크다는 피드백 미결.

---

## 3. 레이아웃 · 간격 (04 보드 · 71장 실측)

### 3.1 화면
| 토큰 | 값 | 뜻 |
|---|---|---|
| `layout/screen-w` · `screen-h` | 360 · 812 | 기준 화면(일부 화면은 812 초과 허용) |
| `layout/gutter` | 20 | 좌우 여백 |
| `layout/content-w` | 320 | 콘텐츠 폭 |
| `layout/nav-h` | 56 | 상단 Nav Bar |
| `layout/tabbar-h` | 79 | 하단 탭(1뎁스만) |
| `layout/footer-h` · `footer-pad-top` | 92 · 12 | 하단 고정 CTA |
| `layout/safe-bottom` | 24 | Footer 아래 패딩 |
| `layout/content-top` · `content-bottom` · `block-gap` | 12 · 16 · 14 | Content 기본 패딩·블록 간격 |

> PRD v1.1의 390×844 · 콘텐츠 350은 **구 규격**이다. 현재는 360×812 · 320.

### 3.2 Content 패딩 변형
| 위·아래 | 언제 |
|---|---|
| 12 · 16 | 대부분 |
| 8 · 16 | 첫 요소가 Section Header |
| 8 · 24 | Footer 위 여유가 필요한 목록형 |
| 72 · 24 | 일러스트 중앙 배치 |

블록 간격 실측: 14(20장) · 12(14장) · 20(12장) · 0(12장, 요소가 자체 여백)

### 3.3 스케일
- **space**: 2 · 4 · 6 · 8 · 10 · 12 · 14 · 16 · 20 · 24 · 32 · 40 (가장 많이 쓰는 값 8·10)
- **size**: touch-min 48 · icon sm 18 / md 20 / lg 24 · control sm 40 / md 48 / lg 56 · **mic 88**
- 스케일 밖 값(간격 3·5·13·18, 반경 10·13·14) 토큰화 여부 **[보류]**

---

## 4. 형태 · 고도 (05 보드)

### 4.1 반경
| 토큰 | 값 | 용도 |
|---|---|---|
| `radius/xs` | 8 | 배지 · 체크박스 |
| `radius/sm` | 12 | S 버튼 · 원문 블록 |
| `radius/md` | 16 | L 버튼 · Notice |
| `radius/lg` | 20 | Card · Callout |
| `radius/xl` | 24 | Dialog |
| `radius/2xl` | 28 | Bottom Sheet 상단 |
| `radius/full` | 999 | Chip · Avatar · Icon Button |

작은 요소일수록 작은 반경. 같은 화면에서 두 단계 이상 건너뛰지 않는다.

### 4.2 고도 — 테두리 대신 고도로 위계
| 스타일 | 값 | 용도 |
|---|---|---|
| `Elevation/Card` | 0,3 blur10 `#1B255A`@8% + 0,1 blur2 @5% | 카드 · 선택 행 |
| `Elevation/Float` | 0,4 blur14 @9% + 0,1 blur3 @6% | 하단 고정 바 · 플로팅 |
| `Elevation/Sheet` | 0,-4 blur24 @10% + 0,0 blur2 @5% | 바텀시트 · Dialog |
| `Elevation/Primary` | 0,8 blur30 spread-18 `#3B4FC0`@28% | primary 채움 카드·큰 버튼 (홈 "증상 정리 시작하기"). 2026-10-14 추가 · 2026-10-15 게시 |
| `Shadow/On Image · Text` | 0,1 blur2 `#131722`@50% + 0,2 blur16 @40% | 사진 위 흰 글자·로고 가독성(홈 배너). 2026-10-15 추가 · 게시 |
| `Shadow/On Image · Icon` | 0,1 blur3 `#131722`@50% | 사진 위 흰 아이콘(홈 배너 알림). 2026-10-15 추가 · 게시 |
| `Surface/Glass` | background blur 24 · 흰 면 78~86% | **콘텐츠 위 크롬 전용** — Nav Bar · Tab Bar · 스크롤 위 CTA 바 |

- 그림자는 **브랜드 틴트**(#1B255A). 검정 그림자 금지 — 회색 때처럼 보인다.
- 3단계를 넘기지 않는다. 떠 있는 층은 화면에 둘까지.
- 선은 목록 구분과 포커스에만.
- 글래스: 테두리 없음 · **불투명 폴백 변이 필수**(iOS backdrop-filter / Android RenderEffect, 저사양·절전 대비) · 두 변이의 레이아웃·대비 동일.

---

## 5. 아이콘 (03 보드)

24×24 격자 · 라이브 20 · 획 1.75 · 라운드 캡/조인 · 단일 색 · **16 이하 금지**.
탭 활성은 **채움형으로 전환**(색만 바꾸지 않는다).

마스터 46종 (`Icon/*`):
`mic` · `mic-listening` · `mic-off` · `waveform` · `chevron-left` · `chevron-right` · `chevron-down` · `chevron-up` · `arrow-right` · `arrow-up` · `close` · `plus` · `minus` · `check` · `check-circle` · `alert-circle` · `alert-triangle` · `info` · `home` · `home-filled` · `note` · `note-filled` · `calendar` · `calendar-filled` · `user` · `user-filled` · `stethoscope` · `edit` · `share` · `copy` · `trash` · `search` · `bell` · `clock` · `camera` · `chat` · `lock` · `empty-box` · `more-horizontal` · `spinner` · `hospital` · `pill` · `heart-pulse` · `body-point` · `search-off` · `wifi-off`

> `pill` · `heart-pulse` 아이콘이 존재하지만 **금지 시각 요소(알약)** 와 충돌한다. 앱 화면 사용처 확인 후 쓰지 않는다.
> 진료 기록 없음 아이콘은 정육면체(empty-box)로 — 피드백 미결.

---

## 6. 컴포넌트 — 앱 실사용 34종 (06 보드)

판단 근거와 금지 사항은 각 컴포넌트 description에 있다. **화면 n회** = 앱 화면 71장에서 실제 사용 횟수(06 보드 표기).

| 컴포넌트 | 범주 | 변이 | 속성 | 사용 | 규칙 | Figma |
|---|---|---|---|---|---|---|
| `Button` | Action | 45 | Type · Size · State · Show Leading/Trailing Icon | 58회 | Primary는 화면당 하나. 높이 56·48·40 고정, 폭 Fill. Label 텍스트 속성 없음 — 문구는 화면마다 직접 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=291-670) |
| `Icon Button` | Action | 12 | Style · Size | 89회 | 상자가 아이콘 크기를 정한다 — L 48/24 · M 40/20 · S 32/18 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=298-715) |
| `Chip` | Action | 4 | Type · State | 52회 | 반경 full. 선택 시 면 대신 테두리·글자가 진해짐(체크 표시 제거 피드백 미결). 2026-10-15 `Type` 속성 추가: `Select`(기존 3상태) · `Info`(누르지 않는 정보 칩, 24 높이 · primary/800 면 · 흰 11 Bold, 2026-10-15 게시) | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=311-823) |
| `Checkbox` | Action | 3 | State | 34회 | 반경 8. 편집 모드 다중 선택 전용 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=311-851) |
| `Toggle` | Action | 2 | State | 3회 | 설정 화면 전용. 켜짐을 색만으로 알리지 않음 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=334-1155) |
| `Text Field` | Input | 5 | State | 11회 | 높이 HUG. 오류는 색 + 문구. 본문 Body/L | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=295-692) |
| `Text Area` | Input | 3 | Show Mic · State | — | 진료 후 메모·추가 질문. 글자 수 표시(0/300) | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=334-1102) |
| `Search Field` | Input | 2 | Value · State | — | 한 줄이 정상이라 56 고정인 예외 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=590-1307) |
| `Severity Slider` | Input | 5 | Show NRS · Level | 1회 | 5단계 스냅. 드래그와 정지점 탭 둘 다 동작 필수 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=339-1293) |
| `Card` | Content | 3 | Emphasis | 9회 | 테두리 대신 Elevation/Card. 반경 20 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=293-656) |
| `KV Row` | Content | 4 | Type | — | 키 폭 고정 → 값 세로 정렬. Emphasis는 카드당 하나 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=333-1100) |
| `List Row` | Content | 3 | Show Summary · Show Select · Type | 50회 | 폭 320 기본. 편집 모드에서 체크박스 슬롯 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=335-1114) |
| `Section Header` | Content | 1 | — | 73회 | 헤더가 위 여백을 가짐 → Content 상단 8 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=334-1156) |
| `Divider` | Content | 1 | — | 26회 | 목록 구분 전용. 상태를 선만으로 전달 금지 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=333-1088) |
| `Badge` | Content | 5 | Tone | 7회 | 반경 8. 상태 이름을 글자로 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=311-834) |
| `Avatar` | Content | 2 | Type | — | 원형 44 고정 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=311-839) |
| `Date Cell` | Content | 4 | State | 120회 | 44 고정. 오늘 = 옅은 면, 선택 = 채움 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=335-1188) |
| `Severity Readout` | Content | 5 | Level | 8회 | 판독 전용. **severity 색은 여기에만**. NRS 등가 값의 출처 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=333-1126) |
| `Bubble` | Content | 2 | Sender (Patient · AI) | 12회 | AI는 면색 없이 라벨만. 색은 환자의 말에만(D4·P5) | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=313-999) |
| `Callout` | Content | — | — | — | 브리핑 카드 '묻고 싶은 것' 전용. 카드 최강 블록(D5) | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=293-657) |
| `Progress Indicator` | Content | 4 | Step | — | 증상 정리 4단계. 끝이 보이게 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=334-1139) |
| `Onboarding Progress` | Content | 4 | Step | 4회 | 온보딩 4장 전용 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=1155-854) |
| `Notice` | Feedback | 4 | Tone | — | 알러지 경고는 Warning | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=292-668) |
| `Dialog` | Feedback | — | — | 5회 | 되돌릴 수 없는 확인에만. 버튼 취소 · 실행 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=312-845) |
| `Tooltip` | Feedback | 2 | State | — | 한 번만 필요한 설명은 접어 둔다(ⓘ) | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=542-1295) |
| `Tooltip Bubble` | Feedback | — | — | — | Tooltip의 말풍선 본체 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=575-1287) |
| `Overlay Scrim` | Overlay | — | — | — | 바텀시트·다이얼로그 뒤 50% | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=335-1189) |
| `Empty State` | Feedback | 4 | Type | — | 빈 화면은 사과가 아니라 초대 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=335-1166) |
| `Nav Bar` | Navigation | 7 | Action · Leading · Trailing | — | 56 고정 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=298-739) |
| `Tab Bar` | Navigation | 4 | Active | — | 3탭(캘린더·홈·기록). 활성은 채움형 아이콘 전환 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=319-1026) |
| `Logo Symbol` | Brand | 5 | Style | — | 심볼 5변이 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=351-1281) |
| `Logo Lockup` | Brand | 3 | Style | — | 워드마크 조합 3변이(Mono Light 포함) | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=351-1303) |
| `Social Login Button` | Brand | 4 | Provider | — | 각 사 공식 규격 — 색·크기 임의 변경 금지 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=383-1302) |
| `Social Login Stack` | Brand | — | — | — | 현재 카카오 단독 | [열기](https://www.figma.com/design/OWKWtDt8lLQLmltq0ztT5O/AX-%ED%95%B4%EC%BB%A4%ED%86%A4---3%EC%B0%A8-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8?node-id=383-1303) |

### 6.1 범주 원칙
| 범주 | 원칙 |
|---|---|
| Action | 누르면 일이 일어나는 것. **브랜드 채움은 여기에만** |
| Input | 환자가 자기 말을 넣는 자리. 본문 Body/L |
| Content | 읽는 것. 유색 큰 면은 환자의 말에만 |
| Patient · AI | 환자의 말과 AI 정리를 섞지 않는다 |
| Feedback · Overlay | 남는 것과 사라지는 것을 구분 |

### 6.2 Figma에 없고 코드에만 있는 것 (Android `core/designsystem/component`)
`Bottom Sheet` · `Bottom CTA Bar` · `Source Quote` · `Quote Block` · `Voice Input` · `Segmented Control` · `Severity Inputs(Scale/Select)` · `Toast` · `Fab` · `Hospital Card` · `Doctor Card` · `Todo Row` · `Card Pick` · `Picker Field` · `Select Bar` · `Add Row` · `Loading`
- 일부는 Figma **Archive**(미사용 15 · v2 9)에 보관돼 있다. 시안에 쓰기 전 Archive에서 원형을 확인하고, 게시 컴포넌트로 승격이 필요하면 **제안 → 컨펌**.
- Toast는 라이브러리에서 제외 결정(피드백: "토스트 안 쓸 듯").
- 리디자인 지장 선택에 쓸 `Severity Select`(세로 카드)는 코드 `SeverityInputs.kt`에 있다. Figma 게시 여부 확인 필요.

### 6.3 추가 · 2026-10-15 게시 완료 (V3 작업에서 새로 만든 것)
Figma `디자인시스템 · 최종` 페이지의 섹션 **"추가 · 새 컴포넌트 · 시스템 (2026-10-15 게시 완료)"** `3610:1849`에 한곳으로 모았다. 맨 위 "추가 · 2026-10-15 게시 완료 목록" 표 `3611:2032`가 정본 목록이다. 2026-10-15 사용자가 라이브러리에 게시했다(96개 변경 중 90개 — 수정 중 3 · 브랜딩 로고 복사본 2 · 작업실 카카오 로그인 제외). 이후 새로 만드는 컴포넌트·스타일도 이 섹션의 맞는 묶음에 넣고 목록에 한 줄 추가한다. 이름은 `[제안]` 없이 컴포넌트 이름 그대로 쓴다(2026-10-15 사용자 결정). 게시는 사용자가 한다. 개발 기준 정리본은 `design.md` v4.0.

2026-10-15 게시로 올라간 것: 컴포넌트 21종 · 색 변수 3 · 효과 스타일 3 · 텍스트 스타일 1, 기존 `Chip` 세트의 `Type=Info` 변형.

| 묶음 | 이름 | 종류 | 쓰는 곳 (10-15 최종 화면) |
|---|---|---|---|
| 01 아이콘 · 스타일 | `Icon/export` | 아이콘 24 | 1e-1 카드 머리글 공유(Icon Button Ghost S 안, 18) · 1e-1-S · 1e-1-X · 1e-1-T |
| 01 | `Elevation/Primary` | 효과 스타일(§4.2) | 홈 증상 정리 CTA(Card / Action) · 1n-1 · 1n-2 계열 |
| 01 | `Shadow/On Image · Text` · `Shadow/On Image · Icon` | 효과 스타일(§4.2) | Home Banner 사진 위 로고·제목·설명 / 알림 아이콘 |
| 01 | `Label/XS` | 텍스트 스타일(§2) | Chip Type=Info · Card / Resume 진행 숫자 |
| 01 | `bg/on-dark` · `border/on-dark` · `bg/primary-strong` | 색 변수(§1.2) | 배너 아바타·링크 구분선 · Card / Action 아이콘 원 · Chip Info 면 |
| 02 홈 | `Home Banner` | 세트 · Banner(1 오늘의 한 줄 · 2 예약 문자 · 3 일정 알림 · 4 이미지 저장 · 5 헤이 메이트) × CTA(Link · None), 6변형 · 단계별 사진 포함 | 1n-1 · 1n-1-4 · 1n-1-5 · 1n-1-B2~B5 · 1n-1-M~M3(Link), 1n-2 첫 홈(None, 334). 헤더는 1n-1과 같게(좌우 20, 로고 114×30), 흰색만 다름 |
| 02 | `Link / On Image` · `Button / Pill Arrow` · `Pager / Glass` | 컴포넌트 3 | 배너 안 링크·버튼·장 수. Link 글자·화살표 `neutral/0` |
| 02 | `Card / Action` · `Card / Resume` · `Card / Schedule` | 컴포넌트 3 | 홈 증상 정리 CTA · 이어서 하기 · 다가오는 일정 |
| 03 메이트 · 음성 | `Mate Card` | 세트 · 4상태 | 1n-1-M · 1n-1-M2 · 1n-1-M3 · 1r-1-M4 |
| 03 | `Voice Input Mate` | 세트 · Listening | 1p-T 진료 후 메모 듣는 중 |
| 03 | `Waveform` | 세트 · L · 말하는 중 | Voice Input Mate 안 |
| 04 브리핑 카드 · 내보내기 | `Card Thumbnail / Long Image` · `Export Preview / Long Image` | 컴포넌트 2 | 1e-1-X 이미지로 내보내기 시트 미리보기 |
| 04 | `Chip` · `Type=Info` | 기존 Chip 세트 변형(Design System 섹션) | 미리보기 "긴 이미지 1장" |
| 05 온보딩 · 일러스트 | `Onboarding Illustration` | 세트 · Step 1/4~4/4(이미지 원래 크기·비율) | 온보딩 V2-00~03 일러스트 칸(320×340) |
| 06 증상 문답 | `Number Badge` | 세트 · Primary · Subtle(글자 속성 Number) | Question Row · Step Row 안 |
| 06 | `Question Row` | 세트 · Default · Selected · Writing · Add · Focus(글자 속성 Question) | 1c-D ~ D4 질문 고르기 |
| 07 설정 · 알림 | `Setting Row` | 세트 · Toggle · Navigation · Value(Title · Description · Value · Show Value) | 1s-1 내 정보 · 1s-3 알림 시간 |
| 07 | `Time Wheel` | 컴포넌트 284×240 · 5줄 | 1s-3 알림 시간 |
| 07 | `Hint Row` | 컴포넌트 · 아이콘 교체 · 글자 속성 Label | 1r-4 · 1r-4-S 문자 안내(share) · 1s-3 알림 예시(bell) |
| 08 일정 추가 | `Step Row` · `Illustration / Share Sheet` | 컴포넌트 2 | 1r-4-S 예약 문자 방법 안내 시트 |

- 삭제(반영 섹션에서 안 씀, 2026-10-15): Home Banner 변형 Pill · Button · Surface · Row, `Pager / Dots`, `Button / Surface`, `Banner Row`, `Illustration / Card Slot`, `Empty State / Card Slot`, Voice Input Mate `Denied`, Waveform `M·듣는 중` · `M·말하는 중` · `L·듣는 중`. V3 페이지(CTA=Pill 5개)와 브랜딩 페이지(Waveform M 3개)에 남은 인스턴스는 원본 없음 상태.
- 단일 `Home Banner`는 세트의 Banner=2 변형으로 합치고 삭제. 세트의 Title · Body 글자 속성은 변형마다 기본 문구가 달라 제거(문구는 인스턴스에서 직접 고침).
- 1s-1 "휴대폰 알림이 꺼져 있어요"는 알러지 정보와 같은 DS `Notice` / `Tone=Warning` 인스턴스로 씀(새 변형 없음).
- 게시 준비 때 기존 `Empty State` 세트의 변형 이름 겹침(`Type=Offline` 2개)을 고쳤다: 마이크 못 씀 변형 → `Type=MicDenied`. 인스턴스 0개라 영향 없음.
- 게시 전 마지막 점검(2026-10-15): 21종 안의 색·글자·효과가 모두 토큰·스타일에 연결됐다. 고친 곳 13: Home Banner 6변형 로고 그림자(0.82배로 줄어 스타일이 풀려 있던 것) → `Shadow/On Image · Text` 연결, Home Banner 안 Link / On Image 5개 선 흰 28%(직접 값) → `border/on-dark`(32%, 원본과 같게), Card Thumbnail 글자 2개 Inter → Pretendard Variable. 썸네일 글자(9.5 · 8.1px)는 축소 미리보기라 텍스트 스타일 없이 둔다(가장 작은 스타일 11px).
- 변경분 있던 기존 컴포넌트 49종(예: `Chip` Type 속성 추가)과 기존 텍스트 스타일 변경분도 같이 게시했다.
- 같은 페이지 `수정 중` 섹션의 `Icon/briefing-cards` · `Tab Bar / Briefing` · `streamline-color:cards`는 사용자가 작업 중이라 손대지 않았고, 이번 게시에서 뺐다.

---

## 7. 통증 5단계 (Severity) — 단일 정의

Slider · Scale · Select · Readout 네 컴포넌트가 같은 정의를 쓴다(Android `MedicalMateSeverity.kt`).

| 단계 | 라벨 | 서술 | NRS 등가 | base | tint |
|---|---|---|---|---|---|
| 1 | 조금 불편해요 | 신경 쓰이지만 하던 일은 계속할 수 있어요 | 1–2 | `severity/1` | `severity/1-tint` |
| 2 | 은근히 아파요 | 자꾸 생각나고 집중이 잘 안 돼요 | 3–4 | `severity/2` | `severity/2-tint` |
| 3 | 꽤 아파요 | 하던 일을 멈추게 될 때가 있어요 | 5–6 | `severity/3` | `severity/3-tint` |
| 4 | 많이 아파요 | 일상생활이 어렵고 참기 힘들어요 | 7–8 | `severity/4` | `severity/4-tint` |
| 5 | 견디기 힘들어요 | 잠도 못 자고 아무것도 못 하겠어요 | 9–10 | `severity/5` | `severity/5-tint` |

- 양끝 라벨: "가벼운 불편" · "매우 심함"
- 안내: "숫자는 진료실에서 의사가 읽어요 / 환자분은 낱말로 고르시면 돼요. 카드에는 NRS 등가로 함께 표시됩니다."
- NRS 등가는 Figma Readout 마스터(`333:1126`) 값. **임상 검증 전이고 AI 계약상 값은 1~5 서열척도**다. 리디자인에서 "참고" 표기로 강등 제안(PRD §9.4).

---

## 8. 로고 (07 보드)

- 심볼: 두 개의 둥근 사각 조각이 맞물린 형태(대화·연결). `Logo Symbol` 5변이
- 락업: 심볼 + 워드마크 "진료메이트" `Logo Lockup` 3변이 — 스플래시·발표 표지는 primary 면 위 Mono Light
- 금지: 십자 · 알약 · 체온계 · 웃는 얼굴 캐릭터

---

## 9. 접근성

| 항목 | 기준 |
|---|---|
| 텍스트 대비 | 4.5:1 (큰 글자 3:1) — WCAG 2.1 AA |
| 비텍스트 대비 | 3:1 (아이콘·경계·컨트롤 상태). **Lighthouse는 안 본다 → 직접 계산** |
| 터치 영역 | 최소 48×48. 작은 컨트롤은 부모에서 영역 확보 |
| 색 단독 금지 | 색 + 형태·낱말 |
| 폰트 배율 | 130%까지 레이아웃 유지(HUG) |
| 플레이스홀더 | `fg/subtle`(6.36) — 유일한 안내라 장식 아님 |
| 예외 | 카카오 버튼은 공식 규격(변경 불가). 주변 안내는 반드시 기준 통과 |

---

## 10. 구현 매핑

### 10.1 Android (Kotlin · Compose) — `com.mist.medicalmate.core.designsystem`
| Figma | 코드 |
|---|---|
| Palette | `Palette.kt` |
| Semantic | `MedicalMateColors.kt` · `BrandColor.kt` |
| Typography | `Type.kt` |
| Scale · Layout | `Dimens.kt` · `MedicalMateRadius.kt` |
| Elevation | `Elevation.kt` · `MedicalMateSurfaceStyle.kt` |
| Severity | `MedicalMateSeverity.kt` |
| Icons · Logo | `MedicalMateIcons.kt` · `MedicalMateLogo.kt` |
| 컴포넌트 | `component/*.kt` (52종) |

### 10.2 HTML 시안 · 아티팩트용 CSS 변수
Claude 아티팩트(발행 페이지)는 외부 폰트가 **Google Fonts만** 허용된다. Pretendard는 Google Fonts에 없으므로 `Noto Sans KR`로 대체하고 폴백 스택에 Pretendard를 둔다(로컬 설치 환경에서는 Pretendard로 렌더).

```css
:root {
  /* Semantic — 화면은 이것만 */
  --bg-canvas:#F5F6FA; --bg-surface:#FFFFFF; --bg-subtle:#EDEFF5;
  --bg-primary:#5566D2; --bg-primary-pressed:#3B4FC0; --bg-primary-subtle:#E3E7FC; --bg-primary-faint:#F2F4FE;
  --bg-info:#F2F4FE; --bg-success:#E4F7ED; --bg-warning:#FFF4D6; --bg-danger:#FFEDEB;
  --bg-inverse:#131722; --bg-inverse-soft:#3A4053;
  --fg-default:#131722; --fg-subtle:#585F73; --fg-muted:#7C8397; --fg-disabled:#C6CAD8;
  --fg-on-primary:#FFFFFF; --fg-primary:#2E3E9E; --fg-link:#2E3E9E;
  --fg-success:#0E7A4A; --fg-warning:#8A5A0B; --fg-danger:#C4302B;
  --border-subtle:#DEE1EB; --border-default:#C6CAD8; --border-strong:#7C8397; --border-focus:#5566D2;
  --severity-1:#FFE3A8; --severity-2:#FFC79B; --severity-3:#FFA894; --severity-4:#F58079; --severity-5:#DC5A55;
  --severity-1-tint:#FFF6E4; --severity-2-tint:#FFEFE4; --severity-3-tint:#FFE9E3; --severity-4-tint:#FDE4E2; --severity-5-tint:#F9DEDD;
  --kakao-bg:#FEE500;
  /* Shape */
  --radius-xs:8px; --radius-sm:12px; --radius-md:16px; --radius-lg:20px; --radius-xl:24px; --radius-2xl:28px; --radius-full:999px;
  /* Elevation (brand tint) */
  --elev-card:0 3px 10px rgba(27,37,90,.08), 0 1px 2px rgba(27,37,90,.05);
  --elev-float:0 4px 14px rgba(27,37,90,.09), 0 1px 3px rgba(27,37,90,.06);
  --elev-sheet:0 -4px 24px rgba(27,37,90,.10), 0 0 2px rgba(27,37,90,.05);
  /* Layout */
  --screen-w:360px; --screen-h:812px; --gutter:20px; --content-w:320px;
  --nav-h:56px; --tabbar-h:79px; --footer-h:92px; --safe-bottom:24px;
  --content-top:12px; --content-bottom:16px; --block-gap:14px;
  --touch-min:48px; --control-sm:40px; --control-md:48px; --control-lg:56px; --mic:88px;
  /* Type */
  --font: 'Pretendard','Noto Sans KR',-apple-system,'Apple SD Gothic Neo',sans-serif;
}
/* 텍스트 스타일 — font: weight size/line-height; letter-spacing */
.display-m { font:700 30px/40px var(--font); letter-spacing:-.02em }
.heading-l { font:700 24px/34px var(--font); letter-spacing:-.02em }
.heading-m { font:600 20px/28px var(--font); letter-spacing:-.015em }
.heading-s { font:600 17px/24px var(--font); letter-spacing:-.01em }
.body-l    { font:400 17px/26px var(--font) }   .body-l-strong { font:600 17px/26px var(--font) }
.body-m    { font:400 15px/24px var(--font) }   .body-m-strong { font:600 15px/24px var(--font) }
.body-s    { font:400 13px/20px var(--font) }   .body-s-strong { font:600 13px/20px var(--font) }
.label-l   { font:600 15px/20px var(--font) }
.label-m   { font:600 13px/18px var(--font) }
.label-s   { font:500 11px/16px var(--font); letter-spacing:.02em }
.numeric-l { font:700 24px/30px var(--font); letter-spacing:-.015em }
.numeric-m { font:600 17px/22px var(--font); letter-spacing:-.01em }
.ko        { word-break:keep-all; overflow-wrap:anywhere; min-width:0 }
```

### 10.3 Figma Plugin API 함정
| 함정 | 대응 |
|---|---|
| `resize()`가 주축 사이징을 FIXED로 바꿈 | resize 후 `primaryAxisSizingMode='AUTO'` 재설정 |
| fills 직접 수정 무시 | `JSON.parse(JSON.stringify(fills))` → 수정 → 재할당 |
| `createNodeFromSvg`가 evenodd 구멍을 메움 | 서브패스 분리 또는 `figma.subtract()` |
| `createComponentFromNode`가 흰 배경을 붙임 | `fills=[]` · `clipsContent=false` |
| `swapComponent` 후 변수 바인딩 소실 | 원시색 기준 재바인딩 |
| `setBoundVariableForPaint` 반환값 미반영 | 재할당 필수 |
| 인스턴스 child에 absolute·삽입 불가 | 마스터에서 수정 |
| `remove()` 후 속성 접근 throw | remove 전에 참조 수집 |
| 페이지 전환 | 매 스크립트 첫 줄 `await figma.setCurrentPageAsync(page)` |
| 화면 카운트 부풀림 | 필터 `width===360 && height>=700 && /^(1[a-z]\|M\d\|V[12])/` + 부모가 SECTION |
| U+2060 word-joiner | `s.replace(/\u2060/g,'')` 정규화 |
| 게시에서 빼기 | 이름 앞에 `.` |
