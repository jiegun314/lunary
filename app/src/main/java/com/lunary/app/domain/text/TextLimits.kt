package com.lunary.app.domain.text

import java.text.BreakIterator

object TextLimits {

    fun graphemeCount(text: String): Int {
        if (text.isEmpty()) return 0
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(text)
        var count = 0
        var index = iterator.first()
        while (index != BreakIterator.DONE) {
            count++
            index = iterator.next()
        }
        return count - 1
    }

    /** Truncates on grapheme boundaries so emoji / combined characters stay intact. */
    fun truncateGraphemes(text: String, maxGraphemes: Int): String {
        if (maxGraphemes <= 0) return ""
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(text)
        var count = 0
        var end = 0
        var next = iterator.next()
        while (next != BreakIterator.DONE) {
            end = next
            count++
            if (count >= maxGraphemes) {
                return if (next < text.length) text.substring(0, end) else text
            }
            next = iterator.next()
        }
        return text
    }
}
