package io.github.toumokorosi01.sushiericdataeditor2.ui.dialog

import io.github.toumokorosi01.sushiericdataeditor2.util.Utility.applyCommonStyle
import javafx.application.Platform
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.FutureTask

/**
 * アプリケーション全体のダイアログ表示を管理・ビルドする型安全なクラス。
 * Fluent API (メソッドチェーン) スタイルで直感的にダイアログを構築できます。
 */
class CustomDialog private constructor(private val type: Alert.AlertType) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private var title: String = if (type == Alert.AlertType.ERROR) "システムエラー" else "確認"
    private var header: String? = if (type == Alert.AlertType.ERROR) "予期しないエラーが発生しました" else "処理を続行しますか？"
    private var contentText: String = ""
    private var owner: Stage? = null

    // 例外用
    private var throwable: Throwable? = null
    private var logLines: List<String> = emptyList()

    // ボタン・スタイル用
    private var okText: String = "OK"
    private var cancelText: String = "キャンセル"
    private var okColorHex: String? = null
    private var cancelColorHex: String? = null

    companion object {
        /** エラーダイアログのビルダーを開始します */
        fun error() = CustomDialog(Alert.AlertType.ERROR)

        /**
         * エラーの種類（ErrorType）を指定してビルダーを開始します。
         * タイトルとヘッダーは ErrorType に定義された標準の文言が自動セットされます。
         */
        fun error(errorType: ErrorType) = CustomDialog(Alert.AlertType.ERROR).apply {
            this.title = errorType.defaultTitle
            this.header = errorType.defaultHeader
        }

        /** 確認ダイアログのビルダーを開始します */
        fun confirmation() = CustomDialog(Alert.AlertType.CONFIRMATION)
    }

    fun title(title: String = "確認") = apply { this.title = title }
    fun header(header: String? = "処理を続行しますか？") = apply { this.header = header }
    fun owner(owner: Stage?) = apply { this.owner = owner }

    /**
     * 【型安全】単一の文字列を本文として設定します。
     * 「\n」による手動改行を含めることも可能です。
     *
     * @param content 本文文字列
     */
    fun content(content: String) = apply {
        this.contentText = content
    }

    /**
     * 【型安全】文字列のリストを本文として設定します。
     * 各要素は内部で自動的に改行コード（\n）で結合されます。
     *
     * @param contentLines 本文の各行を格納したリスト
     */
    fun content(contentLines: List<String>) = apply {
        this.contentText = contentLines.joinToString("\n")
    }

    /** 💡 例外オブジェクトと自動ログを登録する */
    fun exception(e: Throwable?, logs: List<String> = emptyList()) = apply {
        this.throwable = e
        this.logLines = logs
    }

    // --- ボタンカスタマイズ (確認用) ---
    fun okButton(text: String = "OK", colorHex: String? = null) = apply {
        this.okText = text
        if (colorHex != null) this.okColorHex = colorHex
    }

    fun okButton(text: String = "OK", color: Color) = apply {
        this.okText = text
        this.okColorHex = color.toHex()
    }

    fun cancelButton(text: String = "キャンセル", colorHex: String? = null) = apply {
        this.cancelText = text
        if (colorHex != null) this.cancelColorHex = colorHex
    }

    fun cancelButton(text: String = "キャンセル", color: Color) = apply {
        this.cancelText = text
        this.cancelColorHex = color.toHex()
    }

    // --- 実行メソッド ---

    /**
     * ダイアログを画面に表示します。
     * [Alert.AlertType.CONFIRMATION] の場合は OK が押されたら true、それ以外は常に false (またはエラー時は false) を返します。
     */
    fun show(): Boolean {
        // 1. ログ出力の事前処理 (エラータイプのみ)
        if (type == Alert.AlertType.ERROR) {
            logLines.forEach { logger.error(it) }
            if (throwable != null) {
                logger.error("$title: ${throwable!!.message}", throwable)
            } else {
                logger.error("$title: $header - $contentText")
            }
        }

        // 2. UIスレッドで動かすタスクの構築
        val task = FutureTask {
            val alert = Alert(type).apply {
                this.title = this@CustomDialog.title
                this.headerText = this@CustomDialog.header

                // 例外のメッセージがあれば最優先、なければ contentText
                this.contentText = throwable?.localizedMessage ?: this@CustomDialog.contentText.ifEmpty { "詳細不明なエラーです。" }

                // 💡 --- ここからモーダル・親ウィンドウの制御を強化 ---
                if (this@CustomDialog.owner != null) {
                    // 親（Stage）が指定されている場合は、その親ウィンドウだけをロックする（ウィンドウ・モーダル）
                    initOwner(this@CustomDialog.owner)
                    initModality(Modality.WINDOW_MODAL)
                } else {
                    // 💡 親が null の場合は、アプリ全体の全ウィンドウをロックする（アプリケーション・モーダル）
                    // これにより、Stageを指定しなくても自動的に下が触れなくなります！
                    initModality(Modality.APPLICATION_MODAL)
                }

                // テーマの適用
                applyCommonStyle()

                // --- エラー用: スタックトレースの展開 ---
                if (type == Alert.AlertType.ERROR && throwable != null) {
                    val sw = StringWriter()
                    throwable!!.printStackTrace(PrintWriter(sw))
                    val textArea = TextArea(sw.toString()).apply {
                        isEditable = false
                        isWrapText = true
                    }
                    dialogPane.expandableContent = VBox(Label("スタックトレース:"), textArea)
                }

                // --- 確認用: ボタンの生成と色注入 ---
                if (type == Alert.AlertType.CONFIRMATION) {
                    val okButtonType = ButtonType(okText, ButtonBar.ButtonData.OK_DONE)
                    val cancelButtonType = ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE)
                    buttonTypes.setAll(okButtonType, cancelButtonType)

                    if (okColorHex != null) {
                        (dialogPane.lookupButton(okButtonType) as? Button)?.style =
                            "-fx-background-color: $okColorHex; -fx-text-fill: white; -fx-font-weight: bold;"
                    }
                    if (cancelColorHex != null) {
                        (dialogPane.lookupButton(cancelButtonType) as? Button)?.style =
                            "-fx-background-color: $cancelColorHex; -fx-text-fill: white;"
                    }
                }
            }

            val result = alert.showAndWait()
            result.isPresent && result.get().buttonData == ButtonBar.ButtonData.OK_DONE
        }

        // 3. スレッド安全に実行して結果を返す
        if (Platform.isFxApplicationThread()) {
            task.run()
        } else {
            Platform.runLater(task)
        }

        return try {
            task.get()
        } catch (e: Exception) {
            logger.error("ダイアログ表示中に致命的なエラーが発生しました", e)
            false
        }
    }

    /** 💡 javafx.scene.paint.Color を CSS用Hex文字列に変換する拡張関数 */
    private fun Color.toHex(): String {
        return String.format("#%02X%02X%02X", (this.red * 255).toInt(), (this.green * 255).toInt(), (this.blue * 255).toInt())
    }
}