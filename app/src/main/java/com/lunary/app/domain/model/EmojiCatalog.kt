package com.lunary.app.domain.model

/**
 * Curated emoji set used by the title emoji picker. Keeping this list local avoids
 * pulling in a large emoji-picker dependency and guarantees a single-grapheme value.
 *
 * The list is grouped for browsing; [leading] matches the longest possible prefix so
 * multi-codepoint emoji are still stripped correctly from legacy titles.
 */
object EmojiCatalog {
    val all: List<String> = listOf(
        // Faces
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
        "😊", "🙂", "😉", "😍", "🥰", "😘", "😋", "😜",
        "🤪", "🤩", "😎", "🤗", "🤭", "🤔", "🤨", "😐",
        "😴", "😪", "😭", "😢", "😤", "😡", "😱", "🤯",
        "🥳", "🤓", "🥺", "😇", "🤠",

        // Love
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🤍", "🖤",
        "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖",
        "💘", "💝",

        // Nature
        "✨", "⭐", "🌟", "🌈", "☀️", "🌤️", "⛅", "🌧️",
        "⛈️", "🌙", "🌛", "❄️", "🔥", "🌸", "🌺", "🌻",
        "🌷", "🌹", "🌼", "🌿", "🍀", "🌳", "🌲", "🌴",
        "🍁", "🍂", "🌾",

        // Food
        "🎂", "🍰", "🧁", "🍫", "🍬", "🍭", "🍩", "🍪",
        "☕", "🍵", "🥤", "🍺", "🍷", "🍜", "🍕", "🍔",
        "🍟", "🍎", "🍊", "🍓", "🍉", "🍇", "🍋", "🥑",

        // Celebration and travel
        "🎉", "🎊", "🎁", "🎈", "🎀", "🕯️", "🪄", "🎆",
        "🎇", "✈️", "🚗", "🚲", "🚢", "🏠", "🏡", "🏢",
        "🏖️", "🏔️", "🗺️", "🧳",

        // Animals
        "🐱", "🐶", "🐭", "🐰", "🦊", "🐻", "🐼", "🐨",
        "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧",
        "🐦", "🦋", "🐝", "🐞", "🐢", "🐬", "🐳",

        // Objects and hobbies
        "📚", "📖", "✏️", "📝", "🎵", "🎶", "🎧", "🎸",
        "🎹", "🎨", "🖼️", "📷", "📱", "💻", "⌚", "💡",
        "🧸", "🪁", "⚽", "🏀", "🎾", "🏸", "🎮", "🧩",

        // Symbols
        "✅", "❌", "❓", "❗", "💯", "🔔", "🎯", "🏆",
        "🥇", "💎", "👑", "🔮", "🧿",
    )

    private val byLengthDescending: List<String> = all.sortedByDescending { it.length }

    /** Longest matching emoji at the start of [title], if any. */
    fun leading(title: String): String? = byLengthDescending.firstOrNull { title.startsWith(it) }

    /** Title without its leading catalog emoji. */
    fun withoutLeading(title: String): String {
        val emoji = leading(title) ?: return title
        return title.removePrefix(emoji)
    }
}
