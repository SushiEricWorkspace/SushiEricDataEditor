package io.github.sushiericworkspace.sushiericdataeditor2.editor.service

import io.github.sushiericworkspace.common.data.core.ManagedData
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.StoreResult

class EditorSyncService<T : ManagedData<T, *>>(
    private val dataAccess: EditorDataService.DataAccess<T>
) {
    fun fetchOne(id: String): StoreResult<T> = dataAccess.loadStore(id)

    fun fetchAll(): StoreResult<Map<String, T>> {
        val resources = when (val listed = dataAccess.listStoreResources()) {
            is StoreResult.Success -> listed.value
            is StoreResult.Failure -> return listed
        }
        val loaded = linkedMapOf<String, T>()
        resources.forEach { resource ->
            when (val result = dataAccess.loadStore(resource.id)) {
                is StoreResult.Success -> loaded[resource.id] = result.value
                is StoreResult.Failure -> return result
            }
        }
        return StoreResult.Success(loaded)
    }
}
