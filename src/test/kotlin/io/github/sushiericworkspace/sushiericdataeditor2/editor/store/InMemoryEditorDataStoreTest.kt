package io.github.sushiericworkspace.sushiericdataeditor2.editor.store

import io.github.sushiericworkspace.common.data.item.model.ItemBaseData
import io.github.sushiericworkspace.common.data.item.model.PlainTextLoreSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

class InMemoryEditorDataStoreTest {
    @Test
    fun `CRUDとdeepCopyを維持する`() {
        val store = InMemoryEditorDataStore()
        val descriptor = EditorDataDescriptors.item
        val source = ItemBaseData(id = "sword").apply {
            display.displayName = "Sword"
        }

        assertIs<StoreResult.Success<Unit>>(store.save(descriptor, "sword", source))
        val loaded = assertIs<StoreResult.Success<ItemBaseData>>(store.load(descriptor, "sword")).value
        assertNotSame(source, loaded)
        assertEquals("Sword", loaded.display.displayName)
        assertEquals(source.internalId, loaded.internalId)

        assertIs<StoreResult.Success<Unit>>(store.rename(descriptor, "sword", "long_sword"))
        val renamed = assertIs<StoreResult.Success<ItemBaseData>>(
            store.load(descriptor, "long_sword")
        ).value
        assertEquals("long_sword", renamed.id)
        assertEquals(source.internalId, renamed.internalId)
        assertEquals(
            listOf("long_sword"),
            assertIs<StoreResult.Success<List<StoreResource>>>(store.list(descriptor)).value.map { it.id }
        )
        assertIs<StoreResult.Success<Unit>>(store.delete(descriptor, "long_sword"))
        assertIs<StoreResult.Failure>(store.load(descriptor, "long_sword"))
    }

    @Test
    fun `新規作成と複製で異なるinternalIdを生成する`() {
        val descriptor = EditorDataDescriptors.item
        val first = descriptor.createDefault("first")
        val second = descriptor.createDefault("second")

        assertNotEquals(first.internalId, second.internalId)

        first.display.displayName = "Sword"
        first.display.lore.add(mutableListOf(PlainTextLoreSection("複製対象のLore")))
        first.editorMeta.comment.add("original")
        val duplicate = descriptor.duplicateAsNew(first, "copied_sword")

        assertEquals("copied_sword", duplicate.id)
        assertNotEquals(first.internalId, duplicate.internalId)
        assertEquals(first.display, duplicate.display)
        assertEquals("複製対象のLore", (duplicate.display.lore.single().single() as PlainTextLoreSection).text)
        assertNotSame(first.display, duplicate.display)
        assertNotSame(first.display.lore, duplicate.display.lore)
        assertNotSame(first.display.lore.single(), duplicate.display.lore.single())
        assertNotSame(first.editorMeta.comment, duplicate.editorMeta.comment)
    }

    @Test
    fun `不正IDを拒否する`() {
        val result = InMemoryEditorDataStore().save(
            EditorDataDescriptors.item,
            "../outside",
            ItemBaseData(id = "../outside")
        )

        assertEquals(StoreErrorCode.INVALID_ID, assertIs<StoreResult.Failure>(result).error.code)
    }
}
