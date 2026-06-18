package io.github.toumokorosi01.sushiericdataeditor2.serverselect.controller

import io.github.toumokorosi01.sushiericdataeditor2.config.ServerProfile
import io.github.toumokorosi01.sushiericdataeditor2.config.SettingConfigManager
import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File

/**
 * 新規サーバープロファイル作成画面のUI制御を担当するコントローラークラス。
 * ユーザー入力のバリデーション、秘密鍵ファイルの選択、および設定ファイルへの保存処理を行います。
 */
class CreateController {

    @FXML private lateinit var nameField: TextField
    @FXML private lateinit var hostField: TextField
    @FXML private lateinit var portField: TextField
    @FXML private lateinit var userField: TextField
    @FXML private lateinit var folderPathField: TextField
    @FXML private lateinit var keyPathField: TextField

    /**
     * ファイル選択ダイアログを開き、SSH秘密鍵ファイルを選択します。
     * 選択されたファイルの絶対パスを [keyPathField] に反映します。
     */
    @FXML
    @Suppress("unused")
    fun handleSelectKeyFile() {
        val fileChooser = FileChooser().apply {
            title = "秘密鍵を選択"
            // 利便性のため初期ディレクトリをユーザーの .ssh に設定（存在しない場合はホームディレクトリ）
            initialDirectory = File(System.getProperty("user.home"), ".ssh").let {
                if (it.exists()) it else File(System.getProperty("user.home"))
            }
        }

        val stage = keyPathField.scene.window as Stage
        val selectedFile = fileChooser.showOpenDialog(stage)

        if (selectedFile != null) {
            keyPathField.text = selectedFile.absolutePath
        }
    }

    /**
     * 入力された情報をバリデーションし、新しい[ServerProfile]として保存します。
     * 保存に成功した場合、現在のウィンドウを閉じます。
     */
    @FXML
    @Suppress("unused")
    fun handleSave() {
        val name = nameField.text.trim()
        val host = hostField.text.trim()
        val port = portField.text.toIntOrNull()
        val user = userField.text.trim()
        val folderPath = folderPathField.text.trim().trimEnd('/') + "/" // 末尾に"/"をつける
        val keyPath = keyPathField.text.trim()

        // 入力値の空チェックおよびポート番号の形式チェック
        val isInvalid = listOf(name, host, user, folderPath, keyPath).any { it.isBlank() } || port == null

        if (isInvalid) {
            showErrorDialog("入力エラー", "全ての項目を正しく入力してください。")
            return
        }

        // 最新の設定データをロード
        val currentConfig = SettingConfigManager.load()

        // プロファイル名の重複チェック
        val isDuplicate = currentConfig.list.any { it.name == name }

        if (isDuplicate) {
            showErrorDialog("重複エラー", "サーバー名 '$name' は既に登録されています。別の名前を入力してください。")
            return
        }

        // 新しいプロファイルの作成と保存
        val newProfile = ServerProfile(
            name = name,
            host = host,
            port = port,
            user = user,
            path = folderPath,
            key = keyPath
        )

        val updatedConfig = currentConfig.copy(
            list = currentConfig.list + newProfile
        )

        SettingConfigManager.save(updatedConfig)

        // 画面を閉じる
        (nameField.scene.window as Stage).close()
    }

    /**
     * 保存を行わずに作成画面を閉じます。
     */
    @FXML
    @Suppress("unused")
    fun handleCancel() {
        (nameField.scene.window as Stage).close()
    }

    /**
     * エラーメッセージを表示するためのアラートダイアログを出力します。
     *
     * @param title ダイアログのタイトル
     * @param message 表示するエラー内容
     */
    private fun showErrorDialog(title: String, message: String) {
        javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR).apply {
            this.title = title
            this.headerText = null
            this.contentText = message
            showAndWait()
        }
    }
}