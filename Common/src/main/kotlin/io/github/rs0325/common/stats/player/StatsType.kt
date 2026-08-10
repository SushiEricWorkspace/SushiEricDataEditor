package io.github.rs0325.common.stats.player

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

/**
 * ステータスの種類
 *
 * @property display [String] 型の表示名。
 * @property color そのステータスの [net.kyori.adventure.text.format.TextColor] 型の色。
 * @property min 最小値。
 * @property max 最大値。
 * @property default 初期値。
 * @property main メインメニューに表示する場合 `true` そうでなければ `false`
 * */
enum class StatsType(val display: String, val color: TextColor, val min: Double, val max: Double, val default: Double, val main: Boolean) {
    MAX_HEALTH("最大体力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 100.0, true),
    HEALTH_REGEN("治癒力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 100.0, false),

    MAX_STAMINA("最大スタミナ", NamedTextColor.YELLOW, 0.0, Double.MAX_VALUE, 100.0, true),

    INTELLIGENCE("知性", NamedTextColor.AQUA, 0.0, Double.MAX_VALUE, 100.0, true),
    MANA_REGEN("マナ再生力", NamedTextColor.AQUA, 0.0, Double.MAX_VALUE, 2.0, false),
    ABILITY_DAMAGE("魔法攻撃力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 0.0, true),
    ABILITY_CRIT_DAMAGE("魔法会心ダメージ", NamedTextColor.BLUE, 0.0, Double.MAX_VALUE, 50.0, true),
    ABILITY_CRIT_CHANCE("魔法会心率", NamedTextColor.BLUE, 0.0, Double.MAX_VALUE, 0.0, true),

    STRENGTH("筋力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 0.0, true),
    PHYSICS_DAMAGE("物理攻撃力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 0.0, true),
    PHYSICS_CRIT_DAMAGE("物理会心ダメージ", NamedTextColor.BLUE, 0.0, Double.MAX_VALUE, 50.0, true),
    PHYSICS_CRIT_CHANCE("物理会心率", NamedTextColor.BLUE, 0.0, Double.MAX_VALUE, 0.0, true),

    DEFENCE_PENETRATION("防御貫通率", NamedTextColor.GREEN, 0.0, Double.MAX_VALUE, 0.0, false),
    AILMENT_DAMAGE("状態異常ダメージ", NamedTextColor.LIGHT_PURPLE, 0.0, Double.MAX_VALUE, 0.0, false),
    AILMENT_RESISTANCE("状態異常耐性", NamedTextColor.LIGHT_PURPLE, 0.0, Double.MAX_VALUE, 0.0, false),

    BREAK_EFFICIENCY("ブレイク効率", NamedTextColor.RED, -Double.MAX_VALUE, Double.MAX_VALUE, 1.0, false),

    DEFENCE("防御力", NamedTextColor.GREEN, 0.0, Double.MAX_VALUE, 0.0, true),
    SPEED("移動速度", NamedTextColor.WHITE, 0.0, 400.0, 100.0, true);

    fun getKindColor(): NamedTextColor {
        return when (this) {
            ABILITY_DAMAGE, ABILITY_CRIT_CHANCE, ABILITY_CRIT_DAMAGE,
            STRENGTH, PHYSICS_DAMAGE, PHYSICS_CRIT_DAMAGE, PHYSICS_CRIT_CHANCE,
            AILMENT_DAMAGE -> NamedTextColor.RED
            else -> NamedTextColor.GREEN
        }
    }
}