package io.github.toumokorosi01.common

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

enum class EntityStatsType(val display: String, val color: TextColor, val min: Double, val max: Double, val default: Double, val dynamic: Boolean = false) {
    HEALTH("HP", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 100.0, true),
    MAX_HEALTH("最大体力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 100.0),
    ATTACK_DAMAGE("攻撃力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 2.0),
    DEFENCE("防御力", NamedTextColor.GREEN, -Double.MAX_VALUE, Double.MAX_VALUE, 0.0)
}