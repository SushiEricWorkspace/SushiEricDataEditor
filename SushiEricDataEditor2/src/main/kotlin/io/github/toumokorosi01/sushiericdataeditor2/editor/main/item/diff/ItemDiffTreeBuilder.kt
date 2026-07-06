package io.github.toumokorosi01.sushiericdataeditor2.editor.main.item.diff

import io.github.toumokorosi01.common.stats.StatsType
import io.github.toumokorosi01.common.data.item.data.CustomComponentLoreSection
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.item.data.LoreSection
import io.github.toumokorosi01.common.data.item.data.PlainTextLoreSection
import io.github.toumokorosi01.common.data.item.data.StatLoreSection
import io.github.toumokorosi01.common.data.item.data.detail.*
import io.github.toumokorosi01.common.data.item.data.detail.ItemDetailContent
import javafx.scene.control.TreeView
import net.kyori.adventure.text.minimessage.MiniMessage
import javafx.scene.control.CheckBoxTreeItem
import javafx.scene.control.Tooltip
import javafx.util.Callback
import net.kyori.adventure.text.format.TextDecoration
import kotlin.collections.get

class ItemDiffTreeBuilder {

    private val mm = MiniMessage.miniMessage()

    private fun serializeLoreLine(sections: List<LoreSection>?): String {
        if (sections == null) return "(なし)"

        return sections.joinToString(" | ") { section ->
            mm.serialize(section.toComponent())
        }
    }

    /**
     * 値（DiffId）から画面表示用のわかりやすい文字列を生成するヘルパー
     */
    private fun getDisplayString(id: ItemDiffId, origText: String, servText: String): String {
        return when (id.field) {
            ItemDiffField.RARITY -> "レアリティ: $origText ➔ $servText"
            ItemDiffField.DISPLAY_NAME -> "表示名: \"$origText\" ➔ \"$servText\""
            ItemDiffField.LORE -> "Lore [${(id.index ?: 0) + 1}行目]: $origText ➔ $servText"
            ItemDiffField.STATS -> "${id.statsType?.name ?: "未知"}: $origText ➔ $servText"
            ItemDiffField.COMMENT -> "説明文 [${(id.index ?: 0) + 1}行目]: $origText ➔ $servText"
            ItemDiffField.DETAIL -> "詳細データ: $origText ➔ $servText"
        }
    }

    private fun describeLoreSection(section: LoreSection?): String {
        return when (section) {
            null -> "(なし)"

            is PlainTextLoreSection ->
                "通常テキスト(text=${section.text}, secret=${section.secret})"

            is CustomComponentLoreSection ->
                "カスタムComponent(text=${section.getText()}, color=${section.getHexColor() ?: "未設定"})"

            is StatLoreSection ->
                "ステータス(${section.stat.display})"
        }
    }

    private fun diffLoreSection(
        serverSection: LoreSection?,
        originalSection: LoreSection?
    ): List<String> {
        val diffs = mutableListOf<String>()

        if (serverSection?.type != originalSection?.type) {
            diffs.add(
                "種類: ${serverSection?.type?.display ?: "(なし)"} → ${originalSection?.type?.display ?: "(なし)"}"
            )
        }

        when {
            serverSection is PlainTextLoreSection && originalSection is PlainTextLoreSection -> {
                if (serverSection.text != originalSection.text) {
                    diffs.add("text: ${serverSection.text} → ${originalSection.text}")
                }

                if (serverSection.secret != originalSection.secret) {
                    diffs.add("secret: ${serverSection.secret} → ${originalSection.secret}")
                }
            }

            serverSection is StatLoreSection && originalSection is StatLoreSection -> {
                if (serverSection.stat != originalSection.stat) {
                    diffs.add("stat: ${serverSection.stat.display} → ${originalSection.stat.display}")
                }
            }

            serverSection is CustomComponentLoreSection && originalSection is CustomComponentLoreSection -> {
                if (serverSection.getText() != originalSection.getText()) {
                    diffs.add("text: ${serverSection.getText()} → ${originalSection.getText()}")
                }

                if (serverSection.getHexColor() != originalSection.getHexColor()) {
                    diffs.add("color: ${serverSection.getHexColor() ?: "未設定"} → ${originalSection.getHexColor() ?: "未設定"}")
                }

                fun addDecorationDiff(name: String, decoration: TextDecoration) {
                    val serverValue = serverSection.getDecoration(decoration)
                    val originalValue = originalSection.getDecoration(decoration)

                    if (serverValue != originalValue) {
                        diffs.add("$name: $serverValue → $originalValue")
                    }
                }

                addDecorationDiff("bold", TextDecoration.BOLD)
                addDecorationDiff("italic", TextDecoration.ITALIC)
                addDecorationDiff("underlined", TextDecoration.UNDERLINED)
                addDecorationDiff("strikethrough", TextDecoration.STRIKETHROUGH)
                addDecorationDiff("obfuscated", TextDecoration.OBFUSCATED)
            }

            else -> {
                diffs.add("変更前: ${describeLoreSection(serverSection)}")
                diffs.add("変更後: ${describeLoreSection(originalSection)}")
            }
        }

        return diffs
    }

    private fun getLoreDetailTooltip(
        id: ItemDiffId,
        original: ItemData,
        server: ItemData
    ): String? {
        if (id.field != ItemDiffField.LORE) return null

        val lineIndex = id.index ?: return null

        val originalLine = original.display.lore.getOrNull(lineIndex)
        val serverLine = server.display.lore.getOrNull(lineIndex)

        return buildString {
            appendLine("Lore [${lineIndex + 1}行目]")

            val maxSize = maxOf(
                originalLine?.size ?: 0,
                serverLine?.size ?: 0
            )

            for (sectionIndex in 0 until maxSize) {
                val originalSection = originalLine?.getOrNull(sectionIndex)
                val serverSection = serverLine?.getOrNull(sectionIndex)

                val diffs = diffLoreSection(
                    serverSection = serverSection,
                    originalSection = originalSection
                )

                if (diffs.isNotEmpty()) {
                    appendLine()
                    appendLine("セクション ${sectionIndex + 1}:")
                    diffs.forEach { diff ->
                        appendLine("  $diff")
                    }
                }
            }
        }
    }

    fun buildDiffTree(original: ItemData, server: ItemData): TreeView<ItemDiffId?> {
        // ルートノード（カテゴリやルートは内部データを持たないのでnull）
        val rootItem = CheckBoxTreeItem<ItemDiffId?>(null).apply {
            isExpanded = true
            isSelected = false
        }

        // --- 1. レアリティの比較 ---
        if (original.rarity != server.rarity) {
            val dId = ItemDiffId(ItemDiffField.RARITY)
            rootItem.children.add(CheckBoxTreeItem(dId))
        }

        // --- 2. 詳細データの比較 ---
        val detailRoot = CheckBoxTreeItem<ItemDiffId?>(null)

        if (original.itemDetail != server.itemDetail) {
            detailRoot.children.add(CheckBoxTreeItem(ItemDiffId(ItemDiffField.DETAIL)))
        }

        if (detailRoot.children.isNotEmpty()) {
            detailRoot.isExpanded = true
            rootItem.children.add(detailRoot)
        }

        // --- 3. Display設定の比較 ---
        val displayRoot = CheckBoxTreeItem<ItemDiffId?>(null)
        if (original.display.displayName != server.display.displayName) {
            displayRoot.children.add(CheckBoxTreeItem(ItemDiffId(ItemDiffField.DISPLAY_NAME)))
        }
        compareLore(original.display.lore, server.display.lore, displayRoot)
        if (displayRoot.children.isNotEmpty()) {
            displayRoot.isExpanded = true
            rootItem.children.add(displayRoot)
        }

        // --- 4. Statsの比較 ---
        val statsRoot = CheckBoxTreeItem<ItemDiffId?>(null)
        compareStats(original.stats, server.stats, statsRoot)
        if (statsRoot.children.isNotEmpty()) {
            statsRoot.isExpanded = true
            rootItem.children.add(statsRoot)
        }

        // --- 5. EditorMetaの比較 ---
        val metaRoot = CheckBoxTreeItem<ItemDiffId?>(null)
        compareEditorMeta(original.editorMeta.comment, server.editorMeta.comment, metaRoot)
        if (metaRoot.children.isNotEmpty()) {
            metaRoot.isExpanded = true
            rootItem.children.add(metaRoot)
        }

        return TreeView(rootItem).apply {
            isShowRoot = true

            // 💡 内部の DiffId に応じて動的に文字列を作って描画するファクトリ
            cellFactory = Callback { _ ->
                object : javafx.scene.control.cell.CheckBoxTreeCell<ItemDiffId?>() {
                    override fun updateItem(item: ItemDiffId?, empty: Boolean) {
                        super.updateItem(item, empty)

                        // TreeCellは再利用されるので、毎回リセットする
                        tooltip = null

                        if (empty || item == null) {
                            val parentItem = treeItem

                            text = when (parentItem) {
                                displayRoot -> "表示設定 (Display)"
                                statsRoot -> "ステータス (Stats)"
                                metaRoot -> "エディター用メタ (EditorMeta)"
                                detailRoot -> "詳細データ (Detail)"
                                else -> ""
                            }
                        } else {
                            val origVal = getOriginalValueString(item, original)
                            val servVal = getServerValueString(item, server)

                            text = getDisplayString(item, origVal, servVal)

                            val tooltipText = getLoreDetailTooltip(
                                id = item,
                                original = original,
                                server = server
                            ) ?: getDetailTooltip(
                                id = item,
                                original = original,
                                server = server
                            )

                            if (!tooltipText.isNullOrBlank()) {
                                tooltip = Tooltip(tooltipText).apply {
                                    isWrapText = true
                                    maxWidth = 520.0
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 選択データの回収 (パス文字列ではなく、DiffIdオブジェクトのセットを返す) ---
    fun collectCheckedFields(item: CheckBoxTreeItem<ItemDiffId?>): Set<ItemDiffId> {
        val checkedSet = mutableSetOf<ItemDiffId>()
        val value = item.value

        if (item.isSelected && value != null && item.children.isEmpty()) {
            checkedSet.add(value)
        }
        for (child in item.children) {
            if (child is CheckBoxTreeItem<ItemDiffId?>) {
                checkedSet.addAll(collectCheckedFields(child))
            }
        }
        return checkedSet
    }

    // 差分文字抽出用の内部ヘルパー群
    private fun getOriginalValueString(id: ItemDiffId, original: ItemData): String = when(id.field) {
        ItemDiffField.RARITY -> original.rarity.name
        ItemDiffField.DISPLAY_NAME -> original.display.displayName
        ItemDiffField.LORE -> serializeLoreLine(original.display.lore.getOrNull(id.index!!))
        ItemDiffField.STATS -> original.stats[id.statsType]?.toString() ?: "(未設定)"
        ItemDiffField.COMMENT -> original.editorMeta.comment.getOrNull(id.index!!) ?: "(なし)"
        ItemDiffField.DETAIL -> serializeDetail(original)
    }

    private fun getServerValueString(id: ItemDiffId, server: ItemData): String = when(id.field) {
        ItemDiffField.RARITY -> server.rarity.name
        ItemDiffField.DISPLAY_NAME -> server.display.displayName
        ItemDiffField.LORE -> server.display.lore.getOrNull(id.index!!)?.let {
            serializeLoreLine(it)
        } ?: "(削除)"
        ItemDiffField.STATS -> server.stats[id.statsType]?.toString() ?: "(削除)"
        ItemDiffField.COMMENT -> server.editorMeta.comment.getOrNull(id.index!!) ?: "(削除)"
        ItemDiffField.DETAIL -> serializeDetail(server)
    }

    private fun compareLore(
        origLore: List<List<LoreSection>>,
        servLore: List<List<LoreSection>>,
        parentNode: CheckBoxTreeItem<ItemDiffId?>
    ) {
        for (i in 0 until maxOf(origLore.size, servLore.size)) {
            val originalLine = origLore.getOrNull(i)?.let { serializeLoreLine(it) }
            val serverLine = servLore.getOrNull(i)?.let { serializeLoreLine(it) }

            if (originalLine != serverLine) {
                parentNode.children.add(CheckBoxTreeItem(ItemDiffId(ItemDiffField.LORE, index = i)))
            }
        }
    }

    private fun compareStats(origStats: Map<StatsType, Double>, servStats: Map<StatsType, Double>, parentNode: CheckBoxTreeItem<ItemDiffId?>) {
        for (key in (origStats.keys + servStats.keys)) {
            if (origStats[key] != servStats[key]) {
                parentNode.children.add(CheckBoxTreeItem(ItemDiffId(ItemDiffField.STATS, statsType = key)))
            }
        }
    }

    private fun compareEditorMeta(origDesc: List<String>, servDesc: List<String>, parentNode: CheckBoxTreeItem<ItemDiffId?>) {
        for (i in 0 until maxOf(origDesc.size, servDesc.size)) {
            if (origDesc.getOrNull(i) != servDesc.getOrNull(i)) {
                parentNode.children.add(CheckBoxTreeItem(ItemDiffId(ItemDiffField.COMMENT, index = i)))
            }
        }
    }

    private fun serializeDetail(itemData: ItemData): String {
        val detail = itemData.itemDetail

        return buildString {
            append(detail.itemType.name)
            append(" / ")
            append(detail.vanillaId)
            append(" / ")
            append(if (detail.enchantAura) "エンチャントオーラあり" else "エンチャントオーラなし")
            append(" / ")
            append(detail.maxStackSize)
        }
    }

    private fun getDetailTooltip(
        id: ItemDiffId,
        original: ItemData,
        server: ItemData
    ): String? {
        if (id.field != ItemDiffField.DETAIL) return null

        val originalDetail = original.itemDetail
        val serverDetail = server.itemDetail

        return buildString {
            appendLine("詳細データ (Detail)")

            if (serverDetail.itemType != originalDetail.itemType) {
                appendLine("ItemType:")
                appendLine("  サーバー: ${serverDetail.itemType}")
                appendLine("  ローカル: ${originalDetail.itemType}")
                appendLine()
            }

            if (serverDetail.vanillaId != originalDetail.vanillaId) {
                appendLine("Vanilla ID:")
                appendLine("  サーバー: ${serverDetail.vanillaId}")
                appendLine("  ローカル: ${originalDetail.vanillaId}")
                appendLine()
            }

            if (serverDetail.enchantAura != originalDetail.enchantAura) {
                appendLine("エンチャントオーラ:")
                appendLine("  サーバー: ${serverDetail.enchantAura}")
                appendLine("  ローカル: ${originalDetail.enchantAura}")
                appendLine()
            }

            if (serverDetail.maxStackSize != originalDetail.maxStackSize) {
                appendLine("最大スタック数:")
                appendLine("  サーバー: ${serverDetail.maxStackSize}")
                appendLine("  ローカル: ${originalDetail.maxStackSize}")
                appendLine()
            }

            if (serverDetail.content != originalDetail.content) {
                appendLine("Content:")
                appendLine("  サーバー: ${formatDetailContent(serverDetail.content)}")
                appendLine("  ローカル: ${formatDetailContent(originalDetail.content)}")
            }
        }.trim()
    }

    private fun formatDetailContent(content: ItemDetailContent): String {
        return when (content) {
            is SwordData -> "剣"
            is ShortSwordData -> "短剣"
            is LongSwordData -> "長剣 cooldown=${content.cooldown}"
            is AxeData -> "斧"

            is BowData -> {
                "弓 multi=${content.multi}, pierce=${content.pierce}, short=${content.short}, shortInterval=${content.shortInterval}"
            }

            is CrossbowData -> {
                "クロスボウ damageRange=${content.damageRange}, shortInterval=${content.shortInterval}"
            }

            is SpearData -> "槍 cooldown=${content.cooldown}"

            is PotionData -> {
                "ポーション color=${content.color}, effects=${content.effects.size}件"
            }

            is ShieldData -> {
                "盾 cooldown=${content.cooldown}, defenceRate=${content.defenceRate}"
            }

            is ArmorContent -> {
                "防具 color=${content.color ?: "なし"}, trim=${content.trimData ?: "なし"}"
            }

            is OtherData -> "その他"
        }
    }
}