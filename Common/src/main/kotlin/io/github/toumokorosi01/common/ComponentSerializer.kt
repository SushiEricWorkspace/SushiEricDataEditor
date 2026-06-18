package io.github.toumokorosi01.common

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

/**
 * Adventure Component と String を相互変換するための Configurate 用シリアライザー
 */
object ComponentSerializer : TypeSerializer<Component> {
    private val mm = MiniMessage.miniMessage()

    /**
     * YAML(Node)から読み込む際の処理
     */
    override fun deserialize(type: Type, node: ConfigurationNode): Component? {
        val str = node.string ?: return null
        return mm.deserialize(str)
    }

    /**
     * YAML(Node)へ書き込む際の処理
     */
    override fun serialize(type: Type, obj: Component?, node: ConfigurationNode) {
        if (obj == null) {
            node.set(null)
            return
        }
        node.set(mm.serialize(obj))
    }
}