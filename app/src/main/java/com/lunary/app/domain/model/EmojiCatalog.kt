package com.lunary.app.domain.model

/**
 * Curated emoji set used by the title emoji picker. Keeping this list local avoids
 * pulling in a large emoji-picker dependency and guarantees a single-grapheme value.
 */
object EmojiCatalog {
    val all: List<String> = listOf(
        "😀", "😄", "😊", "🥰", "😍", "🤩", "😎", "🥳",
        "😭", "😅", "🤔", "😴", "❤️", "🧡", "💛", "💚",
        "💙", "💜", "🤍", "🖤", "💖", "✨", "⭐", "🌟",
        "🌸", "🌻", "🌹", "🌈", "☀️", "🌙", "❄️", "🔥",
        "🎂", "🎉", "🎁", "🎈", "🍰", "☕", "🍜", "🍎",
        "✈️", "🏠", "🐱", "🐶", "🌳", "📚", "🎵", "⚽"
    ).sortedByDescending { it.length }

    /** Longest matching emoji at the start of [title], if any. */
    fun leading(title: String): String? = all.firstOrNull { title.startsWith(it) }

    /** Title without its leading catalog emoji. */
    fun withoutLeading(title: String): String {
        val emoji = leading(title) ?: return title
        return title.removePrefix(emoji)
    }
}
