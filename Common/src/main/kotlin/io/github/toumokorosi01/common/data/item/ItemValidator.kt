package io.github.toumokorosi01.common.data.item

import io.github.toumokorosi01.common.StatsType
import io.github.toumokorosi01.common.data.core.validation.DataValidator
import io.github.toumokorosi01.common.data.core.validation.PropertyError
import io.github.toumokorosi01.common.data.item.data.ItemData

class ItemValidator(
    private val item: ItemData
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateDisplayInfo())
            addAll(validateStats())
            addAll(validateDetail())
        }
    }

    fun validateDisplayInfo(): List<PropertyError> {
        return item.display.validate()
    }

    fun validateDetail(): List<PropertyError> {
        return item.itemDetail.validate()
    }

    /**
     * stats全体が正常かどうかをチェックします。
     *
     * 各StatsTypeごとに、定義された最小値・最大値の範囲内かを確認します。
     *
     * @return statsに関するエラー一覧
     */
    fun validateStats(): List<PropertyError> {
        return item.stats.flatMap { (statsType, _) ->
            validateStat(statsType)
        }
    }

    /**
     * 指定したStatsTypeだけをチェックします。
     *
     * GUIで特定のステータス入力欄だけを更新したい場合に使います。
     *
     * @param statsType チェック対象のStatsType
     * @return 指定StatsTypeに関するエラー。正常なら空リスト。
     */
    fun validateStat(statsType: StatsType): List<PropertyError> {
        val value = item.stats[statsType] ?: return emptyList()

        val errors = mutableListOf<PropertyError>()

        if (value < statsType.min) errors.add(PropertyError(
            property = item::stats,
            message = "${statsType.display} に (${value}) は設定できません。${statsType.min} 以上の値を設定してください。",
            key = statsType
        ))

        if (value > statsType.max) errors.add(PropertyError(
            property = item::stats,
            message = "${statsType.display} に (${value}) は設定できません。${statsType.max} 以下の値を設定してください。",
            key = statsType
        ))

        return errors
    }
}