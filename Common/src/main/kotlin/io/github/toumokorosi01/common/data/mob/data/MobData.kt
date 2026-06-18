package io.github.toumokorosi01.common.data.mob.data

import io.github.toumokorosi01.common.data.core.ManagedData
import io.github.toumokorosi01.common.data.core.structure.DropItemData
import io.github.toumokorosi01.common.data.core.structure.EditorMeta
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.mob.MobValidator
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

/**
 * Mobデータの仮モデル。
 *
 * 将来的にMobエディタを実装するための最小構成です。
 * 現時点ではID・表示名・最大HP・攻撃力だけを保持します。
 */
@ConfigSerializable
data class MobData(
    /** 完成品かどうか */
    @Setting("completed")
    override var completed: Boolean = false,
    /** 独自ID */
    @Setting("id")
    override var id: String = "",
    /** エンティティデータ */
    @Setting("entity-data")
    var entityData: EntityDetailData = EntityDetailData(),
    /** 表示名 */
    @Setting("display-name")
    var displayName: String = "新規エンティティ",
    /** ドロップアイテム */
    @Setting("drop-items")
    var dropItems: MutableList<DropItemData> = mutableListOf(),
    /** エディター専用 */
    @Setting("editor-meta")
    var editorMeta: EditorMeta = EditorMeta()
) : ManagedData<MobData, MobValidator> {
    /**
     * ファイルI/Oを発生させず、メモリ上で複製します。
     */
    override fun deepCopy(): MobData {
        return this.copy(
            entityData = this.entityData.deepCopy(),
            dropItems = this.dropItems
                .map { it.copy() }
                .toMutableList(),
            editorMeta = this.editorMeta.deepCopy()
        )
    }

    fun validator(items: List<ItemData>): MobValidator {
        return MobValidator(this, items)
    }

    fun validate(items: List<ItemData>): List<PropertyError> {
        return validator(items).validate()
    }
}

