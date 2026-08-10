package io.github.rs0325.common.data.item.data.detail

import io.github.rs0325.common.data.core.VanillaIdConstraint
import io.github.rs0325.common.data.core.validation.PropertyError
import io.github.rs0325.common.data.item.data.ItemType
import io.github.rs0325.common.registry.ItemIdGroups
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class BowData(
    @Setting("multi")
    override var multi: Int = 1,

    @Setting("pierce")
    override var pierce: Int = 0,

    @Setting("angle")
    override var angle: Double = 1.0
) : BowContent {

    override val itemType: ItemType
        get() = ItemType.BOW

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Fixed(ItemIdGroups.bow)

    override fun deepCopy(): ItemDetailContent {
        return this.copy()
    }

    fun validator(): BowValidator {
        return BowValidator(this)
    }

    override fun validate(): List<PropertyError> {
        return validator().validate()
    }
}

class BowValidator(
    bow: BowData
) : BowContentValidator(
    bow = bow
)
