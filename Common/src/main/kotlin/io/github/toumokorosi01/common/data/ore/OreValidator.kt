package io.github.toumokorosi01.common.data.ore

import io.github.toumokorosi01.common.DataRegistry
import io.github.toumokorosi01.common.data.core.structure.DropItemData
import io.github.toumokorosi01.common.data.core.structure.DropItemValidator
import io.github.toumokorosi01.common.data.core.validation.DataValidator
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.ore.data.OreData

class OreValidator(
    private val ore: OreData,
    private val items: List<ItemData>
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateBlockId())
            addAll(validateHardness())
            addAll(validateDropItems())
        }
    }

    /**
     * blockIdが正常かどうかをチェックします。
     *
     * GUI上では、バニラID入力欄の変更時にこの関数だけを呼ぶことで、
     * 該当フィールドのエラー表示を即時更新できます。
     *
     * @return blockIdに関するエラー一覧
     */
    fun validateBlockId(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (!DataRegistry.allBlocks.contains(ore.blockId)) errors.add(
            PropertyError(ore::blockId, "存在しないIDです。")
        )

        return errors
    }

    fun validateHardness(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (ore.hardness < 0.0) errors.add(
            PropertyError(ore::hardness, "硬度は0.0以上の値を設定してください。")
        )

        return errors
    }

    fun validateDropItems(): List<PropertyError> {
        return ore.dropItems.flatMap { dropItem ->
            validateDropItem(dropItem)
        }
    }

    fun validateDropItem(dropItem: DropItemData): List<PropertyError> = DropItemValidator(dropItem, items).validate()
}