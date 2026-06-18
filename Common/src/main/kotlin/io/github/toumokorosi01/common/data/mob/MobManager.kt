package io.github.toumokorosi01.common.data.mob

import io.github.toumokorosi01.common.data.core.ConfigurateDataManager
import io.github.toumokorosi01.common.data.mob.data.MobData

/**
 * [io.github.toumokorosi01.common.data.mob.data.MobData] のYAML保存・読み込みを担当するManager。
 */
object MobManager : ConfigurateDataManager<MobData>(
    dataClass = MobData::class.java
)