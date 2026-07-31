package io.github.toumokorosi01.sushiericdataeditor2.editor.store

import io.github.toumokorosi01.common.data.core.DataType
import io.github.toumokorosi01.common.data.core.ManagedData
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.item.ItemManager
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.mob.MobManager
import io.github.toumokorosi01.common.data.mob.data.MobData
import io.github.toumokorosi01.common.data.ore.OreManager
import io.github.toumokorosi01.common.data.ore.data.OreData
import io.github.toumokorosi01.sushiericdataeditor2.editor.merge.DataMerger
import io.github.toumokorosi01.sushiericdataeditor2.editor.merge.ItemDataMerger
import io.github.toumokorosi01.sushiericdataeditor2.editor.merge.MobDataMerger
import io.github.toumokorosi01.sushiericdataeditor2.editor.merge.OreDataMerger
import java.io.File

class EditorDataDescriptor<T : ManagedData<T, *>>(
    val dataType: DataType<T>,
    val load: (File, String?) -> T?,
    val save: (File, T, Set<String>?) -> Unit,
    val validate: (T, Set<String>) -> List<PropertyError>,
    val merger: DataMerger<T>
) {
    val displayName: String
        get() = dataType.displayName

    val relativeDirectory: String
        get() = dataType.dir.getRawPath()

    fun createDefault(id: String): T = dataType.createDefault(id)

    fun deepCopy(data: T): T = data.deepCopy()
}

object EditorDataDescriptors {
    val item = EditorDataDescriptor(
        dataType = DataType.Item,
        load = ItemManager::load,
        save = { file, data, _ -> ItemManager.save(file, data) },
        validate = { data, _ -> data.validate() },
        merger = ItemDataMerger
    )

    val ore = EditorDataDescriptor(
        dataType = DataType.Ore,
        load = OreManager::load,
        save = { file, data, _ -> OreManager.save(file, data) },
        validate = OreData::validate,
        merger = OreDataMerger
    )

    val mob = EditorDataDescriptor(
        dataType = DataType.Mob,
        load = MobManager::load,
        save = { file, data, itemIds ->
            if (itemIds == null) {
                MobManager.save(file, data)
            } else {
                MobManager.save(file, data, itemIds)
            }
        },
        validate = MobData::validate,
        merger = MobDataMerger
    )

    val all: List<EditorDataDescriptor<out ManagedData<*, *>>> = listOf(item, ore, mob)

    @Suppress("UNCHECKED_CAST")
    fun <T : ManagedData<T, *>> of(dataType: DataType<T>): EditorDataDescriptor<T> {
        return when (dataType) {
            DataType.Item -> item
            DataType.Ore -> ore
            DataType.Mob -> mob
        } as EditorDataDescriptor<T>
    }
}
