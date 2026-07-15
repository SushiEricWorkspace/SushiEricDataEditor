package io.github.toumokorosi01.common.data.mob

import io.github.toumokorosi01.common.registry.VanillaIdRegistry
import io.github.toumokorosi01.common.stats.entity.EntityStatsType
import io.github.toumokorosi01.common.data.core.structure.DropItemData
import io.github.toumokorosi01.common.data.core.structure.DropItemValidator
import io.github.toumokorosi01.common.data.core.validation.DataValidator
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.mob.data.MobData

class MobValidator(
    private val mob: MobData,
    private val items: Set<String>
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateVanillaId())
            addAll(validateDisplayName())
            addAll(validateStats())
            addAll(validateDropItems())
        }
    }

    /**
     * displayNameが正常かどうかをチェックします。
     *
     * GUI上では、表示名入力欄の変更時にこの関数だけを呼ぶことで、
     * 表示名に関するエラーを即時表示できます。
     *
     * @return displayNameに関するエラー一覧
     */
    fun validateDisplayName(): List<PropertyError> {
        return if (mob.displayName.isBlank()) {
            listOf(PropertyError(mob::displayName, "表示名が空欄、または空白のみです。"))
        } else emptyList()
    }

    /**
     * vanillaIdが正常かどうかをチェックします。
     *
     * GUI上では、バニラID入力欄の変更時にこの関数だけを呼ぶことで、
     * 該当フィールドのエラー表示を即時更新できます。
     *
     * @return vanillaIdに関するエラー一覧
     */
    fun validateVanillaId(): List<PropertyError> {
        return if (!VanillaIdRegistry.allEntities.contains(mob.entityData.vanillaId)) {
            listOf(PropertyError(mob.entityData::vanillaId, "存在しないIDです。"))
        } else {
            emptyList()
        }
    }

    fun validateDropItems(): List<PropertyError> {
        return mob.dropItems.flatMap { dropItem ->
            validateDropItem(dropItem)
        }
    }

    fun validateDropItem(dropItem: DropItemData): List<PropertyError> = DropItemValidator(dropItem, items).validate()

    /**
     * stats全体が正常かどうかをチェックします。
     *
     * 各StatsTypeごとに、定義された最小値・最大値の範囲内かを確認します。
     *
     * @return statsに関するエラー一覧
     */
    fun validateStats(): List<PropertyError> {
        return mob.entityData.stats.flatMap { (statsType, _) ->
            validateStat(statsType)
        }
    }

    /**
     * 指定したStatsTypeだけをチェックします。
     *
     * GUIで特定のステータス入力欄だけを更新したい場合に使います。
     *
     * @param entityStatsType チェック対象のStatsType
     * @return 指定StatsTypeに関するエラー。正常なら空リスト。
     */
    fun validateStat(entityStatsType: EntityStatsType): List<PropertyError> {

        val value = mob.entityData.stats[entityStatsType] ?: return emptyList()

        val errorMessage = when {
            value < entityStatsType.min ->
                "${entityStatsType.display} に (${value}) は設定できません。${entityStatsType.min} 以上の値を設定してください。"

            value > entityStatsType.max ->
                "${entityStatsType.display} に (${value}) は設定できません。${entityStatsType.max} 以下の値を設定してください。"

            else -> null
        }

        return if (errorMessage != null) {
            listOf(
                PropertyError(
                    property = mob.entityData::stats,
                    key = entityStatsType,
                    message = errorMessage
                )
            )
        } else {
            emptyList()
        }

    }
}