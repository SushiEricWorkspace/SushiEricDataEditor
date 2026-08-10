package io.github.rs0325.common.data.item.data.detail

import io.github.rs0325.common.data.core.VanillaIdConstraint
import io.github.rs0325.common.data.core.validation.DataValidator
import io.github.rs0325.common.data.core.validation.PropertyError
import io.github.rs0325.common.data.item.data.ItemType
import io.github.rs0325.common.registry.ItemIdGroups
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class SpearData(
    @Setting("cooldown")
    var cooldown: Double = 1.0
) : ItemDetailContent {

    override val itemType: ItemType
        get() = ItemType.SPEAR

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Choices(ItemIdGroups.spears)

    override fun deepCopy(): ItemDetailContent {
        return this.copy()
    }

    fun validator(): SpearValidator {
        return SpearValidator(this)
    }

    fun validate(): List<PropertyError> {
        return validator().validate()
    }
}

class SpearValidator(
    private val spear: SpearData
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateCooldown())
        }
    }

    fun validateCooldown(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (spear.cooldown < 0.0) errors.add(
            PropertyError(spear::cooldown, "0.0以上の値を設定してください。")
        )

        return errors
    }
}
