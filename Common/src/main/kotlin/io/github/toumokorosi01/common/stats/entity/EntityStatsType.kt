package io.github.toumokorosi01.common.stats.entity

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

enum class EntityStatsType(val display: String, val color: TextColor, val min: Double, val max: Double, val default: Double) {
    MAX_HEALTH("最大体力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 100.0),
    ATTACK_DAMAGE("攻撃力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 2.0),
    DEFENCE("防御力", NamedTextColor.GREEN, -Double.MAX_VALUE, Double.MAX_VALUE, 0.0),
    CRIT_RESISTANCE("会心抵抗率", NamedTextColor.AQUA, 0.0, Double.MAX_VALUE, 0.0),
    AILMENT_DAMAGE("状態異常ダメージ", NamedTextColor.LIGHT_PURPLE, 0.0, Double.MAX_VALUE, 0.0),
    AILMENT_RESISTANCE("状態異常耐性", NamedTextColor.LIGHT_PURPLE, 0.0, Double.MAX_VALUE, 0.0),
    SPEED("移動速度", NamedTextColor.WHITE, -Double.MAX_VALUE, Double.MAX_VALUE, 100.0)
}