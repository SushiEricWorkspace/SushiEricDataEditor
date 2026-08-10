package io.github.rs0325.common.data.mob

import io.github.rs0325.common.data.core.ConfigurateDataManager
import io.github.rs0325.common.data.mob.data.MobData
import java.io.File

/**
 * [io.github.rs0325.common.data.mob.data.MobData] のYAML保存・読み込みを担当するManager。
 */
object MobManager : ConfigurateDataManager<MobData>(
    dataClass = MobData::class.java
) {
    fun save(file: File, data: MobData, itemIds: Set<String>) {
        val itemToSave = data.copy().apply {
            refreshCompleted(validate(itemIds))
        }

        super.save(file, itemToSave)
    }
}