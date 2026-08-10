package io.github.rs0325.sushiericdataeditor2.editor.service

import io.github.rs0325.common.data.item.data.ItemData
import io.github.rs0325.sushiericdataeditor2.editor.store.EditorDataDescriptors
import io.github.rs0325.sushiericdataeditor2.editor.store.InMemoryEditorDataStore
import io.github.rs0325.sushiericdataeditor2.editor.store.StoreResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EditorSyncServiceTest {
    @Test
    fun `全同期用取得は対象カテゴリだけを読み込む`() {
        val store = InMemoryEditorDataStore()
        store.save(EditorDataDescriptors.item, "one", ItemData(id = "one"))
        store.save(EditorDataDescriptors.item, "two", ItemData(id = "two"))
        val service = EditorDataService(store)

        val result = EditorSyncService(service.items).fetchAll()

        assertEquals(
            setOf("one", "two"),
            assertIs<StoreResult.Success<Map<String, ItemData>>>(result).value.keys
        )
    }
}
