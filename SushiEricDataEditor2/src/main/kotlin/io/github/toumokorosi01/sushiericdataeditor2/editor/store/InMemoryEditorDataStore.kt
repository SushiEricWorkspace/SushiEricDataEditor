package io.github.toumokorosi01.sushiericdataeditor2.editor.store

import io.github.toumokorosi01.common.data.core.ManagedData
import java.util.concurrent.ConcurrentHashMap

class InMemoryEditorDataStore(
    override val identity: String = "memory"
) : EditorDataStore {
    private val entries = ConcurrentHashMap<String, ManagedData<*, *>>()

    override val kind: EditorDataStoreKind = EditorDataStoreKind.IN_MEMORY
    override val isAvailable: Boolean = true

    override fun <T : ManagedData<T, *>> list(
        descriptor: EditorDataDescriptor<T>
    ): StoreResult<List<StoreResource>> {
        val prefix = "${descriptor.dataType.categoryDirName}/"
        return StoreResult.Success(
            entries.keys
                .asSequence()
                .filter { it.startsWith(prefix) }
                .map { it.removePrefix(prefix) }
                .sorted()
                .map { StoreResource(it, "$it.yml", "memory://$prefix$it.yml") }
                .toList()
        )
    }

    override fun <T : ManagedData<T, *>> load(
        descriptor: EditorDataDescriptor<T>,
        id: String
    ): StoreResult<T> {
        if (!StorePathValidator.isValidId(id)) return invalidId(id)
        val data = entries[key(descriptor, id)]
            ?: return StoreResult.Failure(StoreError(StoreErrorCode.FILE_NOT_FOUND, id))
        @Suppress("UNCHECKED_CAST")
        return StoreResult.Success(descriptor.deepCopy(data as T))
    }

    override fun <T : ManagedData<T, *>> save(
        descriptor: EditorDataDescriptor<T>,
        id: String,
        data: T
    ): StoreResult<Unit> {
        if (!StorePathValidator.isValidId(id)) return invalidId(id)
        entries[key(descriptor, id)] = descriptor.deepCopy(data)
        return StoreResult.Success(Unit)
    }

    override fun <T : ManagedData<T, *>> rename(
        descriptor: EditorDataDescriptor<T>,
        oldId: String,
        newId: String
    ): StoreResult<Unit> {
        if (!StorePathValidator.isValidId(oldId) || !StorePathValidator.isValidId(newId)) {
            return invalidId(newId)
        }
        val oldKey = key(descriptor, oldId)
        val newKey = key(descriptor, newId)
        if (entries.containsKey(newKey)) {
            return StoreResult.Failure(StoreError(StoreErrorCode.ALREADY_EXISTS, newId))
        }
        val current = entries[oldKey]
            ?: return StoreResult.Failure(StoreError(StoreErrorCode.FILE_NOT_FOUND, oldId))
        @Suppress("UNCHECKED_CAST")
        val renamed = descriptor.deepCopy(current as T).apply { id = newId }
        entries[newKey] = renamed
        entries.remove(oldKey)
        return StoreResult.Success(Unit)
    }

    override fun <T : ManagedData<T, *>> delete(
        descriptor: EditorDataDescriptor<T>,
        id: String
    ): StoreResult<Unit> {
        if (!StorePathValidator.isValidId(id)) return invalidId(id)
        return if (entries.remove(key(descriptor, id)) != null) {
            StoreResult.Success(Unit)
        } else {
            StoreResult.Failure(StoreError(StoreErrorCode.FILE_NOT_FOUND, id))
        }
    }

    private fun <T : ManagedData<T, *>> key(
        descriptor: EditorDataDescriptor<T>,
        id: String
    ): String = "${descriptor.dataType.categoryDirName}/$id"

    private fun <T> invalidId(id: String): StoreResult<T> =
        StoreResult.Failure(StoreError(StoreErrorCode.INVALID_ID, id))
}
