package io.github.rs0325.common.data.item.data.detail

import io.github.rs0325.common.data.core.VanillaIdConstraint
import io.github.rs0325.common.data.core.validation.DataValidator
import io.github.rs0325.common.data.core.validation.PropertyError
import io.github.rs0325.common.data.item.data.ItemType
import io.github.rs0325.common.registry.ItemIdGroups
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class LongSwordData(
    @Setting("cooldown")
    var cooldown: Double = 1.0,

    @Setting("range")
    var range: Double = 0.0
) : ItemDetailContent {

    override val itemType: ItemType
        get() = ItemType.LONG_SWORD

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Choices(
            ItemIdGroups.swords
        )

    override fun deepCopy(): ItemDetailContent {
        return this.copy()
    }

    fun validator(): LongSwordValidator {
        return LongSwordValidator(this)
    }

    fun validate(): List<PropertyError> {
        return validator().validate()
    }
}

class LongSwordValidator(
    private val longSword: LongSwordData
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateCooldown())
            addAll(validateRange())
        }
    }

    fun validateCooldown(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (longSword.cooldown < 0.0) errors.add(
            PropertyError(longSword::cooldown, "0.0以上の値を設定してください。")
        )

        return errors
    }

    fun validateRange(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (longSword.range < 0.0) errors.add(
            PropertyError(longSword::range, "0.0以上の値を設定してください。")
        )

        return errors
    }
}