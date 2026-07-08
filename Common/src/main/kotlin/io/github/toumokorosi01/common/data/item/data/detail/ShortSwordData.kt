package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.data.item.data.ItemType
import io.github.toumokorosi01.common.registry.ItemIdGroups
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
class ShortSwordData : ItemDetailContent {

    override val itemType: ItemType
        get() = ItemType.SHORT_SWORD

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Choices(
            ItemIdGroups.swords
        )

    override fun deepCopy(): ItemDetailContent {
        return ShortSwordData()
    }

    override fun equals(other: Any?): Boolean {
        return other is ShortSwordData
    }

    override fun hashCode(): Int {
        return ShortSwordData::class.hashCode()
    }
}