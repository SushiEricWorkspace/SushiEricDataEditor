package io.github.toumokorosi01.common.data.item

import io.github.toumokorosi01.common.StatsType
import io.github.toumokorosi01.common.data.core.ConfigurateDataManager
import io.github.toumokorosi01.common.data.item.data.ItemData
import java.io.File

/**
 * アイテムデータの永続化（保存・読み込み）を管理するリポジトリ。
 * [ConfigurateDataManager] の共通処理を利用し、ItemData固有の補正だけを行います。
 */
object ItemManager : ConfigurateDataManager<ItemData>(
    dataClass = ItemData::class.java
) {
    /**
     * 指定された [File] にアイテムデータを保存します。
     *
     * #### 仕様:
     * - dynamicなステータスは保存対象から除外します。
     * - 値が0.0のステータスは保存対象から除外します。
     * - 保存前に [ItemData.refreshCompleted] を呼び出し、completedを更新します。
     *
     * @param file 保存先のファイル。
     * @param data 保存対象のアイテムデータ。
     */
    override fun save(file: File, data: ItemData) {
        val filteredStats = data.stats.filter { (type, value) ->
            !type.dynamic && value != 0.0
        }.toMutableMap()

        val itemToSave = data.copy(
            stats = filteredStats
        ).apply {
            refreshCompleted(validate())
        }

        super.save(file, itemToSave)
    }

    /**
     * 指定された [File] からアイテムデータを読み込みます。
     *
     * #### 仕様:
     * - preferredIdが指定されている場合はそれをidとして使用します。
     * - preferredIdがnullの場合はファイル名からidを取得します。
     * - statsの値は [StatsType.min] から [StatsType.max] の範囲に補正します。
     * - 補正後に0.0になるステータスは除外します。
     *
     * @param file 読み込み対象のファイル。
     * @param preferredId 本来のアイテムID。一時ファイルを使用する場合などに指定します。
     * @return 読み込まれた [ItemData]。読み込みに失敗した場合はnull。
     */
    override fun load(file: File, preferredId: String?): ItemData? {
        val item = super.load(file, preferredId) ?: return null

        val clampedStats = mutableMapOf<StatsType, Double>().apply {
            putAll(
                item.stats.mapNotNull { (type, value) ->
                    val clamped = value.coerceIn(type.min, type.max)

                    if (clamped == 0.0) {
                        null
                    } else {
                        type to clamped
                    }
                }
            )
        }

        return item.copy(
            stats = clampedStats
        )
    }
}