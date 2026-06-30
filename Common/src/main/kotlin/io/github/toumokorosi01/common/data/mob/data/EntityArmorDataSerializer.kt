package io.github.toumokorosi01.common.data.mob.data

import io.github.toumokorosi01.common.DataRegistry
import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.common.data.core.structure.ArmorTrimData
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object EntityArmorDataSerializer : TypeSerializer<EntityArmorData> {
    override fun deserialize(type: Type, node: ConfigurationNode): EntityArmorData {
        return EntityArmorData(
            vanillaId = node.node("vanilla-id").getString(DataRegistry.defaultItem),
            enchantAura = node.node("enchant-aura").getBoolean(false),
            color = node.node("color").get(HexColor::class.java),
            trimData = node.node("trim-data").get(ArmorTrimData::class.java)
        )
    }

    override fun serialize(type: Type, obj: EntityArmorData?, node: ConfigurationNode) {
        node.raw(null)
        if (obj == null) return

        node.node("vanilla-id").set(obj.vanillaId)
        node.node("enchant-aura").set(obj.enchantAura)
        node.node("color").set(HexColor::class.java, obj.color)
        node.node("trim-data").set(ArmorTrimData::class.java, obj.trimData)
    }
}