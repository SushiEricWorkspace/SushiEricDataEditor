package io.github.sushiericworkspace.sushiericdataeditor2.ui.dialog

import io.github.sushiericworkspace.sushiericdataeditor2.app.AppScreen
import io.github.sushiericworkspace.sushiericdataeditor2.editor.merge.DataConflict
import io.github.sushiericworkspace.sushiericdataeditor2.editor.merge.DataFieldPath
import javafx.geometry.Insets
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.Tooltip
import javafx.scene.layout.VBox
import javafx.stage.Stage

object MergeConflictDialog {
    fun show(
        owner: Stage?,
        dataId: String,
        conflicts: List<DataConflict>
    ): Set<DataFieldPath>? {
        if (conflicts.isEmpty()) return emptySet()
        val applyType = ButtonType("選択内容を適用", ButtonBar.ButtonData.OK_DONE)
        val checks = conflicts.associateWith { conflict ->
            CheckBox("${conflict.displayName}：ローカル値を採用").apply {
                isSelected = true
                tooltip = Tooltip(
                    "編集開始時: ${conflict.baseValue}\nローカル: ${conflict.localValue}\nサーバー: ${conflict.remoteValue}"
                )
            }
        }
        return Dialog<Set<DataFieldPath>>().apply {
            title = "保存競合の解決"
            headerText = "$dataId で競合したフィールドだけを表示しています"
            owner?.let(::initOwner)
            dialogPane.buttonTypes.addAll(applyType, ButtonType.CANCEL)
            dialogPane.stylesheets.add(
                MergeConflictDialog::class.java.getResource(AppScreen.WIDGETS_ONLY.css)!!.toExternalForm()
            )
            dialogPane.content = VBox(10.0).apply {
                padding = Insets(8.0)
                prefWidth = 680.0
                children.addAll(
                    Label("チェックあり: ローカル値、チェックなし: サーバー値"),
                    ScrollPane(VBox(8.0).apply { children.addAll(checks.values) }).apply {
                        isFitToWidth = true
                        prefHeight = 360.0
                    }
                )
            }
            setResultConverter { button ->
                if (button == applyType) {
                    checks.filterValues { it.isSelected }.keys.mapTo(mutableSetOf()) { it.path }
                } else {
                    null
                }
            }
        }.showAndWait().orElse(null)
    }
}
