package com.mist.medicalmate.intake.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AI 트랙이 넘긴 검증 벡터로 검색 규칙을 확인한다.
 *
 * 165케이스이고 **순서까지** 맞아야 한다. 이름·별칭 156개 전부와 문장 3개, 여러 앵커에
 * 걸치는 질의, 0건 케이스가 들어 있다.
 *
 * 이 시험이 있는 이유는 같은 규칙이 서버에도 있기 때문이다. 앱이 로컬로 찾는 것은 한
 * 글자마다 왕복하지 않으려는 것이고, 그렇다고 결과가 갈리면 안 된다. 규칙이 바뀌면 AI 쪽이
 * 벡터를 다시 만들어 주고, 파일이 그대로면 앱도 고칠 것이 없다.
 */
class BodyPartSearchTest {
    @Test
    fun `벡터 165개가 순서까지 같다`() {
        val failures =
            vectors().mapNotNull { case ->
                val actual = searchBodyParts(case.query).map { "${it.id}|${it.matched}|${it.score}" }
                val expected = case.expected
                if (actual == expected) null else "q=${case.query}\n  기대 $expected\n  실제 $actual"
            }

        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `케이스가 165개다`() {
        // 묶음이 바뀌면 개수도 바뀐다. 파일만 갈아 끼우고 지나가지 않게 세어 둔다.
        assertEquals(165, vectors().size)
    }

    @Test
    fun `벡터가 우리 온톨로지와 같은 스냅샷이다`() {
        // 이름이나 별칭이 달라진 채로 벡터만 새것이면 전부 어긋난다. 그때 무엇이 어긋났는지
        // 알 수 있게 스냅샷을 함께 본다.
        assertEquals("f848848baea4", root().getValue("ontology_snapshot").jsonPrimitive.content)
    }

    @Test
    fun `상한이 여덟이다`() {
        assertEquals(8, root().getValue("limit").jsonPrimitive.int)
        assertTrue(vectors().all { it.expected.size <= 8 })
    }

    @Test
    fun `증상이나 오타는 걸리지 않는다`() {
        // 데이터에 부위 이름만 있어 저절로 그렇게 된다. 그 전제가 깨지면 여기서 잡힌다.
        assertEquals(emptyList<BodyPartMatch>(), searchBodyParts("감기"))
        assertEquals(emptyList<BodyPartMatch>(), searchBodyParts("무릅"))
        assertEquals(emptyList<BodyPartMatch>(), searchBodyParts("   "))
        assertEquals(emptyList<BodyPartMatch>(), searchBodyParts(""))
    }

    @Test
    fun `한글 조합 중간 상태는 0건이다`() {
        // 화면이 "0건이면 직전 결과를 유지"해야 하는 이유다. 그냥 그리면 후보가 깜빡인다.
        assertTrue(searchBodyParts("옆").isNotEmpty())
        assertEquals(emptyList<BodyPartMatch>(), searchBodyParts("옆ㄱ"))
        assertTrue(searchBodyParts("옆구").isNotEmpty())
    }

    @Test
    fun `띄어쓰기와 구분 기호를 무시한다`() {
        val spaced = searchBodyParts("허리옆").map { it.id }

        assertEquals(searchBodyParts("허리 옆").map { it.id }, spaced)
        assertTrue("SUR:042" in spaced)
    }

    @Test
    fun `긴 질의는 거절하지 않고 300자에서 자른다`() {
        // 상한 뒤의 부위 이름은 걸리지 않는다. 상한 안에 있으면 걸린다.
        assertEquals(emptyList<BodyPartMatch>(), searchBodyParts("가".repeat(320) + "무릎"))
        assertTrue(searchBodyParts("가".repeat(200) + "무릎").isNotEmpty())
    }

    private fun root(): JsonObject = json.decodeFromString<JsonObject>(
        requireNotNull(javaClass.getResourceAsStream(VECTORS)) { "$VECTORS 없음" }
            .bufferedReader()
            .readText(),
    )

    private fun vectors(): List<SearchCase> = root().getValue("cases").jsonArray.map { case ->
        SearchCase(
            query = case.jsonObject.getValue("q").jsonPrimitive.content,
            expected =
            case.jsonObject.getValue("expect").jsonArray.map { hit ->
                val o = hit.jsonObject
                "${o.getValue("id").jsonPrimitive.content}|" +
                    "${o.getValue("matched").jsonPrimitive.content}|" +
                    "${o.getValue("score").jsonPrimitive.int}"
            },
        )
    }

    private data class SearchCase(val query: String, val expected: List<String>)

    private companion object {
        const val VECTORS = "/ontology/body-search-vectors.json"

        val json = Json { ignoreUnknownKeys = true }
    }
}
