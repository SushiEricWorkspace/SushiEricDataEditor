package io.github.toumokorosi01.common.data.core.structure

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import kotlin.math.pow
import kotlin.random.Random

/** ドロップアイテムの構造 */
@ConfigSerializable
data class DropItemData(
    /** アイテムID */
    @Setting("item-id")
    var id: String = "",

    /** 試行回数 */
    @Setting("n")
    var n: Int = 1,

    /** 一回あたりの成功確率 */
    @Setting("p")
    var p: Double = 1.0
) {
    /** 実際にドロップする個数を抽選する */
    fun rollAmount(random: Random = Random.Default): Int {
        if (n <= 0) return 0
        if (p !in 0.0..1.0) return 0

        var amount = 0

        repeat(n) {
            if (random.nextDouble() < p) {
                amount++
            }
        }

        return amount
    }

    /** 期待値 E(X) = n * p */
    fun expectedValue(): Double {
        return n * p
    }

    /** ちょうど k 回成功する確率 */
    fun probabilityExactly(k: Int): Double {
        if (k !in 0..n) return 0.0
        if (p !in 0.0..1.0) return 0.0

        return combination(n, k) *
                p.pow(k.toDouble()) *
                (1.0 - p).pow((n - k).toDouble())
    }

    /** 1回以上成功する確率 */
    fun probabilityAtLeastOnce(): Double {
        if (p !in 0.0..1.0) return 0.0

        return 1.0 - (1.0 - p).pow(n.toDouble())
    }

    /** k 回以上成功する確率 */
    fun probabilityAtLeast(k: Int): Double {
        if (k <= 0) return 1.0
        if (k > n) return 0.0

        return (k..n).sumOf { probabilityExactly(it) }
    }

    /** k 回以下成功する確率 */
    fun probabilityAtMost(k: Int): Double {
        if (k < 0) return 0.0
        if (k >= n) return 1.0

        return (0..k).sumOf { probabilityExactly(it) }
    }

    private fun combination(n: Int, r: Int): Double {
        if (r !in 0..n) return 0.0

        val k = minOf(r, n - r)
        var result = 1.0

        for (i in 1..k) {
            result *= (n - k + i).toDouble()
            result /= i.toDouble()
        }

        return result
    }
}
