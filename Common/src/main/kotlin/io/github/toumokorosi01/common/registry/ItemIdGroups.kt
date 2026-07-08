package io.github.toumokorosi01.common.registry

object ItemIdGroups {

    fun isLeather(armor: String): Boolean {
        return armor in listOf(
            "leather_boots",
            "leather_chestplate",
            "leather_helmet",
            "leather_leggings",
        )
    }

    fun isTurtle(helmet: String): Boolean {
        return helmet == "turtle_helmet"
    }

    val axes: List<String>
        get() = VanillaIdRegistry.allItems.filter { it.endsWith("_axe") }

    val swords: List<String>
        get() = VanillaIdRegistry.allItems.filter { it.endsWith("_sword") }

    val spears: List<String>
        get() = VanillaIdRegistry.allItems.filter { it.endsWith("_spear") }

    val helmets: List<String>
        get() = VanillaIdRegistry.allItems.filter { it.endsWith("_helmet") }

    val notTurtleHelmets: List<String>
        get() = VanillaIdRegistry.allItems.filter {
            it.endsWith("_helmet") && !isTurtle(it)
        }

    val chestplates: List<String>
        get() = VanillaIdRegistry.allItems.filter { it.endsWith("_chestplate") }

    val legs: List<String>
        get() = VanillaIdRegistry.allItems.filter { it.endsWith("_leggings") }

    val boots: List<String>
        get() = VanillaIdRegistry.allItems.filter { it.endsWith("_boots") }

    private val rawArmors = chestplates + legs + boots

    val armors: List<String>
        get() = rawArmors + helmets

    val notTurtleArmors: List<String>
        get() = rawArmors + notTurtleHelmets

    val potions: List<String>
        get() = VanillaIdRegistry.allItems.filter { it.endsWith("potion") }

    val bow: String
        get() = "bow"

    val crossBow: String
        get() = "crossbow"

    val shield: String
        get() = "shield"
}