package io.github.toumokorosi01.common.data.item.data

import io.github.toumokorosi01.common.data.item.data.detail.AxeData
import io.github.toumokorosi01.common.data.item.data.detail.BootsData
import io.github.toumokorosi01.common.data.item.data.detail.BowData
import io.github.toumokorosi01.common.data.item.data.detail.ChestplateData
import io.github.toumokorosi01.common.data.item.data.detail.CrossbowData
import io.github.toumokorosi01.common.data.item.data.detail.HelmetData
import io.github.toumokorosi01.common.data.item.data.detail.ItemDetailContent
import io.github.toumokorosi01.common.data.item.data.detail.LeggingsData
import io.github.toumokorosi01.common.data.item.data.detail.LongSwordData
import io.github.toumokorosi01.common.data.item.data.detail.OtherData
import io.github.toumokorosi01.common.data.item.data.detail.PotionData
import io.github.toumokorosi01.common.data.item.data.detail.ShieldData
import io.github.toumokorosi01.common.data.item.data.detail.ShortSwordData
import io.github.toumokorosi01.common.data.item.data.detail.SpearData
import io.github.toumokorosi01.common.data.item.data.detail.SwordData
import kotlin.reflect.KClass

enum class ItemType(
    val detailClass: KClass<out ItemDetailContent>,
    val createDefault: () -> ItemDetailContent,
    val display: String
) {
    SWORD(SwordData::class, ::SwordData, "sword"),
    SHORT_SWORD(ShortSwordData::class, ::ShortSwordData, "short sword"),
    LONG_SWORD(LongSwordData::class, ::LongSwordData, "long sword"),
    AXE(AxeData::class, ::AxeData, "axe"),
    BOW(BowData::class, ::BowData, "bow"),
    CROSSBOW(CrossbowData::class, ::CrossbowData, "crossbow"),
    SPEAR(SpearData::class, ::SpearData, "spear"),
    POTION(PotionData::class, ::PotionData, "potion"),
    SHIELD(ShieldData::class, ::ShieldData, "shield"),
    HELMET(HelmetData::class, ::HelmetData, "helmet"),
    CHESTPLATE(ChestplateData::class, ::ChestplateData, "chestplate"),
    LEGGINGS(LeggingsData::class, ::LeggingsData, "leggings"),
    BOOTS(BootsData::class, ::BootsData, "boots"),
    OTHER(OtherData::class, ::OtherData, "item");

    fun createContent(): ItemDetailContent {
        return createDefault()
    }
}