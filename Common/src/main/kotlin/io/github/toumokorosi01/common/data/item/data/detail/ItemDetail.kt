package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.registry.VanillaIdRegistry
import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.common.data.core.DeepCopyable
import io.github.toumokorosi01.common.data.core.VanillaIdConstraint
import io.github.toumokorosi01.common.data.core.structure.ArmorTrimData
import io.github.toumokorosi01.common.data.core.validation.DataValidator
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.item.data.ItemType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class ItemDetail(
    @Setting("enchant-aura")
    var enchantAura: Boolean = false,

    @Setting("content")
    var content: ItemDetailContent = OtherData(),

    @Setting("vanilla-id")
    var vanillaId: String = content.vanillaIdConstraint.choices().firstOrNull()
        ?: VanillaIdRegistry.defaultItem,

    @Setting("max-stack-size")
    var maxStackSize: Int = 1
) : DeepCopyable<ItemDetail> {

    val itemType: ItemType
        get() = content.itemType

    override fun deepCopy(): ItemDetail {
        return this.copy(
            content = this.content.deepCopy()
        )
    }

    fun validator(): ItemDetailValidator {
        return ItemDetailValidator(this)
    }

    fun validate(): List<PropertyError> {
        return validator().validate()
    }

    fun normalizeVanillaIdByContent() {
        val choices = content.vanillaIdConstraint.choices()

        if (choices.isEmpty()) {
            return
        }

        if (vanillaId !in choices) {
            vanillaId = choices.first()
        }
    }
}

class ItemDetailValidator(
    private val detail: ItemDetail
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateVanillaId())
            addAll(validateContent())
            addAll(validateMaxStackSize())
        }
    }

    fun validateVanillaId(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (!VanillaIdRegistry.allItems.contains(detail.vanillaId)) errors.add(
            PropertyError(detail::vanillaId, "存在しないIDです。")
        )

        return errors
    }

    fun validateContent(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        val add: List<PropertyError> = when (val content = detail.content) {
            is LongSwordData -> content.validate()
            is BowData -> content.validate()
            is CrossbowData -> content.validate()
            is SpearData -> content.validate()
            is ShieldData -> content.validate()
            else -> emptyList()
        }

        errors.addAll(add)

        return errors
    }

    fun validateMaxStackSize(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (detail.maxStackSize !in 1..99) errors.add(
            PropertyError(detail::maxStackSize, "範囲外の数値です。")
        )

        return errors
    }
}

sealed interface ItemDetailContent : DeepCopyable<ItemDetailContent> {
    val itemType: ItemType

    val vanillaIdConstraint: VanillaIdConstraint
        get() = VanillaIdConstraint.Free
}

interface ArmorContent : ItemDetailContent {
    var color: HexColor?
    var trimData: ArmorTrimData?
}