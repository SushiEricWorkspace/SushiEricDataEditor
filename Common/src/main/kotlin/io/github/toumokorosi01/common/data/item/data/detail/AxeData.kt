package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.data.item.data.ItemType
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
class AxeData : ItemDetailContent {

    override val itemType: ItemType
        get() = ItemType.AXE

    override fun deepCopy(): ItemDetailContent {
        return AxeData()
    }

    override fun equals(other: Any?): Boolean {
        return other is AxeData
    }

    override fun hashCode(): Int {
        return AxeData::class.hashCode()
    }
}