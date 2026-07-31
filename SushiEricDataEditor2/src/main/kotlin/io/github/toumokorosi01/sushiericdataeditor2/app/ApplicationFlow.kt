package io.github.toumokorosi01.sushiericdataeditor2.app

import io.github.toumokorosi01.sushiericdataeditor2.config.FilePath
import io.github.toumokorosi01.sushiericdataeditor2.editor.offline.OfflineWorkspaceMigrator
import io.github.toumokorosi01.sushiericdataeditor2.editor.offline.WorkspaceMigrationResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.session.EditorSession
import io.github.toumokorosi01.sushiericdataeditor2.editor.store.LocalEditorDataStore
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.update.UpdateChecker
import io.github.toumokorosi01.sushiericdataeditor2.update.UpdateInfo
import io.github.toumokorosi01.sushiericdataeditor2.util.Utility
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

object ApplicationFlow {
    private val logger = LoggerFactory.getLogger(javaClass)
    var showUpdate: (UpdateInfo) -> Unit = {}

    fun showModeSelection(stage: Stage = Stage()) {
        val loader = FXMLLoader(AppScreen::class.java.getResource(AppScreen.MODE_SELECT.fxml!!))
        val root = loader.load<Parent>()
        loader.getController<ModeSelectionController>().configure { mode ->
            prepareMode(stage, mode)
        }

        stage.title = "SushiEricDataEditor2 - 動作モード選択"
        stage.scene = Utility.createScene(AppScreen.MODE_SELECT, customRoot = root)
        stage.isResizable = false
        stage.show()
    }

    private fun prepareMode(stage: Stage, mode: AppMode) {
        showPreparing(stage, if (mode == AppMode.ONLINE) "アップデートを確認しています..." else "オフラインデータを確認しています...")
        thread(isDaemon = true, name = "startup-${mode.name.lowercase()}") {
            when (mode) {
                AppMode.ONLINE -> prepareOnline(stage)
                AppMode.OFFLINE -> prepareOffline(stage)
            }
        }
    }

    private fun prepareOnline(stage: Stage) {
        val coordinator = StartupCoordinator {
            UpdateChecker(
                "https://github.com/toumokorosi01/SushiEricDataEditor/releases/latest/download/update.json"
            ).check()
        }
        when (val result = coordinator.prepare(AppMode.ONLINE)) {
            StartupPreparationResult.Ready -> Platform.runLater {
                EditorSession.prepareOnlineMode()
                stage.close()
                Utility.navigateToServerSelect()
            }
            is StartupPreparationResult.UpdateRequired -> Platform.runLater {
                showUpdate(result.updateInfo)
                Platform.exit()
            }
            is StartupPreparationResult.Failure -> Platform.runLater {
                logger.error("アップデート確認に失敗しました", result.cause)
                CustomDialog.error()
                    .title("アップデート確認エラー")
                    .header("アップデート情報を確認できませんでした")
                    .content("インターネット接続を確認してください。\n\nこのアプリはアップデート確認に失敗したため起動を中止します。")
                    .show()
                Platform.exit()
            }
        }
    }

    private fun prepareOffline(stage: Stage) {
        val root = FilePath.OFFLINE_DIR.toFile()
        val result = OfflineWorkspaceMigrator(root).migrateToCurrent()
        Platform.runLater {
            when (result) {
                is WorkspaceMigrationResult.Success -> {
                    val store = LocalEditorDataStore(root)
                    EditorSession.startOfflineSession(store)
                    stage.close()
                    Utility.navigateToHome("オフライン")
                }
                is WorkspaceMigrationResult.Failure -> {
                    logger.error(
                        "オフラインワークスペースを準備できませんでした: code={}, detail={}",
                        result.error.code,
                        result.error.detail,
                        result.error.cause
                    )
                    CustomDialog.error()
                        .title("オフラインデータエラー")
                        .header("オフラインデータを開けませんでした")
                        .content("${result.error.code}: ${result.error.detail.orEmpty()}")
                        .show()
                    showModeSelection(stage)
                }
            }
        }
    }

    private fun showPreparing(stage: Stage, message: String) {
        stage.scene = Utility.createScene(
            AppScreen.WIDGETS_ONLY,
            width = 420.0,
            height = 140.0,
            customRoot = VBox(Label(message)).apply {
                alignment = Pos.CENTER
                styleClass.add("startup-progress")
            }
        )
        stage.title = "起動準備中"
    }
}
