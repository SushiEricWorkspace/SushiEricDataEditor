package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.data.item.data.ItemType
import io.github.toumokorosi01.common.registry.ItemIdGroups
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
class SwordData : ItemDetailContent {

    override val itemType: ItemType
        get() = ItemType.SWORD

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Choices(
            ItemIdGroups.swords
        )

    override fun deepCopy(): ItemDetailContent {
        return SwordData()
    }

    override fun equals(other: Any?): Boolean {
        return other is SwordData
    }

    override fun hashCode(): Int {
        return SwordData::class.hashCode()
    }
}