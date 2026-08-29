package io.github.sushiericworkspace.sushiericdataeditor2.editor.merge

import io.github.sushiericworkspace.common.data.item.model.mutable.MutableItemBaseData
import io.github.sushiericworkspace.common.data.mob.model.mutable.MutableMobBaseData
import io.github.sushiericworkspace.common.data.ore.model.mutable.MutableOreBaseData

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

object OreDataMerger : DataMerger<MutableOreBaseData> {
    override fun merge(
        base: MutableOreBaseData,
        local: MutableOreBaseData,
        remote: MutableOreBaseData
    ): ThreeWayMergeResult<MutableOreBaseData> {
        val accumulator = MergeAccumulator(remote.deepCopy(), MutableOreBaseData::deepCopy)

        accumulator.mergeValue(DataFields.blockId, base.blockId, local.blockId, remote.blockId) { data, value ->
            data.blockId = value
        }
        accumulator.mergeValue(DataFields.hardness, base.hardness, local.hardness, remote.hardness) { data, value ->
            data.hardness = value
        }
        accumulator.mergeList(
            path = DataFields.dropItems,
            base = base.mutableDropItems,
            local = local.mutableDropItems,
            remote = remote.mutableDropItems,
            copyValue = { it.copy() },
            targetList = { it.mutableDropItems }
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

object MobDataMerger : DataMerger<MutableMobBaseData> {
    override fun merge(
        base: MutableMobBaseData,
        local: MutableMobBaseData,
        remote: MutableMobBaseData
    ): ThreeWayMergeResult<MutableMobBaseData> {
        val accumulator = MergeAccumulator(remote.deepCopy(), MutableMobBaseData::deepCopy)

        accumulator.mergeValue(
            DataFields.displayName,
            base.displayName,
            local.displayName,
            remote.displayName
        ) { data, value -> data.displayName = value }
        accumulator.mergeValue(
            DataFields.vanillaId,
            base.mutableEntityData.vanillaId,
            local.mutableEntityData.vanillaId,
            remote.mutableEntityData.vanillaId
        ) { data, value -> data.mutableEntityData.vanillaId = value }
        accumulator.mergeMap(
            path = DataFields.stats,
            base = base.mutableEntityData.mutableStats,
            local = local.mutableEntityData.mutableStats,
            remote = remote.mutableEntityData.mutableStats,
            keyDisplay = { it.display },
            targetMap = { it.mutableEntityData.mutableStats }
        )
        accumulator.mergeValue(
            DataFields.equipment,
            base.mutableEntityData.mutableEntityEquipment,
            local.mutableEntityData.mutableEntityEquipment,
            remote.mutableEntityData.mutableEntityEquipment
        ) { data, value -> data.mutableEntityData.mutableEntityEquipment = value.deepCopy() }
        accumulator.mergeList(
            path = DataFields.dropItems,
            base = base.mutableDropItems,
            local = local.mutableDropItems,
            remote = remote.mutableDropItems,
            copyValue = { it.copy() },
            targetList = { it.mutableDropItems }
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
