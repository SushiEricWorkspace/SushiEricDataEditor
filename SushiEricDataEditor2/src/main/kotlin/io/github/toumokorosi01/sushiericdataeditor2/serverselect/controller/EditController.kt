package io.github.toumokorosi01.sushiericdataeditor2.serverselect.controller

import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.config.ServerProfile
import io.github.toumokorosi01.sushiericdataeditor2.config.SettingConfigManager
import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.stage.Stage

/**
 * 既存のサーバープロファイルを編集または削除するためのコントローラークラス。
 * 選択されたプロファイルの情報をフィールドに展開し、変更内容の保存やプロファイルの破棄を管理します。
 */
class EditController {
    @FXML private lateinit var nameField: TextField
    @FXML private lateinit var hostField: TextField
    @FXML private lateinit var portField: TextField
    @FXML private lateinit var userField: TextField
    @FXML private lateinit var folderPathField: TextField
    @FXML private lateinit var keyPathField: TextField

    /** 編集対象を特定するための、変更前のプロファイル名 */
    private lateinit var originalName: String

    /**
     * 画面遷移時に呼び出され、指定されたプロファイル情報を各入力フィールドにセットします。
     *
     * @param profile 編集対象の[ServerProfile]
     */
    fun initData(profile: ServerProfile) {
        originalName = profile.name

        nameField.text = profile.name
        hostField.text = profile.host
        portField.text = profile.port.toString()
        userField.text = profile.user
        folderPathField.text = profile.path
        keyPathField.text = profile.key
    }

    /**
     * フィールドの入力内容に基づいてプロファイルを更新し、設定ファイルへ保存します。
     * 名前が変更された場合は、既存のリストから古い名前のデータを削除し、新しいデータを追加します。
     */
    @FXML
    @Suppress("unused")
    fun handleSave() {
        val newName = nameField.text.trim()

        // バリデーション処理（必要に応じて CreateController と同様の実装を追加）

        val currentConfig = SettingConfigManager.load()

        // 名前が変更された場合のみ、新しい名前が他のプロファイルと重複していないかチェック
        if (newName != originalName && currentConfig.list.any { it.name == newName }) {
            CustomDialog.error()
                .title("重複エラー")
                .content("サーバー名 '$newName' は既に使用されています。")
                .show()
            return
        }

        // 元のデータをフィルタリングして除外し、新しいプロファイルを追加してリストを再構成
        val updatedList = currentConfig.list.filter { it.name != originalName } + ServerProfile(
            name = newName,
            host = hostField.text.trim(),
            port = portField.text.toIntOrNull() ?: 22,
            user = userField.text.trim(),
            path = folderPathField.text.trim().trimEnd('/') + "/", // 末尾に"/"をつける
            key = keyPathField.text.trim()
        )

        SettingConfigManager.save(currentConfig.copy(list = updatedList))
        closeWindow()
    }

    /**
     * 現在編集中のプロファイルをリストから削除し、設定ファイルに反映します。
     */
    @FXML
    @Suppress("unused")
    fun handleDelete() {
        // メモ: 実装時には必要に応じて確認ダイアログ（Alert）を表示することを推奨
        val currentConfig = SettingConfigManager.load()
        val updatedList = currentConfig.list.filter { it.name != originalName }

        SettingConfigManager.save(currentConfig.copy(list = updatedList))
        closeWindow()
    }

    /**
     * 秘密鍵ファイルを選択するためのダイアログを表示します。
     * (実装内容は CreateController.handleSelectKeyFile と同様)
     */
    @FXML
    @Suppress("unused")
    fun handleSelectKeyFile() {
        // CreateController と同じロジックを実装
    }

    /**
     * 保存や削除を行わずに編集画面を閉じます。
     */
    @FXML
    @Suppress("unused")
    fun handleCancel() = closeWindow()

    /**
     * 現在のステージを閉じます。
     */
    private fun closeWindow() = (nameField.scene.window as Stage).close()
}