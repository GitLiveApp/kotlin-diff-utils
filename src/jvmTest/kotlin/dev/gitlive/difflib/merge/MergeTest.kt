package dev.gitlive.difflib.merge

import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Test


class MergeTest {
    @Test
    fun test() {
//        val base =     listOf(1, 2, 3, 4, 5, 6).joinToString("\n");
//        val left =     listOf(1, 2, 3, 4, 5, 6).joinToString("\n");
//        val right =    listOf(1, 2, 3, 4, 9, 6).joinToString("\n");
//
        val base = "touch\nmy\ndong"
        val left = "touch\nmy\ndong"
        val right = "touch\nmy\ndong"
        val merged = merge(left, base, right);

        println(merged.joinedResults())
        println(merged.conflict)
    }
}