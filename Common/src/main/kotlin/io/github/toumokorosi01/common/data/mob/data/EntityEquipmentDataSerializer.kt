package io.github.toumokorosi01.common.data.mob.data

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object EntityEquipmentDataSerializer : TypeSerializer<EntityEquipmentData> {
    override fun deserialize(type: Type, node: ConfigurationNode): EntityEquipmentData {
        return EntityEquipmentData(
            head = node.getIfPresent("head", EntityArmorData::class.java),
            chest = node.getIfPresent("chest", EntityArmorData::class.java),
            legs = node.getIfPresent("legs", EntityArmorData::class.java),
            feet = node.getIfPresent("feet", EntityArmorData::class.java),
            mainHand = node.getIfPresent("main-hand", EntityHoldData::class.java),
            offHand = node.getIfPresent("off-hand", EntityHoldData::class.java)
        )
    }

    override fun serialize(type: Type, obj: EntityEquipmentData?, node: ConfigurationNode) {
        node.raw(null)

        if (obj == null) return

        obj.head?.let {
            node.node("head").set(EntityArmorData::class.java, it)
        }

        obj.chest?.let {
            node.node("chest").set(EntityArmorData::class.java, it)
        }

        obj.legs?.let {
            node.node("legs").set(EntityArmorData::class.java, it)
        }

        obj.feet?.let {
            node.node("feet").set(EntityArmorData::class.java, it)
        }

        obj.mainHand?.let {
            node.node("main-hand").set(EntityHoldData::class.java, it)
        }

        obj.offHand?.let {
            node.node("off-hand").set(EntityHoldData::class.java, it)
        }
    }

    private fun <T : Any> ConfigurationNode.getIfPresent(
        key: String,
        type: Class<T>
    ): T? {
        val child = node(key)

        if (child.virtual()) return null
        if (child.empty()) return null

        return child.get(type)
    }
}