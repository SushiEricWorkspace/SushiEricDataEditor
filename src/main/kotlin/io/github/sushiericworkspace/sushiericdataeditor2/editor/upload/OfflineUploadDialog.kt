package io.github.sushiericworkspace.sushiericdataeditor2.editor.upload

import io.github.sushiericworkspace.sushiericdataeditor2.app.AppScreen
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage

object OfflineUploadDialog {
    fun select(
        owner: Stage?,
        profileName: String,
        candidates: List<OfflineUploadCandidate>
    ): Set<UploadKey>? {
        val uploadType = ButtonType("アップロード", ButtonBar.ButtonData.OK_DONE)
        val countLabel = Label()
        val checks = linkedMapOf<OfflineUploadCandidate, CheckBox>()
        candidates.forEach { candidate ->
            checks[candidate] = CheckBox(candidateLabel(candidate)).apply {
                isDisable = !candidate.selectable
                isSelected = candidate.selectable
            }
        }

        fun updateCount() {
            countLabel.text = "${checks.values.count { it.isSelected && !it.isDisable }} 件を選択中"
        }
        checks.values.forEach { checkBox ->
            checkBox.selectedProperty().addListener { _, _, _ -> updateCount() }
        }

        val list = VBox(8.0).apply {
            children.addAll(checks.values)
        }
        val dialog = Dialog<Set<UploadKey>>().apply {
            title = "ローカルデータをアップロード"
            headerText = "アップロード先: $profileName"
            owner?.let(::initOwner)
            dialogPane.buttonTypes.addAll(uploadType, ButtonType.CANCEL)
            dialogPane.stylesheets.add(
                OfflineUploadDialog::class.java.getResource(AppScreen.WIDGETS_ONLY.css)!!.toExternalForm()
            )
            dialogPane.content = VBox(10.0).apply {
                padding = Insets(8.0)
                prefWidth = 620.0
                children.addAll(
                    HBox(8.0,
                        Button("全選択").apply {
                            styleClass.add("btn-secondary")
                            setOnAction {
                                checks.values.filterNot { it.isDisable }.forEach { it.isSelected = true }
                                updateCount()
                            }
                        },
                        Button("全解除").apply {
                            styleClass.add("btn-secondary")
                            setOnAction {
                                checks.values.forEach { it.isSelected = false }
                                updateCount()
                            }
                        },
                        countLabel
                    ).apply { alignment = Pos.CENTER_LEFT },
                    ScrollPane(list).apply {
                        isFitToWidth = true
                        prefHeight = 360.0
                    }
                )
            }
            setResultConverter { button ->
                if (button == uploadType) {
                    checks.filterValues { it.isSelected && !it.isDisable }.keys.mapTo(mutableSetOf()) { it.key }
                } else {
                    null
                }
            }
        }
        updateCount()
        return dialog.showAndWait().orElse(null)
    }

    private fun candidateLabel(candidate: OfflineUploadCandidate): String {
        val state = when (candidate.state) {
            UploadCandidateState.NEW -> "新規"
            UploadCandidateState.OVERWRITE -> "上書き"
            UploadCandidateState.UNAVAILABLE -> "選択不可: ${candidate.error?.code}"
        }
        val migration = if (candidate.requiresFormatUpdate) "、形式更新対象" else ""
        return "[${candidate.key.category.displayName}] ${candidate.key.id}（$state$migration）"
    }
}
