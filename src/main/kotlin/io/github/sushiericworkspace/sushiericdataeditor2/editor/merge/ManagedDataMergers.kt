package io.github.sushiericworkspace.sushiericdataeditor2.editor.merge

import io.github.sushiericworkspace.common.data.item.model.mutable.MutableItemBaseData
import io.github.sushiericworkspace.common.data.mob.model.MobBaseData
import io.github.sushiericworkspace.common.data.ore.model.OreBaseData

object ItemDataMerger : DataMerger<MutableItemBaseData> {
    override fun merge(
        base: MutableItemBaseData,
        local: MutableItemBaseData,
        remote: MutableItemBaseData
    ): ThreeWayMergeResult<MutableItemBaseData> {
        val accumulator = MergeAccumulator(remote.deepCopy(), MutableItemBaseData::deepCopy)

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
            base = base.display.mutableLore,
            local = local.display.mutableLore,
            remote = remote.display.mutableLore,
            copyValue = { line -> line.map { it.deepCopy() }.toMutableList() },
            targetList = { it.display.mutableLore }
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

object OreDataMerger : DataMerger<OreBaseData> {
    override fun merge(
        base: OreBaseData,
        local: OreBaseData,
        remote: OreBaseData
    ): ThreeWayMergeResult<OreBaseData> {
        val accumulator = MergeAccumulator(remote.deepCopy(), OreBaseData::deepCopy)

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

object MobDataMerger : DataMerger<MobBaseData> {
    override fun merge(
        base: MobBaseData,
        local: MobBaseData,
        remote: MobBaseData
    ): ThreeWayMergeResult<MobBaseData> {
        val accumulator = MergeAccumulator(remote.deepCopy(), MobBaseData::deepCopy)

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
