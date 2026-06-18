package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.DataRegistry
import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.common.data.core.DeepCopyable
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

    @Setting("vanilla-id")
    var vanillaId: String = DataRegistry.defaultItem,

    @Setting("content")
    var content: ItemDetailContent = OtherData()
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
}

class ItemDetailValidator(
    private val detail: ItemDetail
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateVanillaId())
            addAll(validateContent())
        }
    }

    fun validateVanillaId(): List<PropertyError> {
        val errors = mutableListOf<PropertyError>()

        if (!DataRegistry.allItems.contains(detail.vanillaId)) errors.add(
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
}

sealed interface ItemDetailContent : DeepCopyable<ItemDetailContent> {
    val itemType: ItemType
}

interface ArmorContent : ItemDetailContent {
    var color: HexColor?
    var trimData: ArmorTrimData?
}