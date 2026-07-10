package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.data.core.VanillaIdConstraint
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.item.data.ItemType
import io.github.toumokorosi01.common.registry.ItemIdGroups
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class ShortBowData(
    @Setting("multi")
    override var multi: Int = 1,

    @Setting("pierce")
    override var pierce: Int = 0,

    @Setting("angle")
    override var angle: Double = 1.0,

    @Setting("short-interval")
    var shortInterval: Double = 1.0

) : BowContent {
    override val itemType: ItemType
        get() = ItemType.SHORT_BOW

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Fixed(ItemIdGroups.bow)

    override fun deepCopy(): ItemDetailContent {
        return this.copy()
    }

    fun validator(): ShortBowValidator {
        return ShortBowValidator(this)
    }

    override fun validate(): List<PropertyError> {
        return validator().validate()
    }
}

class ShortBowValidator(
    private val bow: ShortBowData
) : BowContentValidator (
    bow = bow
) {
    override fun validateAdditional(): List<PropertyError> {
        return validateShortInterval()
    }

    fun validateShortInterval(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (bow.shortInterval < 0.0) errors.add(
            PropertyError(bow::shortInterval, "0.0以上の値を設定してください。")
        )

        return errors
    }
}
