package io.github.toumokorosi01.common.data.item.data

import io.github.toumokorosi01.common.Rarity
import io.github.toumokorosi01.common.data.core.DataType
import io.github.toumokorosi01.common.stats.player.StatsType
import io.github.toumokorosi01.common.data.core.ManagedData
import io.github.toumokorosi01.common.data.core.structure.EditorMeta
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.item.ItemValidator
import io.github.toumokorosi01.common.data.item.data.detail.ItemDetail
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

/**
 * アイテムの全データを保持するルートクラス
 */
@ConfigSerializable
data class ItemData(
    /** 完成品かどうか */
    @Setting("completed")
    override var completed: Boolean = false,
    /** 独自ID */
    @Setting("id")
    override var id: String = "",
    /** アイテムの細かい情報 */
    @Setting("item-detail")
    var itemDetail: ItemDetail = ItemDetail(),
    /** 表示名やLore */
    @Setting("display")
    var display: DisplayInfo = DisplayInfo(),
    /** ステータス */
    @Setting("stats")
    var stats: MutableMap<StatsType, Double> = mutableMapOf(),
    /** レアリティ */
    @Setting("rarity")
    var rarity: Rarity = Rarity.COMMON,
    /** エディター専用 */
    @Setting("editor-meta")
    var editorMeta: EditorMeta = EditorMeta(),
) : ManagedData<ItemData, ItemValidator> {
    override val dataType: DataType<ItemData>
        get() = DataType.Item

    override fun deepCopy(): ItemData {
        return this.copy(
            stats = this.stats.toMutableMap(),
            display = this.display.deepCopy(),
            itemDetail = this.itemDetail.deepCopy(),
            editorMeta = this.editorMeta.copy(
                // List<String> の複製
                comment = this.editorMeta.comment.toMutableList()
            )
        )
    }

    /**
     * このItemData専用のバリデーターを生成します。
     *
     * #### 仕様:
     * - 現在のItemDataインスタンスを検証対象として持つ [io.github.toumokorosi01.common.data.item.ItemValidator] を返します。
     * - バリデーション処理本体は [io.github.toumokorosi01.common.data.item.ItemValidator] 側に委譲します。
     * - GUIなどで特定項目だけを検証したい場合は、この関数で取得したValidatorから
     *   `validateVanillaId()` や `validateStat(...)` などを呼び出します。
     *
     * @return このItemDataを検証対象にした [io.github.toumokorosi01.common.data.item.ItemValidator]。
     */
    fun validator(): ItemValidator {
        return ItemValidator(this)
    }

    fun validate(): List<PropertyError> {
        return validator().validate()
    }
}