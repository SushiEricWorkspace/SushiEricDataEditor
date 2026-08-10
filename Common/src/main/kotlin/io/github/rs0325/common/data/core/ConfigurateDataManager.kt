package io.github.rs0325.common.data.core

import io.github.rs0325.common.ComponentSerializer
import io.github.rs0325.common.HexColor
import io.github.rs0325.common.HexColorSerializer
import io.github.rs0325.common.data.item.data.LoreSection
import io.github.rs0325.common.data.item.data.LoreSectionSerializer
import io.github.rs0325.common.data.item.data.detail.ItemDetailContent
import io.github.rs0325.common.data.item.data.detail.ItemDetailContentSerializer
import io.github.rs0325.common.data.mob.data.EntityArmorData
import io.github.rs0325.common.data.mob.data.EntityArmorDataSerializer
import io.github.rs0325.common.data.mob.data.EntityEquipmentData
import io.github.rs0325.common.data.mob.data.EntityEquipmentDataSerializer
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Path

abstract class ConfigurateDataManager<T : ManagedData<T, *>>(
    private val dataClass: Class<T>
) {
    open fun save(file: File, data: T) {
        file.parentFile?.mkdirs()

        val loader = createLoader(file.toPath())

        try {
            val node = loader.load()
            node.set(dataClass, data)
            loader.save(node)
        } catch (e: ConfigurateException) {
            e.printStackTrace()
        }
    }

    open fun load(file: File, preferredId: String? = null): T? {
        if (!file.exists()) return null

        val loader = createLoader(file.toPath())

        return try {
            val node = loader.load()
            val data = node.get(dataClass) ?: return null

            data.id = preferredId ?: file.nameWithoutExtension

            data
        } catch (e: ConfigurateException) {
            e.printStackTrace()
            null
        }
    }

    fun loadAll(dir: File): List<T> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles { file ->
            file.extension.equals("yml", ignoreCase = true)
        }?.mapNotNull { file ->
            load(file)
        } ?: emptyList()
    }

    private fun createLoader(path: Path): YamlConfigurationLoader {
        return YamlConfigurationLoader.builder()
            .path(path)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.register(HexColor::class.java, HexColorSerializer)
                    builder.register(Component::class.java, ComponentSerializer)
                    builder.register(LoreSection::class.java, LoreSectionSerializer)
                    builder.register(ItemDetailContent::class.java, ItemDetailContentSerializer)
                    builder.register(EntityEquipmentData::class.java, EntityEquipmentDataSerializer)
                    builder.register(EntityArmorData::class.java, EntityArmorDataSerializer)
                }
            }
            .build()
    }
}