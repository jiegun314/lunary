package com.lunary.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmojiCatalogTest {

    @Test
    fun leadingExtractsCatalogEmoji() {
        assertEquals("😄", EmojiCatalog.leading("😄Party"))
        assertEquals("☀️", EmojiCatalog.leading("☀️Sunny"))
        assertNull(EmojiCatalog.leading("Party"))
    }

    @Test
    fun withoutLeadingStripsOnlyTheEmoji() {
        assertEquals("Party", EmojiCatalog.withoutLeading("😄Party"))
        assertEquals("", EmojiCatalog.withoutLeading("😄"))
        assertEquals("派对😂", EmojiCatalog.withoutLeading("派对😂"))
    }

    @Test
    fun catalogContainsUniqueSingleGraphemes() {
        assertEquals(EmojiCatalog.all.size, EmojiCatalog.all.distinct().size)
        EmojiCatalog.all.forEach { emoji ->
            assertEquals(1, com.lunary.app.domain.text.TextLimits.graphemeCount(emoji))
        }
    }
}
