package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.data.core.validation.DataValidator
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.item.data.ItemType
import io.github.toumokorosi01.common.registry.ItemIdGroups
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class CrossbowData(
    @Setting("damage-range")
    var damageRange: Double = 1.0,

    @Setting("short-interval")
    var shortInterval: Double = 1.0
) : ItemDetailContent {

    override val itemType: ItemType
        get() = ItemType.CROSSBOW

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Fixed(ItemIdGroups.crossBow)

    override fun deepCopy(): ItemDetailContent {
        return this.copy()
    }

    fun validator(): CrossBowValidator {
        return CrossBowValidator(this)
    }

    fun validate(): List<PropertyError> {
        return validator().validate()
    }
}

class CrossBowValidator(
    private val crossbow: CrossbowData
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateDamageRange())
            addAll(validateShortInterval())
        }
    }

    fun validateDamageRange(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (crossbow.damageRange < 0.0) errors.add(
            PropertyError(crossbow::damageRange, "0.0以上の値を設定してください。")
        )

        return errors
    }

    fun validateShortInterval(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (crossbow.shortInterval < 0.0) errors.add(
            PropertyError(crossbow::shortInterval, "0.0以上の値を設定してください。")
        )

        return errors
    }
}