package io.github.sushiericworkspace.sushiericservermanager.feature.console

import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementCommandSuggestion

/** コマンド履歴と補完候補の適用を、JavaFXから独立して管理します。 */
class ConsoleCommandModel {
    private val history = mutableListOf<String>()
    private var historyPosition = 0
    private var draft = ""

    /** 実行したコマンドを履歴へ追加します。 */
    fun record(command: String) {
        if (history.lastOrNull() != command) {
            history.add(command)
        }
        resetHistoryNavigation()
    }

    /** 1つ前の履歴を返します。履歴が無い場合は`null`です。 */
    fun previous(current: String): String? {
        if (history.isEmpty()) return null
        if (historyPosition == history.size) draft = current
        if (historyPosition > 0) historyPosition--
        return history[historyPosition]
    }

    /** 1つ後の履歴、または履歴操作前の入力を返します。 */
    fun next(): String? {
        if (historyPosition >= history.size) return null
        historyPosition++
        return if (historyPosition == history.size) draft else history[historyPosition]
    }

    /** 入力編集後の履歴位置を末尾へ戻します。 */
    fun resetHistoryNavigation() {
        historyPosition = history.size
        draft = ""
    }

    /** サーバーから返された置換範囲を使用して候補を適用します。 */
    fun applySuggestion(
        command: String,
        suggestion: ServerManagementCommandSuggestion
    ): ConsoleCompletionApplication? {
        if (suggestion.start !in 0..command.length) return null
        if (suggestion.end !in suggestion.start..command.length) return null

        val completed = command.replaceRange(
            suggestion.start,
            suggestion.end,
            suggestion.text
        )
        return ConsoleCompletionApplication(
            text = completed,
            caretPosition = suggestion.start + suggestion.text.length
        )
    }

    /**
     * 選択位置の候補を返します。
     *
     * Popup表示直後など選択位置が無い場合は先頭候補を返します。
     */
    fun selectSuggestion(
        suggestions: List<ServerManagementCommandSuggestion>,
        selectedIndex: Int
    ): ServerManagementCommandSuggestion? =
        suggestions.getOrNull(selectedIndex) ?: suggestions.firstOrNull()
}

/** 補完候補を適用した入力文字列とキャレット位置です。 */
data class ConsoleCompletionApplication(
    val text: String,
    val caretPosition: Int
)
