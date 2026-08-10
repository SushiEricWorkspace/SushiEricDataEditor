package io.github.rs0325.common.stats.player

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

enum class DynamicStatsType(
    val display: String,
    val color: TextColor,
    val min: Double,
    val max: Double,
    val default: Double
) {
    HEALTH("HP", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 100.0),
    MANA("マナ", NamedTextColor.AQUA, 0.0, Double.MAX_VALUE, 100.0),
    OVER_HEAL("オーバーヒール", NamedTextColor.YELLOW, 0.0, Double.MAX_VALUE, 0.0),
    STAMINA("スタミナ", NamedTextColor.YELLOW, 0.0, Double.MAX_VALUE, 100.0)
}