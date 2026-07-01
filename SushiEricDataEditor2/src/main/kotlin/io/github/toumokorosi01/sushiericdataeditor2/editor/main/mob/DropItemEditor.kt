package io.github.toumokorosi01.sushiericdataeditor2.editor.main.mob

import io.github.toumokorosi01.common.data.core.structure.DropItemData
import io.github.toumokorosi01.common.data.mob.data.MobData
import io.github.toumokorosi01.sushiericdataeditor2.editor.controller.MainController
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

class DropItemEditor(
    private val selectData: MobData,
    private val main: MainController,
    private val refreshButtonVisual: (String) -> Unit,
    private val itemIds: List<String>
) {
    private val dropItems: MutableList<DropItemData> =
        selectData.dropItems.apply { removeAll { it.id !in itemIds } }

    private val contentBox = VBox(5.0).apply {
        padding = Insets(12.0)

        maxWidth = Double.MAX_VALUE
        maxHeight = Region.USE_PREF_SIZE
    }

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

    private val controlArea = createControlArea()

    fun openDropItemEditor() {
        val root = VBox(8.0).apply {

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

            scene = Scene(root, 300.0, 500.0)
        }

        modalStage.scene.stylesheets.add(
            MobEditorLogic::class.java
                .getResource("/css/editor/mob/drop-item-editor.css")!!
                .toExternalForm()
        )

        modalStage.showAndWait()
    }

    private fun refreshDropItemList() {
        contentBox.children.clear()
        dropItems.forEach { dropItemData ->
            contentBox.children.add(dropItemHBox(dropItemData))
        }
    }

    private fun dropItemHBox(itemData: DropItemData): Node {
        return HBox(5.0).apply {
            children.addAll(
                Label(itemData.id),
                VBox(5.0).apply {
                    children.addAll(
                        Label("試行回数: ${itemData.n}"),
                        Label("成功確率: ${itemData.p}")
                    )
                }
            )
        }
    }

    private fun isAlreadyAdded(itemId: String): Boolean {
        return dropItems.any { it.id == itemId }
    }

    private fun createControlArea(): HBox {
        val errorLabel = Label().apply {
            textFill = Color.RED
            isVisible = false
            isManaged = false
        }

        fun applyItemCellStyle(
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

        val comboBox = ComboBox<String>().apply {
            items.addAll(itemIds)

            cellFactory = javafx.util.Callback {
                object : ListCell<String>() {
                    override fun updateItem(item: String?, empty: Boolean) {
                        super.updateItem(item, empty)
                        applyItemCellStyle(this, item, empty)
                    }
                }
            }

            buttonCell = object : ListCell<String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    applyItemCellStyle(this, item, empty)
                }
            }

            value = itemIds.firstOrNull()

            minWidth = 0.0

            maxWidth = Double.MAX_VALUE
        }

        val searchField = TextField().apply {
            promptText = "アイテムIDを検索"

            textProperty().addListener { _, _, query ->
                val result = if (query.isBlank()) {
                    itemIds
                } else {
                    itemIds.filter { it.contains(query, ignoreCase = true) }
                }

                if (result.isEmpty()) {
                    comboBox.items.setAll(itemIds)

                    errorLabel.text = "検索に一致するアイテムID\nがありません"
                    errorLabel.isVisible = true
                    errorLabel.isManaged = true
                } else {
                    comboBox.items.setAll(result)
                    comboBox.value = result.firstOrNull()

                    errorLabel.isVisible = false
                    errorLabel.isManaged = false
                    errorLabel.text = ""
                }

                comboBox.requestLayout()
            }
        }

        val addButton = Button("追加").apply {
            setOnAction {
                val selectedId = comboBox.value ?: return@setOnAction

                dropItems.add(
                    DropItemData(
                        id = selectedId
                    )
                )

                refreshButtonVisual(selectData.id)
                refreshDropItemList()

                comboBox.requestLayout()
            }
        }

        return HBox(5.0).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(5.0)

            styleClass.add("bottom-border")

            children.addAll(
                VBox(5.0).apply {
                    children.addAll(
                        searchField,
                        comboBox,
                        errorLabel
                    )
                },
                addButton
            )
        }
    }
}