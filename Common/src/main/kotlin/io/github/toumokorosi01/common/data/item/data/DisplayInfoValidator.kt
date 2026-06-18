package io.github.toumokorosi01.common.data.item.data

import io.github.toumokorosi01.common.data.core.validation.DataValidator
import io.github.toumokorosi01.common.data.core.validation.PropertyError

class DisplayInfoValidator(
    private val displayInfo: DisplayInfo
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateDisplayName())
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
        val errors = mutableListOf<PropertyError>()

        if (displayInfo.displayName.isBlank()) {
            errors.add(
                PropertyError(displayInfo::displayName, "表示名が空欄、または空白のみです。")
            )
        }

        return errors
    }
}