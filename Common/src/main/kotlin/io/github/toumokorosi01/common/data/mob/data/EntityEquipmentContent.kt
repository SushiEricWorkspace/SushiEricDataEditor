package io.github.toumokorosi01.common.data.mob.data

import io.github.toumokorosi01.common.DataRegistry
import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.common.data.core.DeepCopyable
import io.github.toumokorosi01.common.data.core.structure.ArmorTrimData
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class EntityArmorData(
    @Setting("vanilla-id")
    var vanillaId: String = DataRegistry.defaultItem,
    @Setting("enchant-aura")
    var enchantAura: Boolean = false,
    @Setting("color")
    var color: HexColor? = null,
    @Setting("trim-data")
    var trimData: ArmorTrimData? = ArmorTrimData()
) : DeepCopyable<EntityArmorData> {
    override fun deepCopy(): EntityArmorData {
        return this.copy(
            trimData = this.trimData?.copy()
        )
    }
}

@ConfigSerializable
data class EntityHoldData(
    @Setting("vanilla-id")
    var vanillaId: String = DataRegistry.defaultItem,
    @Setting("enchant-aura")
    var enchantAura: Boolean = false
)
