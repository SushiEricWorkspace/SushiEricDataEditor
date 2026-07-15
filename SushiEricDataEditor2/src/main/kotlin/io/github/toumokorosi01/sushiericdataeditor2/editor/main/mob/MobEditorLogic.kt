package io.github.toumokorosi01.sushiericdataeditor2.editor.main.mob

import io.github.toumokorosi01.common.registry.VanillaIdRegistry
import io.github.toumokorosi01.common.stats.entity.EntityStatsType
import io.github.toumokorosi01.common.data.mob.data.MobData
import io.github.toumokorosi01.sushiericdataeditor2.editor.controller.MainController
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.ValidationResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.DeleteResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.RenameResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.service.EditorDataService
import io.github.toumokorosi01.sushiericdataeditor2.editor.view.EditorView
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.ErrorType
import io.github.toumokorosi01.sushiericdataeditor2.util.NumericSpinnerFactory
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color

class MobEditorLogic(
    main: MainController,
    dataService: EditorDataService
) : EditorView<MobData>(
    main = main,
    dataService = dataService,
    dataAccess = dataService.mobs
) {
    override fun setupSidebar(container: VBox, selectId: String?) {
        container.children.clear()
        selectedButton = null
        sidebarButtons.clear()

        val (fileResources, isSuccess) = dataAccess.listYmlResources()
        if (!isSuccess) {
            CustomDialog.error()
                .title("取得失敗")
                .header("ファイルリストの取得に失敗しました。")
                .owner(main.currentStage)
                .show()
            handleForceBackToSelect()
            return
        }

        fileResources.forEach { file ->
            val id = file.name.removeSuffix(".yml")
            val btn = Button(id).apply {
                isFocusTraversable = false
                this.id = id
                maxWidth = Double.MAX_VALUE
                alignment = Pos.CENTER
                onAction = EventHandler { selectTab(id) }
            }

            val saveMenuItem = MenuItem("保存").apply {
                isDisable = true
                onAction = EventHandler { onSave(id) }
            }

            // 右クリックメニュー
            val contextMenu = ContextMenu().apply {
                items.addAll(
                    MenuItem("IDをコピー").apply {
                        onAction = EventHandler {
                            // クリップボードにIDをコピーする処理
                            val clipboard = Clipboard.getSystemClipboard()
                            val content = ClipboardContent()
                            content.putString(id)
                            clipboard.setContent(content)

                            main.showTimedTopLabel("コピーしました: $id", Color.GREENYELLOW)
                        }
                    },
                    saveMenuItem,
                    MenuItem("IDを変更").apply {
                        style = "-fx-text-fill: -fx-danger-color;"
                        onAction = EventHandler {
                            val inputText = main.requestInput("名前変更") { input ->
                                val containsInvalidChar = !input.matches(Regex("^[a-zA-Z0-9_-]*$"))
                                val isDuplicate = fileResources.any { it.name == "$input.yml" }
                                when {
                                    input.isBlank() -> ValidationResult.Error("名前を入力してください")
                                    containsInvalidChar -> ValidationResult.Error("不正な文字列です")
                                    isDuplicate -> ValidationResult.Error("重複した名称です")
                                    else -> ValidationResult.Success
                                }
                            }

                            if (inputText != null) {
                                val isConfirm = CustomDialog.confirmation()
                                    .title("警告")
                                    .header("破壊的変更")
                                    .content(listOf(
                                        "モブID: $id",
                                        "",
                                        "この操作を実行するとモブIDが変更され、",
                                        "過去のモブIDの付与された",
                                        "Minecraftサーバー上のモブが無効化されます。",
                                        "本当に変更しますか？"
                                    ))
                                    .okButton("変更", Color.RED)
                                    .owner(main.currentStage)
                                    .show()

                                if (isConfirm) when (dataAccess.rename(id, inputText)) {
                                    RenameResult.SUCCESS -> {
                                        // 💡 1. メモリ上のキャッシュMapから古いデータを引っ張り出して中身(id)を書き換え、新しいキーで再登録する
                                        val currentEditData = editingDataMap.remove(id) // removeは削除しつつそのデータを返す
                                        val currentOrigData = originalDataMap.remove(id)

                                        if (currentEditData != null) {
                                            currentEditData.id = inputText // 💡 内部要素のIDを書き換え！
                                            editingDataMap[inputText] = currentEditData
                                        }
                                        if (currentOrigData != null) {
                                            currentOrigData.id = inputText // 💡 内部要素のIDを書き換え！
                                            originalDataMap[inputText] = currentOrigData
                                        }

                                        main.showTimedTopLabel("$id を $inputText に変更しました", Color.GREENYELLOW)

                                        // サイドバーの選択状態を更新する
                                        // この中でselectもされる
                                        setupSidebar(main.sidebarContainer, inputText)
                                    }

                                    RenameResult.FILE_NOT_FOUND -> {
                                        CustomDialog.error()
                                            .title("名前変更エラー")
                                            .header("対象のファイルが見つかりません")
                                            .content("変更元のモブ($id)が、サーバー上で既に削除されている可能性があります。")
                                            .show()
                                        return@EventHandler
                                    }

                                    RenameResult.ALREADY_EXISTS -> {
                                        CustomDialog.error()
                                            .title("名前変更エラー")
                                            .header("同名のファイルが既に存在します")
                                            .content("入力された名称($inputText)は、サーバー上で他のモブに使用されています。\n別の日時や名称を指定してください。")
                                            .show()
                                        return@EventHandler
                                    }

                                    RenameResult.SFTP_INACTIVE, RenameResult.PROFILE_NOT_SELECTED -> {
                                        CustomDialog.error()
                                            .title("接続エラー")
                                            .header("サーバーに接続されていません")
                                            .content("SFTPセッションが切断された可能性があります。再接続してください。")
                                            .show()

                                        dataService.forceBackToSelect()
                                        return@EventHandler
                                    }

                                    RenameResult.FAILED -> {
                                        CustomDialog.error()
                                            .title("システムエラー")
                                            .header("名前変更に失敗しました")
                                            .content("予期しないエラーまたはネットワーク問題が発生しました。詳細はログを確認してください。")
                                            .show()
                                        dataService.forceBackToSelect()
                                        return@EventHandler
                                    }
                                }
                            }
                        }
                    },
                    MenuItem("削除").apply {
                        style = "-fx-text-fill: -fx-danger-color;"
                        onAction = EventHandler {
                            val isConfirm = CustomDialog.confirmation()
                                .title("警告")
                                .header("破壊的変更")
                                .content(listOf(
                                    "モブID: $id",
                                    "",
                                    "この操作を実行するとサーバー上のファイルが物理削除され、",
                                    "元の状態に戻すことはできなくなります。",
                                    "本当に削除しますか？"
                                ))
                                .okButton("削除", Color.RED)
                                .owner(main.currentStage)
                                .show()

                            if (isConfirm) when (dataAccess.delete(id)) {
                                DeleteResult.FAILED, DeleteResult.PROFILE_NOT_SELECTED, DeleteResult.SFTP_INACTIVE -> {
                                    CustomDialog.error(ErrorType.NETWORK_ERROR)
                                        .owner(main.currentStage)
                                        .show()
                                    handleForceBackToSelect()
                                    return@EventHandler
                                }
                                DeleteResult.FILE_NOT_FOUND -> {
                                    CustomDialog.error(ErrorType.FILE_NOT_FOUND)
                                        .content("データを再読み込みします...")
                                        .owner(main.currentStage)
                                        .show()
                                    setupSidebar(main.sidebarContainer)
                                    return@EventHandler
                                }
                                DeleteResult.SUCCESS -> {
                                    main.showTimedTopLabel("$id を削除しました", Color.GREENYELLOW)
                                    setupSidebar(main.sidebarContainer)
                                }
                            }
                        }
                    }
                )

                // 表示される瞬間に、ウィンドウのルート背景を完全に透明にする
                setOnShowing {
                    saveMenuItem.isDisable = (originalDataMap[id] == editingDataMap[id])

                    // PopupWindow が内部で生成している Scene のルートノード（PopupControl.CSSBridge 等）を取得
                    val popupScene = scene
                    val popupRoot = popupScene.root

                    if (popupRoot != null) {
                        // 土台の背景色を完全に透明にする
                        popupRoot.style = "-fx-background-color: transparent;"
                    }
                }
            }

            // ボタンにコンテキストメニューを紐付ける
            // これだけで、JavaFXが自動的に「右クリックされたら出す」という制御をしてくれます
            btn.contextMenu = contextMenu

            container.children.add(btn)

            // 生成したボタンをIDをキーにしてプロパティ（Map）に保存
            sidebarButtons[id] = btn
        }

        // 未保存の変更があるデータに目印をつける
        fileResources.forEach { file ->
            val id = file.name.removeSuffix(".yml")
            refreshButtonVisual(id)
        }

        if (fileResources.isEmpty()) {
            currentSelectedDataId = null
            selectedButton = null
            main.mainContentContainer.children.clear()
            return
        }

        val targetId = selectId?.removeSuffix(".yml") ?: fileResources[0].name.removeSuffix(".yml")
        if (fileResources.any { it.name.removeSuffix(".yml") == targetId }) {
            selectTab(targetId)
        }

        // すべての再構築が終わったら、新鮮なタイマーを1つだけスタート
        startAutoSaveTimer()

        if (restoredCacheCount > 0) {
            main.showTimedTopLabel("自動保存から $restoredCacheCount 件のデータを復元しました", Color.GREENYELLOW)
            restoredCacheCount = 0 // 通知漏れ防止にリセット
        }
    }

    override fun setupMainContent(selectData: MobData) {

        val (fileResources, isSuccess) = dataService.items.listYmlResources()

        if (!isSuccess) {
            CustomDialog.error()
                .title("取得失敗")
                .header("ファイルリストの取得に失敗しました。")
                .owner(main.currentStage)
                .show()
            handleForceBackToSelect()
        }

        val ids = fileResources.map { file ->
            file.name.removeSuffix(".yml")
        }

        main.mainContentContainer.children.setAll(
            VBox(20.0).apply {
                children.addAll(
                    Label("現在編集中のモブ: ${selectData.id}").apply {
                        style = "-fx-font-size: 18px; -fx-font-weight: bold;"
                    },
                    Region().apply {
                        prefHeight = 20.0
                    },
                    HBox(5.0).apply {
                        alignment = Pos.CENTER_LEFT

                        children.addAll(
                            VBox(5.0).apply {

                                val w = 250.0

                                minWidth = w
                                prefWidth = w
                                maxWidth = w

                                children.addAll(
                                    VBox(5.0).apply {
                                        styleClass.add("custom-border")

                                        val errorLabel = Label().apply {
                                            style = "-fx-text-fill: -fx-danger-color;"
                                            text = "表示名が空欄、または空白のみです。"
                                        }

                                        fun errorView() {
                                            val isError = selectData.displayName.isBlank()

                                            errorLabel.isVisible = isError
                                            errorLabel.isManaged = isError
                                        }

                                        errorView()

                                        children.addAll(
                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT

                                                children.addAll(
                                                    Label("表示名:").apply {
                                                        minWidth = Region.USE_PREF_SIZE
                                                    },
                                                    TextField(selectData.displayName).apply {
                                                        textProperty().addListener { _, _, n ->
                                                            selectData.displayName = n
                                                            errorView()
                                                            refreshButtonVisual(selectData.id)
                                                        }
                                                    }
                                                )
                                            },
                                            errorLabel
                                        )
                                    },
                                    VBox(5.0).apply {
                                        styleClass.add("custom-border")

                                        val allEntities = VanillaIdRegistry.allEntities

                                        val errorLabel = Label().apply {
                                            textFill = Color.RED
                                            isVisible = false
                                            isManaged = false
                                        }

                                        val comboBox = ComboBox<String>().apply {
                                            items.addAll(allEntities)

                                            value = if (selectData.entityData.vanillaId in allEntities) {
                                                selectData.entityData.vanillaId
                                            } else {
                                                null
                                            }

                                            valueProperty().addListener { _, _, selected ->
                                                if (selected != null) {
                                                    selectData.entityData.vanillaId = selected
                                                    refreshButtonVisual(selectData.id)

                                                    errorLabel.isVisible = false
                                                    errorLabel.isManaged = false
                                                    errorLabel.text = ""
                                                }
                                            }
                                        }

                                        val searchField = TextField().apply {
                                            promptText = "エンティティIDを検索"

                                            textProperty().addListener { _, _, query ->
                                                val result = VanillaIdRegistry.searchEntities(query)

                                                if (result.isEmpty()) {
                                                    comboBox.items.setAll(allEntities)

                                                    if (selectData.entityData.vanillaId in allEntities) {
                                                        comboBox.value = selectData.entityData.vanillaId
                                                    }

                                                    errorLabel.text = "検索に一致するエンティティIDがありません"
                                                    errorLabel.isVisible = true
                                                    errorLabel.isManaged = true
                                                } else {
                                                    comboBox.items.setAll(result)

                                                    if (selectData.entityData.vanillaId in result) {
                                                        comboBox.value = selectData.entityData.vanillaId
                                                    } else {
                                                        comboBox.value = result.firstOrNull()
                                                    }

                                                    errorLabel.isVisible = false
                                                    errorLabel.isManaged = false
                                                    errorLabel.text = ""
                                                }
                                            }
                                        }

                                        children.addAll(
                                            Label("バニラエンティティID"),
                                            searchField,
                                            comboBox,
                                            errorLabel
                                        )
                                    },
                                    VBox(5.0).apply {
                                        styleClass.add("custom-border")

                                        val statsGrid = GridPane().apply {
                                            hgap = 4.0
                                            vgap = 2.0
                                        }

                                        val statsMap = selectData.entityData.stats

                                        EntityStatsType.entries.forEachIndexed { idx, type ->
                                            val spinner = NumericSpinnerFactory.doubleSpinner(
                                                getter = {
                                                    statsMap.getOrDefault(type, type.default)
                                                },
                                                setter = { value ->
                                                    statsMap[type] = value
                                                    refreshButtonVisual(selectData.id)
                                                },
                                                min = type.min,
                                                max = type.max,
                                                step = 1.0,
                                                allowNegative = type.min < 0,
                                                allowPlus = type.max > 0
                                            )

                                            statsGrid.add(Label("${type.display}:"), 0, idx)
                                            statsGrid.add(spinner, 1, idx)
                                        }

                                        children.addAll(
                                            Label("ステータス"),
                                            statsGrid
                                        )
                                    },
                                    HBox(5.0).apply {
                                        alignment = Pos.CENTER_LEFT
                                        styleClass.add("custom-border")

                                        children.addAll(
                                            Label("装備設定:"),
                                            Button("編集する").apply {
                                                onAction = EventHandler {
                                                    EquipmentEditor(
                                                        selectData = selectData,
                                                        main = main,
                                                        refreshButtonVisual = { id ->
                                                            refreshButtonVisual(id)
                                                        },
                                                        onSave = { id ->
                                                            onSave(id)
                                                        },
                                                        currentDataProvider = { id ->
                                                            editingDataMap[id]
                                                        }
                                                    ).openEquipmentEditor()
                                                }
                                            }
                                        )
                                    },
                                    HBox(5.0).apply {
                                        alignment = Pos.CENTER_LEFT
                                        styleClass.add("custom-border")

                                        children.addAll(
                                            Label("ドロップアイテム:"),
                                            Button("編集する").apply {
                                                onAction = EventHandler {
                                                    DropItemEditor(
                                                        selectData = selectData,
                                                        main = main,
                                                        refreshButtonVisual = { id ->
                                                            refreshButtonVisual(id)
                                                        },
                                                        itemIds = ids,
                                                        onSave = { id ->
                                                            onSave(id)
                                                        },
                                                        currentDataProvider = { id ->
                                                            editingDataMap[id]
                                                        }
                                                    ).openDropItemEditor()
                                                }
                                            }
                                        )
                                    }
                                )
                            },
                            VBox(5.0).apply {

                                val w = 400.0

                                minWidth = w
                                prefWidth = w
                                maxWidth = w

                                children.addAll(
                                    VBox(5.0).apply {
                                        styleClass.add("custom-border")

                                        children.addAll(
                                            Label("コメントアウト"),

                                            TextArea(selectData.editorMeta.comment.joinToString("\n")).apply {
                                                // 自動折り返ししない
                                                isWrapText = false

                                                // 12行分を表示
                                                prefRowCount = 12

                                                // 横幅
                                                prefColumnCount = 40

                                                // 12行以上はTextArea内で縦スクロール
                                                minHeight = 220.0
                                                prefHeight = 220.0
                                                maxHeight = 220.0

                                                // 横に長い場合は横スクロール
                                                prefWidth = 520.0

                                                textProperty().addListener { _, _, text ->
                                                    selectData.editorMeta.comment.clear()
                                                    selectData.editorMeta.comment.addAll(text.lines())
                                                    refreshButtonVisual(selectData.id)
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    override fun resolveSaveConflict(
        dataId: String,
        originalData: MobData,
        currentData: MobData,
        serverData: MobData
    ): MobData? {
        val isConfirm = CustomDialog.confirmation()
            .title("上書き確認")
            .header("サーバー上の${dataAccess.displayName}データが変更されています。")
            .content("このまま保存すると、サーバー上の変更内容を現在の編集内容で上書きします。保存しますか？")
            .owner(main.currentStage)
            .show()

        return if (isConfirm) currentData.deepCopy() else null
    }
}