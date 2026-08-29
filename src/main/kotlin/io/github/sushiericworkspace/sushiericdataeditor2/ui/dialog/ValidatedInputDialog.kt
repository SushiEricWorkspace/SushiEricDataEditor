package io.github.sushiericworkspace.sushiericdataeditor2.ui.dialog

import io.github.sushiericworkspace.sushiericdataeditor2.app.AppScreen
import io.github.sushiericworkspace.sushiericdataeditor2.editor.result.ValidationResult
import io.github.sushiericworkspace.sushiericdataeditor2.util.Utility
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage

object ValidatedInputDialog {
    private const val DIALOG_WIDTH = 350.0

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
            /*
             * 長いメッセージを1行で表示すると見切れるため折り返す。
             * 折り返し幅を確定させるためダイアログ幅を上限にする。
             */
            isWrapText = true
            maxWidth = DIALOG_WIDTH
            isManaged = false
            isVisible = false
        }
        val confirmButton = Button("決定").apply {
            styleClass.add("btn-primary")
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
            prefWidth = DIALOG_WIDTH
        }

        /**
         * 検証結果を表示へ反映します。
         *
         * メッセージの表示・非表示で必要な高さが変わるため、
         * そのつどダイアログの大きさを合わせ直してボタンが隠れないようにします。
         */
        fun applyValidation(validation: ValidationResult) {
            when (validation) {
                is ValidationResult.Success -> {
                    errorLabel.text = ""
                    errorLabel.isManaged = false
                    errorLabel.isVisible = false
                    confirmButton.isDisable = false
                }

                is ValidationResult.Error -> {
                    errorLabel.text = validation.message
                    errorLabel.isManaged = true
                    errorLabel.isVisible = true
                    confirmButton.isDisable = true
                }
            }

            if (stage.isShowing) {
                stage.sizeToScene()
            }
        }

        // 入力のたびに検証し、その時点の結果を表示する。
        inputField.textProperty().addListener { _, _, text ->
            applyValidation(validator(text.orEmpty()))
        }

        confirmButton.setOnAction {
            when (val validation = validator(inputField.text)) {
                is ValidationResult.Success -> {
                    result = inputField.text
                    stage.close()
                }

                is ValidationResult.Error -> applyValidation(validation)
            }
        }

        stage.scene = Utility.createScene(AppScreen.WIDGETS_ONLY, customRoot = root)
        stage.showAndWait()
        return result
    }
}
