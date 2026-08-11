package io.github.sushiericworkspace.sushiericdataeditor2.editor.store

object StorePathValidator {
    private val validId = Regex("^[A-Za-z0-9_-]+$")

    fun isValidId(id: String): Boolean {
        return id.isNotBlank() &&
                id.matches(validId) &&
                id != "." &&
                id != ".."
    }
}
