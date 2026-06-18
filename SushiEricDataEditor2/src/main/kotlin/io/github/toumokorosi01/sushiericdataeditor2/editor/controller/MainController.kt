package io.github.toumokorosi01.sushiericdataeditor2.editor.controller

import io.github.toumokorosi01.sushiericdataeditor2.app.AppScreen
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.ValidationResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.view.EditorView
import javafx.animation.PauseTransition
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.util.Duration
import org.slf4j.LoggerFactory

class MainController {
    @FXML lateinit var actionButtonContainer: HBox
    @FXML lateinit var sidebarContainer: VBox
    @FXML lateinit var mainContentContainer: HBox
    @FXML private lateinit var topInfoLabel: Label

    private val logger = LoggerFactory.getLogger(javaClass)

    // 実行中の保存完了タイマーを保持する変数（最初は null）
    private var topLabelTimer: PauseTransition? = null

    /**
     * 💡 このエディタ画面が表示されているメインウィンドウ(Stage)を
     * サイドバーのパーツから安全に逆引きして返す読み取り専用プロパティ
     */
    val currentStage: Stage?
        get() = sidebarContainer.scene?.window as? Stage

    fun switchView(logic: EditorView) {
        // リセット
        actionButtonContainer.children.clear()
        sidebarContainer.children.clear()
        mainContentContainer.children.clear()
        setTopLabel("")

        // ロジック側にUIを構築してもらう
        logic.setupActions(actionButtonContainer)
        logic.setupSidebar(sidebarContainer)

        // Sceneの生成を待ってからショートカットを登録する
        if (mainContentContainer.scene != null) {
            setupSaveShortcut(logic)
        } else {
            // Sceneがまだなら、Sceneが準備できた瞬間に登録する
            mainContentContainer.sceneProperty().addListener { _, _, newScene ->
                if (newScene != null) {
                    setupSaveShortcut(logic)
                }
            }
        }
    }

    /**
     * トップバーの右側にあるラベルの文字と色（Colorオブジェクト）を更新します。
     */
    fun setTopLabel(text: String, textColor: Color = Color.WHITE) {
        topInfoLabel.text = text
        topInfoLabel.textFill = textColor
    }

    /**
     * トップバーの右側にあるラベルの文字と色を、カラーコード（文字列）で更新します。
     *
     * @param text 表示する文字列
     * @param hexColor カラーコード（例: "#FF0000", "red", "#32CD32" など）
     */
    fun setTopLabel(text: String, hexColor: String) {
        val parsedColor = try { Color.web(hexColor) } catch (_: IllegalArgumentException) { Color.WHITE }
        setTopLabel(text, parsedColor)
    }

    /**
     * 【Colorオブジェクト版】指定した時間（秒）だけトップバーにメッセージを表示し、その後自動で消去します。
     *
     * @param text 表示する文字列
     * @param textColor 文字の色。省略した場合は [Color.WHITE] になります。
     * @param delaySeconds 表示しておく時間（デフォルトは 1.0 秒）
     */
    fun showTimedTopLabel(text: String, textColor: Color = Color.WHITE, delaySeconds: Double = 2.0) {
        // 1. まず文字を表示
        setTopLabel(text, textColor)

        // 2. 動いている古いタイマーがあれば止める（連打対策）
        topLabelTimer?.stop()

        // 3. 新しいタイマーをセットして実行
        topLabelTimer = PauseTransition(Duration.seconds(delaySeconds)).apply {
            setOnFinished {
                setTopLabel("") // 時間が来たら消す
                topLabelTimer = null
            }
            play()
        }
    }

    /**
     * 【カラーコード文字列版】指定した時間（秒）だけトップバーにメッセージを表示し、その後自動で消去します。
     *
     * @param text 表示する文字列
     * @param hexColor カラーコード（例: "#32CD32"）
     * @param delaySeconds 表示しておく時間（デフォルトは 1.0 秒）
     */
    fun showTimedTopLabel(text: String, hexColor: String, delaySeconds: Double = 1.0) {
        val parsedColor = try {
            Color.web(hexColor)
        } catch (_: IllegalArgumentException) {
            Color.WHITE
        }

        // 上の Colorオブジェクト版 を呼び出して処理を共通化
        showTimedTopLabel(text, parsedColor, delaySeconds)
    }

    /**
     * 💡 ウィンドウが閉じる時などにタイマーも強制終了させる
     */
    fun clearTopLabelTimer() {
        topLabelTimer?.stop()
        topLabelTimer = null
    }

    /**
     * 💡 現在のエディタに対して「保存（Ctrl+S / Cmd+S）」のショートカットキーを紐付けます。
     */
    private fun setupSaveShortcut(logic: EditorView) {
        // コンテナのいずれかから現在の Scene（ステージの土台）を取得
        val scene = mainContentContainer.scene ?: return

        // Windows/Linuxは SHORTCUT（Ctrl）、Macなら META（Cmd）を自動判別してくれる便利クラス
        val saveCombination = KeyCodeCombination(
            KeyCode.S,
            KeyCombination.SHORTCUT_DOWN
        )

        // 💡 シーン全体にアクセラレータを登録（キーが押されたら logic.onSave() を実行）
        scene.accelerators[saveCombination] = Runnable {
            logger.info("ショートカットキーが検出されたため、保存処理を実行します。")
            logic.onSave()
        }
    }

    /**
     * 💡 ウィンドウ閉鎖時などに、登録されたショートカットキーを安全に解除します。
     */
    fun clearShortcuts() {
        val scene = mainContentContainer.scene ?: return
        val saveCombination = KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN)
        scene.accelerators.remove(saveCombination)
    }

    /**
     * ユーザーに入力を求めるモーダルダイアログを表示します。
     *
     * このメソッドは [Stage.showAndWait] を使用しているため、ダイアログが閉じられるまで
     * 呼び出し元のスレッドの実行を一時停止（ブロック）し、結果を返します。
     *
     * ### 戻り値の仕様:
     * - **String**: ユーザーがテキストを入力し「決定」を押した場合（空文字を含む）。
     * - **null**: ユーザーが「キャンセル」を押した、またはウィンドウを直接閉じた場合。
     *
     * @param title ダイアログのウィンドウタイトル
     * @param validator ボタン押下時に実行される検証ロジック。重い処理（通信等）を想定し、決定ボタン押下時のみ実行する。
     * @return ユーザーが入力した文字列、またはキャンセルされた場合は null
     *
     * @sample
     * val name = requestInput("新規ファイル名")
     * if (!name.isNullOrBlank()) {
     *     // 入力ありの時の処理
     * }
     */
    fun requestInput(
        title: String,
        validator: (String) -> ValidationResult = { ValidationResult.Success }
    ): String? {
        var result: String? = null
        val stage = Stage().apply {
            initModality(Modality.APPLICATION_MODAL)
            this.title = title
        }

        val inputField = TextField()
        val errorLabel = Label().apply {
            style = "-fx-text-fill: #ff4444;"
            isWrapText = true // 長いエラー文も折り返す
        }

        val btnAdd = Button("決定").apply {
            onAction = javafx.event.EventHandler {
                val text = inputField.text

                // ボタンを押した時だけバリデーションを実行
                this.isDisable = true // 処理中の二重クリック防止
                when (val validation = validator(text)) {
                    is ValidationResult.Success -> {
                        result = text
                        stage.close()
                    }
                    is ValidationResult.Error -> {
                        errorLabel.text = validation.message // エラー文を表示
                        this.isDisable = false // 修正できるようにボタンを復帰
                    }
                }
            }
        }

        val btnCancel = Button("キャンセル").apply {
            onAction = javafx.event.EventHandler { stage.close() }
        }

        val root = VBox(12.0).apply {
            padding = Insets(20.0)
            styleClass.add("common-root")
            children.addAll(
                Label(title).apply { style = "-fx-font-weight: bold;" },
                inputField,
                errorLabel,
                HBox(10.0, btnCancel, btnAdd).apply { alignment = Pos.CENTER_RIGHT }
            )
            prefWidth = 350.0
        }

        stage.scene = Scene(root)
        stage.scene.stylesheets.add(
            MainController::class.java
                .getResource(AppScreen.WIDGETS_ONLY.css)!!
                .toExternalForm()
        )
        stage.showAndWait()

        return result
    }
}