package com.mist.medicalmate.intake.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 목록 검색의 상태 규칙.
 *
 * 찾는 규칙 자체는 `BodyPartSearchTest`가 벡터로 지킨다. 여기서 보는 것은 그 결과를 화면
 * 상태로 어떻게 들고 있느냐다.
 */
class BodyMapSearchStateTest {
    private var state = BodyMapUiState(byList = true)

    private val actions = BodyMapActions { change -> state = change(state) }

    @Test
    fun `검색어를 넣으면 결과가 들어온다`() {
        actions.onSearchChange("무릎")

        assertTrue(state.searching)
        assertEquals(listOf("SUR:091"), state.searchResults.map { it.id })
    }

    @Test
    fun `앵커가 걸리면 그 구역이 딸려 온다`() {
        actions.onSearchChange("다리")

        assertEquals(
            listOf("ANC:014", "SUR:090", "SUR:091", "SUR:097", "SUR:101", "SUR:102"),
            state.searchResults.map { it.id },
        )
    }

    @Test
    fun `한글 조합 중간 상태에서 결과를 지우지 않는다`() {
        // "옆 → 옆ㄱ → 옆구"로 치는 동안 0건이 한 번 지나간다. 그대로 그리면 목록이 깜빡인다.
        actions.onSearchChange("옆")
        val found = state.searchResults

        actions.onSearchChange("옆ㄱ")

        assertEquals(found, state.searchResults)
        assertEquals("옆ㄱ", state.search)
    }

    @Test
    fun `검색어를 비우면 결과도 비운다`() {
        // 못 찾은 것이 아니라 지운 것이다.
        actions.onSearchChange("무릎")

        actions.onSearchChange("")

        assertEquals(emptyList<BodyPartMatch>(), state.searchResults)
        assertFalse(state.searching)
    }

    @Test
    fun `한 곳만 골라진다`() {
        // 검색 결과에서도 고르는 것은 하나다. 다른 줄을 누르면 앞서 고른 것을 대신한다.
        actions.onSearchChange("무릎")
        val left = BodyMapSelection("ANC:014", "SUR:091", BodyMapSide.LEFT)
        val right = BodyMapSelection("ANC:014", "SUR:091", BodyMapSide.RIGHT)

        actions.onPartSelect(left)
        assertEquals(left, state.selection)

        actions.onPartSelect(right)
        assertEquals(right, state.selection)
    }

    @Test
    fun `고른 뒤에도 결과가 남는다`() {
        // 고른 줄에 표시가 켜지는 것을 봐야 한다. 결과가 사라지면 골랐는지 알 수 없다.
        actions.onSearchChange("무릎")

        actions.onPartSelect(BodyMapSelection("ANC:014", "SUR:091", BodyMapSide.LEFT))

        assertTrue(state.searching)
        assertEquals(listOf("SUR:091"), state.searchResults.map { it.id })
    }

    @Test
    fun `검색 결과에서 앵커를 누르면 검색을 닫고 그 구역 목록으로 간다`() {
        // 검색어가 남아 있으면 화면이 계속 결과를 그려서 누른 것이 아무 일도 안 한 것처럼 보인다.
        actions.onSearchChange("다리")

        actions.onAnchorFocus(BodyMapSelection("ANC:014", side = BodyMapSide.LEFT))

        assertFalse(state.searching)
        assertEquals("", state.search)
        assertTrue(state.pickingZone)
    }

    @Test
    fun `인체도로 돌아가면 검색이 닫힌다`() {
        actions.onSearchChange("무릎")

        actions.onListModeToggle()

        assertEquals("", state.search)
        assertEquals(emptyList<BodyPartMatch>(), state.searchResults)
    }

    @Test
    fun `좌우가 갈리는 구역은 두 갈래가 된다`() {
        // 팔·다리는 좌우가 앵커에서 갈려서 구역 점이 기준점 하나뿐이다. 검색은 그 단계를
        // 건너뛰므로 양쪽을 다 만들어 준다.
        val knee = bodyMapChoicesForZone("ANC:014", "SUR:091")

        assertEquals(listOf(BodyMapSide.LEFT, BodyMapSide.RIGHT), knee.map { it.side })
    }

    @Test
    fun `좌우가 없는 구역은 한 갈래다`() {
        // 가슴 가운데는 점이 하나다. 같은 배 안에서도 아랫배는 좌우 두 점이라 두 갈래가 된다.
        assertEquals(listOf(BodyMapSide.CENTER), bodyMapChoicesForZone("ANC:003", "SUR:021").map { it.side })
        assertEquals(2, bodyMapChoicesForZone("ANC:004", "SUR:032").size)
    }

    @Test
    fun `구역으로 앵커를 찾는다`() {
        assertEquals("ANC:014", bodyMapAnchorIdOfZone("SUR:091"))
        // 앵커 id를 넣으면 없다. 검색 결과가 앵커인지 구역인지 이 값으로 가른다.
        assertEquals(null, bodyMapAnchorIdOfZone("ANC:014"))
    }
}
