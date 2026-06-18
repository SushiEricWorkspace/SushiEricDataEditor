package io.github.toumokorosi01.sushiericdataeditor2.serverselect.controller

import io.github.toumokorosi01.sushiericdataeditor2.app.AppScreen
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.util.Utility
import io.github.toumokorosi01.sushiericdataeditor2.editor.controller.HomeController
import io.github.toumokorosi01.sushiericdataeditor2.config.ServerConfig
import io.github.toumokorosi01.sushiericdataeditor2.config.ServerProfile
import io.github.toumokorosi01.sushiericdataeditor2.config.SettingConfigManager
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.ResourceBundle

/**
 * 保存済みサーバーの一覧表示、選択、編集、削除、および新規追加の橋渡しを行うメインの選択画面コントローラー。
 * 動的なUI生成（サーバーリスト）と、各画面（メイン、編集、追加）への遷移を管理します。
 */
class ServerSelector : Initializable {

    private val logger = LoggerFactory.getLogger(javaClass)

    @FXML
    private lateinit var serverListContainer: VBox

    /**
     * FXMLロード完了時に実行される初期化メソッド。
     * 設定ファイルからサーバープロファイル一覧を読み込み、リストを表示します。
     */
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val config: ServerConfig = SettingConfigManager.load()
        refreshServerList(config.list)
    }

    /**
     * サーバープロファイルの一覧に基づき、UI要素（行）を動的に生成してリストコンテナに追加します。
     * 各行にはサーバー名表示、接続、編集、削除の各ボタンが含まれます。
     *
     * @param profileList 表示対象のサーバープロファイルリスト
     */
    private fun refreshServerList(profileList: List<ServerProfile>) {
        serverListContainer.children.clear()
        serverListContainer.spacing = 10.0

        for (profile in profileList) {
            // 行コンテナの生成
            val row = HBox(10.0).apply {
                styleClass.add("server-row")
            }

            // サーバー名ラベル
            val nameLabel = Label(profile.name).apply {
                styleClass.add("server-name")
            }

            // 右寄せ用スペーサー
            val spacer = Region().apply {
                HBox.setHgrow(this, Priority.ALWAYS)
            }

            // 操作ボタン群
            val connectBtn = Button("接続").apply {
                styleClass.addAll("button", "btn-primary")
                setOnAction { handleServerSelection(profile) }
            }

            val editBtn = Button("編集").apply {
                styleClass.addAll("button", "btn-success")
                setOnAction { handleEditServer(profile) }
            }

            val deleteBtn = Button("削除").apply {
                styleClass.addAll("button", "btn-danger")
                setOnAction { handleDeleteServer(profile) }
            }

            row.children.addAll(nameLabel, spacer, connectBtn, editBtn, deleteBtn)
            serverListContainer.children.add(row)
        }
    }

    /**
     * 指定された画面を読み込み、コントローラーを初期化し、ステージを表示します。
     *
     * @param screen 画面定義
     * @param title ウインドウタイトル
     * @param modality モーダル表示するかどうか
     * @param initController コントローラーの初期化処理（必要な場合のみ）
     * @return 表示されたStage
     */
    private fun <T> showScreen(
        screen: AppScreen,
        title: String,
        modality: Modality = Modality.NONE,
        initController: ((T) -> Unit)? = null
    ): Stage {
        // 1. ロードとコントローラー取得
        val loader = FXMLLoader(AppScreen::class.java.getResource(screen.fxml!!))
        val root = loader.load<Parent>()

        // 2. コントローラーがあれば初期化
        if (initController != null) {
            initController(loader.getController<T>())
        }

        // 3. createScene でシーン作成（CSS自動適用）
        val scene = Utility.createScene(screen, customRoot = root)

        // 4. ステージ表示
        return Stage().apply {
            this.title = title
            this.scene = scene
            this.initModality(modality)
            if (modality == Modality.APPLICATION_MODAL) showAndWait() else show()
        }
    }

    /**
     * 選択されたサーバーへの接続を試行し、成功した場合はメイン（ホーム）画面へ遷移します。
     *
     * @param profile 接続対象の[ServerProfile]
     */
    private fun handleServerSelection(profile: ServerProfile) {
        // Controllerの初期化だけ先に判定が必要なケース
        val loader = FXMLLoader(AppScreen::class.java.getResource(AppScreen.HOME.fxml!!))
        val root = loader.load<Parent>()
        val controller = loader.getController<HomeController>()

        if (controller.initData(profile)) {
            Stage().apply {
                title = "SushiEricDataEditor2 - ${profile.name}"
                this.scene = Utility.createScene(AppScreen.HOME, customRoot = root)
                show()
            }
            (serverListContainer.scene.window as Stage).close()
        } else {
            CustomDialog.error().title("接続失敗").content(listOf("接続できませんでした。"))
        }
    }

    /**
     * 新規サーバー追加画面をモーダル表示します。
     * 画面が閉じられた後、リストを最新の情報で更新します。
     */
    @FXML
    @Suppress("unused")
    fun openCreateServerWindow() {
        showScreen<Any>(AppScreen.SERVER_CREATE, "サーバーの追加", Modality.APPLICATION_MODAL)

        // 完了後の再描画
        refreshServerList(SettingConfigManager.load().list)
    }

    /**
     * 既存サーバーの編集画面を表示します。
     *
     * @param profile 編集対象のプロファイル
     */
    private fun handleEditServer(profile: ServerProfile) {
        showScreen<EditController>(AppScreen.SERVER_EDIT, "サーバーの編集", Modality.APPLICATION_MODAL) { controller ->
            controller.initData(profile)
        }

        refreshServerList(SettingConfigManager.load().list)
    }

    /**
     * サーバーの削除確認ダイアログを表示し、承認された場合はプロファイルを削除します。
     *
     * @param profile 削除対象のプロファイル
     */
    private fun handleDeleteServer(profile: ServerProfile) {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "サーバーの削除"
            headerText = "サーバー '${profile.name}' を削除しますか？"
            contentText = "設定から完全に削除されます。よろしいですか？"
        }

        val result = alert.showAndWait()

        if (result.isPresent && result.get() == javafx.scene.control.ButtonType.OK) {
            val currentConfig = SettingConfigManager.load()
            val updatedList = currentConfig.list.filter { it.name != profile.name }

            SettingConfigManager.save(currentConfig.copy(list = updatedList))
            refreshServerList(updatedList)

            logger.info("サーバー '${profile.name}' を削除しました。")
        }
    }
}