package io.github.rs0325.common.data.mob.data

import io.github.rs0325.common.data.core.DeepCopyable
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class EntityEquipmentData(
    @Setting("head")
    var head: EntityArmorData? = null,
    @Setting("chest")
    var chest: EntityArmorData? = null,
    @Setting("legs")
    var legs: EntityArmorData? = null,
    @Setting("feet")
    var feet: EntityArmorData? = null,
    @Setting("main-hand")
    var mainHand: EntityHoldData? = null,
    @Setting("off-hand")
    var offHand: EntityHoldData? = null,
) : DeepCopyable<EntityEquipmentData> {
    override fun deepCopy(): EntityEquipmentData {
        return this.copy(
            head = head?.deepCopy(),
            chest = chest?.deepCopy(),
            legs = legs?.deepCopy(),
            feet = feet?.deepCopy(),
            mainHand = mainHand?.copy(),
            offHand = offHand?.copy()
        )
    }
}
