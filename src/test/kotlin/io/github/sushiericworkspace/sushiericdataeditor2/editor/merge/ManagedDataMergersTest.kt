package io.github.sushiericworkspace.sushiericdataeditor2.editor.merge

import io.github.sushiericworkspace.common.data.item.model.ItemBaseData
import io.github.sushiericworkspace.common.stats.player.StatsType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManagedDataMergersTest {
    @Test
    fun `異なるフィールドの変更は自動マージする`() {
        val base = ItemBaseData(id = "sword").apply {
            display.displayName = "Sword"
            stats[StatsType.PHYSICS_DAMAGE] = 10.0
        }
        val local = base.deepCopy().apply {
            stats[StatsType.PHYSICS_DAMAGE] = 15.0
        }
        val remote = base.deepCopy().apply {
            display.displayName = "Long Sword"
        }

        val result = ItemDataMerger.merge(base, local, remote)

        assertTrue(result.conflicts.isEmpty())
        assertEquals("Long Sword", result.merged.display.displayName)
        assertEquals(15.0, result.merged.stats[StatsType.PHYSICS_DAMAGE])
    }

    @Test
    fun `同じフィールドの異なる変更だけを競合にする`() {
        val base = ItemBaseData(id = "sword").apply { display.displayName = "Sword" }
        val local = base.deepCopy().apply { display.displayName = "Local Sword" }
        val remote = base.deepCopy().apply { display.displayName = "Remote Sword" }

        val result = ItemDataMerger.merge(base, local, remote)

        assertEquals(listOf(DataFields.displayName), result.conflicts.map { it.path })
        assertEquals("Remote Sword", result.merged.display.displayName)
        assertEquals(
            "Local Sword",
            result.resolveWithLocal(setOf(DataFields.displayName)).display.displayName
        )
    }

    @Test
    fun `Mapの異なるキーは競合しない`() {
        val statTypes = StatsType.entries.take(2)
        val first = statTypes[0]
        val second = statTypes[1]
        val base = ItemBaseData(id = "sword").apply {
            stats[first] = first.default
            stats[second] = second.default
        }
        val local = base.deepCopy().apply { stats[first] = first.default + 1.0 }
        val remote = base.deepCopy().apply { stats[second] = second.default + 1.0 }

        val result = ItemDataMerger.merge(base, local, remote)

        assertTrue(result.conflicts.isEmpty())
        assertEquals(local.stats[first], result.merged.stats[first])
        assertEquals(remote.stats[second], result.merged.stats[second])
    }

    @Test
    fun `Listの異なる要素は競合しない`() {
        val base = ItemBaseData(id = "sword").apply {
            editorMeta.comment.addAll(listOf("a", "b"))
        }
        val local = base.deepCopy().apply { editorMeta.comment[0] = "local" }
        val remote = base.deepCopy().apply { editorMeta.comment[1] = "remote" }

        val result = ItemDataMerger.merge(base, local, remote)

        assertTrue(result.conflicts.isEmpty())
        assertEquals(listOf("local", "remote"), result.merged.editorMeta.comment)
    }
}
