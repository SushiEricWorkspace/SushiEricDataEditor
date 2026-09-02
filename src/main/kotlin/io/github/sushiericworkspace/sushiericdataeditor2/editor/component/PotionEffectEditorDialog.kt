package io.github.sushiericworkspace.sushiericdataeditor2.editor.component

import io.github.sushiericworkspace.common.data.effect.model.EffectLevelSupport
import io.github.sushiericworkspace.common.data.effect.model.EffectTimeSupport
import io.github.sushiericworkspace.common.data.effect.model.MutableLevelLessInstantEffectData
import io.github.sushiericworkspace.common.data.effect.model.MutableLevelLessTimedEffectData
import io.github.sushiericworkspace.common.data.effect.model.MutableLeveledInstantEffectData
import io.github.sushiericworkspace.common.data.effect.model.MutableLeveledTimedEffectData
import io.github.sushiericworkspace.common.data.effect.model.MutablePotionEffectData
import io.github.sushiericworkspace.common.data.effect.model.SushiEricEffectType
import io.github.sushiericworkspace.common.data.effect.model.createMutableEffectData
import io.github.sushiericworkspace.common.data.item.model.mutable.detail.MutablePotionData
import io.github.sushiericworkspace.sushiericdataeditor2.app.AppScreen
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ScrollPane
import javafx.scene.control.Spinner
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.util.Callback

internal enum class EffectEditorField {
    LEVEL,
    TIME
}

internal fun visibleEffectEditorFields(
    hasLevel: Boolean,
    hasTime: Boolean
): Set<EffectEditorField> = buildSet {
    if (hasLevel) add(EffectEditorField.LEVEL)
    if (hasTime) add(EffectEditorField.TIME)
}

internal fun visibleEffectEditorFields(
    type: SushiEricEffectType
): Set<EffectEditorField> = visibleEffectEditorFields(
    hasLevel = type is EffectLevelSupport,
    hasTime = type is EffectTimeSupport
)

internal fun createEditorEffectData(
    type: SushiEricEffectType,
    level: Int,
    time: Long
): MutablePotionEffectData = when (val effect = createMutableEffectData(type)) {
    is MutableLeveledTimedEffectData -> effect.apply {
        this.level = level
        this.time = time
    }

    is MutableLeveledInstantEffectData -> effect.apply {
        this.level = level
    }

    is MutableLevelLessTimedEffectData -> effect.apply {
        this.time = time
    }

    is MutableLevelLessInstantEffectData -> effect
}

object PotionEffectEditorDialog {

    fun show(content: MutablePotionData, onConfirmed: () -> Unit) {
        val stage = Stage().apply {
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
        val effectTypeComboBox = createEffectTypeComboBox()

        fun refreshEffectTypeItems() {
            val usedTypes = editingEffects.map { it.type }.toSet()
            val availableTypes = SushiEricEffectType.entries.filter { it !in usedTypes }
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
                    createEffectRow(
                        effect = effect,
                        onDelete = {
                            editingEffects.remove(effect)
                            rebuildEffectList()
                            refreshEffectTypeItems()
                        }
                    )
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
                    createEditorEffectData(
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
                createAddEffectForm(effectTypeComboBox, levelSpinner, timeSpinner, addButton),
                HBox(
                    Button("キャンセル").apply {
                        isFocusTraversable = false
                        styleClass.add("btn-cancel")
                        setOnAction { stage.close() }
                    },
                    Button("OK").apply {
                        isFocusTraversable = false
                        styleClass.add("btn-primary")
                        setOnAction {
                            content.effects = editingEffects
                                .map { it.deepCopy() }
                                .toMutableList()
                            onConfirmed()
                            stage.close()
                        }
                    }
                ).apply {
                    alignment = Pos.CENTER_RIGHT
                    styleClass.addAll("editor-row-hbox", "dialog-actions")
                }
            )
        }

        rebuildEffectList()
        refreshEffectTypeItems()
        stage.scene = Scene(
            StackPane(contentRoot).apply {
                styleClass.add("potion-effect-scene-root")
                padding = Insets(0.0)
            },
            680.0,
            500.0
        ).apply {
            fill = Color.web("#18191A")
            stylesheets.add(
                PotionEffectEditorDialog::class.java
                    .getResource(AppScreen.WIDGETS_ONLY.css)!!
                    .toExternalForm()
            )
        }
        stage.showAndWait()
    }

    private fun createEffectTypeComboBox(): ComboBox<SushiEricEffectType> {
        return ComboBox<SushiEricEffectType>().apply {
            items.addAll(SushiEricEffectType.entries)
            prefWidth = 220.0
            cellFactory = Callback {
                object : ListCell<SushiEricEffectType>() {
                    override fun updateItem(item: SushiEricEffectType?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (empty || item == null) null else item.name
                    }
                }
            }
            buttonCell = object : ListCell<SushiEricEffectType>() {
                override fun updateItem(item: SushiEricEffectType?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else item.name
                }
            }
        }
    }

    private fun createEffectRow(effect: MutablePotionEffectData, onDelete: () -> Unit): GridPane {
        return GridPane().apply {
            styleClass.add("potion-effect-row-grid")
            hgap = 8.0
            vgap = 4.0
            alignment = Pos.CENTER_LEFT
            maxWidth = Double.MAX_VALUE
            add(
                Label(effect.type.name).apply {
                    styleClass.add("editor-label-highlight")
                    minWidth = 170.0
                    prefWidth = 170.0
                    maxWidth = 170.0
                    isWrapText = false
                },
                0,
                0
            )

            var nextColumn = 1

            fun addLevelEditor(initialValue: Int, onChanged: (Int) -> Unit) {
                add(Label("Lv.").fixedLabel(), nextColumn++, 0)
                add(
                    EditorSpinnerFactory.intSpinner(
                        initialValue = initialValue,
                        min = 0,
                        max = 999,
                        step = 1,
                        prefWidth = 90.0,
                        onChanged = onChanged
                    ).fixedWidth(90.0),
                    nextColumn++,
                    0
                )
            }

            fun addTimeEditor(initialValue: Long, onChanged: (Long) -> Unit) {
                add(Label("秒").fixedLabel(), nextColumn++, 0)
                add(
                    EditorSpinnerFactory.intSpinner(
                        initialValue = (initialValue / 20L).toInt(),
                        min = 0,
                        max = 999999,
                        step = 1,
                        prefWidth = 100.0
                    ) { seconds ->
                        onChanged(seconds.toLong() * 20L)
                    }.fixedWidth(100.0),
                    nextColumn++,
                    0
                )
            }

            when (effect) {
                is MutableLeveledTimedEffectData -> {
                    addLevelEditor(effect.level) { effect.level = it }
                    addTimeEditor(effect.time) { effect.time = it }
                }

                is MutableLeveledInstantEffectData ->
                    addLevelEditor(effect.level) { effect.level = it }

                is MutableLevelLessTimedEffectData ->
                    addTimeEditor(effect.time) { effect.time = it }

                is MutableLevelLessInstantEffectData -> Unit
            }

            add(
                Button("削除").apply {
                    isFocusTraversable = false
                    styleClass.add("btn-danger")
                    minWidth = 70.0
                    prefWidth = 70.0
                    maxWidth = 70.0
                    setOnAction { onDelete() }
                },
                nextColumn,
                0
            )
        }
    }

    private fun createAddEffectForm(
        effectType: ComboBox<SushiEricEffectType>,
        levelSpinner: Spinner<Int>,
        timeSpinner: Spinner<Int>,
        addButton: Button
    ): GridPane {
        val levelLabel = Label("レベル:").editorLabel()
        val timeLabel = Label("時間(秒):").editorLabel()

        fun updateInputFields(type: SushiEricEffectType?) {
            val visibleFields = type
                ?.let(::visibleEffectEditorFields)
                .orEmpty()
            val showLevel = EffectEditorField.LEVEL in visibleFields
            val showTime = EffectEditorField.TIME in visibleFields

            levelLabel.isManaged = showLevel
            levelLabel.isVisible = showLevel
            levelSpinner.isManaged = showLevel
            levelSpinner.isVisible = showLevel
            timeLabel.isManaged = showTime
            timeLabel.isVisible = showTime
            timeSpinner.isManaged = showTime
            timeSpinner.isVisible = showTime
        }

        effectType.valueProperty().addListener { _, oldType, newType ->
            if (oldType != newType) {
                levelSpinner.valueFactory.value = 0
                timeSpinner.valueFactory.value = 0
            }
            updateInputFields(newType)
        }
        updateInputFields(effectType.value)

        return GridPane().apply {
            styleClass.add("potion-effect-form-grid")
            hgap = 10.0
            vgap = 8.0
            add(Label("効果:").editorLabel(), 0, 0)
            add(effectType, 1, 0)
            add(levelLabel, 0, 1)
            add(levelSpinner, 1, 1)
            add(timeLabel, 0, 2)
            add(timeSpinner, 1, 2)
            add(addButton, 1, 3)
        }
    }

    private fun Label.editorLabel(): Label = apply {
        styleClass.add("editor-label")
    }

    private fun Label.fixedLabel(): Label = editorLabel().apply {
        minWidth = Region.USE_PREF_SIZE
        prefWidth = Region.USE_COMPUTED_SIZE
    }

    private fun <T> Spinner<T>.fixedWidth(width: Double): Spinner<T> = apply {
        minWidth = width
        prefWidth = width
        maxWidth = width
    }
}
