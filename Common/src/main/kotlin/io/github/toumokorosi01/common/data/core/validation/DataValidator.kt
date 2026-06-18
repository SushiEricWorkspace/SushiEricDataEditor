package io.github.toumokorosi01.common.data.core.validation

interface DataValidator {
    fun validate(): List<PropertyError>
}