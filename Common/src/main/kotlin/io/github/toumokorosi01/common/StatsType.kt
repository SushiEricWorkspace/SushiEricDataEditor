package io.github.toumokorosi01.common

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

/**
 * ステータスの種類
 *
 * @property display [String] 型の表示名。
 * @property color そのステータスの [TextColor] 型の色。
 * @property min 最小値。
 * @property max 最大値。
 * @property default 初期値。
 * @property main メインメニューに表示する場合 `true` そうでなければ `false`
 * */
enum class StatsType(val display: String, val color: TextColor, val min: Double, val max: Double, val default: Double, val main: Boolean, val dynamic: Boolean = false) {
    MAX_HEALTH("最大体力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 100.0, true),
    /** 動的なステータスなのでBASEのみ反映 */
    HEALTH("HP", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 100.0, false, true),
    HEALTH_REGEN("治癒力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 2.0, false),

    INTELLIGENCE("知性", NamedTextColor.AQUA, 0.0, Double.MAX_VALUE, 100.0, true),
    /** 動的なステータスなのでBASEのみ反映 */
    MANA("マナ", NamedTextColor.AQUA, 0.0, Double.MAX_VALUE, 100.0, false, true),
    MANA_REGEN("マナ再生力", NamedTextColor.AQUA, 0.0, Double.MAX_VALUE, 2.0, false),
    ABILITY_DAMAGE("魔法攻撃力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 0.0, true),
    ABILITY_CRIT_DAMAGE("魔法会心ダメージ", NamedTextColor.BLUE, 0.0, Double.MAX_VALUE, 50.0, true),
    ABILITY_CRIT_CHANCE("魔法会心率", NamedTextColor.BLUE, 0.0, Double.MAX_VALUE, 0.0, true),

    STRENGTH("筋力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 0.0, true),
    PHYSICS_DAMAGE("物理攻撃力", NamedTextColor.RED, 0.0, Double.MAX_VALUE, 0.0, true),
    PHYSICS_CRIT_DAMAGE("物理会心ダメージ", NamedTextColor.BLUE, 0.0, Double.MAX_VALUE, 50.0, true),
    PHYSICS_CRIT_CHANCE("物理会心率", NamedTextColor.BLUE, 0.0, Double.MAX_VALUE, 0.0, true),
    PHYSICS_RANGE("物理攻撃範囲", NamedTextColor.YELLOW, 0.0, Double.MAX_VALUE, 0.0, false),

    CRIT_RESISTANCE("会心抵抗率", NamedTextColor.YELLOW, 0.0, Double.MAX_VALUE, 0.0, true),
    DEFENCE_PENETRATION("防御貫通率", NamedTextColor.GREEN, 0.0, Double.MAX_VALUE, 0.0, false),

    DEFENCE("防御力", NamedTextColor.GREEN, 0.0, Double.MAX_VALUE, 0.0, true),
    SPEED("移動速度", NamedTextColor.WHITE, 0.0, 400.0, 0.0, true),

    // --- 属性特効 (Damage to specific attributes) ---
    FIRE_SLAYER("火属性特効", NamedTextColor.GOLD, 0.0, Double.MAX_VALUE, 0.0, false),
    WIND_SLAYER("風属性特効", NamedTextColor.GOLD, 0.0, Double.MAX_VALUE, 0.0, false),
    WATER_SLAYER("水属性特効", NamedTextColor.GOLD, 0.0, Double.MAX_VALUE, 0.0, false),
    EARTH_SLAYER("土属性特効", NamedTextColor.GOLD, 0.0, Double.MAX_VALUE, 0.0, false),

    // --- 属性耐性 (Resistance to specific attributes) ---
    FIRE_RESISTANCE("火属性耐性", NamedTextColor.GRAY, 0.0, Double.MAX_VALUE, 0.0, false),
    WIND_RESISTANCE("風属性耐性", NamedTextColor.GRAY, 0.0, Double.MAX_VALUE, 0.0, false),
    WATER_RESISTANCE("水属性耐性", NamedTextColor.GRAY, 0.0, Double.MAX_VALUE, 0.0, false),
    EARTH_RESISTANCE("土属性耐性", NamedTextColor.GRAY, 0.0, Double.MAX_VALUE, 0.0, false);
}