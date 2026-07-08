package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.common.data.core.structure.ArmorTrimData
import io.github.toumokorosi01.common.data.item.data.ItemType
import io.github.toumokorosi01.common.registry.ItemIdGroups
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class LeggingsData(
    @Setting("color")
    override var color: HexColor? = null,

    @Setting("trim-data")
    override var trimData: ArmorTrimData? = ArmorTrimData()
) : ArmorContent {

    override val itemType: ItemType
        get() = ItemType.LEGGINGS

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Choices(
            ItemIdGroups.legs
        )

    override fun deepCopy(): ItemDetailContent {
        return this.copy(
            trimData = this.trimData?.copy()
        )
    }
}