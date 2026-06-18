package io.github.toumokorosi01.sushiericdataeditor2.editor.result

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}