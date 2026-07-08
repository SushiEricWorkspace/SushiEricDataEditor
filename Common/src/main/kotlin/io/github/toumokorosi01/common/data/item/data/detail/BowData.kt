package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.data.core.validation.DataValidator
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.item.data.ItemType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class BowData(
    @Setting("multi")
    var multi: Int = 1,

    @Setting("short")
    var short: Boolean = false,

    @Setting("short-interval")
    var shortInterval: Double = 1.0,

    @Setting("pierce")
    var pierce: Int = 0,

    @Setting("angle")
    var angle: Double = 1.0
) : ItemDetailContent {

    override val itemType: ItemType
        get() = ItemType.BOW

    override fun deepCopy(): ItemDetailContent {
        return this.copy()
    }

    fun validator(): BowValidator {
        return BowValidator(this)
    }

    fun validate(): List<PropertyError> {
        return validator().validate()
    }
}

class BowValidator(
    private val bow: BowData
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateMulti())
            addAll(validateShortInterval())
            addAll(validatePierce())
            addAll(validateAngle())
        }
    }

    fun validateMulti(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (bow.multi < 0) errors.add(
            PropertyError(bow::multi, "0以上の値を設定してください。")
        )

        return errors
    }

    fun validateShortInterval(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (bow.shortInterval < 0.0) errors.add(
            PropertyError(bow::shortInterval, "0.0以上の値を設定してください。")
        )

        return errors
    }

    fun validatePierce(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (bow.pierce < 0) errors.add(
            PropertyError(bow::pierce, "0以上の値を設定してください。")
        )

        return errors
    }

    fun validateAngle(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (bow.angle < 0.0) errors.add(
            PropertyError(bow::angle, "0.0以上の値を設定してください。")
        )

        return errors
    }
}
