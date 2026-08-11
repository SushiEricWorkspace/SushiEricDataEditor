package io.github.sushiericworkspace.sushiericdataeditor2.app

import io.github.sushiericworkspace.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.sushiericworkspace.sushiericdataeditor2.update.AppVersion
import io.github.sushiericworkspace.sushiericdataeditor2.update.UpdateInfo
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
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

        ApplicationFlow.showUpdate = ::showUpdateDialog
        ApplicationFlow.showModeSelection(stage)
    }

    override fun stop() {
        io.github.sushiericworkspace.sushiericdataeditor2.editor.session.EditorSession.disconnect()
        SingleAppLock.release()
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
