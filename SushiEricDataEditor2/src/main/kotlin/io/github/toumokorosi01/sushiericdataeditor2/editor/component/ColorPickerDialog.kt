package io.github.toumokorosi01.sushiericdataeditor2.editor.component

import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.sushiericdataeditor2.app.AppScreen
import javafx.scene.control.ButtonType
import javafx.scene.control.ColorPicker
import javafx.scene.control.Dialog
import javafx.scene.paint.Color
import javafx.stage.Window

object ColorPickerDialog {

    fun show(
        initialColor: HexColor,
        owner: Window? = null,
        cssPath: String = AppScreen.WIDGETS_ONLY.css
    ): HexColor? {
        val picker = ColorPicker(toFxColor(initialColor)).apply {
            styleClass.add("button")
        }

        val dialog = Dialog<Color>().apply {
            title = "色を選択"
            dialogPane.content = picker
            dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

            owner?.let {
                initOwner(it)
            }

            dialogPane.stylesheets.add(
                ColorPickerDialog::class.java
                    .getResource(cssPath)!!
                    .toExternalForm()
            )

            dialogPane.style = "-fx-background-color: -fx-bg-deep;"

            setResultConverter { buttonType ->
                if (buttonType == ButtonType.OK) picker.value else null
            }
        }

        return dialog.showAndWait()
            .map { toHexColor(it) }
            .orElse(null)
    }

    fun toFxColor(hexColor: HexColor): Color {
        return try {
            Color.web(hexColor.value)
        } catch (_: Exception) {
            Color.WHITE
        }
    }

    fun toHexColor(color: Color): HexColor {
        val red = (color.red * 255).toInt().coerceIn(0, 255)
        val green = (color.green * 255).toInt().coerceIn(0, 255)
        val blue = (color.blue * 255).toInt().coerceIn(0, 255)

        return HexColor.of("#%02X%02X%02X".format(red, green, blue))
    }

    fun isBrightColor(hexColor: HexColor): Boolean {
        val normalized = hexColor.value.removePrefix("#")
        if (normalized.length != 6) return true

        val red = normalized.take(2).toIntOrNull(16) ?: return true
        val green = normalized.substring(2, 4).toIntOrNull(16) ?: return true
        val blue = normalized.substring(4, 6).toIntOrNull(16) ?: return true

        val brightness = (red * 299 + green * 587 + blue * 114) / 1000
        return brightness >= 128

    }
}