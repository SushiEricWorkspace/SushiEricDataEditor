package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.data.item.data.ItemType
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
class OtherData : ItemDetailContent {

    override val itemType: ItemType
        get() = ItemType.OTHER

    override fun deepCopy(): ItemDetailContent {
        return OtherData()
    }

    override fun equals(other: Any?): Boolean {
        return other is OtherData
    }

    override fun hashCode(): Int {
        return OtherData::class.hashCode()
    }
}