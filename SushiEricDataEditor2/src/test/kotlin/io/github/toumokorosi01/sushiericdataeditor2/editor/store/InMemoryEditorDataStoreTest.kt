package io.github.toumokorosi01.sushiericdataeditor2.editor.store

import io.github.toumokorosi01.common.data.item.data.ItemData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame

class InMemoryEditorDataStoreTest {
    @Test
    fun `CRUDとdeepCopyを維持する`() {
        val store = InMemoryEditorDataStore()
        val descriptor = EditorDataDescriptors.item
        val source = ItemData(id = "sword").apply {
            display.displayName = "Sword"
        }

        assertIs<StoreResult.Success<Unit>>(store.save(descriptor, "sword", source))
        val loaded = assertIs<StoreResult.Success<ItemData>>(store.load(descriptor, "sword")).value
        assertNotSame(source, loaded)
        assertEquals("Sword", loaded.display.displayName)

        assertIs<StoreResult.Success<Unit>>(store.rename(descriptor, "sword", "long_sword"))
        assertEquals(
            listOf("long_sword"),
            assertIs<StoreResult.Success<List<StoreResource>>>(store.list(descriptor)).value.map { it.id }
        )
        assertIs<StoreResult.Success<Unit>>(store.delete(descriptor, "long_sword"))
        assertIs<StoreResult.Failure>(store.load(descriptor, "long_sword"))
    }

    @Test
    fun `不正IDを拒否する`() {
        val result = InMemoryEditorDataStore().save(
            EditorDataDescriptors.item,
            "../outside",
            ItemData(id = "../outside")
        )

        assertEquals(StoreErrorCode.INVALID_ID, assertIs<StoreResult.Failure>(result).error.code)
    }
}
