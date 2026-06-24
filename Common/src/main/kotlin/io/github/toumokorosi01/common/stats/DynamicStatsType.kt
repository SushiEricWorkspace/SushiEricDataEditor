package io.github.toumokorosi01.common.stats

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

enum class DynamicStatsType(
    val display: String,
    val color: TextColor,
    val min: Double,
    val max: Double
) {
    HEALTH("HP", NamedTextColor.RED, 0.0, Double.MAX_VALUE),
    MANA("マナ", NamedTextColor.AQUA, 0.0, Double.MAX_VALUE)
}