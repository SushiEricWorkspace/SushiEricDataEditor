package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.common.data.core.VanillaIdConstraint
import io.github.toumokorosi01.common.data.core.structure.PotionEffectData
import io.github.toumokorosi01.common.data.item.data.ItemType
import io.github.toumokorosi01.common.registry.ItemIdGroups
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class PotionData(
    @Setting("color")
    var color: HexColor = HexColor.of("#FFFFFF"),

    @Setting("effects")
    var effects: MutableList<PotionEffectData> = mutableListOf()
) : ItemDetailContent {

    override val itemType: ItemType
        get() = ItemType.POTION

    override val vanillaIdConstraint: VanillaIdConstraint =
        VanillaIdConstraint.Choices(
            ItemIdGroups.potions
        )

    override fun deepCopy(): ItemDetailContent {
        return this.copy(
            effects = this.effects
                .map { it.deepCopy() }
                .toMutableList()
        )
    }
}