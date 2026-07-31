package io.github.toumokorosi01.sushiericdataeditor2.app

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.layout.VBox

class ModeSelectionController {
    @FXML private lateinit var rootPane: VBox
    @FXML private lateinit var onlineButton: Button
    @FXML private lateinit var offlineButton: Button

    private var onSelected: ((AppMode) -> Unit)? = null

    fun configure(onSelected: (AppMode) -> Unit) {
        this.onSelected = onSelected
    }

    @FXML
    @Suppress("unused")
    private fun selectOnline() {
        disableActions()
        onSelected?.invoke(AppMode.ONLINE)
    }

    @FXML
    @Suppress("unused")
    private fun selectOffline() {
        disableActions()
        onSelected?.invoke(AppMode.OFFLINE)
    }

    private fun disableActions() {
        onlineButton.isDisable = true
        offlineButton.isDisable = true
    }
}
