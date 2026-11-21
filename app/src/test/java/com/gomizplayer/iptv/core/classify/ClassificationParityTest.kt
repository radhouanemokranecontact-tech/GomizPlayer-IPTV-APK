package com.gomizplayer.iptv.core.classify

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Parity test: reads the shared fixtures (same file used by the PC/JS test) and
 * asserts the Kotlin classifier produces the same kinds.
 * A tiny regex reader is used to avoid adding a JSON dependency to unit tests.
 */
class ClassificationParityTest {

    private data class Case(val name: String, val input: Map<String, String>, val expected: String)

    private fun readFixtures(): List<Case> {
        val text = javaClass.classLoader.getResourceAsStream("classification-fixtures.json")
            ?.bufferedReader()?.readText()
            ?: throw AssertionError("classification-fixtures.json not found in test resources")
        // Each case is one line: { "name": "...", "input": { ... }, "expected": "..." }
        return text.lines().mapNotNull { line ->
            if (!line.trim().startsWith("{ \"name\"")) return@mapNotNull null
            val name = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: return@mapNotNull null
            val expected = Regex("\"kotlinExpected\"\\s*:\\s*\"([^\"]+)\"").find(line)?.groupValues?.get(1)
                ?: Regex("\"expected\"\\s*:\\s*\"([^\"]+)\"").find(line)?.groupValues?.get(1)
                ?: return@mapNotNull null
            val inputBody = Regex("\"input\"\\s*:\\s*\\{([^}]*)\\}").find(line)?.groupValues?.get(1) ?: ""
            val input = Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"").findAll(inputBody)
                .associate { it.groupValues[1] to it.groupValues[2] }
            Case(name, input, expected)
        }
    }

    @Test
    fun kotlinMatchesSharedFixtures() {
        val cases = readFixtures()
        assert(cases.isNotEmpty()) { "No parity cases loaded" }
        cases.forEach { c ->
            val kind = Classification.classifyMedia(
                MediaMeta(
                    url = c.input["url"].orEmpty(),
                    name = c.input["name"].orEmpty(),
                    group = c.input["group"].orEmpty(),
                    type = c.input["type"].orEmpty(),
                    kind = c.input["kind"].orEmpty()
                )
            )
            assertEquals("Parity failed for ${c.name}", c.expected.uppercase(), kind.name)
        }
    }
}
