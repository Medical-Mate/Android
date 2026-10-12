package com.mist.medicalmate.calendar.share

/**
 * 문자에 적힌 병원 이름(#270).
 *
 * 예약 문자는 대개 머리에 `[서울OO병원]`처럼 보낸 곳을 적는다. 대괄호 안을 먼저 보고, 없으면
 * 본문에서 찾는다. `[Web발신]`·`[광고]`처럼 병원이 아닌 머리는 이름 꼴이 아니라 건너뛴다.
 *
 * **과 이름으로 끝나는 의원도 병원이다.** 동네 의원은 `튼튼정형외과`·`밝은안과`처럼 `의원`을
 * 떼고 부르는 일이 흔하다. 다만 과 이름만 홀로 선 것(`정형외과 진료 예약`)은 어느 병원인지가
 * 아니라 어느 과인지라 건너뛴다.
 *
 * **띄어 쓴 이름(`강남 세브란스 병원`)은 붙여 쓴 이름을 못 찾았을 때만 본다.** 띄어쓰기를 받으면
 * `예약하신 병원`·`해당 병원` 같은 말이 이름으로 잡히기 쉽다. 규칙은 [spacedHospitalIn]에 있다.
 *
 * 진료과가 바로 뒤에 붙어 있으면 함께 든다. 같은 병원의 다른 과는 다른 일정이다.
 *
 * **보이지 않는 공백을 먼저 고른다.** 문자 앱이 띄어쓰기 자리에 줄바꿈 없는 공백(U+00A0)이나 전각
 * 공백을 넣어 보내고, 폭 없는 문자를 끼우기도 한다. 정규식의 `\s`는 그것을 공백으로 보지 않아서
 * `한국튼튼 정형외과`가 한 낱말로 붙는다.
 */
internal fun hospitalIn(text: String): String? = hospitalInPlain(text.replace(INVISIBLE, "").replace(ODD_SPACES, " "))

private fun hospitalInPlain(text: String): String? {
    val bracketed = BRACKET.findAll(text).map { it.groupValues[1] }.toList()
    return bracketed.firstNotNullOfOrNull(::joinedHospitalIn)
        ?: bracketed.firstNotNullOfOrNull { spacedHospitalIn(it, header = true) }
        ?: joinedHospitalIn(text)
        ?: spacedHospitalIn(text, header = false)
}

/** 붙여 쓴 병원 이름. 과 이름만 홀로 선 것은 건너뛰고 다음을 본다. */
private fun joinedHospitalIn(part: String): String? = HOSPITAL.findAll(part)
    .firstOrNull { !BARE_DEPARTMENT.matches(it.groupValues[1]) }
    ?.value
    ?.trim()

/**
 * 띄어 쓴 병원 이름. `병원`·`의원`·진료과가 한 낱말로 서고 그 앞에 이름 같은 낱말이 붙은 것이다.
 *
 * 앞말은 두 낱말까지만 받는다(`강남 세브란스 병원`). 그보다 길면 인사말이나 안내 문구까지 딸려 온다.
 * 다음 앞말에서 멈춘다.
 *
 * - 이름 글자가 아닌 것이 섞인 낱말(`10/15`·`안내]`). 앞에 괄호가 붙은 낱말(`[강남`)은 괄호를 떼고
 *   받되 거기서 멈춘다 — 이름의 시작이다.
 * - 숫자 낱말(`2시`·`15일`), 가리키는 말(`해당`·`저희`·`다른`), 안내 문구가 든 말(`예약`·`안내`),
 *   사람을 부르는 말(`홍길동님`).
 * - 본문에서는 꾸밈 끝말(`예약하신`·`가까운`의 `신`·`운` 같은 것)도 멈춘다. 머리([header])에서는
 *   받는다 — 머리는 보낸 곳의 이름이라 `[밝은 안과]`의 `밝은`이 이름이다.
 */
private fun spacedHospitalIn(part: String, header: Boolean): String? {
    val words = part.trim().split(SPACES)
    return words.indices.firstNotNullOfOrNull { i ->
        val suffix = SUFFIX_WORD.find(words[i])?.groupValues?.get(1)
        val name = suffix?.let { nameBefore(words, i, header) }
        name?.let {
            val department = words.getOrNull(i + 1)?.takeIf { TRAILING_DEPARTMENT.matches(it) }
            listOfNotNull(name, suffix, department).joinToString(" ")
        }
    }
}

/** [index] 낱말 앞의 이름. 이름이 아닌 낱말이나 괄호가 붙은 낱말에서 멈춘다. */
private fun nameBefore(words: List<String>, index: Int, header: Boolean): String? {
    val name = ArrayDeque<String>()
    var j = index - 1
    var open = true
    while (open && j >= 0 && name.size < MAX_NAME_WORDS) {
        val raw = words[j]
        val word = raw.trimStart { !NAME_CHAR.matches(it.toString()) }
        open = NAME_WORD.matches(word) && !isNotName(word, header)
        if (open) {
            name.addFirst(word)
            // 앞에 괄호가 붙어 있었다. 이름의 시작이다.
            open = word.length == raw.length
        }
        j--
    }
    return name.joinToString(" ").ifEmpty { null }
}

private fun isNotName(word: String, header: Boolean): Boolean = word in NOT_NAME_WORDS ||
    NOT_NAME_PART.containsMatchIn(word) ||
    word.last() == '님' ||
    (!header && word.last() in MODIFIER_ENDINGS)

/** 띄어 쓴 이름에서 받는 앞말 낱말 수. */
private const val MAX_NAME_WORDS = 2

private val BRACKET = Regex("""[\[【]([^\]】]{2,40})[\]】]""")

private val SPACES = Regex("\\s+")

/** 폭 없는 문자와 줄 나눔 하이픈. 글자 사이에 끼어 있어도 지운다. */
private val INVISIBLE = Regex("[\\u200B-\\u200D\\u2060\\uFEFF\\u00AD]")

/** `\s`가 공백으로 보지 않는 공백들. 줄바꿈 없는 공백, 여러 폭의 공백, 전각 공백이다. */
private val ODD_SPACES = Regex("[\\u00A0\\u1680\\u2000-\\u200A\\u202F\\u205F\\u3000]")

/**
 * 진료과 이름. 병원 이름의 끝, 병원 뒤에 붙는 과, 홀로 선 과를 가려내는 데 함께 쓴다.
 *
 * `~의학과`를 `[가-힣]+의학과`로 묶지 않는다. 그러면 `튼튼재활의학과`가 통째로 과 이름이 되어
 * 홀로 선 과로 버려진다. 큰 병원의 세부 과(`소화기내과`)도 하나씩 적는다 — 빠지면 `서울대 병원
 * 소화기내과`에서 `소화기내과`가 병원 이름으로 잡힌다.
 */
private const val DEPARTMENT =
    "(?:소화기내과|순환기내과|호흡기내과|내분비내과|신장내과|감염내과|혈액종양내과|류마티스내과|알레르기내과" +
        "|정신건강의학과|마취통증의학과|통증의학과|재활의학과|가정의학과|영상의학과|비뇨의학과|응급의학과" +
        "|직업환경의학과|진단검사의학과|핵의학과|방사선종양학과|병리과|한방내과" +
        "|소아청소년과|이비인후과|산부인과|정형외과|신경외과|성형외과|흉부외과|비뇨기과" +
        "|소아과|청소년과|피부과|신경과|내과|외과|안과)"

private const val SUFFIX = "(?:병원|의원|치과|클리닉|의료원|보건소|메디컬센터|$DEPARTMENT)"

/**
 * 병원 이름을 이루는 글자.
 *
 * 한글·영문·숫자에 전각 영문·숫자(`ＯＯ병원`)를 더한다. 가림 글자(`서울○○병원`·`서울**병원`)는
 * 이름 **안에서만** 받는다 — 첫 글자로 받으면 `*튼튼정형외과*`가 `*튼튼정형외과`로 잡힌다.
 * `&`·`·`·`-`는 받지 않는다. 이름 안보다 말과 말 사이에 더 자주 와서(`예약 안내·튼튼정형외과`)
 * 앞말까지 이름으로 붙는다.
 */
private const val NAME_HEAD = "[가-힣A-Za-z0-9Ａ-Ｚａ-ｚ０-９○◯]"
private const val NAME_BODY = "[가-힣A-Za-z0-9Ａ-Ｚａ-ｚ０-９○◯●◎*＊]"

private val HOSPITAL = Regex("($NAME_HEAD$NAME_BODY*$SUFFIX)(?:\\s[가-힣]{0,6}$DEPARTMENT)?")

/** 과 이름만 홀로 선 것. `정형외과의원`도 어느 병원인지가 없다. */
private val BARE_DEPARTMENT = Regex("$DEPARTMENT(?:의원)?")

private val NAME_CHAR = Regex(NAME_HEAD)

private val NAME_WORD = Regex("$NAME_HEAD$NAME_BODY*")

/**
 * 한 낱말로 선 병원 꼬리. 뒤에 조사(`병원에서`)나 문장 부호가 붙어도 된다. 진료과 뒤의 `의원`은
 * 함께 든다(`튼튼 정형외과의원`).
 */
private val SUFFIX_WORD =
    Regex(
        "^((?:$DEPARTMENT)?의원|$SUFFIX)" +
            "(?:$|[^가-힣A-Za-z0-9]|에서|에게|에|은|는|이|가|을|를|의|으로|로|입니다|이에요|예요|과|와|도)",
    )

private val TRAILING_DEPARTMENT = Regex("[가-힣]{0,6}$DEPARTMENT")

private val NOT_NAME_WORDS =
    setOf(
        "해당", "저희", "우리", "본", "타", "그", "이", "저", "다른", "인근", "근처", "동네", "가까운",
        "담당", "지정", "협력", "일반", "전문", "종합", "대학", "큰", "오전", "오후", "오늘", "내일", "모레",
        "및", "또는", "고객", "환자", "안녕하세요",
    )

/** 안내 문구가 든 낱말이나 숫자로 시작하는 낱말(`2시`·`15일`·`2026년`). */
private val NOT_NAME_PART = Regex("예약|안내|알림|문자|발신|접수|진료|^[0-9０-９]")

/** 꾸밈 끝말과 조사. 본문의 `예약하신 병원`·`가까운 병원`·`그 병원`을 거른다. */
private const val MODIFIER_ENDINGS = "은는을를이가의에로와과도만신한할된던운실갈"
