package io.github.toumokorosi01.common.data.ore

import io.github.toumokorosi01.common.data.core.ConfigurateDataManager
import io.github.toumokorosi01.common.data.ore.data.OreData

/**
 * [io.github.toumokorosi01.common.data.ore.data.OreData] のYAML保存・読み込みを担当するManager。
 */
object OreManager : ConfigurateDataManager<OreData>(
    dataClass = OreData::class.java
)