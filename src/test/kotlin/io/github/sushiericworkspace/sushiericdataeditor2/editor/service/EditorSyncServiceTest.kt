package io.github.sushiericworkspace.sushiericdataeditor2.editor.service

import io.github.sushiericworkspace.common.data.item.model.ItemBaseData
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.EditorDataDescriptors
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.InMemoryEditorDataStore
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.StoreResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EditorSyncServiceTest {
    @Test
    fun `全同期用取得は対象カテゴリだけを読み込む`() {
        val store = InMemoryEditorDataStore()
        store.save(EditorDataDescriptors.item, "one", ItemBaseData(id = "one"))
        store.save(EditorDataDescriptors.item, "two", ItemBaseData(id = "two"))
        val service = EditorDataService(store)

        val result = EditorSyncService(service.items).fetchAll()

        assertEquals(
            setOf("one", "two"),
            assertIs<StoreResult.Success<Map<String, ItemBaseData>>>(result).value.keys
        )
    }
}
