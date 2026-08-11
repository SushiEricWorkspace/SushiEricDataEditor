package io.github.sushiericworkspace.sushiericdataeditor2.editor.controller

import io.github.sushiericworkspace.common.data.core.ManagedData
import io.github.sushiericworkspace.sushiericdataeditor2.app.AppScreen
import io.github.sushiericworkspace.sushiericdataeditor2.app.AppMode
import io.github.sushiericworkspace.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.sushiericworkspace.sushiericdataeditor2.util.Utility
import io.github.sushiericworkspace.sushiericdataeditor2.ui.dialog.ErrorType
import io.github.sushiericworkspace.sushiericdataeditor2.config.FilePath
import io.github.sushiericworkspace.sushiericdataeditor2.config.ServerProfile
import io.github.sushiericworkspace.sushiericdataeditor2.editor.main.item.ItemEditorLogic
import io.github.sushiericworkspace.sushiericdataeditor2.editor.main.mob.MobEditorLogic
import io.github.sushiericworkspace.sushiericdataeditor2.editor.service.EditorDataService
import io.github.sushiericworkspace.sushiericdataeditor2.editor.session.EditorSession
import io.github.sushiericworkspace.sushiericdataeditor2.editor.view.EditorView
import io.github.sushiericworkspace.sushiericdataeditor2.editor.view.EditorWindowManager
import io.github.sushiericworkspace.sushiericdataeditor2.editor.upload.OfflineUploadDialog
import io.github.sushiericworkspace.sushiericdataeditor2.editor.upload.OfflineUploadService
import io.github.sushiericworkspace.sushiericdataeditor2.editor.upload.UploadCandidateState
import io.github.sushiericworkspace.sushiericdataeditor2.editor.upload.UploadScanResult
import io.github.sushiericworkspace.sushiericdataeditor2.editor.upload.OfflineUploadResult
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.scene.control.Button
import javafx.scene.control.Label
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
    @FXML private lateinit var modeLabel: Label
    @FXML private lateinit var uploadLocalButton: Button
    @FXML private lateinit var backButton: Button

    private val sshManager = EditorSession.sshManager

    /** 現在接続中のサーバープロファイル */
    private var selectedProfile: ServerProfile? = null

    /**
     * 画面が表示された後、あるいは適切なタイミングで呼び出します。
     * 親ウィンドウの「閉じる」イベントを監視します。
     */
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val mode = EditorSession.mode
        modeLabel.text = when (mode) {
            AppMode.ONLINE -> "オンライン：${EditorSession.sshManager.currentProfile?.name.orEmpty()}"
            AppMode.OFFLINE -> "オフライン"
            null -> "モード未選択"
        }
        uploadLocalButton.isManaged = mode == AppMode.ONLINE
        uploadLocalButton.isVisible = mode == AppMode.ONLINE
        backButton.text = if (mode == AppMode.ONLINE) "サーバー選択へ戻る" else "モード選択へ戻る"

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
            EditorSession.startOnlineSession()
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
        val online = EditorSession.mode == AppMode.ONLINE
        val isConfirm = CustomDialog.confirmation()
            .header(if (online) "切断の確認" else "モード選択へ戻りますか？")
            .content(if (online) "サーバーとの接続を切り、選択画面に戻りますか？" else "保存済みのオフラインデータは維持されます。")
            .show()
        if (!isConfirm) return

        if (online) Utility.navigateToServerSelect() else Utility.navigateToModeSelect()
    }

    @FXML
    @Suppress("unused")
    fun onUploadLocalData() {
        val service = EditorSession.dataService ?: return
        if (!service.isRemote) return
        setUploadBusy(true, "ローカルデータを確認中...")

        val scanTask = object : Task<UploadScanResult>() {
            override fun call(): UploadScanResult {
                return OfflineUploadService(
                    workspaceRoot = FilePath.OFFLINE_DIR.toFile(),
                    remoteStore = service.store
                ).scan()
            }
        }
        scanTask.setOnSucceeded {
            setUploadBusy(false)
            when (val result = scanTask.value) {
                is UploadScanResult.Failure -> showUploadError(result.error.code.name, result.error.detail)
                is UploadScanResult.Success -> selectAndUpload(result, service)
                null -> showUploadError("INTERNAL_ERROR", null)
            }
        }
        scanTask.setOnFailed {
            setUploadBusy(false)
            logger.error("アップロード対象の確認に失敗しました", scanTask.exception)
            showUploadError("INTERNAL_ERROR", scanTask.exception?.message)
        }
        Thread(scanTask, "offline-upload-scan").apply {
            isDaemon = true
            start()
        }
    }

    private fun selectAndUpload(scan: UploadScanResult.Success, service: EditorDataService) {
        val profileName = EditorSession.sshManager.currentProfile?.name ?: return
        val selected = OfflineUploadDialog.select(
            owner = rootPane.scene?.window as? javafx.stage.Stage,
            profileName = profileName,
            candidates = scan.candidates
        ) ?: return
        if (selected.isEmpty()) return

        val overwrite = scan.candidates
            .filter { it.key in selected && it.state == UploadCandidateState.OVERWRITE }
            .mapTo(linkedSetOf()) { it.key }
        if (overwrite.isNotEmpty()) {
            val approved = CustomDialog.confirmation()
                .title("上書き確認")
                .header("${overwrite.size} 件の既存データを上書きします")
                .content(overwrite.joinToString("\n") { "[${it.category.displayName}] ${it.id}" })
                .owner(rootPane.scene?.window as? javafx.stage.Stage)
                .show()
            if (!approved) return
        }

        setUploadBusy(true, "${selected.size} 件をアップロード中...")
        val uploadTask = object : Task<OfflineUploadResult>() {
            override fun call(): OfflineUploadResult {
                return OfflineUploadService(
                    workspaceRoot = FilePath.OFFLINE_DIR.toFile(),
                    remoteStore = service.store
                ).upload(selected, overwrite)
            }
        }
        uploadTask.setOnSucceeded {
            setUploadBusy(false)
            val result = uploadTask.value
            val failureText = result.failed.joinToString("\n") {
                "[${it.key.category.displayName}] ${it.key.id}: ${it.error.code}"
            }
            CustomDialog.information()
                .title("アップロード結果")
                .header("成功 ${result.succeeded.size} 件、失敗 ${result.failed.size} 件")
                .content(failureText.ifBlank { "選択したデータをアップロードしました。オフラインデータは保持されています。" })
                .owner(rootPane.scene?.window as? javafx.stage.Stage)
                .show()
        }
        uploadTask.setOnFailed {
            setUploadBusy(false)
            logger.error("オフラインデータのアップロードに失敗しました", uploadTask.exception)
            showUploadError("INTERNAL_ERROR", uploadTask.exception?.message)
        }
        Thread(uploadTask, "offline-upload").apply {
            isDaemon = true
            start()
        }
    }

    private fun setUploadBusy(busy: Boolean, text: String = "ローカルデータをアップロード") {
        uploadLocalButton.isDisable = busy
        uploadLocalButton.text = text
    }

    private fun showUploadError(code: String, detail: String?) {
        CustomDialog.error()
            .title("アップロードエラー")
            .header("ローカルデータをアップロードできませんでした")
            .content("$code${detail?.let { ": $it" }.orEmpty()}")
            .owner(rootPane.scene?.window as? javafx.stage.Stage)
            .show()
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
        if (EditorSession.mode == AppMode.ONLINE && !sshManager.isSftpActive) {
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
                val profileName = service.cacheIdentity

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
