package io.github.toumokorosi01.common.data.core.structure

import io.github.toumokorosi01.common.data.core.validation.DataValidator
import io.github.toumokorosi01.common.data.core.validation.PropertyError

class DropItemValidator(
    private val dropItem: DropItemData,
    private val items: Set<String>
) : DataValidator {
    override fun validate(): List<PropertyError> {
        return buildList {
            addAll(validateId())
            addAll(validateN())
            addAll(validateP())
        }
    }

    fun validateId(): List<PropertyError> {
        return if (!items.any { it == dropItem.id }) {
            listOf(PropertyError(
                property = dropItem::id,
                message = "存在しないアイテムIDです。"
            ))
        } else {
            emptyList()
        }
    }

    fun validateN(): List<PropertyError> {
        return if (dropItem.n < 1) {
            listOf(PropertyError(
                property = dropItem::n,
                message = "試行回数に ${dropItem.n} は設定できません。1 以上の値を設定してください。"
            ))
        } else emptyList()
    }

    fun validateP(): List<PropertyError> {
        val error = when {
            dropItem.p < 0.0 -> PropertyError(
                property = dropItem::p,
                message = "成功確率に ${dropItem.p} は設定できません。0.0 以上の値を設定してください。"
            )
            dropItem.p > 1.0 -> PropertyError(
                property = dropItem::p,
                message = "成功確率に ${dropItem.p} は設定できません。1.0 以下の値を設定してください。"
            )
            else -> null
        }
        return if (error != null) {
            listOf(error)
        } else {
            emptyList()
        }
    }
}