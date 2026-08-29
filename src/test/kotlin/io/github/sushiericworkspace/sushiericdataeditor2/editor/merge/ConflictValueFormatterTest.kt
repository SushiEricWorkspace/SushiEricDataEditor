package io.github.sushiericworkspace.sushiericdataeditor2.editor.merge

import io.github.sushiericworkspace.common.data.item.model.mutable.MutableItemBaseData
import io.github.sushiericworkspace.common.data.item.model.mutable.MutablePlainTextLoreSection
import io.github.sushiericworkspace.common.data.item.model.mutable.detail.MutableAxeData
import io.github.sushiericworkspace.common.data.item.model.mutable.detail.MutableShortSwordData
import io.github.sushiericworkspace.common.data.item.model.mutable.detail.MutableSwordData
import io.github.sushiericworkspace.common.stats.player.StatsType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConflictValueFormatterTest {
    @Test
    fun `種別固有データはどの種別かが分かる文字列にする`() {
        val base = MutableItemBaseData(id = "sword").apply { itemDetail.content = MutableSwordData() }
        val local = base.deepCopy().apply { itemDetail.content = MutableAxeData() }
        val remote = base.deepCopy().apply { itemDetail.content = MutableShortSwordData() }

        val conflict = ItemDataMerger.merge(base, local, remote)
            .conflicts
            .single { it.path == DataFields.detailContent }

        assertEquals("剣", ConflictValueFormatter.format(conflict.baseValue))
        assertEquals("斧", ConflictValueFormatter.format(conflict.localValue))
        assertEquals("短剣", ConflictValueFormatter.format(conflict.remoteValue))
    }

    @Test
    fun `Mapから削除された競合値はなしと表示する`() {
        val stat = StatsType.PHYSICS_DAMAGE
        val base = MutableItemBaseData(id = "sword").apply { stats[stat] = 10.0 }
        val local = base.deepCopy().apply { stats.remove(stat) }
        val remote = base.deepCopy().apply { stats[stat] = 20.0 }

        val conflict = ItemDataMerger.merge(base, local, remote).conflicts.single()

        assertEquals("10.0", ConflictValueFormatter.format(conflict.baseValue))
        assertEquals("なし", ConflictValueFormatter.format(conflict.localValue))
        assertEquals("20.0", ConflictValueFormatter.format(conflict.remoteValue))
    }

    @Test
    fun `Lore行はセクションの表示内容を含む文字列にする`() {
        val base = MutableItemBaseData(id = "sword").apply {
            display.mutableLore.add(mutableListOf(MutablePlainTextLoreSection(text = "base")))
        }
        val local = base.deepCopy().apply {
            display.mutableLore[0] = mutableListOf(MutablePlainTextLoreSection(text = "local"))
        }
        val remote = base.deepCopy().apply {
            display.mutableLore[0] = mutableListOf(MutablePlainTextLoreSection(text = "remote"))
        }

        val conflict = ItemDataMerger.merge(base, local, remote).conflicts.single()

        assertTrue(ConflictValueFormatter.format(conflict.baseValue).contains("base"))
        assertTrue(ConflictValueFormatter.format(conflict.localValue).contains("local"))
        assertTrue(ConflictValueFormatter.format(conflict.remoteValue).contains("remote"))
    }

    @Test
    fun `値なしと空文字を区別して表示する`() {
        assertEquals("なし", ConflictValueFormatter.format(null))
        assertEquals("（空）", ConflictValueFormatter.format(""))
        assertEquals("（空）", ConflictValueFormatter.format(emptyList<String>()))
        assertEquals("Sword", ConflictValueFormatter.format("Sword"))
    }
}
