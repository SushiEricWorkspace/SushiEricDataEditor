package io.github.toumokorosi01.sushiericdataeditor2.editor.merge

import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.mob.data.MobData
import io.github.toumokorosi01.common.data.ore.data.OreData

object ItemDataMerger : DataMerger<ItemData> {
    override fun merge(
        base: ItemData,
        local: ItemData,
        remote: ItemData
    ): ThreeWayMergeResult<ItemData> {
        val accumulator = MergeAccumulator(remote.deepCopy(), ItemData::deepCopy)

        accumulator.mergeValue(DataFields.rarity, base.rarity, local.rarity, remote.rarity) { data, value ->
            data.rarity = value
        }
        accumulator.mergeValue(
            DataFields.enchantAura,
            base.itemDetail.enchantAura,
            local.itemDetail.enchantAura,
            remote.itemDetail.enchantAura
        ) { data, value -> data.itemDetail.enchantAura = value }
        accumulator.mergeValue(
            DataFields.vanillaId,
            base.itemDetail.vanillaId,
            local.itemDetail.vanillaId,
            remote.itemDetail.vanillaId
        ) { data, value -> data.itemDetail.vanillaId = value }
        accumulator.mergeValue(
            DataFields.maxStackSize,
            base.itemDetail.maxStackSize,
            local.itemDetail.maxStackSize,
            remote.itemDetail.maxStackSize
        ) { data, value -> data.itemDetail.maxStackSize = value }
        accumulator.mergeValue(
            DataFields.detailContent,
            base.itemDetail.content,
            local.itemDetail.content,
            remote.itemDetail.content
        ) { data, value -> data.itemDetail.content = value.deepCopy() }
        accumulator.mergeValue(
            DataFields.displayName,
            base.display.displayName,
            local.display.displayName,
            remote.display.displayName
        ) { data, value -> data.display.displayName = value }
        accumulator.mergeMap(
            path = DataFields.stats,
            base = base.stats,
            local = local.stats,
            remote = remote.stats,
            keyDisplay = { it.display },
            targetMap = { it.stats }
        )
        accumulator.mergeList(
            path = DataFields.lore,
            base = base.display.lore,
            local = local.display.lore,
            remote = remote.display.lore,
            copyValue = { line -> line.map { it.deepCopy() }.toMutableList() },
            targetList = { it.display.lore }
        )
        accumulator.mergeList(
            path = DataFields.comments,
            base = base.editorMeta.comment,
            local = local.editorMeta.comment,
            remote = remote.editorMeta.comment,
            targetList = { it.editorMeta.comment }
        )

        return accumulator.result()
    }
}

object OreDataMerger : DataMerger<OreData> {
    override fun merge(
        base: OreData,
        local: OreData,
        remote: OreData
    ): ThreeWayMergeResult<OreData> {
        val accumulator = MergeAccumulator(remote.deepCopy(), OreData::deepCopy)

        accumulator.mergeValue(DataFields.blockId, base.blockId, local.blockId, remote.blockId) { data, value ->
            data.blockId = value
        }
        accumulator.mergeValue(DataFields.hardness, base.hardness, local.hardness, remote.hardness) { data, value ->
            data.hardness = value
        }
        accumulator.mergeList(
            path = DataFields.dropItems,
            base = base.dropItems,
            local = local.dropItems,
            remote = remote.dropItems,
            copyValue = { it.copy() },
            targetList = { it.dropItems }
        )
        accumulator.mergeList(
            path = DataFields.comments,
            base = base.editorMeta.comment,
            local = local.editorMeta.comment,
            remote = remote.editorMeta.comment,
            targetList = { it.editorMeta.comment }
        )

        return accumulator.result()
    }
}

object MobDataMerger : DataMerger<MobData> {
    override fun merge(
        base: MobData,
        local: MobData,
        remote: MobData
    ): ThreeWayMergeResult<MobData> {
        val accumulator = MergeAccumulator(remote.deepCopy(), MobData::deepCopy)

        accumulator.mergeValue(
            DataFields.displayName,
            base.displayName,
            local.displayName,
            remote.displayName
        ) { data, value -> data.displayName = value }
        accumulator.mergeValue(
            DataFields.vanillaId,
            base.entityData.vanillaId,
            local.entityData.vanillaId,
            remote.entityData.vanillaId
        ) { data, value -> data.entityData.vanillaId = value }
        accumulator.mergeMap(
            path = DataFields.stats,
            base = base.entityData.stats,
            local = local.entityData.stats,
            remote = remote.entityData.stats,
            keyDisplay = { it.display },
            targetMap = { it.entityData.stats }
        )
        accumulator.mergeValue(
            DataFields.equipment,
            base.entityData.entityEquipment,
            local.entityData.entityEquipment,
            remote.entityData.entityEquipment
        ) { data, value -> data.entityData.entityEquipment = value.deepCopy() }
        accumulator.mergeList(
            path = DataFields.dropItems,
            base = base.dropItems,
            local = local.dropItems,
            remote = remote.dropItems,
            copyValue = { it.copy() },
            targetList = { it.dropItems }
        )
        accumulator.mergeList(
            path = DataFields.comments,
            base = base.editorMeta.comment,
            local = local.editorMeta.comment,
            remote = remote.editorMeta.comment,
            targetList = { it.editorMeta.comment }
        )

        return accumulator.result()
    }
}
