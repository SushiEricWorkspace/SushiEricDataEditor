package io.github.toumokorosi01.sushiericdataeditor2.app

import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.update.AppVersion
import io.github.toumokorosi01.sushiericdataeditor2.update.UpdateChecker
import io.github.toumokorosi01.sushiericdataeditor2.update.UpdateInfo
import io.github.toumokorosi01.sushiericdataeditor2.util.Utility
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlin.concurrent.thread

/**
 * JavaFX アプリケーションのメインライフサイクルを管理するクラス。
 * ウィンドウの初期化、シーンのロード、およびテーマの適用を行います。
 */
class MainApp : Application() {

    /**
     * JavaFX アプリケーションの開始点。
     * サーバー選択画面をロードし、テーマを適用してメインステージを表示します。
     *
     * @param stage アプリケーションのプライマリステージ
     */
    override fun start(stage: Stage) {

        if (!SingleAppLock.tryLock()) {
            CustomDialog
                .error()
                .title("起動エラー")
                .header("すでに起動しています")
                .content("このアプリはすでに起動中です。")
                .show()

            Platform.exit()
            return
        }

        showCheckingUpdateStage(stage)
        checkUpdateBeforeStart(stage)

        Utility.navigateToServerSelect()
    }

    override fun stop() {
        SingleAppLock.release()
    }

    private fun showCheckingUpdateStage(stage: Stage) {
        val label = Label("アップデートを確認しています...")

        val root = VBox(label).apply {
            alignment = Pos.CENTER
            prefWidth = 360.0
            prefHeight = 120.0
        }

        stage.title = "起動準備中"
        stage.scene = Scene(root)
        stage.isResizable = false
        stage.show()
    }

    private fun checkUpdateBeforeStart(stage: Stage) {
        thread(isDaemon = true) {
            try {
                val checker = UpdateChecker(
                    updateJsonUrl = "https://github.com/toumokorosi01/SushiEricServerProject/releases/latest/download/update.json"
                )

                val updateInfo = checker.check()

                Platform.runLater {
                    if (updateInfo != null) {
                        showUpdateDialog(updateInfo)
                    }

                    stage.close()
                    Utility.navigateToServerSelect()
                }
            } catch (e: Exception) {
                e.printStackTrace()

                Platform.runLater {
                    CustomDialog
                        .error()
                        .title("アップデート確認エラー")
                        .header("アップデート情報を確認できませんでした")
                        .content(
                            "インターネット接続を確認してください。\n\n" +
                                    "このアプリはアップデート確認に失敗したため起動を中止します。"
                        )
                        .show()

                    Platform.exit()
                }
            }
        }
    }

    private fun showUpdateDialog(updateInfo: UpdateInfo) {
        val message = buildString {
            appendLine("新しいバージョンがあります。")
            appendLine()
            appendLine("現在のバージョン: ${AppVersion.CURRENT}")
            appendLine("最新バージョン: ${updateInfo.version}")
            appendLine()

            if (updateInfo.notes.isNotEmpty()) {
                appendLine("変更内容:")
                updateInfo.notes.forEach { note ->
                    appendLine("- $note")
                }
            }
        }

        Alert(Alert.AlertType.INFORMATION).apply {
            title = "アップデート確認"
            headerText = "アップデートがあります"
            contentText = message
            buttonTypes.setAll(ButtonType.OK)
        }.showAndWait()
    }
}

/**
 * アプリケーションの起動用エントリーポイント。
 * 実行環境のOS互換性をチェックし、問題がなければ JavaFX ランタイムを起動します。
 */
fun main(args: Array<String>) {
    Launcher.main(args)
}