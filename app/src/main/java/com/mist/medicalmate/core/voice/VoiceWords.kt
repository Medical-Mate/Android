package com.mist.medicalmate.core.voice

/**
 * 여러 화면이 함께 쓰는 말 풀이(#282).
 *
 * 받아쓰기가 같은 말을 그때그때 다르게 띄어 적고 끝에 마침표를 붙인다. 비교하기 전에 띄어쓰기와
 * 문장부호를 지운다([compact]).
 */
object VoiceWords {
    /** 띄어쓰기와 문장부호를 지운다. "일정 추가." → "일정추가" */
    fun compact(text: String): String = text.filterNot { it.isWhitespace() || it in PUNCTUATION }

    /** 확인에 "네"라고 했는지. "네, 저장해 줘"도 네다. */
    fun isYes(text: String): Boolean {
        val heard = compact(text)
        return !isNo(text) && YES.any { heard.startsWith(it) || heard == it }
    }

    /** 확인에 "아니요"라고 했는지. */
    fun isNo(text: String): Boolean {
        val heard = compact(text)
        return NO.any { heard.startsWith(it) }
    }

    /** 패널을 닫으라는 말. "그만", "닫아 줘". */
    fun isStop(text: String): Boolean {
        val heard = compact(text)
        return STOP.any { heard.startsWith(it) } && heard.length <= STOP_MAX_LENGTH
    }

    /** 저장하라는 말. "저장해 줘", "등록해", "이대로 완료". */
    fun isSave(text: String): Boolean {
        val heard = compact(text)
        return SAVE.any { it in heard }
    }

    /**
     * 목록에서 몇 번째인지. "첫 번째", "두번째 거", "3번". 0부터 센다. 없으면 null이다.
     *
     * 받아쓰기가 숫자를 글자로도 아라비아 숫자로도 적는다. 둘 다 받는다.
     */
    fun ordinal(text: String): Int? {
        val heard = compact(text)
        val word = ORDINAL_WORDS.indexOfFirst { heard.contains("${it}번째") }
        val digit = DIGIT_ORDINAL.find(heard)?.groupValues?.get(1)?.toInt()
        return when {
            word >= 0 -> word
            digit != null -> digit - 1
            heard.contains("마지막") -> LAST
            else -> null
        }
    }

    /** [ordinal]이 "마지막"을 이 값으로 돌려준다. 쓰는 쪽이 목록 끝으로 바꾼다. */
    const val LAST = -1

    private const val PUNCTUATION = ".,?!~"

    private const val STOP_MAX_LENGTH = 8

    private val YES = listOf("네", "예", "응", "그래", "좋아", "맞아", "저장", "오케이", "확인", "부탁")

    private val NO = listOf("아니", "아뇨", "취소", "싫어", "잠깐", "잠시만")

    private val STOP = listOf("그만", "닫아", "끝", "종료", "됐어")

    private val SAVE = listOf("저장", "등록해", "완료", "이대로")

    private val ORDINAL_WORDS = listOf("첫", "두", "세", "네", "다섯", "여섯", "일곱", "여덟", "아홉", "열")

    private val DIGIT_ORDINAL = Regex("""(\d{1,2})(?:번째|번)""")
}
