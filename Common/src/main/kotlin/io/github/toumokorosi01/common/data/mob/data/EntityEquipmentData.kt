package io.github.toumokorosi01.common.data.mob.data

import io.github.toumokorosi01.common.data.core.DeepCopyable

data class EntityEquipmentData(
    var head: EntityArmorData? = null,
    var chest: EntityArmorData? = null,
    var legs: EntityArmorData? = null,
    var feet: EntityArmorData? = null,
    var mainHand: EntityHoldData? = null,
    var offHand: EntityHoldData? = null
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
