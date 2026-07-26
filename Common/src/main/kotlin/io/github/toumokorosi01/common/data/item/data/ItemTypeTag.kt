package io.github.toumokorosi01.common.data.item.data

import io.github.toumokorosi01.common.data.item.data.ItemType.*

enum class ItemTypeTag(val itemTypes: Set<ItemType>) {
    MELEE(setOf(
        SWORD, SHORT_SWORD, LONG_SWORD,
        AXE, SPEAR
    )),
    SWORDS(setOf(
        SWORD, SHORT_SWORD, LONG_SWORD
    )),
    PROJECTILE(setOf(
        BOW, SHORT_BOW, CROSSBOW
    )),
    ARMORS(setOf(
        HELMET, CHESTPLATE, LEGGINGS, BOOTS
    ))
}