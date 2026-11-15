package com.lunary.app.domain.text

import org.junit.Assert.assertEquals
import org.junit.Test

class TextLimitsTest {

    @Test
    fun shorterThanLimitIsReturnedUnchanged() {
        assertEquals("纪念日", TextLimits.truncateGraphemes("纪念日", 120))
    }

    @Test
    fun truncatesAtGraphemeBoundary() {
        assertEquals("你好世", TextLimits.truncateGraphemes("你好世界", 3))
    }

    @Test
    fun keepsCombiningMarkIntact() {
        val text = "ae\u0301b"
        assertEquals("ae\u0301", TextLimits.truncateGraphemes(text, 2))
    }

    @Test
    fun countsGraphemes() {
        assertEquals(0, TextLimits.graphemeCount(""))
        assertEquals(3, TextLimits.graphemeCount("你好世"))
        assertEquals(2, TextLimits.graphemeCount("ae\u0301"))
    }

    @Test
    fun zeroLimitReturnsEmpty() {
        assertEquals("", TextLimits.truncateGraphemes("abc", 0))
    }
}
