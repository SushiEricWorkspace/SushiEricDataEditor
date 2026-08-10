package io.github.rs0325.common

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

enum class Rarity(val color: TextColor, val rarityNumber: Int) {
    COMMON(NamedTextColor.WHITE, 0),
    UNCOMMON(NamedTextColor.GREEN, 1),
    RARE(NamedTextColor.BLUE, 2),
    EPIC(NamedTextColor.DARK_PURPLE, 3),
    LEGENDARY(NamedTextColor.GOLD, 4),
    MYTHIC(NamedTextColor.LIGHT_PURPLE, 5),
    VERY_MYTHIC(NamedTextColor.RED, 6),
    ULTIMATE(NamedTextColor.DARK_RED, 7);

    companion object {
        // 外部ツールで打ち間違えてもエラーにならないよう安全に変換する
        fun fromString(name: String): Rarity =
            runCatching { valueOf(name.uppercase()) }.getOrDefault(COMMON)
    }
}

