package io.github.toumokorosi01.sushiericdataeditor2.editor.item

import io.github.toumokorosi01.common.Dir
import io.github.toumokorosi01.common.data.item.LoreLineEditor
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.item.data.LoreSectionType
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.CustomDialog
import io.github.toumokorosi01.sushiericdataeditor2.editor.item.diff.DiffField
import io.github.toumokorosi01.sushiericdataeditor2.ui.dialog.ErrorType
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.LoadResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.SaveResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.service.EditorDataService
import io.github.toumokorosi01.sushiericdataeditor2.editor.view.EditorView
import io.github.toumokorosi01.sushiericdataeditor2.editor.controller.MainController
import io.github.toumokorosi01.sushiericdataeditor2.editor.item.diff.RewriteConfirmation
import io.github.toumokorosi01.sushiericdataeditor2.editor.item.tree.ItemTreeBuilder
import io.github.toumokorosi01.sushiericdataeditor2.editor.item.tree.LoreDragDropTreeCell
import io.github.toumokorosi01.sushiericdataeditor2.editor.item.tree.LoreTreeUiIdMemory
import io.github.toumokorosi01.sushiericdataeditor2.editor.item.tree.TreeRow
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.ValidationResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.DeleteResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.result.dataservice.RenameResult
import io.github.toumokorosi01.sushiericdataeditor2.editor.tree.EditorContextMenuFactory
import io.github.toumokorosi01.sushiericdataeditor2.editor.tree.EditorFolderGraphicFactory
import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
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
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.Duration
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.image.ImageView
import javafx.util.converter.IntegerStringConverter
import org.slf4j.LoggerFactory

class ItemEditorLogic(main: MainController, dataService: EditorDataService) : EditorView(main, dataService) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private var selectedButton: Button? = null

    // ID（String）をキーにしたキャッシュMap
    private val editingDataMap = mutableMapOf<String, ItemData>()
    private val originalDataMap = mutableMapOf<String, ItemData>()

    // 現在画面に表示しているアイテムのID
    private var currentSelectedItemId: String? = null

    // サイドバーに並んでいるボタンをIDで即座に引き出せるようにするプロパティ
    private val sidebarButtons = mutableMapOf<String, Button>()

    // クラスのプロパティにタイマーを保持
    private var autoSaveTimeline: Timeline? = null

    private var restoredCacheCount = 0

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

        val (fileResources, isSuccess) = dataService.listYmlResources(Dir.Item.Stats)
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
                            val clipboard = javafx.scene.input.Clipboard.getSystemClipboard()
                            val content = javafx.scene.input.ClipboardContent()
                            content.putString(id)
                            clipboard.setContent(content)

                            main.showTimedTopLabel("コピーしました: $id", Color.GREENYELLOW)
                        }
                    },
                    saveMenuItem,
                    MenuItem("IDを変更").apply {
                        style = "-fx-text-fill: #ff4444;"
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

                                if (isConfirm) when (dataService.rename(id, inputText)) {
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
                        style = "-fx-text-fill: #ff4444;" // 危険アクションっぽく赤文字に
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

                            if (isConfirm) when (dataService.delete(id)) {
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

                // 💡 【超重要】表示される瞬間に、ウィンドウのルート背景を完全に透明にする
                setOnShowing {
                    saveMenuItem.isDisable = (originalDataMap[id] == editingDataMap[id])

                    // PopupWindow が内部で生成している Scene のルートノード（PopupControl.CSSBridge 等）を取得
                    val popupScene = scene
                    val popupRoot = popupScene.root

                    if (popupRoot != null) {
                        // 土台の背景色を完全に透明にする（これで四角い白トゲの親玉が消滅します）
                        popupRoot.style = "-fx-background-color: transparent;"
                    }
                }
            }

            // 💡 ボタンにコンテキストメニューを紐付ける
            // これだけで、JavaFXが自動的に「右クリックされたら出す」という制御をしてくれます
            btn.contextMenu = contextMenu

            container.children.add(btn)

            // 💡 生成したボタンをIDをキーにしてプロパティ（Map）に保存！
            sidebarButtons[id] = btn
        }

        // 未保存の変更があるデータに目印をつける
        fileResources.forEach { file ->
            val id = file.name.removeSuffix(".yml")
            refreshButtonVisual(id)
        }

        if (fileResources.isEmpty()) return

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

    override fun selectTab(targetId: String) {
        this.currentSelectedItemId = targetId // 現在選択中のIDを更新

        val hasCache = editingDataMap.containsKey(targetId)
        val isUnchanged = hasCache && (originalDataMap[targetId] == editingDataMap[targetId])

        // 💡 最初から両方 Map に入っているので、ここを無駄に通過すること自体がなくなります！
        if (!hasCache || isUnchanged) {
            val (data, accessResult) = dataService.load(targetId)

            // 取得失敗時は必ず null
            if (data == null) {
                when (accessResult) {
                    LoadResult.SUCCESS -> {
                        // dataがnullなのにSUCCESSなのはデータ構造の矛盾（実質的なエラー）
                        // 今の構造的に起きないが念のため
                        CustomDialog.error(ErrorType.INTERNAL_ERROR)
                            .content("データが空（null）です。")
                            .owner(main.currentStage)
                            .show()
                    }
                    LoadResult.INVALID_YAML, LoadResult.FILE_NOT_FOUND -> {
                        val errorType = if (accessResult == LoadResult.INVALID_YAML) ErrorType.INVALID_YAML else ErrorType.FILE_NOT_FOUND
                        CustomDialog.error(errorType)
                            .content("データを再読み込みします...")
                            .owner(main.currentStage)
                            .show()
                    }
                    LoadResult.FAILED, LoadResult.PROFILE_NOT_SELECTED, LoadResult.SFTP_INACTIVE -> {
                        CustomDialog.error(ErrorType.NETWORK_ERROR)
                            .owner(main.currentStage)
                            .show()
                        handleForceBackToSelect()
                    }
                }
                return
            }

            editingDataMap[targetId] = data
            originalDataMap[targetId] = data.deepCopy()
        }

        selectButtonById(targetId)
        setupMainContent(editingDataMap[targetId]!!)
    }

    override fun setupActions(container: HBox) {
        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
        container.children.setAll(
            Button("アイテム保存").apply {
                translateY = -1.0
                isFocusTraversable = false
                maxHeight = Double.MAX_VALUE
                minHeight = 0.0
                onAction = EventHandler { onSave() }
            },
            spacer,
            Button("新規作成").apply {
                translateY = -1.0
                isFocusTraversable = false
                maxHeight = Double.MAX_VALUE
                minHeight = 0.0
                onAction = EventHandler { handleCreateNewItem() }
            }
        )
    }

    override fun onSave(targetItemId: String?) {
        val itemId = targetItemId ?: currentSelectedItemId ?: return
        val currentEdit = editingDataMap[itemId] ?: return
        val original = originalDataMap[itemId] ?: return

        if (original == currentEdit) return

        val (serverData, accessResult) = dataService.load(itemId)

        println(accessResult.name)

        var saveData = currentEdit.deepCopy()

        // 💡 2. ロードした結果ステータスに応じた条件分岐
        when (accessResult) {
            LoadResult.FAILED, LoadResult.PROFILE_NOT_SELECTED, LoadResult.SFTP_INACTIVE -> {
                CustomDialog.error(ErrorType.INTERNAL_ERROR)
                    .content(listOf(
                        "ネットワークまたはその他の例外が発生しました。",
                        "選択画面へ戻ります。"
                    ))
                    .owner(main.currentStage)
                    .show()

                handleForceBackToSelect()
                return
            }

            LoadResult.FILE_NOT_FOUND -> {
                // サーバー上にまだファイルがない場合はコンフリクト判定をスキップし、そのまま手元のデータで保存
                logger.info("サーバー上にファイルが存在しないため、新規ファイルとして保存します: $itemId")
            }

            LoadResult.FAILED -> {
                // その他の予期せぬ通信エラーやI/Oエラー
                CustomDialog.error(ErrorType.NETWORK_ERROR)
                    .owner(main.currentStage)
                    .show()

                handleForceBackToSelect()
                return
            }

            LoadResult.INVALID_YAML -> {
                // サーバーのファイルが壊れている場合。強制上書きするかユーザーに確認を取る
                val isConfirm = CustomDialog.confirmation()
                    .title("データ破損警告")
                    .header("サーバー上のYAMLデータが不正、または破損しています。")
                    .content("このまま保存すると、サーバー上の破損データは現在の編集内容で完全に上書きされます。強制保存しますか？")
                    .owner(main.currentStage)
                    .show()

                if (!isConfirm) {
                    logger.info("サーバーデータのYAML破損のため、ユーザーが保存を中止しました。")
                    return
                }
            }

            LoadResult.SUCCESS -> {
                // 正常にサーバーデータが取得できた場合のみ、既存のコンフリクト判定（マージツリー）を行う
                if (serverData == null) return // 防衛コード

                if (original != serverData) {
                    val dialog = RewriteConfirmation(original, serverData)
                    val currentStage = main.sidebarContainer.scene.window as? Stage
                    if (currentStage != null) {
                        dialog.initOwner(currentStage)
                    }
                    dialog.showAndWait()

                    if (dialog.isConfirmed) {
                        // 文字列ではなく、チェックされたデータ（DiffIdのSet）を取り出す
                        val checkedFields = dialog.selectedCheckedFields
                        val finalSaveData = serverData.deepCopy()

                        // 完成フラグは内部用なので確認ダイアログの選択に関わらず、
                        // 常に自分が手元で編集していた値を最優先で適用して保護する
                        finalSaveData.completed = currentEdit.completed

                        // 単一フィールドの型安全マージ
                        if (checkedFields.any { it.field == DiffField.RARITY }) finalSaveData.rarity = currentEdit.rarity
                        if (checkedFields.any { it.field == DiffField.DETAIL }) finalSaveData.itemDetail = currentEdit.itemDetail.deepCopy()
                        if (checkedFields.any { it.field == DiffField.DISPLAY_NAME }) finalSaveData.display.displayName = currentEdit.display.displayName

                        // Lore（行単位）の型安全マージ
                        val maxLoreSize = maxOf(currentEdit.display.lore.size, serverData.display.lore.size)
                        for (i in 0 until maxLoreSize) {
                            val isChecked = checkedFields.any { it.field == DiffField.LORE && it.index == i }
                            if (isChecked) {
                                val currentLine = currentEdit.display.lore.getOrNull(i)
                                if (currentLine != null) {
                                    if (i < finalSaveData.display.lore.size) finalSaveData.display.lore[i] = currentLine else finalSaveData.display.lore.add(currentLine)
                                } else {
                                    if (i < finalSaveData.display.lore.size) finalSaveData.display.lore.removeAt(i)
                                }
                            }
                        }

                        // Stats（キー単位）の型安全マージ
                        for (key in (currentEdit.stats.keys + serverData.stats.keys)) {
                            val isChecked = checkedFields.any { it.field == DiffField.STATS && it.statsType == key }
                            if (isChecked) {
                                val currentVal = currentEdit.stats[key]
                                if (currentVal != null) finalSaveData.stats[key] = currentVal else finalSaveData.stats.remove(key)
                            }
                        }

                        // 説明文（行単位）の型安全マージ
                        val maxDescSize = maxOf(currentEdit.editorMeta.comment.size, serverData.editorMeta.comment.size)
                        for (i in 0 until maxDescSize) {
                            val isChecked = checkedFields.any { it.field == DiffField.COMMENT && it.index == i }
                            if (isChecked) {
                                val currentLine = currentEdit.editorMeta.comment.getOrNull(i)
                                if (currentLine != null) {
                                    if (i < finalSaveData.editorMeta.comment.size) finalSaveData.editorMeta.comment[i] = currentLine else finalSaveData.editorMeta.comment.add(currentLine)
                                } else {
                                    if (i < finalSaveData.editorMeta.comment.size) finalSaveData.editorMeta.comment.removeAt(i)
                                }
                            }
                        }

                        saveData = finalSaveData
                    } else return
                }
            }
        }

        // 💡 3. 最終保存処理（元の挙動のまま変更なし）
        when (dataService.save(itemId, saveData)) {
            SaveResult.SUCCESS -> {
                originalDataMap[itemId] = saveData.deepCopy()
                editingDataMap[itemId] = saveData.deepCopy()

                if (itemId == currentSelectedItemId) {
                    selectTab(itemId)
                } else {
                    refreshButtonVisual(itemId)
                }

                dataService.deleteLocalBackup(itemId)

                // 💡 タイマーを一度再起動して、次の自動保存をここから3分後にリセットする
                stopAutoSaveTimer()
                startAutoSaveTimer()

                main.showTimedTopLabel("$itemId を保存しました", Color.GREENYELLOW)
            }
            SaveResult.SFTP_INACTIVE -> {
                CustomDialog.error(ErrorType.SFTP_ERROR)
                    .owner(main.currentStage)
                    .show()
                handleForceBackToSelect()
            }
            SaveResult.FAILED -> {
                CustomDialog.error(ErrorType.INTERNAL_ERROR)
                    .owner(main.currentStage)
                    .show()
                handleForceBackToSelect()
            }
        }
    }

    override fun onClose(): Boolean {
        logger.info("アイテムエディタのクローズ処理を開始します。未保存の変更をローカルへ即時保存します。")

        // 💡 どのような経路（通常・強制）で閉じられても、その瞬間の最新データを100%確実にローカルへ退避
        executeAutoSave()

        // 安全にタイマーを停止
        stopAutoSaveTimer()

        main.clearTopLabelTimer()
        main.clearShortcuts()

        editingDataMap.clear()
        originalDataMap.clear()
        sidebarButtons.clear()
        selectedButton = null
        currentSelectedItemId = null

        return true // 閉じてOK
    }

    private fun handleCreateNewItem() {
        val (fileResources, isSuccess) = dataService.listYmlResources(Dir.Item.Stats)
        if (!isSuccess) {
            CustomDialog.error()
                .title("取得失敗")
                .header("ファイルリストの取得に失敗しました。")
                .owner(main.currentStage)
                .show()
            handleForceBackToSelect()
            return
        }

        val inputText = main.requestInput("アイテム追加") { input ->
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
            val data = ItemData(id = inputText)
            when (dataService.save(inputText, data)) {
                SaveResult.SUCCESS -> {
                    // 1. 新規作成データを先にキャッシュに登録しておく
                    editingDataMap[inputText] = data
                    originalDataMap[inputText] = data.deepCopy()

                    // 2. サイドバーを再描画（これでリストに新しいボタンが追加される）
                    setupSidebar(main.sidebarContainer)

                    // 3. 💡 【不具合解決】直接メインを作るのではなく、統一された selectTab(id) を呼び出す！
                    // これにより、ハイライト適用、初期データの画面ロード、リスナーの登録がすべて自動で行われます
                    selectTab(inputText)
                }
                SaveResult.SFTP_INACTIVE -> {
                    CustomDialog.error(ErrorType.SFTP_ERROR)
                        .owner(main.currentStage)
                        .show()
                    handleForceBackToSelect()
                    return
                }
                SaveResult.FAILED -> {
                    CustomDialog.error(ErrorType.INTERNAL_ERROR)
                        .owner(main.currentStage)
                        .show()
                    handleForceBackToSelect()
                    return
                }
            }
        }
    }

    /**
     * 起動時に外側から自動保存バックアップ（新旧ペア）を注入するための関数
     */
    fun injectAutoSaveCaches(editingCaches: Map<String, ItemData>, originalCaches: Map<String, ItemData>) {
        if (editingCaches.isEmpty()) return

        logger.info("外部から ${editingCaches.size} 件の自動保存（新旧ペア）キャッシュが注入されました。")

        // 💡 編集データとオリジナルデータを両方とも最初から完全に復元しておく！
        editingDataMap.putAll(editingCaches)
        originalDataMap.putAll(originalCaches)

        restoredCacheCount = editingCaches.size
    }

    /**
     * ネットワーク切断など、アプリ側からエディタを強制終了して選択画面へ戻す安全な処理
     */
    private fun handleForceBackToSelect() {
        logger.warn("ネットワーク切断または不正な状態を検知したため、エディタを強制終了します。")
        cancelOpen()
        dataService.forceBackToSelect()
    }

    private fun setupMainContent(itemData: ItemData) {
        previewCanvas = PreviewCanvas(
            itemData = itemData,
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
        ((main.mainContentContainer.children[0] as VBox).children[0] as Label).text = "現在編集中のアイテム: ${itemData.id}"

        // キャッシュからルートオブジェクトを取得、なければ初期展開状態で登録
        val rootItem = treeCache.getOrPut(itemData.id) { TreeItem<TreeRow>(TreeRow.Folder.Lore).apply { isExpanded = true } }

        val expandedMap = expandedStateCache.getOrPut(itemData.id) { mutableMapOf() }

        // 構造を再構築する
        createItemTreeBuilder(
            itemId = itemData.id,
            itemData = itemData,
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
                    val lineSystem = LoreLineEditor(itemData.display, row.lineIndex)

                    ContextMenu().apply {
                        val moveForward = MenuItem("前の行と入れ替え").apply {
                            onAction = EventHandler {
                                val fromIndex = row.lineIndex
                                val toIndex = row.lineIndex - 1

                                lineSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.lineMoved(itemData.id, fromIndex, toIndex)

                                refreshButtonVisual(itemData.id)
                                handleRefresh(TreeRow.Folder.Lore)
                            }
                        }

                        val moveBack = MenuItem("後ろの行と入れ替え").apply {
                            onAction = EventHandler {
                                val fromIndex = row.lineIndex
                                val toIndex = row.lineIndex + 1

                                lineSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.lineMoved(itemData.id, fromIndex, toIndex)

                                refreshButtonVisual(itemData.id)
                                handleRefresh(TreeRow.Folder.Lore)
                            }
                        }

                        val moveToSpecification = MenuItem("指定した行と入れ替え").apply {
                            onAction = EventHandler {
                                val insertIndex = showInsertIndexDialog("行を入れ替え", "入れ替え先の行を選択してください", lineSystem.getLineSize()) ?: return@EventHandler

                                lineSystem.moveTo(insertIndex)
                                loreTreeUiIdMemory.lineMoved(itemData.id, row.lineIndex, insertIndex)

                                refreshButtonVisual(itemData.id)
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
                                        loreTreeUiIdMemory.lineInserted(itemData.id, row.lineIndex)

                                        refreshButtonVisual(itemData.id)
                                        handleRefresh(TreeRow.Folder.Lore)
                                    }
                                )
                            },
                            Menu("後ろに行を追加").apply {
                                items.addAll(
                                    createLoreSectionTypeMenuItems { type ->
                                        val idx = row.lineIndex + 1

                                        LoreLineEditor(itemData.display, idx).add(type)
                                        loreTreeUiIdMemory.lineInserted(itemData.id, idx)

                                        refreshButtonVisual(itemData.id)
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
                                                loreTreeUiIdMemory.sectionInserted(itemData.id, row.lineUiId, sectionIndex)

                                                refreshButtonVisual(itemData.id)
                                                handleRefresh(row)
                                            }
                                        )
                                    },
                                    Menu("先頭にセクションを追加").apply {
                                        items.addAll(
                                            createLoreSectionTypeMenuItems { type ->
                                                val sectionIndex = 0

                                                lineSystem.section(sectionIndex).add(type)
                                                loreTreeUiIdMemory.sectionInserted(itemData.id, row.lineUiId, sectionIndex)

                                                refreshButtonVisual(itemData.id)
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
                                                loreTreeUiIdMemory.sectionInserted(itemData.id, row.lineUiId, insertIndex)

                                                refreshButtonVisual(itemData.id)
                                                handleRefresh(row)
                                            }
                                        )
                                    }
                                )
                            },
                            MenuItem("この行を削除").apply {
                                onAction = EventHandler {
                                    lineSystem.remove()
                                    loreTreeUiIdMemory.lineRemoved(itemData.id, row.lineIndex)

                                    refreshButtonVisual(itemData.id)
                                    handleRefresh(TreeRow.Folder.Lore)
                                }
                            }
                        )

                        setOnShowing {
                            val lineSize = LoreLineEditor(itemData.display, 0).getLineSize()

                            moveForward.isDisable = row.lineIndex <= 0
                            moveBack.isDisable = row.lineIndex >= lineSize - 1
                            moveToSpecification.isDisable = lineSize <= 1

                            scene?.root?.style = "-fx-background-color: transparent;"
                        }
                    }
                }

                is TreeRow.Folder.LoreSection -> {
                    fun refreshParentLine() {
                        refreshButtonVisual(itemData.id)
                        handleRefresh(
                            TreeRow.Folder.LoreLine(
                                lineIndex = row.lineIndex,
                                lineUiId = row.lineUiId
                            )
                        )
                    }

                    val sectionSystem = LoreLineEditor(itemData.display, row.lineIndex).section(row.sectionIndex)

                    ContextMenu().apply {
                        val moveForward = MenuItem("前のセクションと入れ替え").apply {
                            onAction = EventHandler {
                                val fromIndex = row.sectionIndex
                                val toIndex = row.sectionIndex - 1

                                sectionSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.sectionMoved(itemData.id, row.lineUiId, fromIndex, toIndex)

                                refreshParentLine()
                            }
                        }

                        val moveBack = MenuItem("後ろのセクションと入れ替え").apply {
                            onAction = EventHandler {
                                val fromIndex = row.sectionIndex
                                val toIndex = row.sectionIndex + 1

                                sectionSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.sectionMoved(itemData.id, row.lineUiId, fromIndex, toIndex)

                                refreshParentLine()
                            }
                        }

                        val moveToSpecification = MenuItem("指定したセクションと入れ替え").apply {
                            onAction = EventHandler {
                                val sectionSize = LoreLineEditor(itemData.display, row.lineIndex).getSectionSize()
                                val maxIndex = (sectionSize - 1).coerceAtLeast(0)

                                val insertIndex = showInsertIndexDialog(
                                    title = "セクションを入れ替え",
                                    header = "入れ替え先のセクションを選択してください",
                                    maxIndex = maxIndex
                                ) ?: return@EventHandler

                                sectionSystem.moveTo(insertIndex)
                                loreTreeUiIdMemory.sectionMoved(itemData.id, row.lineUiId, row.sectionIndex, insertIndex)

                                refreshParentLine()
                            }
                        }

                        val remove = MenuItem("このセクションを削除").apply {
                            onAction = EventHandler {
                                sectionSystem.remove()
                                loreTreeUiIdMemory.sectionRemoved(itemData.id, row.lineUiId, row.sectionIndex)

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
                                        loreTreeUiIdMemory.sectionInserted(itemData.id, row.lineUiId, row.sectionIndex)

                                        refreshParentLine()
                                    }
                                )
                            },
                            Menu("後ろにセクションを追加").apply {
                                items.addAll(
                                    createLoreSectionTypeMenuItems { type ->
                                        val idx = row.sectionIndex + 1

                                        LoreLineEditor(itemData.display, row.lineIndex).section(idx).add(type)
                                        loreTreeUiIdMemory.sectionInserted(itemData.id, row.lineUiId, idx)

                                        refreshParentLine()
                                    }
                                )
                            },
                            remove
                        )

                        setOnShowing {
                            val sectionSize = LoreLineEditor(itemData.display, row.lineIndex).getSectionSize()

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

                                            LoreLineEditor(itemData.display, insertIndex).add(type)
                                            loreTreeUiIdMemory.lineInserted(
                                                itemId = itemData.id,
                                                index = insertIndex
                                            )

                                            refreshButtonVisual(itemData.id)
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
                                            val insertIndex = LoreLineEditor(itemData.display, 0).getLineSize()

                                            LoreLineEditor(itemData.display, insertIndex).add(type)
                                            loreTreeUiIdMemory.lineInserted(
                                                itemId = itemData.id,
                                                index = insertIndex
                                            )

                                            refreshButtonVisual(itemData.id)
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
                                            val maxIndex = LoreLineEditor(itemData.display, 0).getLineSize()

                                            val insertIndex = showInsertIndexDialog("行を追加", "追加する位置を選択してください", maxIndex) ?: return@createLoreSectionTypeMenuItems

                                            LoreLineEditor(itemData.display, insertIndex).add(type)
                                            loreTreeUiIdMemory.lineInserted(itemData.id, insertIndex)

                                            refreshButtonVisual(itemData.id)
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

                    val lineSystem = LoreLineEditor(itemData.display, row.lineIndex)

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
                                loreTreeUiIdMemory.lineMoved(itemData.id, row.lineIndex, toIndex)

                                refreshButtonVisual(itemData.id)
                                handleRefresh(TreeRow.Folder.Lore)

                                event.consume()
                            }
                        },
                        Button("▼").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-add")

                            isDisable = row.lineIndex >= LoreLineEditor(itemData.display, 0).getLineSize() - 1

                            onAction = EventHandler { event ->
                                val fromIndex = row.lineIndex
                                val toIndex = row.lineIndex + 1

                                lineSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.lineMoved(itemData.id, fromIndex, toIndex)

                                refreshButtonVisual(itemData.id)
                                handleRefresh(TreeRow.Folder.Lore)

                                event.consume()
                            }
                        },
                        Button("×").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-remove")

                            onAction = EventHandler { event ->
                                lineSystem.remove()
                                loreTreeUiIdMemory.lineRemoved(itemData.id, row.lineIndex)

                                refreshButtonVisual(itemData.id)
                                handleRefresh(TreeRow.Folder.Lore)

                                event.consume()
                            }
                        }
                    )
                }

                is TreeRow.Folder.LoreSection -> HBox(8.0).apply {
                    fun refreshParentLine() {
                        refreshButtonVisual(itemData.id)
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

                    val sectionSystem = LoreLineEditor(itemData.display, row.lineIndex).section(row.sectionIndex)

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
                                loreTreeUiIdMemory.sectionMoved(itemData.id, row.lineUiId, fromIndex, toIndex)

                                refreshParentLine()

                                event.consume()
                            }
                        },
                        Button("▼").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-add")

                            isDisable = row.sectionIndex >= LoreLineEditor(
                                itemData.display,
                                row.lineIndex
                            ).getSectionSize() - 1

                            onAction = EventHandler { event ->
                                val fromIndex = row.sectionIndex
                                val toIndex = row.sectionIndex + 1

                                sectionSystem.moveTo(toIndex)
                                loreTreeUiIdMemory.sectionMoved(itemData.id, row.lineUiId, fromIndex, toIndex)

                                refreshParentLine()

                                event.consume()
                            }
                        },
                        Button("×").apply {
                            applyTreeInlineButtonSize()

                            styleClass.add("tree-inline-button-remove")

                            isDisable = LoreLineEditor(itemData.display, row.lineIndex).getSectionSize() <= 1

                            onAction = EventHandler { event ->
                                sectionSystem.remove()
                                loreTreeUiIdMemory.sectionRemoved(itemData.id, row.lineUiId, row.sectionIndex)

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
                itemData = itemData,
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
        val currentItemId = currentSelectedItemId ?: return
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

    /**
     * 指定したIDのボタンを選択（アクティブ）状態に切り替える
     */
    private fun selectButtonById(id: String) {
        val target = sidebarButtons[id] ?: return
        val previousButton = selectedButton

        // 1. 選択中のボタンの参照を更新
        selectedButton = target

        // 2. 選択が外れた古いボタンの見た目を再計算（変更があれば緑、なければ通常へ）
        if (previousButton != null) {
            refreshButtonVisual(previousButton.id ?: "")
        }

        // 3. 新しく選択されたボタンの見た目を再計算（青ハイライトへ）
        refreshButtonVisual(id)
    }

    /**
     * 指定したアイテムIDのボタンの見た目を、最新の状態（選択中か、変更ありか）をもとに一元更新する
     */
    private fun refreshButtonVisual(itemId: String) {
        val btn = sidebarButtons[itemId] ?: return

        // プレビュー更新
        previewCanvas?.refreshPreview()

        // 現在の2つの状態をフラグとして取得
        val isSelected = (btn == selectedButton)
        val isModified = (editingDataMap[itemId] != originalDataMap[itemId])

        // 状態の組み合わせによってスタイルを一意に決定する
        btn.style = when {
            // A. 選択中の場合（変更の有無に関わらず、選択ハイライトを最優先）
            isSelected -> "-fx-background-color: #3a86ff; -fx-text-fill: white; -fx-font-weight: bold;"

            // B. 選択中ではないが、変更がある場合（文字色を緑にする）
            isModified -> "-fx-text-fill: #2ECC71; -fx-font-weight: bold;"

            // C. どちらでもない場合（通常の未選択ボタン）
            else -> ""
        }
    }

    /**
     * 手元で変更されたデータ（editing != original）だけをローカルに自動保存する
     */
    private fun executeAutoSave() {
        // 変更があるデータだけをフィルタリング
        val changedData = editingDataMap.filter { (id, data) -> data != originalDataMap[id] }
        if (changedData.isEmpty()) return

        logger.info("【自動保存】未保存の変更を検知しました（${changedData.size} 件）。ローカルキャッシュを更新します。")

        changedData.forEach { (id, currentData) ->
            // 編集中の最新データを保存
            dataService.saveToLocalBackup(id, "editing", currentData)

            // ベースとなったオリジナルを保存
            val originalData = originalDataMap[id]
            if (originalData != null) {
                dataService.saveToLocalBackup(id, "original", originalData)
            }
        }

        main.showTimedTopLabel("${changedData.size} 件の項目を自動バックアップしました。", Color.GREENYELLOW)
    }

    /**
     * 自動保存タイマーを開始する
     */
    private fun startAutoSaveTimer() {
        if (autoSaveTimeline != null) return // 二重起動防止

        autoSaveTimeline = Timeline(
            KeyFrame(Duration.minutes(3.0), { // 3分ごとにチェック
                executeAutoSave()
            })
        ).apply {
            cycleCount = Animation.INDEFINITE
            play()
        }
        logger.info("自動保存タイマーを開始しました（3分間隔）")
    }

    /**
     * 自動保存タイマーを停止する
     */
    private fun stopAutoSaveTimer() {
        autoSaveTimeline?.stop()
        autoSaveTimeline = null
        logger.info("自動保存タイマーを停止しました")
    }
}