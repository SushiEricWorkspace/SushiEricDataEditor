package io.github.rs0325.common.data.core.validation

interface DataValidator {
    fun validate(): List<PropertyError>
}