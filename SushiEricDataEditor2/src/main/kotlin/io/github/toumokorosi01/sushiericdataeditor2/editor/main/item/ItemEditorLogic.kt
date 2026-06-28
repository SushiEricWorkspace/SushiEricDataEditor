package io.github.toumokorosi01.sushiericdataeditor2.editor.main.item

import io.github.toumokorosi01.common.data.item.LoreLineEditor
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.item.data.LoreSectionType
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.editor.main.item.diff.ItemDiffField
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.ErrorType
import io.github.toumokorosi01.sushiericdataeditor2.editor.service.EditorDataService
import io.github.toumokorosi01.sushiericdataeditor2.editor.view.EditorView
import io.github.toumokorosi01.sushiericdataeditor2.editor.controller.MainController
import io.github.toumokorosi01.sushiericdataeditor2.editor.main.item.diff.RewriteConfirmation
import io.github.toumokorosi01.sushiericdataeditor2.editor.main.item.tree.ItemTreeBuilder
import io.github.toumokorosi01.sushiericdataeditor2.editor.main.item.tree.LoreDragDropTreeCell
import io.github.toumokorosi01.sushiericdataeditor2.editor.main.item.tree.LoreTreeUiIdMemory
import io.github.toumokorosi01.sushiericdataeditor2.editor.main.item.tree.TreeRow
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.ValidationResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.DeleteResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.RenameResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.tree.EditorContextMenuFactory
import io.github.toumokorosi01.sushiericdataeditor2.editor.tree.EditorFolderGraphicFactory
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.ContextMenu
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.ScrollPane
import javafx.scene.control.Spinner
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.util.converter.IntegerStringConverter

class ItemEditorLogic(
    main: MainController,
    dataService: EditorDataService
) : EditorView<ItemData>(
    main = main,
    dataService = dataService,
    dataAccess = dataService.items
) {
    // 1. ツリーのビュー本体（画面に1つ）
    private val treeView = TreeView<TreeRow>().apply {
        prefHeight = 550.0
        prefWidth = 600.0
        isShowRoot = false
        styleClass.add("editor-tree-view")
    }

    // 2. アイテムIDをキーにして、そのアイテムごとの「ツリーの根（構造）」をキャッシュ
    private val treeCache = mutableMapOf<String, TreeItem<TreeRow>>()

    private val expandedStateCache = mutableMapOf<String, MutableMap<String, Boolean>>()

    private val loreTreeUiIdMemory = LoreTreeUiIdMemory()

    private val previewImageView = ImageView().apply {
        isPreserveRatio = true
    }

    private val previewScrollPane = ScrollPane(previewImageView).apply {
        minWidth = 280.0
        minHeight = 560.0

        prefWidth = 280.0
        prefHeight = 560.0

        maxWidth = 280.0
        maxHeight = 560.0

        isFitToWidth = false
        isFitToHeight = false

        hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
    }

    private var previewCanvas: PreviewCanvas? = null

    override fun setupSidebar(container: VBox, selectId: String?) {
        // 再描画が走る前に、一旦古いタイマーを確実に停止・破棄する
        stopAutoSaveTimer()

        container.children.clear()
        selectedButton = null
        sidebarButtons.clear() // 再描画時に古いキャッシュをクリア

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

            // 1. まず、後から呼び出せるように「保存」アイテムを変数に分ける
            val saveMenuItem = MenuItem("保存").apply {
                // 💡 生成時は一旦 false（または true）で置いておく。どうせ開く瞬間に上書きされるため
                isDisable = true
                onAction = EventHandler { onSave(id) } // 引数はループ内の「id」を渡す
            }

            // 💡 右クリックメニュー（ContextMenu）の作成
            val contextMenu = ContextMenu().apply {
                // メニューにアイテムを追加
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
                                        "アイテムID: $id",
                                        "",
                                        "この操作を実行するとアイテムIDが変更され、",
                                        "過去のアイテムIDの付与された",
                                        "Minecraftサーバー上のアイテムが無効化されます。",
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

                                        treeCache.remove(id)?.let { treeCache[inputText] = it }
                                        expandedStateCache.remove(id)?.let { expandedStateCache[inputText] = it }
                                        loreTreeUiIdMemory.renameItem(
                                            oldItemId = id,
                                            newItemId = inputText
                                        )

                                        main.showTimedTopLabel("$id を $inputText に変更しました", Color.GREENYELLOW)

                                        // サイドバーの選択状態を更新する
                                        // この中でselectもされる
                                        setupSidebar(main.sidebarContainer, inputText)
                                    }

                                    RenameResult.FILE_NOT_FOUND -> {
                                        CustomDialog.error()
                                            .title("名前変更エラー")
                                            .header("対象のファイルが見つかりません")
                                            .content("変更元のアイテム($id)が、サーバー上で既に削除されている可能性があります。")
                                            .show()
                                        return@EventHandler
                                    }

                                    RenameResult.ALREADY_EXISTS -> {
                                        CustomDialog.error()
                                            .title("名前変更エラー")
                                            .header("同名のファイルが既に存在します")
                                            .content("入力された名称($inputText)は、サーバー上で他のアイテムに使用されています。\n別の日時や名称を指定してください。")
                                            .show()
                                        return@EventHandler
                                    }

                                    RenameResult.SFTP_INACTIVE, RenameResult.PROFILE_NOT_SELECTED -> {
                                        CustomDialog.error()
                                            .title("接続エラー")
                                            .header("サーバーに接続されていません")
                                            .content("SFTPセッションが切断された可能性があります。再接続してください。")
                                            .show()

                                        // 必要に応じてサーバー選択画面に戻す
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
                                    "アイテムID: $id",
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
                                    treeCache.remove(id)
                                    expandedStateCache.remove(id)
                                    loreTreeUiIdMemory.clearItem(id)
                                    setupSidebar(main.sidebarContainer)
                                }
                            }
                        }
                    }
                )

                // 【超重要】表示される瞬間に、ウィンドウのルート背景を完全に透明にする
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

    override fun resolveSaveConflict(
        dataId: String,
        originalData: ItemData,
        currentData: ItemData,
        serverData: ItemData
    ): ItemData? {
        val dialog = RewriteConfirmation(originalData, serverData)
        val currentStage = main.sidebarContainer.scene.window as? Stage

        if (currentStage != null) {
            dialog.initOwner(currentStage)
        }

        dialog.showAndWait()

        if (!dialog.isConfirmed) return null

        val checkedFields = dialog.selectedCheckedFields
        val finalSaveData = serverData.deepCopy()

        finalSaveData.completed = currentData.completed

        if (checkedFields.any { it.field == ItemDiffField.RARITY }) {
            finalSaveData.rarity = currentData.rarity
        }

        if (checkedFields.any { it.field == ItemDiffField.DETAIL }) {
            finalSaveData.itemDetail = currentData.itemDetail.deepCopy()
        }

        if (checkedFields.any { it.field == ItemDiffField.DISPLAY_NAME }) {
            finalSaveData.display.displayName = currentData.display.displayName
        }

        val maxLoreSize = maxOf(currentData.display.lore.size, serverData.display.lore.size)
        for (i in 0 until maxLoreSize) {
            val isChecked = checkedFields.any {
                it.field == ItemDiffField.LORE && it.index == i
            }

            if (isChecked) {
                val currentLine = currentData.display.lore.getOrNull(i)

                if (currentLine != null) {
                    if (i < finalSaveData.display.lore.size) {
                        finalSaveData.display.lore[i] = currentLine
                    } else {
                        finalSaveData.display.lore.add(currentLine)
                    }
                } else {
                    if (i < finalSaveData.display.lore.size) {
                        finalSaveData.display.lore.removeAt(i)
                    }
                }
            }
        }

        for (key in currentData.stats.keys + serverData.stats.keys) {
            val isChecked = checkedFields.any {
                it.field == ItemDiffField.STATS && it.statsType == key
            }

            if (isChecked) {
                val currentVal = currentData.stats[key]

                if (currentVal != null) {
                    finalSaveData.stats[key] = currentVal
                } else {
                    finalSaveData.stats.remove(key)
                }
            }
        }

        val maxDescSize = maxOf(currentData.editorMeta.comment.size, serverData.editorMeta.comment.size)
        for (i in 0 until maxDescSize) {
            val isChecked = checkedFields.any {
                it.field == ItemDiffField.COMMENT && it.index == i
            }

            if (isChecked) {
                val currentLine = currentData.editorMeta.comment.getOrNull(i)

                if (currentLine != null) {
                    if (i < finalSaveData.editorMeta.comment.size) {
                        finalSaveData.editorMeta.comment[i] = currentLine
                    } else {
                        finalSaveData.editorMeta.comment.add(currentLine)
                    }
                } else {
                    if (i < finalSaveData.editorMeta.comment.size) {
                        finalSaveData.editorMeta.comment.removeAt(i)
                    }
                }
            }
        }

        return finalSaveData
    }

    override fun setupMainContent(selectData: ItemData) {
        previewCanvas = PreviewCanvas(
            itemData = selectData,
            imageView = previewImageView
        )

        previewCanvas?.refreshPreview()

        // 初回のみコンテナにベースUIを追加
        if (main.mainContentContainer.children.isEmpty()) {
            main.mainContentContainer.children.addAll(
                VBox(20.0).apply {
                    children.addAll(
                        Label().apply {
                            style = "-fx-font-size: 18px; -fx-font-weight: bold;"
                        },
                        treeView
                    )
                },
                previewScrollPane
            )
        }

        // ラベルの更新
        ((main.mainContentContainer.children[0] as VBox).children[0] as Label).text = "現在編集中のアイテム: ${selectData.id}"

        // キャッシュからルートオブジェクトを取得、なければ初期展開状態で登録
        val rootItem = treeCache.getOrPut(selectData.id) { TreeItem<TreeRow>(TreeRow.Folder.Lore).apply { isExpanded = true } }

        val expandedMap = expandedStateCache.getOrPut(selectData.id) { mutableMapOf() }

        // 構造を再構築する
        createItemTreeBuilder(
            itemId = selectData.id,
            itemData = selectData,
            expandedMap = expandedMap
        ).rebuildRoot(rootItem)

        // ビューに結びつけ
        treeView.root = rootItem

        fun Button.applyTreeInlineButtonSize(): Button = apply {
            styleClass.add("tree-inline-button")
            isFocusTraversable = false

            minWidth = 36.0
            prefWidth = Region.USE_COMPUTED_SIZE
            maxWidth = Region.USE_PREF_SIZE

            minHeight = 24.0
            prefHeight = 24.0
            maxHeight = 24.0
        }

        fun Label.applyTreeFolderLabelSize(): Label = apply {
            minHeight = Region.USE_PREF_SIZE
            prefHeight = Region.USE_COMPUTED_SIZE
            maxHeight = Region.USE_PREF_SIZE
        }

        fun HBox.applyTreeFolderRowSize(): HBox = apply {
            minHeight = Region.USE_PREF_SIZE
            prefHeight = Region.USE_COMPUTED_SIZE
            maxHeight = Region.USE_PREF_SIZE
        }

        fun showInsertIndexDialog(
            title: String,
            header: String,
            maxIndex: Int
        ): Int? {
            val dialog = Dialog<Int>().apply {
                this.title = title
                this.headerText = header
            }

            val insertButtonType = ButtonType("追加", ButtonBar.ButtonData.OK_DONE)

            dialog.dialogPane.buttonTypes.addAll(
                insertButtonType,
                ButtonType.CANCEL
            )

            val spinner = Spinner<Int>(0, maxIndex, maxIndex).apply {
                isEditable = true
                prefWidth = 120.0

                valueFactory.converter = IntegerStringConverter()

                editor.textProperty().addListener { _, _, newValue ->
                    if (newValue.isBlank()) return@addListener

                    val value = newValue.toIntOrNull()
                    if (value == null) {
                        editor.text = valueFactory.value.toString()
                        return@addListener
                    }

                    val safeValue = value.coerceIn(0, maxIndex)
                    if (safeValue != value) {
                        editor.text = safeValue.toString()
                    }
                }
            }

            dialog.dialogPane.content = VBox(8.0).apply {
                children.addAll(
                    Label("挿入位置: 0 ～ $maxIndex"),
                    spinner
                )
            }

            dialog.setResultConverter { button ->
                if (button == insertButtonType) {
                    spinner.value.coerceIn(0, maxIndex)
                } else {
                    null
                }
            }

            return dialog.showAndWait().orElse(null)
        }

        fun createLoreSectionTypeMenuItems(
            onSelected: (LoreSectionType) -> Unit
        ): List<MenuItem> {
            return LoreSectionType.entries.map { type ->
                MenuItem(type.display).apply {
                    onAction = EventHandler {
                        onSelected(type)
                    }
                }
            }
        }

        val contextMenuFactory = EditorContextMenuFactory<TreeRow> { row ->
            when (row) {
                is TreeRow.Folder.LoreLine -> {
                    val lineSystem = LoreLineEditor(selectData.display, row.lineIndex)

                    ContextMenu().apply {
                        val moveForward = MenuItem("前の行と入れ替え").apply {
                            onAction = EventHandler {
                                val fromIndex = row.lineIndex
                                val toIndex = row.lineIndex - 1

                                lineSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.lineMoved(selectData.id, fromIndex, toIndex)

                                refreshButtonVisual(selectData.id)
                                handleRefresh(TreeRow.Folder.Lore)
                            }
                        }

                        val moveBack = MenuItem("後ろの行と入れ替え").apply {
                            onAction = EventHandler {
                                val fromIndex = row.lineIndex
                                val toIndex = row.lineIndex + 1

                                lineSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.lineMoved(selectData.id, fromIndex, toIndex)

                                refreshButtonVisual(selectData.id)
                                handleRefresh(TreeRow.Folder.Lore)
                            }
                        }

                        val moveToSpecification = MenuItem("指定した行と入れ替え").apply {
                            onAction = EventHandler {
                                val insertIndex = showInsertIndexDialog("行を入れ替え", "入れ替え先の行を選択してください", lineSystem.getLineSize()) ?: return@EventHandler

                                lineSystem.moveTo(insertIndex)
                                loreTreeUiIdMemory.lineMoved(selectData.id, row.lineIndex, insertIndex)

                                refreshButtonVisual(selectData.id)
                                handleRefresh(TreeRow.Folder.Lore)
                            }
                        }

                        items.addAll(
                            moveForward,
                            moveBack,
                            moveToSpecification,
                            Menu("前に行を追加").apply {
                                items.addAll(
                                    createLoreSectionTypeMenuItems { type ->
                                        lineSystem.add(type)
                                        loreTreeUiIdMemory.lineInserted(selectData.id, row.lineIndex)

                                        refreshButtonVisual(selectData.id)
                                        handleRefresh(TreeRow.Folder.Lore)
                                    }
                                )
                            },
                            Menu("後ろに行を追加").apply {
                                items.addAll(
                                    createLoreSectionTypeMenuItems { type ->
                                        val idx = row.lineIndex + 1

                                        LoreLineEditor(selectData.display, idx).add(type)
                                        loreTreeUiIdMemory.lineInserted(selectData.id, idx)

                                        refreshButtonVisual(selectData.id)
                                        handleRefresh(TreeRow.Folder.Lore)
                                    }
                                )
                            },
                            Menu("セクション操作").apply {
                                items.addAll(
                                    Menu("末尾にセクションを追加").apply {
                                        items.addAll(
                                            createLoreSectionTypeMenuItems { type ->
                                                val sectionIndex = lineSystem.getSectionSize()
                                                lineSystem.section(sectionIndex).add(type)
                                                loreTreeUiIdMemory.sectionInserted(selectData.id, row.lineUiId, sectionIndex)

                                                refreshButtonVisual(selectData.id)
                                                handleRefresh(row)
                                            }
                                        )
                                    },
                                    Menu("先頭にセクションを追加").apply {
                                        items.addAll(
                                            createLoreSectionTypeMenuItems { type ->
                                                val sectionIndex = 0

                                                lineSystem.section(sectionIndex).add(type)
                                                loreTreeUiIdMemory.sectionInserted(selectData.id, row.lineUiId, sectionIndex)

                                                refreshButtonVisual(selectData.id)
                                                handleRefresh(row)
                                            }
                                        )
                                    },
                                    Menu("指定位置にセクションを追加").apply {
                                        items.addAll(
                                            createLoreSectionTypeMenuItems { type ->
                                                val maxIndex = lineSystem.getSectionSize()

                                                val insertIndex = showInsertIndexDialog("セクションを追加", "追加する位置を選択してください", maxIndex) ?: return@createLoreSectionTypeMenuItems

                                                lineSystem.section(insertIndex).add(type)
                                                loreTreeUiIdMemory.sectionInserted(selectData.id, row.lineUiId, insertIndex)

                                                refreshButtonVisual(selectData.id)
                                                handleRefresh(row)
                                            }
                                        )
                                    }
                                )
                            },
                            MenuItem("この行を削除").apply {
                                onAction = EventHandler {
                                    lineSystem.remove()
                                    loreTreeUiIdMemory.lineRemoved(selectData.id, row.lineIndex)

                                    refreshButtonVisual(selectData.id)
                                    handleRefresh(TreeRow.Folder.Lore)
                                }
                            }
                        )

                        setOnShowing {
                            val lineSize = LoreLineEditor(selectData.display, 0).getLineSize()

                            moveForward.isDisable = row.lineIndex <= 0
                            moveBack.isDisable = row.lineIndex >= lineSize - 1
                            moveToSpecification.isDisable = lineSize <= 1

                            scene?.root?.style = "-fx-background-color: transparent;"
                        }
                    }
                }

                is TreeRow.Folder.LoreSection -> {
                    fun refreshParentLine() {
                        refreshButtonVisual(selectData.id)
                        handleRefresh(
                            TreeRow.Folder.LoreLine(
                                lineIndex = row.lineIndex,
                                lineUiId = row.lineUiId
                            )
                        )
                    }

                    val sectionSystem = LoreLineEditor(selectData.display, row.lineIndex).section(row.sectionIndex)

                    ContextMenu().apply {
                        val moveForward = MenuItem("前のセクションと入れ替え").apply {
                            onAction = EventHandler {
                                val fromIndex = row.sectionIndex
                                val toIndex = row.sectionIndex - 1

                                sectionSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.sectionMoved(selectData.id, row.lineUiId, fromIndex, toIndex)

                                refreshParentLine()
                            }
                        }

                        val moveBack = MenuItem("後ろのセクションと入れ替え").apply {
                            onAction = EventHandler {
                                val fromIndex = row.sectionIndex
                                val toIndex = row.sectionIndex + 1

                                sectionSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.sectionMoved(selectData.id, row.lineUiId, fromIndex, toIndex)

                                refreshParentLine()
                            }
                        }

                        val moveToSpecification = MenuItem("指定したセクションと入れ替え").apply {
                            onAction = EventHandler {
                                val sectionSize = LoreLineEditor(selectData.display, row.lineIndex).getSectionSize()
                                val maxIndex = (sectionSize - 1).coerceAtLeast(0)

                                val insertIndex = showInsertIndexDialog(
                                    title = "セクションを入れ替え",
                                    header = "入れ替え先のセクションを選択してください",
                                    maxIndex = maxIndex
                                ) ?: return@EventHandler

                                sectionSystem.moveTo(insertIndex)
                                loreTreeUiIdMemory.sectionMoved(selectData.id, row.lineUiId, row.sectionIndex, insertIndex)

                                refreshParentLine()
                            }
                        }

                        val remove = MenuItem("このセクションを削除").apply {
                            onAction = EventHandler {
                                sectionSystem.remove()
                                loreTreeUiIdMemory.sectionRemoved(selectData.id, row.lineUiId, row.sectionIndex)

                                refreshParentLine()
                            }
                        }

                        items.addAll(
                            moveForward,
                            moveBack,
                            moveToSpecification,
                            Menu("前にセクションを追加").apply {
                                items.addAll(
                                    createLoreSectionTypeMenuItems { type ->
                                        sectionSystem.add(type)
                                        loreTreeUiIdMemory.sectionInserted(selectData.id, row.lineUiId, row.sectionIndex)

                                        refreshParentLine()
                                    }
                                )
                            },
                            Menu("後ろにセクションを追加").apply {
                                items.addAll(
                                    createLoreSectionTypeMenuItems { type ->
                                        val idx = row.sectionIndex + 1

                                        LoreLineEditor(selectData.display, row.lineIndex).section(idx).add(type)
                                        loreTreeUiIdMemory.sectionInserted(selectData.id, row.lineUiId, idx)

                                        refreshParentLine()
                                    }
                                )
                            },
                            remove
                        )

                        setOnShowing {
                            val sectionSize = LoreLineEditor(selectData.display, row.lineIndex).getSectionSize()

                            moveForward.isDisable = row.sectionIndex <= 0
                            moveBack.isDisable = row.sectionIndex >= sectionSize - 1
                            moveToSpecification.isDisable = sectionSize <= 1
                            remove.isDisable = sectionSize <= 1

                            scene?.root?.style = "-fx-background-color: transparent;"
                        }
                    }
                }

                else -> null
            }
        }

        val folderGraphicFactory = EditorFolderGraphicFactory<TreeRow> { row ->
            when (row) {
                TreeRow.Folder.Lore -> HBox(6.0).apply {
                    alignment = Pos.CENTER_LEFT
                    styleClass.add("editor-row-hbox")
                    applyTreeFolderRowSize()

                    children.addAll(
                        Label(row.label).apply {
                            styleClass.add("editor-label-highlight")
                            applyTreeFolderLabelSize()
                        },
                        Button("先頭に行を追加").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-add")

                            onAction = EventHandler { event ->
                                ContextMenu().apply {
                                    setOnShowing { scene?.root?.style = "-fx-background-color: transparent;" }

                                    items.addAll(
                                        createLoreSectionTypeMenuItems { type ->
                                            val insertIndex = 0

                                            LoreLineEditor(selectData.display, insertIndex).add(type)
                                            loreTreeUiIdMemory.lineInserted(
                                                itemId = selectData.id,
                                                index = insertIndex
                                            )

                                            refreshButtonVisual(selectData.id)
                                            handleRefresh(row)
                                        }
                                    )
                                }.show(this, Side.BOTTOM, 0.0, 0.0)

                                event.consume()
                            }
                        },
                        Button("末尾に行を追加").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-add")

                            onAction = EventHandler { event ->
                                ContextMenu().apply {
                                    setOnShowing { scene?.root?.style = "-fx-background-color: transparent;" }

                                    items.addAll(
                                        createLoreSectionTypeMenuItems { type ->
                                            val insertIndex = LoreLineEditor(selectData.display, 0).getLineSize()

                                            LoreLineEditor(selectData.display, insertIndex).add(type)
                                            loreTreeUiIdMemory.lineInserted(
                                                itemId = selectData.id,
                                                index = insertIndex
                                            )

                                            refreshButtonVisual(selectData.id)
                                            handleRefresh(row)
                                        }
                                    )
                                }.show(this, Side.BOTTOM, 0.0, 0.0)

                                event.consume()
                            }
                        },
                        Button("指定位置に行を追加").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-add")

                            onAction = EventHandler { event ->
                                ContextMenu().apply {
                                    setOnShowing { scene?.root?.style = "-fx-background-color: transparent;" }

                                    items.addAll(
                                        createLoreSectionTypeMenuItems { type ->
                                            val maxIndex = LoreLineEditor(selectData.display, 0).getLineSize()

                                            val insertIndex = showInsertIndexDialog("行を追加", "追加する位置を選択してください", maxIndex) ?: return@createLoreSectionTypeMenuItems

                                            LoreLineEditor(selectData.display, insertIndex).add(type)
                                            loreTreeUiIdMemory.lineInserted(selectData.id, insertIndex)

                                            refreshButtonVisual(selectData.id)
                                            handleRefresh(row)
                                        }
                                    )
                                }.show(this, Side.BOTTOM, 0.0, 0.0)

                                event.consume()
                            }
                        }
                    )
                }

                is TreeRow.Folder.LoreLine -> HBox(8.0).apply {
                    alignment = Pos.CENTER_LEFT
                    styleClass.add("editor-row-hbox")
                    applyTreeFolderRowSize()

                    val lineSystem = LoreLineEditor(selectData.display, row.lineIndex)

                    children.addAll(
                        Label(row.label).apply {
                            styleClass.add("editor-label-highlight")
                            applyTreeFolderLabelSize()
                        },
                        Button("▲").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-add")

                            isDisable = row.lineIndex <= 0

                            onAction = EventHandler { event ->
                                val toIndex = row.lineIndex - 1

                                lineSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.lineMoved(selectData.id, row.lineIndex, toIndex)

                                refreshButtonVisual(selectData.id)
                                handleRefresh(TreeRow.Folder.Lore)

                                event.consume()
                            }
                        },
                        Button("▼").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-add")

                            isDisable = row.lineIndex >= LoreLineEditor(selectData.display, 0).getLineSize() - 1

                            onAction = EventHandler { event ->
                                val fromIndex = row.lineIndex
                                val toIndex = row.lineIndex + 1

                                lineSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.lineMoved(selectData.id, fromIndex, toIndex)

                                refreshButtonVisual(selectData.id)
                                handleRefresh(TreeRow.Folder.Lore)

                                event.consume()
                            }
                        },
                        Button("×").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-remove")

                            onAction = EventHandler { event ->
                                lineSystem.remove()
                                loreTreeUiIdMemory.lineRemoved(selectData.id, row.lineIndex)

                                refreshButtonVisual(selectData.id)
                                handleRefresh(TreeRow.Folder.Lore)

                                event.consume()
                            }
                        }
                    )
                }

                is TreeRow.Folder.LoreSection -> HBox(8.0).apply {
                    fun refreshParentLine() {
                        refreshButtonVisual(selectData.id)
                        handleRefresh(
                            TreeRow.Folder.LoreLine(
                                lineIndex = row.lineIndex,
                                lineUiId = row.lineUiId
                            )
                        )
                    }

                    alignment = Pos.CENTER_LEFT
                    styleClass.add("editor-row-hbox")
                    applyTreeFolderRowSize()

                    val sectionSystem = LoreLineEditor(selectData.display, row.lineIndex).section(row.sectionIndex)

                    children.addAll(
                        Label(row.label).apply {
                            styleClass.add("editor-label-highlight")
                            applyTreeFolderLabelSize()
                        },
                        Button("▲").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-add")

                            isDisable = row.sectionIndex <= 0

                            onAction = EventHandler { event ->
                                val fromIndex = row.sectionIndex
                                val toIndex = row.sectionIndex - 1

                                sectionSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.sectionMoved(selectData.id, row.lineUiId, fromIndex, toIndex)

                                refreshParentLine()

                                event.consume()
                            }
                        },
                        Button("▼").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-add")

                            isDisable = row.sectionIndex >= LoreLineEditor(
                                selectData.display,
                                row.lineIndex
                            ).getSectionSize() - 1

                            onAction = EventHandler { event ->
                                val fromIndex = row.sectionIndex
                                val toIndex = row.sectionIndex + 1

                                sectionSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.sectionMoved(selectData.id, row.lineUiId, fromIndex, toIndex)

                                refreshParentLine()

                                event.consume()
                            }
                        },
                        Button("×").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-remove")

                            isDisable = LoreLineEditor(selectData.display, row.lineIndex).getSectionSize() <= 1

                            onAction = EventHandler { event ->
                                sectionSystem.remove()
                                loreTreeUiIdMemory.sectionRemoved(selectData.id, row.lineUiId, row.sectionIndex)

                                refreshParentLine()

                                event.consume()
                            }
                        }
                    )
                }

                else -> null
            }
        }

        treeView.setCellFactory {
            LoreDragDropTreeCell(
                itemData = selectData,
                refreshButtonVisual = ::refreshButtonVisual,
                onRefresh = { row -> handleRefresh(row) },
                loreTreeUiIdMemory = loreTreeUiIdMemory,
                folderGraphicFactory = folderGraphicFactory,
                contextMenuFactory = contextMenuFactory
            )
        }
    }

    private fun createItemTreeBuilder(
        itemId: String,
        itemData: ItemData,
        expandedMap: MutableMap<String, Boolean>
    ): ItemTreeBuilder {
        val lineSize = LoreLineEditor(itemData.display, 0).getLineSize()
        val lineUiIds = loreTreeUiIdMemory.getLineIds(
            itemId = itemId,
            lineSize = lineSize
        )

        return ItemTreeBuilder(
            itemData = itemData,
            expandedMap = expandedMap,
            lineUiIds = lineUiIds,
            getSectionUiIds = { lineUiId, sectionSize ->
                loreTreeUiIdMemory.getSectionIds(
                    itemId = itemId,
                    lineUiId = lineUiId,
                    sectionSize = sectionSize
                )
            }
        )
    }

    private fun handleRefresh(targetRow: TreeRow) {
        val currentItemId = currentSelectedDataId ?: return
        val currentData = editingDataMap[currentItemId] ?: return
        val targetItem = findTreeItemByRow(treeView.root, targetRow) ?: return

        val expandedMap = expandedStateCache.getOrPut(currentItemId) {
            mutableMapOf()
        }

        val builder = createItemTreeBuilder(
            itemId = currentItemId,
            itemData = currentData,
            expandedMap = expandedMap
        )

        when (targetRow) {
            is TreeRow.Folder.LoreLine -> {
                builder.rebuildLoreLine(targetItem, targetRow.lineIndex)
            }

            else -> {
                builder.rebuildRoot(targetItem)
            }
        }
    }

    private fun findTreeItemByRow(root: TreeItem<TreeRow>, targetRow: TreeRow): TreeItem<TreeRow>? {
        if (root.value == targetRow) return root
        for (child in root.children) {
            val found = findTreeItemByRow(child, targetRow)
            if (found != null) return found
        }
        return null
    }

    override fun refreshButtonVisual(id: String) {
        // プレビュー更新
        previewCanvas?.refreshPreview()
        super.refreshButtonVisual(id)
    }
}