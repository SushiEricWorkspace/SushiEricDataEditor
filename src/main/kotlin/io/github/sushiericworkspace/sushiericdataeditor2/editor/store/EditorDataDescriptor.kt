package io.github.sushiericworkspace.sushiericdataeditor2.editor.store

import io.github.sushiericworkspace.common.data.core.SushiEricDataType
import io.github.sushiericworkspace.common.data.core.ManagedData
import io.github.sushiericworkspace.common.data.core.validation.SushiEricValidationError
import io.github.sushiericworkspace.common.data.item.ItemManager
import io.github.sushiericworkspace.common.data.item.model.ItemBaseData
import io.github.sushiericworkspace.common.data.mob.MobManager
import io.github.sushiericworkspace.common.data.mob.model.MobBaseData
import io.github.sushiericworkspace.common.data.ore.OreManager
import io.github.sushiericworkspace.common.data.ore.model.OreBaseData
import io.github.sushiericworkspace.sushiericdataeditor2.editor.merge.DataMerger
import io.github.sushiericworkspace.sushiericdataeditor2.editor.merge.ItemDataMerger
import io.github.sushiericworkspace.sushiericdataeditor2.editor.merge.MobDataMerger
import io.github.sushiericworkspace.sushiericdataeditor2.editor.merge.OreDataMerger
import java.io.File

class EditorDataDescriptor<T : ManagedData<T, *>>(
    val dataType: SushiEricDataType<T>,
    val load: (File, String?) -> T?,
    val save: (File, T, Set<String>?) -> Unit,
    val validate: (T, Set<String>) -> List<SushiEricValidationError>,
    val merger: DataMerger<T>,
    private val duplicateForNewEntry: (T) -> T = { it.deepCopy() }
) {
    val displayName: String
        get() = dataType.displayName

    val relativeDirectory: String
        get() = dataType.dir.getRawPath()

    fun createDefault(id: String): T = dataType.createDefault(id)

    fun deepCopy(data: T): T = data.deepCopy()

    /**
     * 既存データを別データとして複製し、新しい公開IDを設定します。
     *
     * データ種別固有の永続識別子がある場合は、[duplicateForNewEntry]側で再生成します。
     */
    fun duplicateAsNew(data: T, newId: String): T = duplicateForNewEntry(data).apply {
        id = newId
    }
}

object EditorDataDescriptors {
    val item = EditorDataDescriptor(
        dataType = SushiEricDataType.Item,
        load = ItemManager::load,
        save = { file, data, _ -> ItemManager.save(file, data) },
        validate = { data, _ -> data.validate() },
        merger = ItemDataMerger,
        duplicateForNewEntry = ItemBaseData::duplicateAsNew
    )

    val ore = EditorDataDescriptor(
        dataType = SushiEricDataType.Ore,
        load = OreManager::load,
        save = { file, data, _ -> OreManager.save(file, data) },
        validate = OreBaseData::validate,
        merger = OreDataMerger
    )

    val mob = EditorDataDescriptor(
        dataType = SushiEricDataType.Mob,
        load = MobManager::load,
        save = { file, data, itemIds ->
            if (itemIds == null) {
                MobManager.save(file, data)
            } else {
                MobManager.save(file, data, itemIds)
            }
        },
        validate = MobBaseData::validate,
        merger = MobDataMerger
    )

    val all: List<EditorDataDescriptor<out ManagedData<*, *>>> = listOf(item, ore, mob)

    @Suppress("UNCHECKED_CAST")
    fun <T : ManagedData<T, *>> of(dataType: SushiEricDataType<T>): EditorDataDescriptor<T> {
        return when (dataType) {
            SushiEricDataType.Item -> item
            SushiEricDataType.Ore -> ore
            SushiEricDataType.Mob -> mob
        } as EditorDataDescriptor<T>
    }
}
