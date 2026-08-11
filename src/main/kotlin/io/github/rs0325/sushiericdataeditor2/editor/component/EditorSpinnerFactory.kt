package io.github.rs0325.sushiericdataeditor2.editor.component

import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.control.TextFormatter
import javafx.scene.input.KeyCode
import kotlin.math.pow
import kotlin.math.roundToInt

object EditorSpinnerFactory {

    fun intSpinner(
        initialValue: Int,
        min: Int = 0,
        max: Int = 999,
        step: Int = 1,
        prefWidth: Double? = null,
        onChanged: (Int) -> Unit
    ): Spinner<Int> {
        return Spinner<Int>().apply {
            isEditable = true

            valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(
                min,
                max,
                initialValue.coerceIn(min, max),
                step
            )

            prefWidth?.let {
                this.prefWidth = it
            }

            editor.textFormatter = TextFormatter<String> { change ->
                val newText = change.controlNewText

                if (newText.matches(Regex("\\d*"))) {
                    change
                } else {
                    null
                }
            }

            fun commitEditorValue() {
                val fixedValue = editor.text
                    .toIntOrNull()
                    ?.coerceIn(min, max)
                    ?: min

                if (valueFactory.value != fixedValue) {
                    valueFactory.value = fixedValue
                }

                editor.text = fixedValue.toString()
                onChanged(fixedValue)
            }

            editor.focusedProperty().addListener { _, _, focused ->
                if (!focused) {
                    commitEditorValue()
                }
            }

            editor.setOnKeyPressed { event ->
                if (event.code == KeyCode.ENTER) {
                    commitEditorValue()
                    event.consume()
                }
            }

            valueProperty().addListener { _, _, newValue ->
                val fixedValue = (newValue ?: min).coerceIn(min, max)

                if (valueFactory.value != fixedValue) {
                    valueFactory.value = fixedValue
                }

                editor.text = fixedValue.toString()
                onChanged(fixedValue)
            }
        }
    }

    fun doubleSpinner(
        initialValue: Double,
        min: Double = 0.0,
        max: Double = 999.0,
        step: Double = 0.1,
        prefWidth: Double? = null,
        decimalPlaces: Int = 1,
        onChanged: (Double) -> Unit
    ): Spinner<Double> {
        return Spinner<Double>().apply {
            isEditable = true

            valueFactory = SpinnerValueFactory.DoubleSpinnerValueFactory(
                min,
                max,
                initialValue.coerceIn(min, max),
                step
            )

            prefWidth?.let {
                this.prefWidth = it
            }

            editor.textFormatter = TextFormatter<String> { change ->
                val newText = change.controlNewText

                if (newText.matches(Regex("\\d*(\\.\\d*)?"))) {
                    change
                } else {
                    null
                }
            }

            fun fix(value: Double): Double {
                val coerced = value.coerceIn(min, max)
                val scale = 10.0.pow(decimalPlaces.toDouble())

                return (coerced * scale).roundToInt() / scale
            }

            fun format(value: Double): String {
                return "%.${decimalPlaces}f".format(value)
            }

            fun commitEditorValue() {
                val fixedValue = editor.text
                    .toDoubleOrNull()
                    ?.let { fix(it) }
                    ?: min

                if (valueFactory.value != fixedValue) {
                    valueFactory.value = fixedValue
                }

                editor.text = format(fixedValue)
                onChanged(fixedValue)
            }

            editor.focusedProperty().addListener { _, _, focused ->
                if (!focused) {
                    commitEditorValue()
                }
            }

            editor.setOnKeyPressed { event ->
                if (event.code == KeyCode.ENTER) {
                    commitEditorValue()
                    event.consume()
                }
            }

            valueProperty().addListener { _, _, newValue ->
                val fixedValue = fix(newValue ?: min)

                if (valueFactory.value != fixedValue) {
                    valueFactory.value = fixedValue
                }

                editor.text = format(fixedValue)
                onChanged(fixedValue)
            }
        }
    }
}