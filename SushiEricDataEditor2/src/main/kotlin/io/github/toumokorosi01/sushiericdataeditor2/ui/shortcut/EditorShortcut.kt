package io.github.toumokorosi01.sushiericdataeditor2.ui.shortcut

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination

/**
 * エディタで使用するショートカットキーの種類。
 *
 * 今は保存のみだが、今後「検索」「新規作成」「削除」などを追加する場合は、
 * このenumに項目を増やす。
 */
enum class EditorShortcut(
    val displayName: String,
    val combination: KeyCodeCombination
) {
    SAVE(
        displayName = "保存",
        combination = KeyCodeCombination(
            KeyCode.S,
            KeyCombination.SHORTCUT_DOWN
        )
    )
}