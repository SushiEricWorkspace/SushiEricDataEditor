package io.github.sushiericworkspace.sushiericdataeditor2.editor.result

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}