package io.github.toumokorosi01.sushiericdataeditor2.editor.main.item

import io.github.toumokorosi01.common.DataRegistry
import io.github.toumokorosi01.common.EffectType
import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.common.Rarity
import io.github.toumokorosi01.common.stats.StatsType
import io.github.toumokorosi01.common.data.core.structure.ArmorTrimData
import io.github.toumokorosi01.common.data.core.structure.ArmorTrimRegistry
import io.github.toumokorosi01.common.data.core.structure.PotionEffectData
import io.github.toumokorosi01.common.data.item.LoreLineEditor
import io.github.toumokorosi01.common.data.item.data.CustomComponentLoreSection
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.item.data.ItemType
import io.github.toumokorosi01.common.data.item.data.ItemType.*
import io.github.toumokorosi01.common.data.item.data.LoreSectionType
import io.github.toumokorosi01.common.data.item.data.PlainTextLoreSection
import io.github.toumokorosi01.common.data.item.data.StatLoreSection
import io.github.toumokorosi01.common.data.item.data.detail.ArmorContent
import io.github.toumokorosi01.common.data.item.data.detail.BowData
import io.github.toumokorosi01.common.data.item.data.detail.CrossbowData
import io.github.toumokorosi01.common.data.item.data.detail.LongSwordData
import io.github.toumokorosi01.common.data.item.data.detail.PotionData
import io.github.toumokorosi01.common.data.item.data.detail.ShieldData
import io.github.toumokorosi01.common.data.item.data.detail.SpearData
import io.github.toumokorosi01.sushiericdataeditor2.app.AppScreen
import io.github.toumokorosi01.sushiericdataeditor2.editor.component.ColorPickerDialog
import io.github.toumokorosi01.sushiericdataeditor2.editor.component.EditorSpinnerFactory
import io.github.toumokorosi01.sushiericdataeditor2.editor.tree.EditorGraphicFactory
import io.github.toumokorosi01.sushiericdataeditor2.editor.main.item.tree.TreeRow
import io.github.toumokorosi01.sushiericdataeditor2.util.NumericSpinnerFactory
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ColorPicker
import javafx.scene.control.ComboBox
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ScrollPane
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.controlsfx.control.ToggleSwitch

/**
 * Itemエディタ専用のEditor行Graphic生成クラス。
 *
 * 共通TreeCellから呼び出され、ItemDataに応じた入力UIを生成する。
 */
class ItemEditorFactory(
    private val itemData: ItemData,
    private val refreshButtonVisual: (String) -> Unit
) : EditorGraphicFactory<TreeRow> {

    private val decorationDisplay = linkedMapOf(
        TextDecoration.OBFUSCATED to "難読化",
        TextDecoration.BOLD to  "太字",
        TextDecoration.STRIKETHROUGH to "取り消し線",
        TextDecoration.UNDERLINED to "下線",
        TextDecoration.ITALIC to "斜体"
    )

    private val textColorDisplay = linkedMapOf<TextColor, String>(
        NamedTextColor.WHITE to "white",
        NamedTextColor.BLACK to "black",
        NamedTextColor.DARK_BLUE to "dark_blue",
        NamedTextColor.DARK_GREEN to "dark_green",
        NamedTextColor.DARK_AQUA to "dark_aqua",
        NamedTextColor.DARK_RED to "dark_red",
        NamedTextColor.DARK_PURPLE to "dark_purple",
        NamedTextColor.GOLD to "gold",
        NamedTextColor.GRAY to "gray",
        NamedTextColor.DARK_GRAY to "dark_gray",
        NamedTextColor.BLUE to "blue",
        NamedTextColor.GREEN to "green",
        NamedTextColor.AQUA to "aqua",
        NamedTextColor.RED to "red",
        NamedTextColor.LIGHT_PURPLE to "light_purple",
        NamedTextColor.YELLOW to "yellow"
    )

    override fun createGraphic(row: TreeRow): Node {
        val editorRow = row as? TreeRow.Editor
            ?: return Label(row.label)

        return VBox(5.0).apply {
            children.add(
                when (editorRow) {
                    is TreeRow.Editor.DisplayName -> HBox(5.0).apply {
                        alignment = Pos.CENTER_LEFT
                        styleClass.add("editor-row-hbox")

                        val errorLabel = Label().apply {
                            style = "-fx-text-fill: -fx-danger-color;"
                        }

                        fun errorView() {
                            val errorMessage = itemData.display.validator().validateDisplayName().getOrNull(0)?.message

                            val isNotNull = errorMessage != null

                            if (isNotNull) {
                                errorLabel.text = errorMessage
                            } else {
                                errorLabel.text = ""
                            }

                            errorLabel.isVisible = isNotNull
                            errorLabel.isManaged = isNotNull
                        }

                        errorView()

                        children.addAll(
                            Label("表示名:").apply {
                                minWidth = Region.USE_PREF_SIZE
                            },
                            TextField(itemData.display.displayName).apply {
                                textProperty().addListener { _, _, n ->
                                    itemData.display.displayName = n
                                    errorView()
                                    refreshButtonVisual(itemData.id)
                                }
                            },
                            errorLabel
                        )
                    }

                    is TreeRow.Editor.Rarity -> HBox(5.0).apply {
                        alignment = Pos.CENTER_LEFT
                        styleClass.add("editor-row-hbox")

                        children.addAll(
                            Label("レアリティ:").apply {
                                minWidth = Region.USE_PREF_SIZE
                            },
                            ComboBox<Rarity>().apply {
                                items.addAll(Rarity.entries)
                                value = itemData.rarity
                                valueProperty().addListener { _, _, n ->
                                    if (n != null) {
                                        itemData.rarity = n
                                        refreshButtonVisual(itemData.id)
                                    }
                                }
                            }
                        )
                    }

                    is TreeRow.Editor.DetailContent -> HBox(5.0).apply {
                        alignment = Pos.CENTER_LEFT
                        styleClass.add("editor-row-hbox")

                        fun contentDisplay(type: ItemType) {
                            val content = itemData.itemDetail.content
                            children.clear()

                            val contentController = VBox(6.0).apply {
                                styleClass.add("editor-row-vbox")

                                val addList: List<Region> = when (type) {
                                    SWORD -> emptyList()

                                    SHORT_SWORD -> emptyList()

                                    LONG_SWORD -> {
                                        content as LongSwordData

                                        listOf(
                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("クールダウン:"),
                                                    EditorSpinnerFactory.doubleSpinner(
                                                        initialValue = content.cooldown,
                                                        min = 0.0,
                                                        max = 999.0,
                                                        step = 0.1,
                                                        decimalPlaces = 1
                                                    ) { value ->
                                                        content.cooldown = value
                                                        refreshButtonVisual(itemData.id)
                                                    }
                                                )
                                            }
                                        )
                                    }

                                    AXE -> emptyList()

                                    BOW -> {
                                        content as BowData

                                        val siNode = HBox(5.0).apply {
                                            alignment = Pos.CENTER_LEFT
                                            styleClass.add("editor-row-hbox")

                                            isVisible = content.short
                                            isManaged = content.short

                                            children.addAll(
                                                Label("連射速度:"),
                                                EditorSpinnerFactory.doubleSpinner(
                                                    initialValue = content.shortInterval,
                                                    min = 0.0,
                                                    max = 999.0,
                                                    step = 0.1,
                                                    decimalPlaces = 1
                                                ) { value ->
                                                    content.shortInterval = value
                                                    refreshButtonVisual(itemData.id)
                                                }
                                            )
                                        }

                                        val angleNode = HBox(5.0).apply {
                                            alignment = Pos.CENTER_LEFT
                                            styleClass.add("editor-row-hbox")

                                            isVisible = content.multi > 0
                                            isManaged = content.multi > 0

                                            children.addAll(
                                                Label("マルチショット角度:"),
                                                EditorSpinnerFactory.doubleSpinner(
                                                    initialValue = content.angle,
                                                    min = 0.0,
                                                    max = 360.0,
                                                    step = 0.1,
                                                    decimalPlaces = 1
                                                ) { value ->
                                                    content.angle = value
                                                    refreshButtonVisual(itemData.id)
                                                }
                                            )
                                        }

                                        listOf(
                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("マルチショット:"),
                                                    EditorSpinnerFactory.intSpinner(
                                                        initialValue = content.multi,
                                                        min = 0,
                                                        max = 999,
                                                        step = 1
                                                    ) { value ->
                                                        content.multi = value

                                                        angleNode.isVisible = value > 0
                                                        angleNode.isManaged = value > 0

                                                        refreshButtonVisual(itemData.id)
                                                    }
                                                )
                                            },

                                            angleNode,

                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("貫通:"),
                                                    EditorSpinnerFactory.intSpinner(
                                                        initialValue = content.pierce,
                                                        min = 0,
                                                        max = 999,
                                                        step = 1
                                                    ) { value ->
                                                        content.pierce = value
                                                        refreshButtonVisual(itemData.id)
                                                    }
                                                )
                                            },

                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("連射:"),
                                                    ToggleSwitch().apply {
                                                        isSelected = content.short

                                                        selectedProperty().addListener { _, _, value ->
                                                            content.short = value

                                                            siNode.isVisible = value
                                                            siNode.isManaged = value

                                                            refreshButtonVisual(itemData.id)
                                                        }
                                                    }
                                                )
                                            },

                                            siNode
                                        )
                                    }

                                    CROSSBOW -> {
                                        content as CrossbowData

                                        listOf(
                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("ダメージ範囲:"),
                                                    EditorSpinnerFactory.doubleSpinner(
                                                        initialValue = content.damageRange,
                                                        min = 0.0,
                                                        max = 999.0,
                                                        step = 0.1,
                                                        decimalPlaces = 1
                                                    ) { value ->
                                                        content.damageRange = value
                                                        refreshButtonVisual(itemData.id)
                                                    }
                                                )
                                            },

                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("連射速度:"),
                                                    EditorSpinnerFactory.doubleSpinner(
                                                        initialValue = content.shortInterval,
                                                        min = 0.0,
                                                        max = 999.0,
                                                        step = 0.1,
                                                        decimalPlaces = 1
                                                    ) { value ->
                                                        content.shortInterval = value
                                                        refreshButtonVisual(itemData.id)
                                                    }
                                                )
                                            }
                                        )
                                    }

                                    SPEAR -> {
                                        content as SpearData

                                        listOf(
                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("クールダウン:"),
                                                    EditorSpinnerFactory.doubleSpinner(
                                                        initialValue = content.cooldown,
                                                        min = 0.0,
                                                        max = 999.0,
                                                        step = 0.1,
                                                        decimalPlaces = 1
                                                    ) { value ->
                                                        content.cooldown = value
                                                        refreshButtonVisual(itemData.id)
                                                    }
                                                )
                                            }
                                        )
                                    }

                                    POTION -> {
                                        content as PotionData

                                        fun showPotionEffectDialog() {
                                            val dialogStage = Stage().apply {
                                                title = "ポーション効果編集"
                                                initModality(Modality.APPLICATION_MODAL)
                                            }

                                            val editingEffects = content.effects
                                                .map { it.deepCopy() }
                                                .toMutableList()

                                            val effectListBox = VBox(6.0).apply {
                                                styleClass.addAll("editor-row-vbox", "potion-effect-list-box")
                                                maxWidth = Double.MAX_VALUE
                                            }

                                            val effectTypeComboBox = ComboBox<EffectType>().apply {
                                                items.addAll(EffectType.entries)
                                                prefWidth = 220.0

                                                cellFactory = Callback {
                                                    object : ListCell<EffectType>() {
                                                        override fun updateItem(item: EffectType?, empty: Boolean) {
                                                            super.updateItem(item, empty)
                                                            text = if (empty || item == null) null else item.name
                                                        }
                                                    }
                                                }

                                                buttonCell = object : ListCell<EffectType>() {
                                                    override fun updateItem(item: EffectType?, empty: Boolean) {
                                                        super.updateItem(item, empty)
                                                        text = if (empty || item == null) null else item.name
                                                    }
                                                }
                                            }

                                            fun refreshEffectTypeItems() {
                                                val usedTypes = editingEffects.map { it.type }.toSet()
                                                val availableTypes = EffectType.entries.filter { it !in usedTypes }

                                                effectTypeComboBox.items.setAll(availableTypes)
                                                effectTypeComboBox.value = availableTypes.firstOrNull()
                                            }

                                            fun rebuildEffectList() {
                                                effectListBox.children.clear()

                                                if (editingEffects.isEmpty()) {
                                                    effectListBox.children.add(
                                                        Label("効果がありません").apply {
                                                            styleClass.add("editor-label")
                                                        }
                                                    )
                                                    return
                                                }

                                                editingEffects.forEach { effect ->
                                                    effectListBox.children.add(
                                                        GridPane().apply {
                                                            styleClass.add("potion-effect-row-grid")
                                                            hgap = 8.0
                                                            vgap = 4.0
                                                            alignment = Pos.CENTER_LEFT
                                                            maxWidth = Double.MAX_VALUE

                                                            val typeLabel = Label(effect.type.name).apply {
                                                                styleClass.add("editor-label-highlight")
                                                                minWidth = 170.0
                                                                prefWidth = 170.0
                                                                maxWidth = 170.0
                                                                isWrapText = false
                                                            }

                                                            val levelLabel = Label("Lv.").apply {
                                                                styleClass.add("editor-label")
                                                                minWidth = Region.USE_PREF_SIZE
                                                                prefWidth = Region.USE_COMPUTED_SIZE
                                                            }

                                                            val levelSpinner = EditorSpinnerFactory.intSpinner(
                                                                initialValue = effect.level,
                                                                min = 0,
                                                                max = 999,
                                                                step = 1,
                                                                prefWidth = 90.0
                                                            ) { value ->
                                                                effect.level = value
                                                            }.apply {
                                                                minWidth = 90.0
                                                                prefWidth = 90.0
                                                                maxWidth = 90.0
                                                            }

                                                            val secondsLabel = Label("秒").apply {
                                                                styleClass.add("editor-label")
                                                                minWidth = Region.USE_PREF_SIZE
                                                                prefWidth = Region.USE_COMPUTED_SIZE
                                                            }

                                                            val secondsSpinner = EditorSpinnerFactory.intSpinner(
                                                                initialValue = (effect.time / 20L).toInt(),
                                                                min = 0,
                                                                max = 999999,
                                                                step = 1,
                                                                prefWidth = 100.0
                                                            ) { seconds ->
                                                                effect.time = seconds.toLong() * 20L
                                                            }.apply {
                                                                minWidth = 100.0
                                                                prefWidth = 100.0
                                                                maxWidth = 100.0
                                                            }

                                                            val deleteButton = Button("削除").apply {
                                                                isFocusTraversable = false
                                                                styleClass.add("btn-danger")

                                                                minWidth = 70.0
                                                                prefWidth = 70.0
                                                                maxWidth = 70.0

                                                                setOnAction {
                                                                    editingEffects.remove(effect)
                                                                    rebuildEffectList()
                                                                    refreshEffectTypeItems()
                                                                }
                                                            }

                                                            add(typeLabel, 0, 0)
                                                            add(levelLabel, 1, 0)
                                                            add(levelSpinner, 2, 0)
                                                            add(secondsLabel, 3, 0)
                                                            add(secondsSpinner, 4, 0)
                                                            add(deleteButton, 5, 0)
                                                        }
                                                    )
                                                }
                                            }

                                            val levelSpinner = EditorSpinnerFactory.intSpinner(
                                                initialValue = 0,
                                                min = 0,
                                                max = 999,
                                                step = 1
                                            ) {}

                                            val timeSpinner = EditorSpinnerFactory.intSpinner(
                                                initialValue = 0,
                                                min = 0,
                                                max = 999999,
                                                step = 1
                                            ) {}

                                            val addButton = Button("追加").apply {
                                                isFocusTraversable = false
                                                styleClass.add("btn-success")

                                                setOnAction {
                                                    val selectedType = effectTypeComboBox.value ?: return@setOnAction

                                                    if (editingEffects.any { it.type == selectedType }) return@setOnAction

                                                    editingEffects.add(
                                                        PotionEffectData(
                                                            type = selectedType,
                                                            level = levelSpinner.value ?: 0,
                                                            time = (timeSpinner.value ?: 0).toLong() * 20L
                                                        )
                                                    )

                                                    refreshEffectTypeItems()
                                                    rebuildEffectList()
                                                }
                                            }

                                            val contentRoot = VBox(12.0).apply {
                                                padding = Insets(15.0)
                                                prefWidth = 680.0
                                                prefHeight = 460.0
                                                maxWidth = Double.MAX_VALUE
                                                maxHeight = Double.MAX_VALUE
                                                styleClass.addAll("editor-row-vbox", "potion-effect-dialog-root")

                                                children.addAll(
                                                    Label("現在の効果:").apply {
                                                        styleClass.add("editor-label-highlight")
                                                    },

                                                    ScrollPane(effectListBox).apply {
                                                        styleClass.add("potion-effect-list-scroll")
                                                        isFitToWidth = true
                                                        prefHeight = 220.0
                                                        maxWidth = Double.MAX_VALUE
                                                    },

                                                    GridPane().apply {
                                                        styleClass.add("potion-effect-form-grid")
                                                        hgap = 10.0
                                                        vgap = 8.0

                                                        add(
                                                            Label("効果:").apply {
                                                                styleClass.add("editor-label")
                                                            },
                                                            0,
                                                            0
                                                        )
                                                        add(effectTypeComboBox, 1, 0)

                                                        add(
                                                            Label("レベル:").apply {
                                                                styleClass.add("editor-label")
                                                            },
                                                            0,
                                                            1
                                                        )
                                                        add(levelSpinner, 1, 1)

                                                        add(
                                                            Label("時間(秒):").apply {
                                                                styleClass.add("editor-label")
                                                            },
                                                            0,
                                                            2
                                                        )
                                                        add(timeSpinner, 1, 2)

                                                        add(addButton, 1, 3)
                                                    },

                                                    HBox(8.0).apply {
                                                        alignment = Pos.CENTER_RIGHT
                                                        styleClass.add("editor-row-hbox")

                                                        children.addAll(
                                                            Button("キャンセル").apply {
                                                                isFocusTraversable = false
                                                                styleClass.add("btn-cancel")

                                                                setOnAction {
                                                                    dialogStage.close()
                                                                }
                                                            },

                                                            Button("OK").apply {
                                                                isFocusTraversable = false
                                                                styleClass.add("btn-cancel")

                                                                setOnAction {
                                                                    content.effects = editingEffects
                                                                        .map { it.deepCopy() }
                                                                        .toMutableList()

                                                                    refreshButtonVisual(itemData.id)
                                                                    dialogStage.close()
                                                                }
                                                            }
                                                        )
                                                    }
                                                )
                                            }

                                            val sceneRoot = StackPane(contentRoot).apply {
                                                styleClass.add("potion-effect-scene-root")
                                                padding = Insets(0.0)
                                            }

                                            rebuildEffectList()
                                            refreshEffectTypeItems()

                                            dialogStage.scene = Scene(sceneRoot, 680.0, 500.0).apply {
                                                fill = Color.web("#18191A")

                                                stylesheets.add(
                                                    ItemEditorFactory::class.java
                                                        .getResource(AppScreen.WIDGETS_ONLY.css)!!
                                                        .toExternalForm()
                                                )
                                            }

                                            dialogStage.showAndWait()
                                        }

                                        listOf(
                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("色:").apply {
                                                        styleClass.add("editor-label")
                                                    },

                                                    ColorPicker(ColorPickerDialog.toFxColor(content.color)).apply {
                                                        valueProperty().addListener { _, _, newColor ->
                                                            if (newColor == null) return@addListener

                                                            content.color = ColorPickerDialog.toHexColor(newColor)
                                                            refreshButtonVisual(itemData.id)
                                                        }
                                                    }
                                                )
                                            },

                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                val effectCountLabel = Label("効果数: ${content.effects.size}").apply {
                                                    styleClass.add("editor-label")
                                                }

                                                children.addAll(
                                                    effectCountLabel,

                                                    Button("効果を編集").apply {
                                                        isFocusTraversable = false

                                                        setOnAction {
                                                            showPotionEffectDialog()
                                                            effectCountLabel.text = "効果数: ${content.effects.size}"
                                                        }
                                                    }
                                                )
                                            }
                                        )
                                    }

                                    SHIELD -> {
                                        content as ShieldData

                                        listOf(
                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("クールダウン:"),
                                                    EditorSpinnerFactory.doubleSpinner(
                                                        initialValue = content.cooldown,
                                                        min = 0.0,
                                                        max = 999.0,
                                                        step = 0.1,
                                                        decimalPlaces = 1
                                                    ) { value ->
                                                        content.cooldown = value
                                                        refreshButtonVisual(itemData.id)
                                                    }
                                                )
                                            },

                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("防御率:"),
                                                    EditorSpinnerFactory.doubleSpinner(
                                                        initialValue = content.defenceRate,
                                                        min = 0.0,
                                                        max = 1.0,
                                                        step = 0.1,
                                                        decimalPlaces = 1
                                                    ) { value ->
                                                        content.defenceRate = value
                                                        refreshButtonVisual(itemData.id)
                                                    }
                                                )
                                            }
                                        )
                                    }

                                    HELMET, CHESTPLATE, LEGGINGS, BOOTS -> {
                                        content as ArmorContent

                                        val colorPicker = ColorPicker(
                                            ColorPickerDialog.toFxColor(content.color ?: HexColor.of("#FFFFFF"))
                                        ).apply {
                                            valueProperty().addListener { _, _, newColor ->
                                                if (newColor == null) return@addListener

                                                content.color = ColorPickerDialog.toHexColor(newColor)
                                                refreshButtonVisual(itemData.id)
                                            }
                                        }

                                        val colorLine = HBox(5.0).apply {
                                            alignment = Pos.CENTER_LEFT
                                            styleClass.add("editor-row-hbox")

                                            isVisible = content.color != null
                                            isManaged = content.color != null

                                            children.addAll(
                                                Label("色:").apply {
                                                    styleClass.add("editor-label")
                                                },

                                                colorPicker
                                            )
                                        }

                                        val patternCombo = ComboBox<ArmorTrimRegistry.Pattern>().apply {
                                            items.addAll(ArmorTrimRegistry.Pattern.entries)

                                            value = content.trimData?.pattern
                                                ?: ArmorTrimRegistry.Pattern.COAST

                                            valueProperty().addListener { _, oldPattern, newPattern ->
                                                if (newPattern == null || newPattern == oldPattern) return@addListener

                                                val trimData = content.trimData ?: ArmorTrimData().also {
                                                    content.trimData = it
                                                }

                                                trimData.pattern = newPattern
                                                refreshButtonVisual(itemData.id)
                                            }
                                        }

                                        val materialCombo = ComboBox<ArmorTrimRegistry.Material>().apply {
                                            items.addAll(ArmorTrimRegistry.Material.entries)

                                            value = content.trimData?.material
                                                ?: ArmorTrimRegistry.Material.IRON

                                            valueProperty().addListener { _, oldMaterial, newMaterial ->
                                                if (newMaterial == null || newMaterial == oldMaterial) return@addListener

                                                val trimData = content.trimData ?: ArmorTrimData().also {
                                                    content.trimData = it
                                                }

                                                trimData.material = newMaterial
                                                refreshButtonVisual(itemData.id)
                                            }
                                        }

                                        val trimLine = VBox(5.0).apply {
                                            styleClass.add("editor-row-vbox")

                                            isVisible = content.trimData != null
                                            isManaged = content.trimData != null

                                            children.addAll(
                                                HBox(5.0).apply {
                                                    alignment = Pos.CENTER_LEFT
                                                    styleClass.add("editor-row-hbox")

                                                    children.addAll(
                                                        Label("模様:").apply {
                                                            styleClass.add("editor-label")
                                                        },

                                                        patternCombo
                                                    )
                                                },

                                                HBox(5.0).apply {
                                                    alignment = Pos.CENTER_LEFT
                                                    styleClass.add("editor-row-hbox")

                                                    children.addAll(
                                                        Label("素材:").apply {
                                                            styleClass.add("editor-label")
                                                        },

                                                        materialCombo
                                                    )
                                                }
                                            )
                                        }

                                        listOf(
                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("着色:").apply {
                                                        styleClass.add("editor-label")
                                                    },

                                                    ToggleSwitch().apply {
                                                        isSelected = content.color != null

                                                        selectedProperty().addListener { _, _, value ->
                                                            if (value) {
                                                                val defaultColor = HexColor.of("#FFFFFF")

                                                                content.color = defaultColor
                                                                colorPicker.value = ColorPickerDialog.toFxColor(defaultColor)

                                                                colorLine.isVisible = true
                                                                colorLine.isManaged = true
                                                            } else {
                                                                content.color = null

                                                                colorLine.isVisible = false
                                                                colorLine.isManaged = false
                                                            }

                                                            refreshButtonVisual(itemData.id)
                                                        }
                                                    }
                                                )
                                            },

                                            colorLine,

                                            HBox(5.0).apply {
                                                alignment = Pos.CENTER_LEFT
                                                styleClass.add("editor-row-hbox")

                                                children.addAll(
                                                    Label("装飾:").apply {
                                                        styleClass.add("editor-label")
                                                    },

                                                    ToggleSwitch().apply {
                                                        isSelected = content.trimData != null

                                                        selectedProperty().addListener { _, _, value ->
                                                            if (value) {
                                                                val defaultTrimData = ArmorTrimData()

                                                                content.trimData = defaultTrimData

                                                                patternCombo.value = defaultTrimData.pattern
                                                                materialCombo.value = defaultTrimData.material

                                                                trimLine.isVisible = true
                                                                trimLine.isManaged = true
                                                            } else {
                                                                content.trimData = null

                                                                trimLine.isVisible = false
                                                                trimLine.isManaged = false
                                                            }

                                                            refreshButtonVisual(itemData.id)
                                                        }
                                                    }
                                                )
                                            },

                                            trimLine
                                        )
                                    }

                                    OTHER -> emptyList()
                                }

                                children.addAll(addList)
                            }

                            children.addAll(
                                VBox(6.0).apply {
                                    styleClass.add("editor-row-vbox")

                                    val allItems = DataRegistry.allItems

                                    val errorLabel = Label().apply {
                                        textFill = Color.RED
                                        isVisible = false
                                        isManaged = false
                                    }

                                    val comboBox = ComboBox<String>().apply {
                                        items.addAll(allItems)

                                        value = if (itemData.itemDetail.vanillaId in allItems) {
                                            itemData.itemDetail.vanillaId
                                        } else {
                                            null
                                        }

                                        valueProperty().addListener { _, _, selected ->
                                            if (selected != null) {
                                                itemData.itemDetail.vanillaId = selected
                                                refreshButtonVisual(itemData.id)

                                                errorLabel.isVisible = false
                                                errorLabel.isManaged = false
                                                errorLabel.text = ""
                                            }
                                        }
                                    }

                                    val searchField = TextField().apply {
                                        promptText = "アイテムIDを検索"

                                        textProperty().addListener { _, _, query ->
                                            val result = DataRegistry.searchItems(query)

                                            if (result.isEmpty()) {
                                                comboBox.items.setAll(allItems)

                                                if (itemData.itemDetail.vanillaId in allItems) {
                                                    comboBox.value = itemData.itemDetail.vanillaId
                                                }

                                                errorLabel.text = "検索に一致するアイテムIDがありません"
                                                errorLabel.isVisible = true
                                                errorLabel.isManaged = true
                                            } else {
                                                comboBox.items.setAll(result)

                                                if (itemData.itemDetail.vanillaId in result) {
                                                    comboBox.value = itemData.itemDetail.vanillaId
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
                                        Label("バニラID:"),
                                        searchField,
                                        comboBox,
                                        errorLabel,
                                        HBox(5.0).apply {
                                            alignment = Pos.CENTER_LEFT
                                            styleClass.add("editor-row-hbox")

                                            children.addAll(
                                                Label("エンチャントオーラ:"),
                                                ToggleSwitch().apply {
                                                    isSelected = itemData.itemDetail.enchantAura

                                                    selectedProperty().addListener { _, _, value ->
                                                        if (value == null) return@addListener
                                                        itemData.itemDetail.enchantAura = value
                                                        refreshButtonVisual(itemData.id)
                                                    }
                                                }
                                            )
                                        },
                                        HBox(5.0).apply {
                                            alignment = Pos.CENTER_LEFT
                                            styleClass.add("editor-row-hbox")

                                            children.addAll(
                                                Label("最大スタック数:"),
                                                NumericSpinnerFactory.intSpinner(
                                                    getter = {
                                                        itemData.itemDetail.maxStackSize
                                                    },
                                                    setter = { value ->
                                                        itemData.itemDetail.maxStackSize = value
                                                        refreshButtonVisual(itemData.id)
                                                    },
                                                    min = 1,
                                                    max = 99,
                                                    step = 1,
                                                    allowNegative = false,
                                                    allowPlus = true
                                                )
                                            )
                                        },
                                        ComboBox<ItemType>().apply {
                                            items.addAll(entries)
                                            value = content.itemType

                                            valueProperty().addListener { _, oldType, newType ->
                                                if (newType == null || newType == oldType) return@addListener

                                                itemData.itemDetail.content = newType.createDefault()
                                                contentDisplay(newType)
                                                refreshButtonVisual(itemData.id)
                                            }
                                        }
                                    )
                                },
                                contentController
                            )
                        }

                        contentDisplay(itemData.itemDetail.itemType)
                    }

                    is TreeRow.Editor.StatsContent -> VBox(8.0).apply {
                        styleClass.add("editor-row-vbox")

                        fun formatStatValue(value: Double): String {
                            return if (value % 1.0 == 0.0) {
                                value.toInt().toString()
                            } else {
                                value.toString()
                            }
                        }

                        fun nonZeroInitial(type: StatsType): Double {
                            val candidate = type.min + 1.0

                            return when {
                                candidate != 0.0 && candidate in type.min..type.max -> {
                                    candidate
                                }

                                type.min != 0.0 -> {
                                    type.min
                                }

                                1.0 in type.min..type.max -> {
                                    1.0
                                }

                                -1.0 in type.min..type.max -> {
                                    -1.0
                                }

                                else -> {
                                    candidate.coerceIn(type.min, type.max)
                                }
                            }
                        }

                        fun fixZero(type: StatsType, value: Double): Double {
                            return if (value == 0.0) {
                                nonZeroInitial(type)
                            } else {
                                value.coerceIn(type.min, type.max)
                            }
                        }

                        fun commitSpinnerValue(spinner: Spinner<Double>) {
                            val converter = spinner.valueFactory.converter
                            spinner.valueFactory.value = converter.fromString(spinner.editor.text)
                        }

                        fun createDoubleSpinner(
                            type: StatsType,
                            initialValue: Double
                        ): Spinner<Double> {
                            return Spinner<Double>().apply {
                                valueFactory = SpinnerValueFactory.DoubleSpinnerValueFactory(
                                    type.min,
                                    type.max,
                                    fixZero(type, initialValue),
                                    1.0
                                )

                                isEditable = true
                                prefWidth = 90.0

                                valueFactory.converter = object : StringConverter<Double>() {
                                    override fun toString(value: Double?): String {
                                        return value?.let { formatStatValue(it) } ?: ""
                                    }

                                    override fun fromString(string: String?): Double {
                                        val parsed = string?.toDoubleOrNull()
                                            ?: valueFactory.value

                                        return fixZero(type, parsed)
                                    }
                                }

                                editor.setOnAction {
                                    valueFactory.value = valueFactory.converter.fromString(editor.text)
                                }

                                focusedProperty().addListener { _, _, focused ->
                                    if (!focused) {
                                        valueFactory.value = valueFactory.converter.fromString(editor.text)
                                    }
                                }
                            }
                        }

                        fun applySmallButtonSize(button: Button) {
                            button.minWidth = 56.0
                            button.prefWidth = 56.0
                            button.maxWidth = 56.0

                            button.minHeight = 26.0
                            button.prefHeight = 26.0
                            button.maxHeight = 26.0

                            button.style = """
                                -fx-padding: 0 8px;
                                -fx-font-size: 12px;
                            """.trimIndent()
                        }

                        fun rebuildStatsList(container: VBox) {
                            container.children.clear()

                            itemData.stats.entries
                                .forEach { (type, value) ->
                                    val spinner = createDoubleSpinner(type, value)

                                    spinner.valueProperty().addListener { _, _, newValue ->
                                        if (newValue != null && newValue != 0.0) {
                                            itemData.stats[type] = newValue
                                            refreshButtonVisual(itemData.id)
                                        }
                                    }

                                    container.children.add(
                                        HBox(8.0).apply {
                                            alignment = Pos.CENTER_LEFT
                                            styleClass.add("editor-row-hbox")

                                            children.addAll(
                                                Label("${type.display}:").apply {
                                                    styleClass.add("editor-label")
                                                    minWidth = 120.0
                                                },

                                                spinner,

                                                Button("削除").apply {
                                                    styleClass.add("btn-danger")
                                                    applySmallButtonSize(this)

                                                    setOnAction {
                                                        itemData.stats.remove(type)
                                                        refreshButtonVisual(itemData.id)
                                                        rebuildStatsList(container)
                                                    }
                                                }
                                            )
                                        }
                                    )
                                }
                        }

                        val statsListBox = VBox(6.0).apply {
                            styleClass.add("editor-row-vbox")
                        }

                        fun showAddStatsDialog() {
                            val typeComboBox = ComboBox<StatsType>().apply {
                                items.addAll(
                                    StatsType.entries.filterNot { it in itemData.stats.keys }
                                )

                                cellFactory = Callback {
                                    object : ListCell<StatsType>() {
                                        override fun updateItem(item: StatsType?, empty: Boolean) {
                                            super.updateItem(item, empty)
                                            text = if (empty || item == null) null else item.display
                                        }
                                    }
                                }

                                buttonCell = object : ListCell<StatsType>() {
                                    override fun updateItem(item: StatsType?, empty: Boolean) {
                                        super.updateItem(item, empty)
                                        text = if (empty || item == null) null else item.display
                                    }
                                }

                                value = items.firstOrNull()
                            }

                            val valueSpinner = Spinner<Double>().apply {
                                isEditable = true
                                prefWidth = 90.0
                            }

                            fun updateSpinnerForType(type: StatsType?) {
                                if (type == null) return

                                valueSpinner.valueFactory = SpinnerValueFactory.DoubleSpinnerValueFactory(
                                    type.min,
                                    type.max,
                                    nonZeroInitial(type),
                                    1.0
                                )

                                valueSpinner.valueFactory.converter = object : StringConverter<Double>() {
                                    override fun toString(value: Double?): String {
                                        return value?.let { formatStatValue(it) } ?: ""
                                    }

                                    override fun fromString(string: String?): Double {
                                        val parsed = string?.toDoubleOrNull()
                                            ?: valueSpinner.valueFactory.value

                                        return fixZero(type, parsed)
                                    }
                                }
                            }

                            updateSpinnerForType(typeComboBox.value)

                            typeComboBox.valueProperty().addListener { _, _, type ->
                                updateSpinnerForType(type)
                            }

                            val errorLabel = Label().apply {
                                textFill = Color.RED
                                isVisible = false
                                isManaged = false
                            }

                            val content = VBox(8.0).apply {
                                styleClass.add("editor-row-vbox")

                                children.addAll(
                                    Label("追加するステータス:").apply {
                                        styleClass.add("editor-label")
                                    },
                                    typeComboBox,
                                    Label("値:").apply {
                                        styleClass.add("editor-label")
                                    },
                                    valueSpinner,
                                    errorLabel
                                )
                            }

                            val dialog = Dialog<Pair<StatsType, Double>>().apply {
                                title = "ステータスを追加"
                                dialogPane.content = content
                                dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

                                dialogPane.stylesheets.add(
                                    ItemEditorFactory::class.java
                                        .getResource(AppScreen.WIDGETS_ONLY.css)!!
                                        .toExternalForm()
                                )

                                dialogPane.style = """
                                    -fx-background-color: -fx-bg-deep;
                                """.trimIndent()

                                setOnShown {
                                    dialogPane.scene?.root?.style = """
                                    -fx-background-color: -fx-bg-deep;
                                    """.trimIndent()
                                }

                                setResultConverter { buttonType ->
                                    if (buttonType != ButtonType.OK) {
                                        null
                                    } else {
                                        val type = typeComboBox.value

                                        if (type == null) {
                                            null
                                        } else {
                                            commitSpinnerValue(valueSpinner)

                                            val value = valueSpinner.value

                                            if (value == null || value == 0.0) {
                                                null
                                            } else {
                                                type to fixZero(type, value)
                                            }
                                        }
                                    }
                                }
                            }

                            valueSpinner.editor.setOnAction {
                                val type = typeComboBox.value
                                val value = valueSpinner.value

                                if (type != null && value != null && value != 0.0) {
                                    itemData.stats[type] = fixZero(type, value)
                                    refreshButtonVisual(itemData.id)
                                    rebuildStatsList(statsListBox)
                                    dialog.close()
                                }
                            }

                            dialog.showAndWait().ifPresent { (type, value) ->
                                itemData.stats[type] = value
                                refreshButtonVisual(itemData.id)
                                rebuildStatsList(statsListBox)
                            }
                        }

                        val addButton = Button("追加").apply {
                            styleClass.add("btn-primary")
                            applySmallButtonSize(this)

                            setOnAction {
                                showAddStatsDialog()
                            }
                        }

                        children.addAll(
                            HBox(8.0).apply {
                                alignment = Pos.CENTER_LEFT
                                styleClass.add("editor-row-hbox")

                                children.addAll(
                                    Label("ステータス:").apply {
                                        styleClass.add("editor-label")
                                    },
                                    addButton
                                )
                            },

                            statsListBox
                        )

                        rebuildStatsList(statsListBox)
                    }

                    is TreeRow.Editor.LoreContent -> VBox(6.0).apply {
                        styleClass.add("editor-row-vbox")

                        val sectionEditor = LoreLineEditor(itemData.display, editorRow.lineIndex).section(editorRow.sectionIndex)

                        fun rebuildContent() {
                            children.clear()

                            val section = sectionEditor.currentSectionOrNull()

                            children.add(
                                HBox(5.0).apply {
                                    alignment = Pos.CENTER_LEFT
                                    styleClass.add("editor-row-hbox")

                                    children.addAll(
                                        Label("テキストタイプ:").apply {
                                            minWidth = Region.USE_PREF_SIZE
                                        },
                                        ComboBox<LoreSectionType>().apply {
                                            items.addAll(LoreSectionType.entries)
                                            value = section?.type

                                            cellFactory = Callback {
                                                object : ListCell<LoreSectionType>() {
                                                    override fun updateItem(item: LoreSectionType?, empty: Boolean) {
                                                        super.updateItem(item, empty)
                                                        text = if (empty || item == null) null else item.display
                                                    }
                                                }
                                            }

                                            buttonCell = object : ListCell<LoreSectionType>() {
                                                override fun updateItem(item: LoreSectionType?, empty: Boolean) {
                                                    super.updateItem(item, empty)
                                                    text = if (empty || item == null) null else item.display
                                                }
                                            }

                                            valueProperty().addListener { _, oldType, newType ->
                                                if (newType == null || newType == oldType) return@addListener

                                                sectionEditor.replaceCurrentSection(newType)

                                                refreshButtonVisual(itemData.id)
                                                rebuildContent()
                                            }
                                        }
                                    )
                                }
                            )

                            when (section) {
                                is PlainTextLoreSection -> children.addAll(
                                    HBox(5.0).apply {
                                        alignment = Pos.CENTER_LEFT
                                        styleClass.add("editor-row-hbox")

                                        children.addAll(
                                            Label("テキスト:").apply {
                                                minWidth = Region.USE_PREF_SIZE
                                            },
                                            TextField(section.text).apply {
                                                HBox.setHgrow(this, Priority.ALWAYS)
                                                maxWidth = Double.MAX_VALUE

                                                textProperty().addListener { _, _, text ->
                                                    if (text == null) return@addListener
                                                    section.text = text
                                                    refreshButtonVisual(itemData.id)
                                                }
                                            }
                                        )
                                    },
                                    HBox(5.0).apply {
                                        alignment = Pos.CENTER_LEFT
                                        styleClass.add("editor-row-hbox")

                                        children.addAll(
                                            Label("シークレット:"),
                                            ToggleSwitch().apply {
                                                isSelected = section.secret

                                                selectedProperty().addListener { _, _, value ->
                                                    if (value == null) return@addListener
                                                    section.secret = value
                                                    refreshButtonVisual(itemData.id)
                                                }
                                            }
                                        )
                                    }
                                )
                                is StatLoreSection -> children.add(
                                    HBox(5.0).apply {
                                        alignment = Pos.CENTER_LEFT
                                        styleClass.add("editor-row-hbox")

                                        children.addAll(
                                            Label("ステータス:"),
                                            ComboBox<StatsType>().apply {
                                                items.addAll(StatsType.entries)

                                                cellFactory = Callback {
                                                    object : ListCell<StatsType>() {
                                                        override fun updateItem(item: StatsType?, empty: Boolean) {
                                                            super.updateItem(item, empty)
                                                            text = if (empty || item == null) null else item.display
                                                        }
                                                    }
                                                }

                                                buttonCell = object : ListCell<StatsType>() {
                                                    override fun updateItem(item: StatsType?, empty: Boolean) {
                                                        super.updateItem(item, empty)
                                                        text = if (empty || item == null) null else item.display
                                                    }
                                                }

                                                value = section.stat
                                                valueProperty().addListener { _, _, stat ->
                                                    if (stat == null) return@addListener
                                                    section.stat = stat

                                                    refreshButtonVisual(itemData.id)
                                                }
                                            }
                                        )
                                    }
                                )
                                is CustomComponentLoreSection -> children.addAll(
                                    HBox(5.0).apply {
                                        alignment = Pos.CENTER_LEFT
                                        styleClass.add("editor-row-hbox")

                                        children.addAll(
                                            Label("テキスト:").apply {
                                                minWidth = Region.USE_PREF_SIZE
                                            },
                                            TextField(section.getText()).apply {
                                                HBox.setHgrow(this, Priority.ALWAYS)
                                                maxWidth = Double.MAX_VALUE

                                                textProperty().addListener { _, _, text ->
                                                    if (text == null) return@addListener
                                                    section.editText(text)
                                                    refreshButtonVisual(itemData.id)
                                                }
                                            }
                                        )
                                    },
                                    HBox(5.0).apply {
                                        alignment = Pos.CENTER_LEFT
                                        styleClass.add("editor-row-hbox")

                                        children.addAll(
                                            GridPane().apply {
                                                hgap = 12.0
                                                vgap = 8.0

                                                alignment = Pos.CENTER_LEFT

                                                decorationDisplay.entries.forEachIndexed { index, entry ->
                                                    val decoration = entry.key
                                                    val label = entry.value

                                                    val splitIndex = 3

                                                    val row = if (index < splitIndex) {
                                                        index
                                                    } else {
                                                        index - splitIndex
                                                    }

                                                    val colBase = if (index < splitIndex) {
                                                        0
                                                    } else {
                                                        2
                                                    }

                                                    add(Label("$label:"), colBase, row)

                                                    add(ToggleSwitch().apply {
                                                        isSelected = section.isDecorationEnabledForItemLore(decoration)

                                                        selectedProperty().addListener { _, _, value ->
                                                            if (value == null) return@addListener
                                                            section.editDecoration(decoration, value)
                                                            refreshButtonVisual(itemData.id)
                                                        }
                                                    }, colBase + 1, row)
                                                }
                                            },
                                            VBox(4.0).apply {
                                                styleClass.add("editor-row-vbox")

                                                fun updateColorButtonStyle(button: Button, hexColor: HexColor) {
                                                    button.minWidth = 80.0
                                                    button.prefWidth = 80.0
                                                    button.maxWidth = 80.0

                                                    button.minHeight = 20.0
                                                    button.prefHeight = 20.0
                                                    button.maxHeight = 20.0

                                                    button.style = """
            -fx-background-color: ${hexColor.value};
            -fx-text-fill: ${if (ColorPickerDialog.isBrightColor(hexColor)) "#000000" else "#ffffff"};
            -fx-background-radius: 6px;
            -fx-padding: 0;
        """.trimIndent()
                                                }

                                                val hexCodeLabel = "HexCode"

                                                val colorComboBox = ComboBox<String>().apply {
                                                    items.addAll(textColorDisplay.values)
                                                    items.add(hexCodeLabel)

                                                    value = textColorDisplay.entries
                                                        .firstOrNull { (color, _) -> color == section.getColor() }
                                                        ?.value
                                                        ?: hexCodeLabel
                                                }

                                                var initialized = false

                                                val initialHexColor = HexColor.orNull(section.getHexColor() ?: "#ffffff")
                                                    ?: HexColor.of("#ffffff")

                                                val hexColorButton = Button(initialHexColor.value).apply {
                                                    isVisible = colorComboBox.value == hexCodeLabel
                                                    isManaged = isVisible

                                                    updateColorButtonStyle(this, initialHexColor)

                                                    setOnAction {
                                                        val currentHexColor = HexColor.orNull(section.getHexColor() ?: "#ffffff")
                                                            ?: HexColor.of("#ffffff")

                                                        val selectedColor = ColorPickerDialog.show(
                                                            initialColor = currentHexColor,
                                                            owner = scene?.window
                                                        ) ?: return@setOnAction

                                                        section.editColor(selectedColor.value)

                                                        text = selectedColor.value
                                                        updateColorButtonStyle(this, selectedColor)

                                                        refreshButtonVisual(itemData.id)
                                                    }
                                                }

                                                colorComboBox.valueProperty().addListener { _, _, selectedLabel ->
                                                    if (selectedLabel == hexCodeLabel) {
                                                        hexColorButton.isVisible = true
                                                        hexColorButton.isManaged = true

                                                        val hexColor = if (initialized) {
                                                            HexColor.of("#ffffff")
                                                        } else {
                                                            HexColor.orNull(section.getHexColor() ?: "#ffffff")
                                                                ?: HexColor.of("#ffffff")
                                                        }

                                                        section.editColor(hexColor.value)

                                                        hexColorButton.text = hexColor.value
                                                        updateColorButtonStyle(hexColorButton, hexColor)

                                                        refreshButtonVisual(itemData.id)
                                                    } else {
                                                        hexColorButton.isVisible = false
                                                        hexColorButton.isManaged = false

                                                        val selectedColor = textColorDisplay.entries
                                                            .firstOrNull { (_, label) -> label == selectedLabel }
                                                            ?.key

                                                        selectedColor?.let {
                                                            section.editColor(it)
                                                            refreshButtonVisual(itemData.id)
                                                        }
                                                    }
                                                }

                                                initialized = true

                                                children.addAll(
                                                    colorComboBox,
                                                    hexColorButton
                                                )
                                            }
                                        )
                                    }
                                )
                                null -> {}
                            }
                        }

                        rebuildContent()
                    }
                    is TreeRow.Editor.Comment -> VBox(6.0).apply {
                        styleClass.add("editor-row-vbox")

                        children.addAll(
                            Label("コメントアウト").apply {
                                styleClass.add("editor-label")
                            },

                            TextArea(itemData.editorMeta.comment.joinToString("\n")).apply {
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
                                    itemData.editorMeta.comment.clear()
                                    itemData.editorMeta.comment.addAll(text.lines())
                                    refreshButtonVisual(itemData.id)
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}