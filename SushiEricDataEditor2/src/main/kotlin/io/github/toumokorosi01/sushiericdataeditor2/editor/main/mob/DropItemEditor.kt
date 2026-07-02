package io.github.toumokorosi01.sushiericdataeditor2.editor.main.mob

import io.github.toumokorosi01.common.data.core.structure.DropItemData
import io.github.toumokorosi01.common.data.mob.data.MobData
import io.github.toumokorosi01.sushiericdataeditor2.editor.controller.MainController
import io.github.toumokorosi01.sushiericdataeditor2.util.NumericSpinnerFactory
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import kotlin.math.floor

/**
 * モブのドロップアイテムを編集するためのモーダルエディタ。
 *
 * このエディタでは、対象モブの `dropItems` を参照し、
 * 有効なアイテムID一覧に存在するドロップアイテムだけを編集対象として扱う。
 *
 * 画面上部にはアイテムID検索用の入力欄と追加用ComboBoxを表示し、
 * 画面下部には現在設定されているドロップアイテム一覧を表示する。
 *
 * すでに追加済みのアイテムIDはComboBox上で黄色表示されるが、
 * 重複追加自体は許可する。
 *
 * @property selectData 編集対象のモブデータ。
 * @property main モーダル表示や親Stage取得に使用するメインコントローラー。
 * @property refreshButtonVisual モブデータの変更状態をサイドバー表示へ反映する処理。
 * @property itemIds 追加候補として表示する有効なアイテムID一覧。
 */
class DropItemEditor(
    private val selectData: MobData,
    private val main: MainController,
    private val refreshButtonVisual: (String) -> Unit,
    private val itemIds: List<String>
) {
    /**
     * 編集対象のドロップアイテム一覧。
     *
     * `selectData.dropItems` の参照をそのまま保持するため、
     * このリストへの追加、削除は `selectData.dropItems` にも反映される。
     *
     * 初期化時に、`itemIds` に存在しない無効なドロップアイテムは除外する。
     */
    private val dropItems: MutableList<DropItemData> =
        selectData.dropItems.apply { removeAll { it.id !in itemIds } }

    /**
     * ドロップアイテム一覧を配置するVBox。
     *
     * `ScrollPane` の中身として使用し、`refreshDropItemList` で内容を再構築する。
     */
    private val contentBox = VBox(0.0).apply {
        maxWidth = Double.MAX_VALUE
        maxHeight = Region.USE_PREF_SIZE
    }

    /**
     * ドロップアイテム一覧をスクロール表示する領域。
     *
     * 横幅は親に合わせ、縦方向は内容が増えた場合にスクロールできるようにする。
     */
    private val itemArea = ScrollPane().apply {
        isFitToWidth = true
        isFitToHeight = false

        hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED

        minWidth = 0.0
        minHeight = 0.0
        maxWidth = Double.MAX_VALUE
        maxHeight = Double.MAX_VALUE

        content = contentBox
    }

    /** 検索欄、ComboBoxに使用する横幅。 */
    private val inputAreaWidth = 190.0

    /**
     * 追加対象のアイテムIDを選択するComboBox。
     *
     * すでに `dropItems` に含まれているIDは黄色表示する。
     * `controlArea` の生成時に使用するため、`controlArea` より先に初期化する。
     */
    private val itemComboBox = ComboBox<String>().apply {
        items.addAll(itemIds)

        cellFactory = javafx.util.Callback {
            createItemComboBoxCell()
        }

        buttonCell = createItemComboBoxCell()

        value = itemIds.firstOrNull()

        minWidth = inputAreaWidth
        prefWidth = inputAreaWidth
        maxWidth = inputAreaWidth
    }

    /**
     * アイテム検索欄、ComboBox、追加ボタンを配置する操作エリア。
     */
    private val controlArea = createControlArea()

    /**
     * ドロップアイテム編集用のモーダル画面を開く。
     *
     * 画面構築後、現在の `dropItems` をもとに一覧を再描画し、
     * 親ウィンドウに対するモーダルとして表示する。
     */
    fun openDropItemEditor() {
        val root = VBox(0.0).apply {
            children.addAll(
                controlArea,
                itemArea
            )

            VBox.setVgrow(itemArea, Priority.ALWAYS)
        }

        refreshDropItemList()

        val modalStage = Stage().apply {
            title = "ドロップアイテム"

            initOwner(main.currentStage)
            initModality(Modality.WINDOW_MODAL)

            scene = Scene(root, 355.0, 500.0)
        }

        modalStage.scene.stylesheets.add(
            MobEditorLogic::class.java
                .getResource("/css/editor/mob/drop-item-editor.css")!!
                .toExternalForm()
        )

        modalStage.showAndWait()
    }

    /**
     * 現在の `dropItems` をもとに、ドロップアイテム一覧を再構築する。
     *
     * 追加や削除が行われた後に呼び出すことで、
     * 画面上の一覧表示を最新状態に更新する。
     */
    private fun refreshDropItemList() {
        contentBox.children.clear()
        dropItems.forEach { dropItemData ->
            contentBox.children.addAll(
                dropItemHBox(dropItemData),
                Region().apply {
                    minHeight = 1.0
                    prefHeight = 1.0
                    maxHeight = 1.0
                    maxWidth = Double.MAX_VALUE

                    style = "-fx-background-color: -fx-line-color;"
                }
            )
        }
    }

    /**
     * 1件分のドロップアイテム表示行を生成する。
     *
     * アイテムID、試行回数、成功確率、削除ボタンを表示する。
     * 削除ボタンが押された場合は対象データを `dropItems` から削除し、
     * 一覧表示とComboBoxの色表示を更新する。
     *
     * @param itemData 表示対象のドロップアイテムデータ。
     * @return 1件分の表示行Node。
     */
    private fun dropItemHBox(itemData: DropItemData): Node {
        return HBox(5.0).apply {
            alignment = Pos.CENTER_LEFT

            padding = Insets(10.0)

            val expectedLabel = Label().apply {
                style = "-fx-text-fill: #FFD54F;"
            }

            fun updateExpected() {
                val raw = itemData.expectedValue() * 100
                val value = floor(raw * 1000.0) / 1000.0

                expectedLabel.text = "期待値: $value%"
            }

            updateExpected()

            children.addAll(
                Label(itemData.id).apply {
                    style = "-fx-font-size: 16px; -fx-font-weight: bold;"
                },
                Region().apply {
                    prefWidth = 10.0
                },
                VBox(5.0).apply {
                    children.addAll(
                        HBox(5.0).apply {
                            alignment = Pos.CENTER_LEFT

                            children.addAll(
                                Label("試行回数:"),
                                NumericSpinnerFactory.intSpinner(
                                    getter = { itemData.n },
                                    setter = { value ->
                                        itemData.n = value
                                        updateExpected()
                                        refreshButtonVisual(selectData.id)
                                    },
                                    min = 1,
                                    allowNegative = false,
                                    allowPlus = true,
                                    width = 90.0
                                )
                            )
                        },
                        HBox(5.0).apply {
                            alignment = Pos.CENTER_LEFT

                            children.addAll(
                                Label("成功確率:"),
                                NumericSpinnerFactory.doubleSpinner(
                                    getter = { itemData.p },
                                    setter = { value ->
                                        itemData.p = value
                                        updateExpected()
                                        refreshButtonVisual(selectData.id)
                                    },
                                    max = 1.0,
                                    allowNegative = false,
                                    allowPlus = true,
                                    width = 90.0
                                )
                            )
                        }
                    )
                },
                VBox(5.0).apply {
                    children.addAll(
                        expectedLabel,
                        Button("削除").apply {

                            style = "-fx-background-color: -fx-danger-color;"

                            setOnAction {
                                val index = dropItems.indexOfFirst { it === itemData }

                                if (index >= 0) {
                                    dropItems.removeAt(index)
                                } else {
                                    dropItems.remove(itemData)
                                }

                                notifyDropItemsChanged()
                            }
                        }
                    )
                },

            )
        }
    }

    /**
     * 指定したアイテムIDが、すでにドロップアイテムとして追加されているかを判定する。
     *
     * ComboBoxのセル表示で、追加済みIDを黄色表示するために使用する。
     *
     * @param itemId 判定するアイテムID。
     * @return 追加済みの場合は `true`、未追加の場合は `false`。
     */
    private fun isAlreadyAdded(itemId: String): Boolean {
        return dropItems.any { it.id == itemId }
    }

    /**
     * 画面上部の操作エリアを生成する。
     *
     * アイテムID検索欄、アイテム選択ComboBox、検索エラー表示、
     * 追加ボタンをまとめて配置する。
     *
     * 検索欄に入力された文字列で `itemIds` を絞り込み、
     * 一致するIDがない場合はエラーメッセージを表示する。
     *
     * @return 操作エリアのHBox。
     */
    private fun createControlArea(): VBox {
        val errorLabel = Label().apply {
            textFill = Color.RED
            isVisible = false
            isManaged = false
        }

        // 追加前の一時入力データ
        val resultDropData = DropItemData()

        val searchField = TextField().apply {
            promptText = "アイテムIDを検索"

            minWidth = inputAreaWidth
            prefWidth = inputAreaWidth
            maxWidth = inputAreaWidth

            textProperty().addListener { _, _, query ->
                val result = if (query.isBlank()) {
                    itemIds
                } else {
                    itemIds.filter { it.contains(query, ignoreCase = true) }
                }

                if (result.isEmpty()) {
                    itemComboBox.items.setAll(itemIds)

                    errorLabel.text = "検索に一致するアイテムIDがありません"
                    errorLabel.isVisible = true
                    errorLabel.isManaged = true
                } else {
                    itemComboBox.items.setAll(result)
                    itemComboBox.value = result.firstOrNull()

                    errorLabel.isVisible = false
                    errorLabel.isManaged = false
                    errorLabel.text = ""
                }

                refreshItemComboBoxStyle()
            }
        }

        val nSpinner = NumericSpinnerFactory.intSpinner(
            getter = { resultDropData.n },
            setter = { value ->
                resultDropData.n = value
            },
            min = 1,
            allowNegative = false,
            allowPlus = true,
            width = 90.0
        )

        val pSpinner = NumericSpinnerFactory.doubleSpinner(
            getter = { resultDropData.p },
            setter = { value ->
                resultDropData.p = value
            },
            min = 0.0,
            max = 1.0,
            step = 0.01,
            allowNegative = false,
            allowPlus = true,
            width = 90.0
        )

        val addButton = Button("追加").apply {
            setOnAction {
                val selectedId = itemComboBox.value ?: return@setOnAction

                dropItems.add(
                    DropItemData(
                        id = selectedId,
                        n = resultDropData.n,
                        p = resultDropData.p
                    )
                )

                notifyDropItemsChanged()
            }
        }

        return VBox(5.0).apply {
            padding = Insets(5.0)

            styleClass.add("bottom-border")

            children.addAll(
                HBox(5.0).apply {
                    alignment = Pos.CENTER_LEFT

                    children.addAll(
                        VBox(5.0).apply {

                            minWidth = inputAreaWidth
                            prefWidth = inputAreaWidth
                            maxWidth = inputAreaWidth

                            children.addAll(
                                searchField,
                                itemComboBox,
                                errorLabel
                            )
                        },
                        VBox(5.0).apply {
                            children.addAll(
                                HBox(5.0).apply {
                                    alignment = Pos.CENTER_LEFT

                                    children.addAll(
                                        Label("試行回数:"),
                                        nSpinner
                                    )
                                },
                                HBox(5.0).apply {
                                    alignment = Pos.CENTER_LEFT

                                    children.addAll(
                                        Label("成功確率:"),
                                        pSpinner
                                    )
                                }
                            )
                        }
                    )
                },
                addButton
            )
        }
    }

    /**
     * アイテム選択ComboBox用のセルを生成する。
     *
     * セルの表示内容と色は `applyItemCellStyle` に委譲する。
     *
     * @return アイテムID表示用のListCell。
     */
    private fun createItemComboBoxCell(): ListCell<String> {
        return object : ListCell<String>() {
            override fun updateItem(item: String?, empty: Boolean) {
                super.updateItem(item, empty)
                applyItemCellStyle(this, item, empty)
            }
        }
    }

    /**
     * ComboBox内のアイテムIDセルに表示テキストとスタイルを適用する。
     *
     * `dropItems` にすでに含まれているIDは黄色かつ太字で表示し、
     * 未追加のIDは通常表示に戻す。
     *
     * `ListCell` は再利用されるため、空セルや未追加セルでは必ず表示内容とスタイルをリセットする。
     *
     * @param cell スタイルを適用するセル。
     * @param item 表示対象のアイテムID。
     * @param empty 空セルかどうか。
     */
    private fun applyItemCellStyle(
        cell: ListCell<String>,
        item: String?,
        empty: Boolean
    ) {
        if (empty || item == null) {
            cell.text = null
            cell.style = ""
            return
        }

        cell.text = item

        cell.style = if (isAlreadyAdded(item)) {
            "-fx-text-fill: #FFD54F; -fx-font-weight: bold;"
        } else {
            ""
        }
    }

    /**
     * アイテム選択ComboBoxの表示スタイルを再評価する。
     *
     * 追加や削除によって `dropItems` の内容が変わった後、
     * 選択中セルとドロップダウン内セルの色表示を最新状態に更新する。
     */
    private fun refreshItemComboBoxStyle() {
        val selected = itemComboBox.value
        val currentItems = itemComboBox.items.toList()

        itemComboBox.buttonCell = createItemComboBoxCell()
        itemComboBox.items.setAll(currentItems)

        itemComboBox.value = if (selected in currentItems) {
            selected
        } else {
            currentItems.firstOrNull()
        }
    }

    /**
     * ドロップアイテム変更後の共通更新処理。
     *
     * サイドバー上の変更状態表示、ドロップアイテム一覧表示、
     * ComboBoxの追加済み色表示をまとめて更新する。
     */
    private fun notifyDropItemsChanged() {
        refreshButtonVisual(selectData.id)
        refreshDropItemList()
        refreshItemComboBoxStyle()
    }
}