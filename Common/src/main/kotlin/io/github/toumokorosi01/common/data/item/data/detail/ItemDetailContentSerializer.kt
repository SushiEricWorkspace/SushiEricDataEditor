package io.github.toumokorosi01.common.data.item.data.detail

import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.common.data.core.structure.ArmorTrimData
import io.github.toumokorosi01.common.data.core.structure.PotionEffectData
import io.github.toumokorosi01.common.data.item.data.ItemType
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object ItemDetailContentSerializer : TypeSerializer<ItemDetailContent> {

    override fun deserialize(
        type: Type,
        node: ConfigurationNode
    ): ItemDetailContent {
        val itemType = node.node("item-type").get(ItemType::class.java)
            ?: ItemType.OTHER

        return when (itemType) {
            ItemType.SWORD -> SwordData()
            ItemType.SHORT_SWORD -> ShortSwordData()
            ItemType.LONG_SWORD -> LongSwordData(
                cooldown = node.node("cooldown").getDouble(1.0)
            )
            ItemType.AXE -> AxeData()
            ItemType.BOW -> BowData(
                multi = node.node("multi").getInt(1),
                short = node.node("short").getBoolean(false),
                shortInterval = node.node("short-interval").getDouble(1.0),
                pierce = node.node("pierce").getInt(0)
            )
            ItemType.CROSSBOW -> CrossbowData(
                damageRange = node.node("damage-range").getDouble(1.0),
                shortInterval = node.node("short-interval").getDouble(1.0)
            )
            ItemType.SPEAR -> SpearData(
                cooldown = node.node("cooldown").getDouble(1.0)
            )
            ItemType.POTION -> PotionData(
                color = node.node("color").get(HexColor::class.java)
                    ?: HexColor.of("#FFFFFF"),
                effects = node.node("effects")
                    .getList(PotionEffectData::class.java, emptyList())
                    .toMutableList()
            )
            ItemType.SHIELD -> ShieldData(
                cooldown = node.node("cooldown").getDouble(1.0),
                defenceRate = node.node("defence-rate").getDouble(1.0)
            )
            ItemType.HELMET -> HelmetData(
                color = node.node("color").get(HexColor::class.java),
                trimData = node.node("trim-data").get(ArmorTrimData::class.java)
            )
            ItemType.CHESTPLATE -> ChestplateData(
                color = node.node("color").get(HexColor::class.java),
                trimData = node.node("trim-data").get(ArmorTrimData::class.java)
            )
            ItemType.LEGGINGS -> LeggingsData(
                color = node.node("color").get(HexColor::class.java),
                trimData = node.node("trim-data").get(ArmorTrimData::class.java)
            )
            ItemType.BOOTS -> BootsData(
                color = node.node("color").get(HexColor::class.java),
                trimData = node.node("trim-data").get(ArmorTrimData::class.java)
            )
            ItemType.OTHER -> OtherData()
        }
    }

    override fun serialize(
        type: Type,
        obj: ItemDetailContent?,
        node: ConfigurationNode
    ) {
        if (obj == null) {
            node.raw(null)
            return
        }

        node.node("item-type").set(ItemType::class.java, obj.itemType)

        when (obj) {
            is SwordData -> Unit

            is ShortSwordData -> Unit

            is LongSwordData -> {
                node.node("cooldown").set(obj.cooldown)
            }

            is AxeData -> Unit

            is BowData -> {
                node.node("multi").set(obj.multi)
                node.node("short").set(obj.short)
                node.node("short-interval").set(obj.shortInterval)
                node.node("pierce").set(obj.pierce)
            }

            is CrossbowData -> {
                node.node("damage-range").set(obj.damageRange)
                node.node("short-interval").set(obj.shortInterval)
            }

            is SpearData -> {
                node.node("cooldown").set(obj.cooldown)
            }

            is PotionData -> {
                node.node("color").set(HexColor::class.java, obj.color)
                node.node("effects").setList(PotionEffectData::class.java, obj.effects)
            }

            is ShieldData -> {
                node.node("cooldown").set(obj.cooldown)
                node.node("defence-rate").set(obj.defenceRate)
            }

            is HelmetData -> {
                node.node("color").set(HexColor::class.java, obj.color)
                node.node("trim-data").set(ArmorTrimData::class.java, obj.trimData)
            }

            is ChestplateData -> {
                node.node("color").set(HexColor::class.java, obj.color)
                node.node("trim-data").set(ArmorTrimData::class.java, obj.trimData)
            }

            is LeggingsData -> {
                node.node("color").set(HexColor::class.java, obj.color)
                node.node("trim-data").set(ArmorTrimData::class.java, obj.trimData)
            }

            is BootsData -> {
                node.node("color").set(HexColor::class.java, obj.color)
                node.node("trim-data").set(ArmorTrimData::class.java, obj.trimData)
            }

            is OtherData -> Unit

            else -> {
                error("未対応のItemDetailContentです: ${obj::class.qualifiedName}")
            }
        }
    }
}