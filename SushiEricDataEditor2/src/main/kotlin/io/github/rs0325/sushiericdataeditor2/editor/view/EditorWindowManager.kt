package io.github.rs0325.sushiericdataeditor2.editor.view

import io.github.rs0325.sushiericdataeditor2.app.AppScreen
import io.github.rs0325.sushiericdataeditor2.editor.controller.MainController
import io.github.rs0325.sushiericdataeditor2.util.Utility
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.stage.Stage

object EditorWindowManager {
    // 開いているエディタをロジックのクラス名などで管理
    private val activeEditors = mutableMapOf<String, Stage>()

    fun openEditor(
        key: String,
        title: String,
        loader: FXMLLoader,
        logicFactory: (MainController) -> EditorView<*>
    ) {
        // マップにあるが、実際には閉じられている Stage がないかチェック
        val existingStage = activeEditors[key]
        if (existingStage != null && existingStage.isShowing) {
            existingStage.toFront()
            return
        } else {
            // 閉じられているのにマップに残っている場合は掃除
            activeEditors.remove(key)
        }

        val root = loader.load<Parent>()
        val mainController = loader.getController<MainController>()

        val newStage = Stage().apply {
            this.title = title
            this.scene = Utility.createScene(AppScreen.BASE, customRoot = root)
        }

        val logic = logicFactory(mainController)
        mainController.switchView(logic)

        if (logic.openCancelled) {
            return
        }

        newStage.setOnCloseRequest { event ->
            if (!logic.onClose()) {
                event.consume()
            } else {
                activeEditors.remove(key)
            }
        }

        activeEditors[key] = newStage
        newStage.show()
    }

    /**
     * 現在管理しているすべてのウィンドウを閉じ、リストをクリアします。
     */
    fun closeAll() {
        val stages = activeEditors.values.toList()

        stages.forEach { stage ->
            stage.close()
        }

        activeEditors.clear()
    }
}