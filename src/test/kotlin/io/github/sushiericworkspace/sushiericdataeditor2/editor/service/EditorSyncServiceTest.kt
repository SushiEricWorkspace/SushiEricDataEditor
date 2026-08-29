package io.github.sushiericworkspace.sushiericdataeditor2.editor.service

import io.github.sushiericworkspace.common.data.item.model.mutable.MutableItemBaseData
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
        store.save(EditorDataDescriptors.item, "one", MutableItemBaseData(id = "one"))
        store.save(EditorDataDescriptors.item, "two", MutableItemBaseData(id = "two"))
        val service = EditorDataService(store)

        val result = EditorSyncService(service.items).fetchAll()

        assertEquals(
            setOf("one", "two"),
            assertIs<StoreResult.Success<Map<String, MutableItemBaseData>>>(result).value.keys
        )
    }
}
