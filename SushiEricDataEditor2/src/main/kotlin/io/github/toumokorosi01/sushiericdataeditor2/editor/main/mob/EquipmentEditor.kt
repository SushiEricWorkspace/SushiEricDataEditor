package io.github.toumokorosi01.sushiericdataeditor2.editor.main.mob

import io.github.toumokorosi01.common.DataRegistry
import io.github.toumokorosi01.common.HexColor
import io.github.toumokorosi01.common.data.core.structure.ArmorTrimData
import io.github.toumokorosi01.common.data.core.structure.ArmorTrimRegistry
import io.github.toumokorosi01.common.data.mob.data.EntityArmorData
import io.github.toumokorosi01.common.data.mob.data.EntityEquipmentData
import io.github.toumokorosi01.common.data.mob.data.EntityHoldData
import io.github.toumokorosi01.common.data.mob.data.MobData
import io.github.toumokorosi01.sushiericdataeditor2.editor.component.ColorPickerDialog
import io.github.toumokorosi01.sushiericdataeditor2.editor.controller.MainController
import io.github.toumokorosi01.sushiericdataeditor2.ui.shortcut.EditorShortcut
import io.github.toumokorosi01.sushiericdataeditor2.ui.shortcut.ShortcutManager
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import org.controlsfx.control.ToggleSwitch
import javafx.util.StringConverter

/**
 * モブの装備情報を編集するためのモーダルエディタ。
 *
 * このエディタでは、対象モブの `entityEquipment` を直接編集する。
 * 各装備スロットの有効化、バニラID、エンチャントオーラ、防具色、装飾などの変更は、
 * 現在保持している `selectData` に即時反映される。
 *
 * モーダル内で保存ショートカットを実行した場合、
 * 親エディタ側では通常通り保存処理と画面再構築が行われる。
 * その結果、親側の `MobData` インスタンスが差し替わる可能性があるため、
 * 保存成功後は `currentDataProvider` から最新の `MobData` を取得し直し、
 * モーダル側も最新のデータ参照へ更新する。
 *
 * @property selectData 現在このモーダルが編集対象として扱っているモブデータ。
 * @property main モーダル表示や親Stage取得に使用するメインコントローラー。
 * @property refreshButtonVisual モブデータの変更状態をサイドバー表示へ反映する処理。
 * @property onSave サーバーへの保存処理。保存に成功した場合は `true` を返す。
 * @property currentDataProvider 指定IDに対応する最新の編集中 `MobData` を取得する処理。
 */
class EquipmentEditor(
    private var selectData: MobData,
    private val main: MainController,
    private val refreshButtonVisual: (String) -> Unit,
    private val onSave: (String?) -> Boolean,
    private val currentDataProvider: (String) -> MobData?
) {
    /**
     * 編集対象モブのID。
     *
     * 保存後に `selectData` の参照を取り直す場合でも、
     * 同じモブデータを取得できるように保持しておく。
     */
    private val dataId = selectData.id

    /**
     * 現在の `selectData` が持つ装備データ。
     *
     * 固定された装備データ参照を保持せず、
     * 常に現在の `selectData.entityData.entityEquipment` を返す。
     *
     * これにより、保存後に `selectData` を最新インスタンスへ差し替えた場合でも、
     * 古い装備データを編集し続けることを防ぐ。
     */
    private val equipmentData
        get() = selectData.entityData.entityEquipment

    /**
     * 装備スロット一覧を表示するサイドバー。
     *
     * `refreshSidebar` によって現在の `equipmentData` をもとに再構築する。
     * 保存後に `selectData` が差し替わった場合も、この領域を作り直すことで
     * スロットの有効状態表示を最新データに合わせる。
     */
    private val sidebarArea = VBox(0.0).apply {
        minWidth = 160.0
        prefWidth = 160.0
        maxWidth = 160.0

        maxHeight = Double.MAX_VALUE
    }

    /**
     * 選択中スロットの編集内容を表示するスクロール領域。
     *
     * スロット切り替えや保存後のデータ再取得時に、
     * `refreshMainContent` によって中身を再構築する。
     */
    private val mainContentArea = ScrollPane().apply {
        isFitToWidth = true
        isFitToHeight = false

        hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED

        maxWidth = Double.MAX_VALUE
        maxHeight = Double.MAX_VALUE
    }

    /**
     * 現在選択状態として表示しているスロットボタン。
     *
     * スロット選択時に選択スタイルを付け替えるために使用する。
     * サイドバー再構築時には古いボタン参照を破棄する。
     */
    private var selectedSlotButton: Button? = null

    /**
     * 現在メイン編集領域に表示している装備スロット。
     */
    private var selectedSlot: EquipmentSlot = EquipmentSlot.Head

    /**
     * 装備編集用のモーダル画面を開く。
     *
     * サイドバーとメイン編集領域を構築し、
     * 親ウィンドウに対するモーダルとして表示する。
     *
     * 保存ショートカットが実行された場合は通常の保存処理を呼び出し、
     * 保存成功後に親エディタ側の最新 `MobData` を取得し直して、
     * モーダル側の表示も最新データで再構築する。
     */
    fun openEquipmentEditor() {
        val root = HBox(8.0).apply {
            isFillHeight = true

            children.addAll(
                sidebarArea,
                mainContentArea
            )

            HBox.setHgrow(mainContentArea, Priority.ALWAYS)
        }

        refreshSidebar()
        refreshMainContent()

        val modalStage = Stage().apply {
            title = "装備設定"

            initOwner(main.currentStage)
            initModality(Modality.WINDOW_MODAL)

            scene = Scene(root, 500.0, 350.0)
        }

        modalStage.scene.stylesheets.add(
            MobEditorLogic::class.java
                .getResource("/css/editor/mob/equipment-editor.css")!!
                .toExternalForm()
        )

        ShortcutManager.register(
            scene = modalStage.scene,
            shortcut = EditorShortcut.SAVE
        ) {
            val saved = onSave(dataId)

            if (saved) {
                reloadSelectData()
            }
        }

        modalStage.setOnHidden {
            ShortcutManager.unregisterAll(modalStage.scene)
        }

        modalStage.showAndWait()
    }

    /**
     * 親エディタ側が保持している最新の `MobData` を取得し直す。
     *
     * モーダル内で保存ショートカットを実行すると、
     * 親エディタ側では通常通り保存処理と画面再構築が行われる。
     * その結果、親側の編集中データが新しい `MobData` インスタンスに差し替わるため、
     * モーダル側も最新の参照へ更新する必要がある。
     *
     * 最新データを取得できた場合は、編集対象を差し替えたうえで、
     * サイドバー、メイン編集領域、変更状態表示を更新する。
     */
    private fun reloadSelectData() {
        val latestData = currentDataProvider(dataId) ?: return

        selectData = latestData

        refreshSidebar()
        refreshMainContent()
        refreshButtonVisual(dataId)
    }

    /**
     * 装備スロット一覧のサイドバーを再構築する。
     *
     * 各スロットの装備有無は、現在の `equipmentData` をもとに判定する。
     * 保存後に `selectData` が差し替わった場合も、このメソッドを呼ぶことで
     * サイドバー表示を最新データに合わせ直す。
     */
    private fun refreshSidebar() {
        selectedSlotButton = null

        sidebarArea.children.clear()

        sidebarArea.children.add(createSeparatorLine())

        EquipmentSlot.entries.forEach { slot ->
            sidebarArea.children.addAll(
                createSlotButton(slot),
                createSeparatorLine()
            )
        }

        val filler = Region().apply {
            minHeight = 0.0
            maxHeight = Double.MAX_VALUE
            maxWidth = Double.MAX_VALUE
            style = "-fx-background-color: -fx-bg-shallow;"
        }

        sidebarArea.children.add(filler)
        VBox.setVgrow(filler, Priority.ALWAYS)
    }

    /**
     * 指定した装備スロットに対応するサイドバーボタンを生成する。
     *
     * ボタン内にはスロット名と有効化用のトグルスイッチを配置する。
     * トグル状態を変更した場合は、現在の `equipmentData` に有効状態を反映し、
     * 選択中スロットであればメイン編集領域も更新する。
     *
     * @param slot ボタンを生成する装備スロット。
     * @return 装備スロット選択用のボタン。
     */
    private fun createSlotButton(slot: EquipmentSlot): Button {
        lateinit var button: Button

        button = createToggleButton(
            title = slot.displayName,
            initialSelected = slot.isEquipped(equipmentData),
            onSwitchChanged = { isSelected ->
                slot.setEnabled(equipmentData, isSelected)

                if (selectedSlot == slot) {
                    refreshMainContent()
                }

                refreshButtonVisual(dataId)
            },
            onButtonAction = {
                selectSlot(slot, button)
            }
        )

        button.styleClass.add("equipment-slot-button")

        if (selectedSlot == slot) {
            selectedSlotButton = button
            button.styleClass.add("selected-slot-button")
        }

        return button
    }

    /**
     * メイン編集領域に表示する装備スロットを切り替える。
     *
     * 以前選択されていたボタンから選択スタイルを外し、
     * 新しく選択されたボタンに選択スタイルを付与する。
     * その後、選択スロットに応じてメイン編集領域を再構築する。
     *
     * @param slot 新しく選択する装備スロット。
     * @param button 選択されたスロットボタン。
     */
    private fun selectSlot(
        slot: EquipmentSlot,
        button: Button
    ) {
        selectedSlotButton?.styleClass?.remove("selected-slot-button")

        selectedSlot = slot
        selectedSlotButton = button

        if (!button.styleClass.contains("selected-slot-button")) {
            button.styleClass.add("selected-slot-button")
        }

        refreshMainContent()
    }

    /**
     * 選択中スロットに対応するメイン編集領域を再構築する。
     *
     * 選択中スロットが未装備の場合は未装備表示を行い、
     * 装備済みの場合は防具用または手持ち用の編集UIを生成する。
     */
    private fun refreshMainContent() {
        val content = VBox(5.0).apply {
            padding = Insets(12.0)

            maxWidth = Double.MAX_VALUE
            maxHeight = Double.MAX_VALUE

            children.setAll(
                if (!selectedSlot.isEquipped(equipmentData)) {
                    createEmptyEquipmentView(selectedSlot)
                } else {
                    createEditorForSelectedSlot()
                }
            )
        }

        mainContentArea.content = content
    }

    /**
     * 現在選択中の装備スロットに対応する編集UIを生成する。
     *
     * 防具スロットの場合は `createArmorEditor`、
     * 手持ちスロットの場合は `createHoldEditor` を使用する。
     *
     * @return 選択中スロットの編集UI。
     */
    private fun createEditorForSelectedSlot(): Node {
        return when (selectedSlot) {
            EquipmentSlot.Head -> createArmorEditor(
                getter = { equipmentData.head },
                setter = { value -> equipmentData.head = value }
            )

            EquipmentSlot.Chest -> createArmorEditor(
                getter = { equipmentData.chest },
                setter = { value -> equipmentData.chest = value }
            )

            EquipmentSlot.Legs -> createArmorEditor(
                getter = { equipmentData.legs },
                setter = { value -> equipmentData.legs = value }
            )

            EquipmentSlot.Feet -> createArmorEditor(
                getter = { equipmentData.feet },
                setter = { value -> equipmentData.feet = value }
            )

            EquipmentSlot.MainHand -> createHoldEditor(
                getter = { equipmentData.mainHand },
                setter = { value -> equipmentData.mainHand = value }
            )

            EquipmentSlot.OffHand -> createHoldEditor(
                getter = { equipmentData.offHand },
                setter = { value -> equipmentData.offHand = value }
            )
        }
    }

    /**
     * 指定した装備スロットが未装備であることを表示するUIを生成する。
     *
     * @param slot 未装備表示を行う装備スロット。
     * @return 未装備メッセージを表示するNode。
     */
    private fun createEmptyEquipmentView(slot: EquipmentSlot): Node {
        return StackPane(
            Label("${slot.displayName}は未装備です").apply {
                style = "-fx-font-size: 18px;"
            }
        ).apply {
            minHeight = 160.0
            maxWidth = Double.MAX_VALUE
            maxHeight = Double.MAX_VALUE
        }
    }

    /**
     * スロット選択用のトグル付きボタンを生成する。
     *
     * ボタン本体のクリックでは `onButtonAction` を実行し、
     * 内部のトグルスイッチ変更時には `onSwitchChanged` を実行する。
     *
     * @param title ボタンに表示するスロット名。
     * @param initialSelected トグルスイッチの初期状態。
     * @param onSwitchChanged トグルスイッチの状態が変更されたときに実行する処理。
     * @param onButtonAction ボタン本体がクリックされたときに実行する処理。
     * @return トグルスイッチ付きのボタン。
     */
    private fun createToggleButton(
        title: String,
        initialSelected: Boolean,
        onSwitchChanged: (Boolean) -> Unit = {},
        onButtonAction: () -> Unit = {}
    ): Button {
        val toggleSwitch = ToggleSwitch().apply {
            isSelected = initialSelected
            isFocusTraversable = false

            selectedProperty().addListener { _, _, newValue ->
                onSwitchChanged(newValue)
            }
        }

        return Button().apply {
            maxWidth = Double.MAX_VALUE
            isFocusTraversable = true
            styleClass.add("equipment-slot-button")

            graphic = HBox(8.0).apply {
                alignment = Pos.CENTER_LEFT
                maxWidth = Double.MAX_VALUE

                children.addAll(
                    Label(title),
                    Region().apply {
                        HBox.setHgrow(this, Priority.ALWAYS)
                    },
                    toggleSwitch
                )
            }

            setOnAction {
                onButtonAction()
            }
        }
    }

    /**
     * サイドバー内のスロットボタン同士を区切る線を生成する。
     *
     * @return 高さ1pxの区切り線。
     */
    private fun createSeparatorLine(): Region {
        return Region().apply {
            minHeight = 1.0
            prefHeight = 1.0
            maxHeight = 1.0
            maxWidth = Double.MAX_VALUE
            styleClass.add("equipment-slot-separator")
        }
    }

    /**
     * 防具スロット用の編集UIを生成する。
     *
     * バニラID、エンチャントオーラ、防具色、装飾を編集できる。
     * 変更内容は `getter` で取得した `EntityArmorData` に直接反映し、
     * 必要に応じて `setter` で現在の `equipmentData` へ再設定する。
     *
     * @param getter 対象スロットの防具データを取得する処理。
     * @param setter 対象スロットの防具データを設定する処理。
     * @return 防具編集UI。
     */
    private fun createArmorEditor(
        getter: () -> EntityArmorData?,
        setter: (EntityArmorData?) -> Unit
    ): Node {
        val armorData = getter() ?: return createEmptyEquipmentView(selectedSlot)

        return VBox(8.0).apply {
            val colorBox = VBox(5.0).apply {
                styleClass.add("custom-border")

                /**
                 * 防具色表示ボタンの表示状態と色を更新する。
                 *
                 * 色が `null` の場合はボタンを非表示にし、
                 * 色が設定されている場合は背景色と文字色を更新する。
                 *
                 * @param button 表示を更新する色ボタン。
                 * @param color 表示する防具色。
                 */
                fun updateColorButton(
                    button: Button,
                    color: HexColor?
                ) {
                    val visible = color != null

                    button.isVisible = visible
                    button.isManaged = visible

                    if (color == null) {
                        button.text = ""
                        button.style = ""
                        return
                    }

                    val textColor = if (ColorPickerDialog.isBrightColor(color)) {
                        "#000000"
                    } else {
                        "#FFFFFF"
                    }

                    button.text = color.value
                    button.style = """
                        -fx-background-color: ${color.value};
                        -fx-text-fill: $textColor;
                        -fx-background-radius: 4px;
                        -fx-border-radius: 4px;
                        -fx-border-color: -fx-line-color;
                        -fx-border-width: 1px;
                    """.trimIndent()
                }

                val colorButton = Button().apply {
                    minWidth = 90.0
                    updateColorButton(this, armorData.color)

                    setOnAction {
                        val currentColor = armorData.color ?: HexColor.of("#FFFFFF")

                        val selectedColor = ColorPickerDialog.show(
                            initialColor = currentColor,
                            owner = main.currentStage,
                            cssPath = "/css/editor/mob/equipment-editor.css"
                        ) ?: return@setOnAction

                        armorData.color = selectedColor
                        setter(armorData)

                        updateColorButton(this, selectedColor)
                        refreshButtonVisual(dataId)
                    }
                }

                children.addAll(
                    HBox(5.0).apply {
                        val colorSwitch = ToggleSwitch().apply {
                            isSelected = armorData.color != null

                            selectedProperty().addListener { _, _, newValue ->
                                if (newValue == null) return@addListener

                                armorData.color = if (newValue) {
                                    armorData.color ?: HexColor.of("#FFFFFF")
                                } else {
                                    null
                                }

                                updateColorButton(colorButton, armorData.color)

                                setter(armorData)
                                refreshButtonVisual(dataId)
                            }
                        }

                        children.addAll(
                            Label("色:"),
                            colorSwitch
                        )
                    },
                    colorButton
                )
            }

            /**
             * 防具色編集欄の表示状態を更新する。
             *
             * 選択中アイテムが革防具の場合のみ色編集欄を表示する。
             * 革防具以外が選択された場合は、防具色を `null` に戻す。
             *
             * @param selected 現在選択されているバニラID。
             * @param armorData 更新対象の防具データ。
             */
            fun colorBoxRefresh(
                selected: String,
                armorData: EntityArmorData
            ) {
                val isLeather = selected in DataRegistry.leatherItems

                colorBox.isVisible = isLeather
                colorBox.isManaged = isLeather

                if (!isLeather) {
                    armorData.color = null
                }
            }

            val trimBox = VBox(5.0).apply {
                styleClass.add("custom-border")

                lateinit var patternBox: HBox
                lateinit var materialBox: HBox
                lateinit var patternComboBox: ComboBox<ArmorTrimRegistry.Pattern>
                lateinit var materialComboBox: ComboBox<ArmorTrimRegistry.Material>

                /**
                 * 装飾関連コントロールの表示状態と選択値を更新する。
                 *
                 * `armorData.trimData` が存在する場合のみ、
                 * 模様と素材の選択欄を表示する。
                 */
                fun refreshTrimControls() {
                    val hasTrim = armorData.trimData != null

                    patternBox.isVisible = hasTrim
                    patternBox.isManaged = hasTrim

                    materialBox.isVisible = hasTrim
                    materialBox.isManaged = hasTrim

                    armorData.trimData?.let { trimData ->
                        patternComboBox.value = trimData.pattern
                        materialComboBox.value = trimData.material
                    }
                }

                patternBox = HBox(5.0).apply {
                    patternComboBox = ComboBox<ArmorTrimRegistry.Pattern>().apply {
                        items.addAll(ArmorTrimRegistry.Pattern.entries)

                        converter = object : StringConverter<ArmorTrimRegistry.Pattern>() {
                            override fun toString(pattern: ArmorTrimRegistry.Pattern?): String {
                                return pattern?.displayName ?: ""
                            }

                            override fun fromString(string: String?): ArmorTrimRegistry.Pattern? {
                                return ArmorTrimRegistry.Pattern.entries.firstOrNull {
                                    it.displayName == string
                                }
                            }
                        }

                        value = armorData.trimData?.pattern
                            ?: ArmorTrimRegistry.Pattern.entries[0]

                        valueProperty().addListener { _, _, selected ->
                            if (selected == null) return@addListener

                            val trimData = armorData.trimData ?: ArmorTrimData().also {
                                armorData.trimData = it
                            }

                            trimData.pattern = selected

                            setter(armorData)
                            refreshButtonVisual(dataId)
                            refreshTrimControls()
                        }
                    }

                    children.addAll(
                        Label("模様:"),
                        patternComboBox
                    )
                }

                materialBox = HBox(5.0).apply {
                    materialComboBox = ComboBox<ArmorTrimRegistry.Material>().apply {
                        items.addAll(ArmorTrimRegistry.Material.entries)

                        converter = object : StringConverter<ArmorTrimRegistry.Material>() {
                            override fun toString(material: ArmorTrimRegistry.Material?): String {
                                return material?.displayName ?: ""
                            }

                            override fun fromString(string: String?): ArmorTrimRegistry.Material? {
                                return ArmorTrimRegistry.Material.entries.firstOrNull {
                                    it.displayName == string
                                }
                            }
                        }

                        value = armorData.trimData?.material
                            ?: ArmorTrimRegistry.Material.entries[0]

                        valueProperty().addListener { _, _, selected ->
                            if (selected == null) return@addListener

                            val trimData = armorData.trimData ?: ArmorTrimData().also {
                                armorData.trimData = it
                            }

                            trimData.material = selected

                            setter(armorData)
                            refreshButtonVisual(dataId)
                            refreshTrimControls()
                        }
                    }

                    children.addAll(
                        Label("素材:"),
                        materialComboBox
                    )
                }

                val trimSwitch = ToggleSwitch().apply {
                    isSelected = armorData.trimData != null

                    selectedProperty().addListener { _, _, newValue ->
                        if (newValue == null) return@addListener

                        armorData.trimData = if (newValue) {
                            armorData.trimData ?: ArmorTrimData()
                        } else {
                            null
                        }

                        setter(armorData)
                        refreshButtonVisual(dataId)
                        refreshTrimControls()
                    }
                }

                children.addAll(
                    HBox(5.0).apply {
                        children.addAll(
                            Label("装飾:"),
                            trimSwitch
                        )
                    },
                    patternBox,
                    materialBox
                )

                refreshTrimControls()
            }

            /**
             * 装飾編集欄の表示状態を更新する。
             *
             * 選択中アイテムが防具の場合のみ装飾編集欄を表示する。
             * 防具以外が選択された場合は、装飾データを `null` に戻す。
             *
             * @param selected 現在選択されているバニラID。
             * @param armorData 更新対象の防具データ。
             * @param notifyChanged 変更通知とsetter呼び出しを行うかどうか。
             */
            fun trimBoxRefresh(
                selected: String,
                armorData: EntityArmorData,
                notifyChanged: Boolean
            ) {
                val isArmor = DataRegistry.isArmor(selected)

                trimBox.isVisible = isArmor
                trimBox.isManaged = isArmor

                if (!isArmor) {
                    armorData.trimData = null
                }

                if (notifyChanged) {
                    setter(armorData)
                    refreshButtonVisual(dataId)
                }
            }

            colorBoxRefresh(armorData.vanillaId, armorData)

            trimBoxRefresh(
                selected = armorData.vanillaId,
                armorData = armorData,
                notifyChanged = false
            )

            children.addAll(
                VBox(5.0).apply {
                    styleClass.add("custom-border")

                    val allItems = DataRegistry.allItems

                    val errorLabel = Label().apply {
                        textFill = Color.RED
                        isVisible = false
                        isManaged = false
                    }

                    val comboBox = ComboBox<String>().apply {
                        items.addAll(allItems)

                        value = if (armorData.vanillaId in allItems) {
                            armorData.vanillaId
                        } else {
                            null
                        }

                        valueProperty().addListener { _, _, selected ->
                            if (selected != null) {
                                armorData.vanillaId = selected

                                colorBoxRefresh(selected, armorData)

                                trimBoxRefresh(
                                    selected = selected,
                                    armorData = armorData,
                                    notifyChanged = true
                                )

                                setter(armorData)
                                refreshButtonVisual(dataId)

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

                                if (armorData.vanillaId in allItems) {
                                    comboBox.value = armorData.vanillaId
                                }

                                errorLabel.text = "検索に一致するアイテムIDがありません"
                                errorLabel.isVisible = true
                                errorLabel.isManaged = true
                            } else {
                                comboBox.items.setAll(result)

                                if (armorData.vanillaId in result) {
                                    comboBox.value = armorData.vanillaId
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
                        Label("バニラID"),
                        searchField,
                        comboBox,
                        errorLabel
                    )
                },
                HBox(5.0).apply {
                    styleClass.add("custom-border")

                    children.addAll(
                        Label("エンチャントオーラ:"),
                        ToggleSwitch().apply {
                            isSelected = armorData.enchantAura

                            selectedProperty().addListener { _, _, newValue ->
                                if (newValue == null) return@addListener

                                armorData.enchantAura = newValue
                                setter(armorData)

                                refreshButtonVisual(dataId)
                            }
                        }
                    )
                },
                colorBox,
                trimBox
            )
        }
    }

    /**
     * 手持ちスロット用の編集UIを生成する。
     *
     * バニラIDとエンチャントオーラを編集できる。
     * 変更内容は `getter` で取得した `EntityHoldData` に直接反映し、
     * 必要に応じて `setter` で現在の `equipmentData` へ再設定する。
     *
     * @param getter 対象スロットの手持ちデータを取得する処理。
     * @param setter 対象スロットの手持ちデータを設定する処理。
     * @return 手持ち装備編集UI。
     */
    private fun createHoldEditor(
        getter: () -> EntityHoldData?,
        setter: (EntityHoldData?) -> Unit
    ): Node {
        val holdData = getter() ?: return createEmptyEquipmentView(selectedSlot)

        return VBox(8.0).apply {
            children.addAll(
                VBox(5.0).apply {
                    styleClass.add("custom-border")

                    val allItems = DataRegistry.allItems

                    val errorLabel = Label().apply {
                        textFill = Color.RED
                        isVisible = false
                        isManaged = false
                    }

                    val comboBox = ComboBox<String>().apply {
                        items.addAll(allItems)

                        value = if (holdData.vanillaId in allItems) {
                            holdData.vanillaId
                        } else {
                            null
                        }

                        valueProperty().addListener { _, _, selected ->
                            if (selected != null) {
                                holdData.vanillaId = selected

                                setter(holdData)
                                refreshButtonVisual(dataId)

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

                                if (holdData.vanillaId in allItems) {
                                    comboBox.value = holdData.vanillaId
                                }

                                errorLabel.text = "検索に一致するアイテムIDがありません"
                                errorLabel.isVisible = true
                                errorLabel.isManaged = true
                            } else {
                                comboBox.items.setAll(result)

                                if (holdData.vanillaId in result) {
                                    comboBox.value = holdData.vanillaId
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
                        Label("バニラID"),
                        searchField,
                        comboBox,
                        errorLabel
                    )
                },
                HBox(5.0).apply {
                    styleClass.add("custom-border")

                    children.addAll(
                        Label("エンチャントオーラ:"),
                        ToggleSwitch().apply {
                            isSelected = holdData.enchantAura

                            selectedProperty().addListener { _, _, newValue ->
                                if (newValue == null) return@addListener

                                holdData.enchantAura = newValue
                                setter(holdData)

                                refreshButtonVisual(dataId)
                            }
                        }
                    )
                }
            )
        }
    }
}

private sealed class EquipmentSlot(
    val displayName: String
) {
    abstract fun isEquipped(equipmentData: EntityEquipmentData): Boolean
    abstract fun setEnabled(equipmentData: EntityEquipmentData, enabled: Boolean)

    data object Head : EquipmentSlot("頭") {
        override fun isEquipped(equipmentData: EntityEquipmentData): Boolean {
            return equipmentData.head != null
        }

        override fun setEnabled(equipmentData: EntityEquipmentData, enabled: Boolean) {
            equipmentData.head = if (enabled) {
                equipmentData.head ?: EntityArmorData()
            } else {
                null
            }
        }
    }

    data object Chest : EquipmentSlot("胴") {
        override fun isEquipped(equipmentData: EntityEquipmentData): Boolean {
            return equipmentData.chest != null
        }

        override fun setEnabled(equipmentData: EntityEquipmentData, enabled: Boolean) {
            equipmentData.chest = if (enabled) {
                equipmentData.chest ?: EntityArmorData()
            } else {
                null
            }
        }
    }

    data object Legs : EquipmentSlot("脚") {
        override fun isEquipped(equipmentData: EntityEquipmentData): Boolean {
            return equipmentData.legs != null
        }

        override fun setEnabled(equipmentData: EntityEquipmentData, enabled: Boolean) {
            equipmentData.legs = if (enabled) {
                equipmentData.legs ?: EntityArmorData()
            } else {
                null
            }
        }
    }

    data object Feet : EquipmentSlot("足") {
        override fun isEquipped(equipmentData: EntityEquipmentData): Boolean {
            return equipmentData.feet != null
        }

        override fun setEnabled(equipmentData: EntityEquipmentData, enabled: Boolean) {
            equipmentData.feet = if (enabled) {
                equipmentData.feet ?: EntityArmorData()
            } else {
                null
            }
        }
    }

    data object MainHand : EquipmentSlot("メインハンド") {
        override fun isEquipped(equipmentData: EntityEquipmentData): Boolean {
            return equipmentData.mainHand != null
        }

        override fun setEnabled(equipmentData: EntityEquipmentData, enabled: Boolean) {
            equipmentData.mainHand = if (enabled) {
                equipmentData.mainHand ?: EntityHoldData()
            } else {
                null
            }
        }
    }

    data object OffHand : EquipmentSlot("オフハンド") {
        override fun isEquipped(equipmentData: EntityEquipmentData): Boolean {
            return equipmentData.offHand != null
        }

        override fun setEnabled(equipmentData: EntityEquipmentData, enabled: Boolean) {
            equipmentData.offHand = if (enabled) {
                equipmentData.offHand ?: EntityHoldData()
            } else {
                null
            }
        }
    }

    companion object {
        val entries: List<EquipmentSlot>
            get() = listOf(
                Head,
                Chest,
                Legs,
                Feet,
                MainHand,
                OffHand
            )
    }
}