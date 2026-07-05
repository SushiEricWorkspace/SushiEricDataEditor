package io.github.toumokorosi01.common.data.mob

import io.github.toumokorosi01.common.Dir
import io.github.toumokorosi01.common.data.core.ConfigurateDataManager
import io.github.toumokorosi01.common.data.item.ItemManager
import io.github.toumokorosi01.common.data.mob.data.MobData
import java.io.File

/**
 * [io.github.toumokorosi01.common.data.mob.data.MobData] のYAML保存・読み込みを担当するManager。
 */
object MobManager : ConfigurateDataManager<MobData>(
    dataClass = MobData::class.java
) {
    override fun save(file: File, data: MobData) {
        val itemToSave = data.copy().apply {
            val items = ItemManager.loadAll(Dir.Item.Stats.resolve(File(Dir.BASE_ROOT)))
            refreshCompleted(validate(items))
        }

        super.save(file, itemToSave)
    }
}