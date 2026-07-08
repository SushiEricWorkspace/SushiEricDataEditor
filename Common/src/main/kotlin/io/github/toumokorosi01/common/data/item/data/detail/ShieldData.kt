package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.data.core.validation.DataValidator
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.item.data.ItemType
import io.github.toumokorosi01.common.registry.ItemIdGroups
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class ShieldData(
    @Setting("cooldown")
    var cooldown: Double = 1.0,

    @Setting("defence-rate")
    var defenceRate: Double = 1.0
) : ItemDetailContent {

    override val itemType: ItemType
        get() = ItemType.SHIELD

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Fixed(ItemIdGroups.shield)

    override fun deepCopy(): ItemDetailContent {
        return this.copy()
    }

    fun validator(): ShieldValidator {
        return ShieldValidator(this)
    }

    fun validate(): List<PropertyError> {
        return validator().validate()
    }
}

class ShieldValidator(
    private val shield: ShieldData
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateCooldown())
            addAll(validateDefenceRate())
        }
    }

    fun validateCooldown(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (shield.cooldown < 0.0) errors.add(
            PropertyError(shield::cooldown, "0.0以上の値を設定してください。")
        )

        return errors
    }

    fun validateDefenceRate(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (shield.defenceRate < 0.0) errors.add(
            PropertyError(shield::cooldown, "0.0以上の値を設定してください。")
        )

        return errors
    }
}
