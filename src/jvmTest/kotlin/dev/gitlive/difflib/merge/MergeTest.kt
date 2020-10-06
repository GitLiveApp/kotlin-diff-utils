package dev.gitlive.difflib.merge

import org.junit.Assert.*
import org.junit.Test


class MergeTest {
    @Test
    fun threeWayDongTest() {
        val base =  "touch\nmy\ndong"
        val left =  "touch\nmy\ndong"
        val right = "touch\nmy\ndong"
        val merged = merge(left, base, right)
        assertFalse(merged.conflict)
        assertEquals(merged.joinedResults(), listOf("touch\n, my\n, dong"))
    }

    @Test
    fun simpleMergeTest() {
        val merged = merge("foo", "foo", "bar")
        assertFalse(merged.conflict)
        assertEquals(merged.joinedResults(), listOf("bar"))
    }

    @Test
    fun mergeConflictTest() {
        val merged = merge("foo", "bar", "baz")
        assertTrue(merged.conflict)
    }

    @Test
    fun twoDifferentMergesTest() {
        val base =      listOf(1,2,3,4,5,6).joinToString("\n")
        val left =      listOf(2,3,4,5,6).joinToString("\n")
        val right=      listOf(1,2,3,4,9,6).joinToString("\n")
        val expected=   listOf(2,3,4,9,6).joinToString("\n, ")

        val merged = merge(left, base, right)
        assertFalse(merged.conflict)
        assertEquals((merged.joinedResults() as List<*>).first(), expected)
    }
}