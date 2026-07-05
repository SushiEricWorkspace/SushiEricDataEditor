package io.github.toumokorosi01.common.data.ore.data

import io.github.toumokorosi01.common.data.core.DataType
import io.github.toumokorosi01.common.data.core.ManagedData
import io.github.toumokorosi01.common.data.core.structure.DropItemData
import io.github.toumokorosi01.common.data.core.structure.EditorMeta
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.ore.OreValidator
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

/**
 * 鉱石データの仮モデル。
 *
 * 将来的にOreエディタを実装するための最小構成です。
 * 現時点ではItemDataのような詳細構造は持たせず、ID・表示名・生成量だけを保持します。
 */
@ConfigSerializable
data class OreData(
    /** 完成品かどうか */
    @Setting("completed")
    override var completed: Boolean = false,
    /** 独自ID */
    @Setting("id")
    override var id: String = "",
    /** ブロックID */
    @Setting("block-id")
    var blockId: String = "",
    /** 硬度 */
    @Setting("hardness")
    var hardness: Double = 5.0,
    /** ドロップアイテム */
    @Setting("drop-items")
    var dropItems: MutableList<DropItemData> = mutableListOf(),
    /** エディター専用 */
    @Setting("editor-meta")
    var editorMeta: EditorMeta = EditorMeta()
) : ManagedData<OreData, OreValidator> {
    override val dataType: DataType<OreData>
        get() = DataType.Ore

    override fun deepCopy(): OreData {
        return this.copy(
            dropItems = this.dropItems
                .map { it.copy() }
                .toMutableList(),
            editorMeta = this.editorMeta.deepCopy()
        )
    }

    fun validator(items: Set<String>): OreValidator {
        return OreValidator(this, items)
    }

    fun validate(items: Set<String>): List<PropertyError> {
        return validator(items).validate()
    }
}