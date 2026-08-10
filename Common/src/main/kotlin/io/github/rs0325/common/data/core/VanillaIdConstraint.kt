package io.github.rs0325.common.data.core

import io.github.rs0325.common.registry.VanillaIdRegistry

/**
 * アイテムのcontentごとに許可するバニラIDの制約を表す。
 *
 * この制約は、ItemDetailのvanillaIdに対して、
 * 全てのアイテムIDを許可するか、特定のIDに固定するか、指定された候補リストからのみ選択させるかを管理する。
 *
 * 主な用途は、エディタ上でcontentに応じてvanillaIdの入力欄を制限したり、
 * content変更時に不正なvanillaIdを自動補正したりすることである。
 *
 * 使用例:
 * ```kotlin
 * data class BowData(
 *     val multi: Int = 1,
 *     val short: Boolean = false,
 *     val shortInterval: Double = 0.5,
 *     val pierce: Int = 0,
 *     val angle: Double = 1.0
 * ) : ItemDetailContent {
 *     override val vanillaIdConstraint: VanillaIdConstraint =
 *         VanillaIdConstraint.Fixed("minecraft:bow")
 * }
 *
 * data class SwordData(
 *     val dummy: Boolean = false
 * ) : ItemDetailContent {
 *     override val vanillaIdConstraint: VanillaIdConstraint =
 *         VanillaIdConstraint.Choices(VanillaIdRegistry.swords)
 * }
 *
 * data class OtherData(
 *     val dummy: Boolean = false
 * ) : ItemDetailContent {
 *     override val vanillaIdConstraint: VanillaIdConstraint =
 *         VanillaIdConstraint.Free
 * }
 * ```
 *
 * エディタ側での使用例:
 * ```kotlin
 * val choices = itemDetail.content.vanillaIdConstraint.choices()
 *
 * comboBox.items.setAll(choices)
 *
 * if (itemDetail.vanillaId !in choices) {
 *     itemDetail.vanillaId = choices.first()
 * }
 * ```
 */
sealed interface VanillaIdConstraint {

    /**
     * 全てのバニラアイテムIDを選択できることを表す。
     *
     * この制約では、[choices] が [io.github.rs0325.common.registry.VanillaIdRegistry.allItems] を返す。
     *
     * 使用例:
     * ```kotlin
     * override val vanillaIdConstraint: VanillaIdConstraint =
     *     VanillaIdConstraint.Free
     * ```
     */
    data object Free : VanillaIdConstraint

    /**
     * バニラIDを1つのIDに固定する制約。
     *
     * 例えば弓専用のcontentでは、`minecraft:bow` のみに固定するために使用する。
     *
     * @property id 固定するバニラID。
     *
     * 使用例:
     * ```kotlin
     * override val vanillaIdConstraint: VanillaIdConstraint =
     *     VanillaIdConstraint.Fixed("minecraft:bow")
     * ```
     */
    data class Fixed(
        val id: String
    ) : VanillaIdConstraint

    /**
     * バニラIDを指定された候補リストの中からのみ選択できるようにする制約。
     *
     * 例えば剣系アイテムでは、木の剣、石の剣、鉄の剣などのIDだけを許可するために使用する。
     *
     * @property ids 許可するバニラIDのリスト。
     *
     * 使用例:
     * ```kotlin
     * override val vanillaIdConstraint: VanillaIdConstraint =
     *     VanillaIdConstraint.Choices(VanillaIdRegistry.swords)
     * ```
     */
    data class Choices(
        val ids: List<String>
    ) : VanillaIdConstraint

    /**
     * この制約から、エディタで選択可能なバニラID候補を取得する。
     *
     * [Free]の場合は、全てのバニラアイテムIDを返す。
     * [Fixed]の場合は、固定IDのみを含むリストを返す。
     * [Choices]の場合は、許可されたIDリストをそのまま返す。
     *
     * 使用例:
     * ```kotlin
     * val choices = constraint.choices()
     *
     * comboBox.items.setAll(choices)
     *
     * if (itemDetail.vanillaId !in choices) {
     *     itemDetail.vanillaId = choices.first()
     * }
     * ```
     *
     * @return 選択可能なID候補。
     */
    fun choices(): List<String> {
        return when (this) {
            Free -> VanillaIdRegistry.allItems
            is Fixed -> listOf(id)
            is Choices -> ids
        }
    }

    fun search(query: String): List<String> {
        val choices = choices()

        if (query.isBlank()) {
            return choices
        }

        return choices.filter {
            it.contains(query, ignoreCase = true)
        }
    }
}