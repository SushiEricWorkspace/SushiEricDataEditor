package io.github.toumokorosi01.common.data.core.structure

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class ArmorTrimData(
    @Setting("pattern")
    var pattern: ArmorTrimRegistry.Pattern = ArmorTrimRegistry.Pattern.COAST,

    @Setting("material")
    var material: ArmorTrimRegistry.Material = ArmorTrimRegistry.Material.IRON
)

data class RegistryDisplayEntry(
    val id: String,
    val displayName: String
)

/**
 * Minecraft標準の防具装飾(Armor Trim) ID一覧。
 *
 * Minecraft/Fabric本体のRegistryを直接参照せず、
 * JavaFXエディター側でも安全に使える静的Registryとして管理する。
 *
 * 保存値には displayName ではなく、必ず id を使用する。
 */
object ArmorTrimRegistry {

    enum class Pattern(
        val id: String,
        val displayName: String
    ) {
        SENTRY("minecraft:sentry", "略奪者風"),
        DUNE("minecraft:dune", "砂丘風"),
        COAST("minecraft:coast", "海洋風"),
        WILD("minecraft:wild", "密林風"),
        WARD("minecraft:ward", "監獄風"),
        EYE("minecraft:eye", "要塞風"),
        VEX("minecraft:vex", "ヴェックス風"),
        TIDE("minecraft:tide", "潮流風"),
        SNOUT("minecraft:snout", "ブタの鼻風"),
        RIB("minecraft:rib", "あばら模様"),
        SPIRE("minecraft:spire", "尖塔風"),
        WAYFINDER("minecraft:wayfinder", "先駆者風"),
        SHAPER("minecraft:shaper", "職人風"),
        SILENCE("minecraft:silence", "静寂風"),
        RAISER("minecraft:raiser", "牧者風"),
        HOST("minecraft:host", "主人風"),
        FLOW("minecraft:flow", "渦巻き風"),
        BOLT("minecraft:bolt", "稲妻風");

        companion object {
            val ids: List<String> = entries.map { it.id }

            val displayEntries: List<RegistryDisplayEntry> =
                entries.map { RegistryDisplayEntry(it.id, it.displayName) }

            fun contains(id: String): Boolean {
                return entries.any { it.id == id }
            }

            fun displayNameOf(id: String): String {
                return entries.firstOrNull { it.id == id }?.displayName ?: id
            }

            fun fromId(id: String): Pattern? {
                return entries.firstOrNull { it.id == id }
            }
        }
    }

    enum class Material(
        val id: String,
        val displayName: String
    ) {
        QUARTZ("minecraft:quartz", "クォーツ"),
        IRON("minecraft:iron", "鉄"),
        NETHERITE("minecraft:netherite", "ネザライト"),
        REDSTONE("minecraft:redstone", "レッドストーン"),
        COPPER("minecraft:copper", "銅"),
        GOLD("minecraft:gold", "金"),
        EMERALD("minecraft:emerald", "エメラルド"),
        DIAMOND("minecraft:diamond", "ダイヤモンド"),
        LAPIS("minecraft:lapis", "ラピスラズリ"),
        AMETHYST("minecraft:amethyst", "アメジスト"),
        RESIN("minecraft:resin", "樹脂");

        companion object {
            val ids: List<String> = entries.map { it.id }

            val displayEntries: List<RegistryDisplayEntry> =
                entries.map { RegistryDisplayEntry(it.id, it.displayName) }

            fun contains(id: String): Boolean {
                return entries.any { it.id == id }
            }

            fun displayNameOf(id: String): String {
                return entries.firstOrNull { it.id == id }?.displayName ?: id
            }

            fun fromId(id: String): Material? {
                return entries.firstOrNull { it.id == id }
            }
        }
    }
}