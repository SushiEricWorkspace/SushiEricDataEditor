package io.github.rs0325.common.data.item.data.detail

import io.github.rs0325.common.HexColor
import io.github.rs0325.common.data.core.VanillaIdConstraint
import io.github.rs0325.common.data.core.structure.ArmorTrimData
import io.github.rs0325.common.data.item.data.ItemType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class HelmetData(
    @Setting("color")
    override var color: HexColor? = null,

    @Setting("trim-data")
    override var trimData: ArmorTrimData? = ArmorTrimData()
) : ArmorContent {

    override val itemType: ItemType
        get() = ItemType.HELMET

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Free

    override fun deepCopy(): ItemDetailContent {
        return this.copy(
            trimData = this.trimData?.copy()
        )
    }
}