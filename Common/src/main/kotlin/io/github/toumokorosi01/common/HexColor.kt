package io.github.toumokorosi01.common

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

@JvmInline
value class HexColor private constructor(val value: String) {
    companion object {
        private val REGEX = Regex("^#?[0-9A-Fa-f]{6}$")

        fun of(value: String): HexColor {
            require(REGEX.matches(value)) {
                "HexColorは #RRGGBB または RRGGBB 形式である必要があります: $value"
            }

            val normalized = if (value.startsWith("#")) value else "#$value"
            return HexColor(normalized.uppercase())
        }

        fun orNull(value: String): HexColor? {
            return runCatching { of(value) }.getOrNull()
        }
    }

    override fun toString(): String = value
}

object HexColorSerializer : TypeSerializer<HexColor> {

    override fun deserialize(
        type: Type,
        node: ConfigurationNode
    ): HexColor {
        val value = node.string
            ?: return HexColor.of("#FFFFFF")

        return HexColor.of(value)
    }

    override fun serialize(
        type: Type,
        obj: HexColor?,
        node: ConfigurationNode
    ) {
        if (obj == null) {
            node.raw(null)
            return
        }

        node.set(obj.value)
    }
}