package io.github.sushiericworkspace.sushiericservermanager.editor.main.item

import io.github.sushiericworkspace.common.stats.player.StatsType
import kotlin.math.abs

/**
 * アイテムのステータス値の初期値、補正、表示の規則です。
 *
 * ステータス追加時と追加済みステータスの編集で同じ規則を使います。
 * 0は「未設定」を表すため、値として保持しません。
 */

/**
 * ステータス追加時の初期値を返します。
 *
 * 範囲内で0でなければ[StatsType.default]を使います。既定値が0の場合は、
 * 範囲内の小さな非0値（1、なければ-1）を選びます。
 * 範囲の最小値から算出しないため、`-Double.MAX_VALUE`のような極端な値にはなりません。
 */
internal fun initialItemStatValue(type: StatsType): Double {
    val default = type.default

    return when {
        default != 0.0 && default in type.min..type.max -> default
        1.0 in type.min..type.max -> 1.0
        -1.0 in type.min..type.max -> -1.0
        type.min != 0.0 -> type.min
        else -> type.max
    }
}

/**
 * 入力値を保持できる値へ補正します。
 *
 * 0は未設定を表すため[initialItemStatValue]へ置き換え、それ以外は範囲内へ収めます。
 */
internal fun normalizeItemStatValue(type: StatsType, value: Double): Double {
    if (value.isNaN() || value == 0.0) {
        return initialItemStatValue(type)
    }

    return value.coerceIn(type.min, type.max)
}

/**
 * 入力文字列を値へ変換します。
 *
 * 数値として読めない場合は[fallback]を使い、結果を[normalizeItemStatValue]で補正します。
 */
internal fun parseItemStatValue(type: StatsType, text: String?, fallback: Double): Double {
    val parsed = text?.trim()?.toDoubleOrNull() ?: fallback

    return normalizeItemStatValue(type, parsed)
}

/**
 * 値を表示用の文字列にします。
 *
 * 整数値は小数点なしで表示します。`Int`へ縮小変換しないため、`Int`の範囲を超える値も
 * 桁を失いません。`Long`の範囲も超える整数値と小数は`Double.toString()`の表記
 * （`1.0E20`など）で表示し、そのまま再入力できます。
 */
internal fun formatItemStatValue(value: Double): String {
    if (value.isFinite() && value % 1.0 == 0.0 && abs(value) < Long.MAX_VALUE.toDouble()) {
        return value.toLong().toString()
    }

    return value.toString()
}
