package io.github.toumokorosi01.common.data.item.data

import io.github.toumokorosi01.common.data.core.DeepCopyable
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

/**
 * 表示名やLoreに関する構造
 */
@ConfigSerializable
data class DisplayInfo(
    /** 表示名 */
    @Setting("display-name")
    var displayName: String = "",
    /** 説明文 */
    @Setting("lore")
    var lore: MutableList<MutableList<LoreSection>> = mutableListOf()
) : DeepCopyable<DisplayInfo> {
    override fun deepCopy(): DisplayInfo {
        return this.copy(
            lore = this.lore
                .map { line ->
                    line.map { section ->
                        section.deepCopy()
                    }.toMutableList()
                }
                .toMutableList()
        )
    }

    fun validator(): DisplayInfoValidator {
        return DisplayInfoValidator(this)
    }

    fun validate(): List<PropertyError> {
        return validator().validate()
    }
}
