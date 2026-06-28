package io.github.toumokorosi01.sushiericdataeditor2.editor.controller

import io.github.toumokorosi01.common.data.core.ManagedData
import io.github.toumokorosi01.sushiericdataeditor2.app.AppScreen
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.util.Utility
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.ErrorType
import io.github.toumokorosi01.sushiericdataeditor2.config.FilePath
import io.github.toumokorosi01.sushiericdataeditor2.config.ServerProfile
import io.github.toumokorosi01.sushiericdataeditor2.editor.main.item.ItemEditorLogic
import io.github.toumokorosi01.sushiericdataeditor2.editor.main.mob.MobEditorLogic
import io.github.toumokorosi01.sushiericdataeditor2.editor.service.EditorDataService
import io.github.toumokorosi01.sushiericdataeditor2.editor.session.EditorSession
import io.github.toumokorosi01.sushiericdataeditor2.editor.view.EditorView
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
        openManagedDataEditor(
            key = "ITEM_EDITOR",
            title = "アイテムエディタ",
            dataAccessProvider = { it.items },
            logicFactory = { mainController, service ->
                ItemEditorLogic(
                    main = mainController,
                    dataService = service
                )
            }
        )
    }

    @FXML
    @Suppress("unused")
    fun onOpenMobEditor() {
        openManagedDataEditor(
            key = "MOB_EDITOR",
            title = "モブエディタ",
            dataAccessProvider = { it.mobs },
            logicFactory = { mainController, service ->
                MobEditorLogic(
                    main = mainController,
                    dataService = service
                )
            }
        )
    }

    private fun <T : ManagedData<T, *>, L : EditorView<T>> openManagedDataEditor(
        key: String,
        title: String,
        dataAccessProvider: (EditorDataService) -> EditorDataService.DataAccess<T>,
        logicFactory: (MainController, EditorDataService) -> L
    ) {
        if (!sshManager.isSftpActive) {
            CustomDialog.error(ErrorType.CONNECTION_FAILED).show()
            Utility.navigateToServerSelect()
            return
        }

        val service = EditorSession.dataService
        if (service == null) {
            logger.error("データサービスが見つかりません。")
            return
        }

        val dataAccess = dataAccessProvider(service)
        val loader = FXMLLoader(javaClass.getResource(AppScreen.BASE.fxml!!))

        EditorWindowManager.openEditor(
            key = key,
            title = title,
            loader = loader
        ) { mainController ->
            val logic = logicFactory(mainController, service)

            try {
                val profileName = service.currentProfileName ?: return@openEditor logic

                val dataDir = FilePath.AUTOSAVE_DIR.toFile()
                    .resolve(profileName)
                    .resolve(dataAccess.dataType.categoryDirName)

                val editingDir = dataDir.resolve("editing")

                if (editingDir.exists()) {
                    val editingCaches = mutableMapOf<String, T>()
                    val originalCaches = mutableMapOf<String, T>()

                    editingDir
                        .listFiles { file ->
                            file.isFile && file.extension.lowercase() == "yml"
                        }
                        ?.forEach { file ->
                            val id = file.nameWithoutExtension
                            val backupPair = dataAccess.loadBackupPair(id)

                            if (backupPair != null) {
                                editingCaches[id] = backupPair.first
                                originalCaches[id] = backupPair.second
                            }
                        }

                    logic.injectAutoSaveCaches(
                        editingCaches = editingCaches,
                        originalCaches = originalCaches
                    )
                }
            } catch (e: Exception) {
                logger.error("${dataAccess.displayName}エディタ起動時の自動保存スキャンに失敗しました", e)
            }

            logic
        }
    }
}