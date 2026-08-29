package io.github.sushiericworkspace.sushiericdataeditor2.editor.merge

import io.github.sushiericworkspace.common.data.item.model.LoreSection
import io.github.sushiericworkspace.common.data.item.model.detail.ItemDetailContent
import io.github.sushiericworkspace.common.data.item.model.mutable.MutableLoreSection
import io.github.sushiericworkspace.common.data.item.model.mutable.detail.MutableItemDetailContent
import io.github.sushiericworkspace.sushiericdataeditor2.ui.format.ItemDetailContentFormatter
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * [DataConflict]が保持する競合値を、競合解決ダイアログへ表示するための文字列へ整形します。
 *
 * #### 仕様:
 * - [DataConflict.baseValue]、[DataConflict.localValue]、[DataConflict.remoteValue]はいずれも
 *   `Any?`のため、種別ごとの整形をこのクラスへ集約し、3つの値すべてを同じ経路で整形します。
 * - Map・Listの競合値は[MergeAccumulator]が要素の有無を包んだ状態で保持するため、
 *   包みを外したうえで中身を整形し、要素が存在しない場合は「なし」と表示します。
 * - 種別固有データは[ItemDetailContentFormatter]へ委譲します。
 *   これらは既定の`toString()`がオブジェクト参照になり、そのままでは表示に使えません。
 * - Loreセクションは色や装飾の違いが競合になり得るため、MiniMessage形式で装飾込みの文字列にします。
 * - 上記に該当しない値はdata classなどの`toString()`をそのまま使用します。
 */
object ConflictValueFormatter {

    /** 値そのものが存在しないことを表す表示文字列。 */
    private const val ABSENT = "なし"

    /** 値は存在するが中身が空であることを表す表示文字列。 */
    private const val EMPTY = "（空）"

    private val miniMessage = MiniMessage.miniMessage()

    /**
     * 競合値を表示用の文字列へ整形します。
     *
     * @param value 整形対象の競合値。
     * @return 競合内容が読み取れる1行の文字列。
     */
    fun format(value: Any?): String {
        return when (value) {
            null,
            MergeAccumulator.EntryValue.Missing,
            MergeAccumulator.IndexValue.Missing -> ABSENT

            is MergeAccumulator.EntryValue.Present<*> -> format(value.value)
            is MergeAccumulator.IndexValue.Present<*> -> format(value.value)

            is ItemDetailContent -> ItemDetailContentFormatter.format(value)
            is MutableItemDetailContent ->
                ItemDetailContentFormatter.format(value.freeze())
            is LoreSection -> miniMessage.serialize(value.toComponent())
            is MutableLoreSection -> miniMessage.serialize(value.toComponent())

            is Collection<*> -> {
                if (value.isEmpty()) EMPTY else value.joinToString(" | ") { format(it) }
            }

            is Map<*, *> -> {
                if (value.isEmpty()) EMPTY else value.entries.joinToString(", ") { (key, entry) ->
                    "${format(key)}=${format(entry)}"
                }
            }

            is String -> value.ifBlank { EMPTY }

            else -> value.toString()
        }
    }
}
