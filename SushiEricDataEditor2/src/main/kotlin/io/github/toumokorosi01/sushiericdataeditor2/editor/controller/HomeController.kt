package io.github.toumokorosi01.sushiericdataeditor2.editor.controller

import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.sushiericdataeditor2.app.AppScreen
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.util.Utility
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.ErrorType
import io.github.toumokorosi01.sushiericdataeditor2.config.FilePath
import io.github.toumokorosi01.sushiericdataeditor2.config.ServerProfile
import io.github.toumokorosi01.sushiericdataeditor2.editor.item.ItemEditorLogic
import io.github.toumokorosi01.sushiericdataeditor2.editor.service.EditorDataService
import io.github.toumokorosi01.sushiericdataeditor2.editor.session.EditorSession
import io.github.toumokorosi01.sushiericdataeditor2.editor.session.sessionValue
import io.github.toumokorosi01.sushiericdataeditor2.editor.view.EditorWindowManager
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.layout.VBox
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.ResourceBundle

/**
 * サーバー接続後のメイン操作画面（ホーム画面）を管理するコントローラー。
 * ファイルリストの表示、SSHセッションの維持、および切断処理を担当します。
 */
class HomeController : Initializable {

    private val logger = LoggerFactory.getLogger(javaClass)

    /** FXMLの一番外側の要素 */
    @FXML
    private lateinit var rootPane: VBox

    private val sshManager = EditorSession.sshManager

    private val dataService by sessionValue { EditorSession.dataService }

    /** 現在接続中のサーバープロファイル */
    private var selectedProfile: ServerProfile? = null

    /**
     * 画面が表示された後、あるいは適切なタイミングで呼び出します。
     * 親ウィンドウの「閉じる」イベントを監視します。
     */
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // Platform.runLater を使って Stage が確実に生成された後に処理
        javafx.application.Platform.runLater {
            val stage = rootPane.scene?.window as? javafx.stage.Stage
            stage?.setOnCloseRequest {
                // 親が閉じられたら、エディタウィンドウをすべて閉じる
                EditorWindowManager.closeAll()
                // SSH接続も忘れずに切断
                EditorSession.disconnect()
            }
        }
    }

    /**
     * 指定されたプロファイルを用いてサーバーに接続し、初期データをロードします。
     *
     * @param profile 接続先の [ServerProfile]
     * @return 接続に成功した場合は true、失敗した場合は false
     */
    fun initData(profile: ServerProfile): Boolean {
        this.selectedProfile = profile

        val isConnected = sshManager.connect(profile)

        // 接続成功時にサービスをインスタンス化
        if (isConnected) {
            val service = EditorDataService(sshManager)
            EditorSession.dataService = service // 💡 共有エリアに保存
        }

        return isConnected
    }

    /**
     * ユーザーに確認を求めた後、サーバーとの接続を終了してサーバー選択画面に戻ります。
     * FXML上の「切断」ボタンなどから呼び出されます。
     */
    @FXML
    @Suppress("unused")
    fun handleDisconnect() {
        val isConfirm = CustomDialog.confirmation()
            .header("切断の確認")
            .content("サーバーとの接続を切り、選択画面に戻りますか？")
            .show()
        if (!isConfirm) return

        Utility.navigateToServerSelect()
    }

    @FXML
    @Suppress("unused")
    fun onOpenItemEditor() {
        if (!sshManager.isSftpActive) {
            CustomDialog.error(ErrorType.CONNECTION_FAILED).show()
            Utility.navigateToServerSelect()
            return
        }

        val service = EditorSession.dataService // 💡 共有エリアから取得
        if (service == null) {
            logger.error("データサービスが見つかりません。")
            return
        }

        val loader = FXMLLoader(javaClass.getResource(AppScreen.BASE.fxml!!))

        EditorWindowManager.openEditor(
            key = "ITEM_EDITOR",
            title = "アイテムエディタ",
            loader = loader
        ) { mainController ->
            val logic = ItemEditorLogic(mainController, dataService!!)

            try {
                val profileName = dataService!!.currentProfileName ?: return@openEditor logic

                // 💡 編集用フォルダとオリジナル用フォルダのパス
                val itemsDir = FilePath.AUTOSAVE_DIR.toFile().resolve(profileName).resolve("items")
                val editingDir = itemsDir.resolve("editing")

                if (editingDir.exists()) {
                    val editingCaches = mutableMapOf<String, ItemData>()
                    val originalCaches = mutableMapOf<String, ItemData>()

                    // 1. 編集中のキャッシュをスキャン
                    editingDir.listFiles { file -> file.isFile && file.extension.lowercase() == "yml" }?.forEach { file ->
                        val id = file.nameWithoutExtension

                        // 手元の編集データをロード
                        val raw = dataService!!.loadBackupPair(id)
                        if (raw != null) {
                            editingCaches[id] = raw.first
                            originalCaches[id] = raw.second
                        }
                    }

                    // 💡 2. 編集データとオリジナルデータの両方を一括注入する！
                    logic.injectAutoSaveCaches(editingCaches, originalCaches)
                }
            } catch (e: Exception) {
                logger.error("起動時の自動保存スキャンに失敗しました", e)
            }

            logic
        }
    }
}