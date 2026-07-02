package io.github.toumokorosi01.common.data.mob.data

import io.github.toumokorosi01.common.DataRegistry
import io.github.toumokorosi01.common.EntityStatsType
import io.github.toumokorosi01.common.data.core.DeepCopyable
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class EntityDetailData(
    @Setting("vanilla-id")
    var vanillaId: String = DataRegistry.defaultEntity,

    @Setting("stats")
    var stats: MutableMap<EntityStatsType, Double> =
        EntityStatsType.entries
            .associateWith { type -> type.default }
            .toMutableMap(),

    @Setting("equipment")
    var entityEquipment: EntityEquipmentData = EntityEquipmentData()
) : DeepCopyable<EntityDetailData> {
    override fun deepCopy(): EntityDetailData {
        return this.copy(
            stats = this.stats.toMutableMap(),
            entityEquipment = this.entityEquipment.deepCopy()
        )
    }

    fun safeGetStats(type: EntityStatsType): Double {
        return stats.getOrDefault(type, type.default)
    }
}