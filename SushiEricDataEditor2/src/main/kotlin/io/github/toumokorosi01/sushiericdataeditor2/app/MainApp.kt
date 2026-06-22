package io.github.toumokorosi01.sushiericdataeditor2.app

import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.update.AppVersion
import io.github.toumokorosi01.sushiericdataeditor2.update.UpdateChecker
import io.github.toumokorosi01.sushiericdataeditor2.update.UpdateInfo
import io.github.toumokorosi01.sushiericdataeditor2.util.Utility
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.HBox
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
                    updateJsonUrl = "https://github.com/toumokorosi01/SushiEricDataEditor/releases/latest/download/update.json"
                )

                val updateInfo = checker.check()

                Platform.runLater {
                    if (updateInfo != null) {
                        showUpdateDialog(updateInfo)
                        Platform.exit()
                        return@runLater
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
        val downloadUrl = updateInfo.downloadUrlForCurrentOs()

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

        val messageArea = TextArea(message).apply {
            isEditable = false
            isWrapText = true
            prefRowCount = 8
            prefColumnCount = 48
        }

        val urlArea = TextArea(downloadUrl).apply {
            isEditable = false
            isWrapText = true
            prefRowCount = 2
            prefColumnCount = 48
        }

        val openLink = Hyperlink("ダウンロードページを開く").apply {
            setOnAction {
                hostServices.showDocument(downloadUrl)
            }
        }

        val copyButton = Button("URLをコピー").apply {
            setOnAction {
                Clipboard.getSystemClipboard().setContent(
                    ClipboardContent().apply {
                        putString(downloadUrl)
                    }
                )
            }
        }

        val root = VBox(10.0).apply {
            padding = Insets(10.0)
            children.addAll(
                Label("新しいバージョンがあります。アプリを更新してください。"),
                messageArea,
                Label("ダウンロードURL:"),
                urlArea,
                HBox(10.0, openLink, copyButton)
            )
        }

        Dialog<Unit>().apply {
            title = "アップデート確認"
            headerText = "アップデートがあります"
            dialogPane.content = root
            dialogPane.buttonTypes.setAll(ButtonType.OK)
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