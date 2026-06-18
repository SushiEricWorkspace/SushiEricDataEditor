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
) {
    SWORD(SwordData::class, ::SwordData),
    SHORT_SWORD(ShortSwordData::class, ::ShortSwordData),
    LONG_SWORD(LongSwordData::class, ::LongSwordData),
    AXE(AxeData::class, ::AxeData),
    BOW(BowData::class, ::BowData),
    CROSSBOW(CrossbowData::class, ::CrossbowData),
    SPEAR(SpearData::class, ::SpearData),
    POTION(PotionData::class, ::PotionData),
    SHIELD(ShieldData::class, ::ShieldData),
    HELMET(HelmetData::class, ::HelmetData),
    CHESTPLATE(ChestplateData::class, ::ChestplateData),
    LEGGINGS(LeggingsData::class, ::LeggingsData),
    BOOTS(BootsData::class, ::BootsData),
    OTHER(OtherData::class, ::OtherData);

    fun createContent(): ItemDetailContent {
        return createDefault()
    }
}