package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.common.registry.ItemIdGroups
import io.github.toumokorosi01.common.data.core.structure.ArmorTrimData
import io.github.toumokorosi01.common.data.item.data.ItemType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class ChestplateData(
    @Setting("color")
    override var color: HexColor? = null,

    @Setting("trim-data")
    override var trimData: ArmorTrimData? = ArmorTrimData()
) : ArmorContent {

    override val itemType: ItemType
        get() = ItemType.CHESTPLATE

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Choices(
            ItemIdGroups.chestplates
        )

    override fun deepCopy(): ItemDetailContent {
        return this.copy(
            trimData = this.trimData?.copy()
        )
    }
}