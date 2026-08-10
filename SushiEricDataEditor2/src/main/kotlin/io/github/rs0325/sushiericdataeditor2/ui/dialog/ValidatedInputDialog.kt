package io.github.rs0325.sushiericdataeditor2.ui.dialog

import io.github.rs0325.sushiericdataeditor2.app.AppScreen
import io.github.rs0325.sushiericdataeditor2.editor.result.ValidationResult
import io.github.rs0325.sushiericdataeditor2.util.Utility
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage

object ValidatedInputDialog {

    fun show(
        title: String,
        validator: (String) -> ValidationResult
    ): String? {
        var result: String? = null

        val stage = Stage().apply {
            initModality(Modality.APPLICATION_MODAL)
            this.title = title
        }
        val inputField = TextField()
        val errorLabel = Label().apply {
            styleClass.add("error-label")
            isWrapText = true
            isManaged = false
            isVisible = false
        }
        val confirmButton = Button("決定").apply {
            styleClass.add("btn-primary")
            setOnAction {
                isDisable = true
                when (val validation = validator(inputField.text)) {
                    is ValidationResult.Success -> {
                        result = inputField.text
                        stage.close()
                    }

                    is ValidationResult.Error -> {
                        errorLabel.text = validation.message
                        errorLabel.isManaged = true
                        errorLabel.isVisible = true
                        isDisable = false
                    }
                }
            }
        }
        val cancelButton = Button("キャンセル").apply {
            styleClass.add("btn-cancel")
            setOnAction { stage.close() }
        }
        val actions = HBox(cancelButton, confirmButton).apply {
            styleClass.add("dialog-actions")
        }
        val root = VBox(
            Label(title).apply {
                styleClass.add("validated-input-dialog-title")
            },
            inputField,
            errorLabel,
            actions
        ).apply {
            styleClass.addAll("common-root", "validated-input-dialog")
            prefWidth = 350.0
        }

        stage.scene = Utility.createScene(AppScreen.WIDGETS_ONLY, customRoot = root)
        stage.showAndWait()
        return result
    }
}
