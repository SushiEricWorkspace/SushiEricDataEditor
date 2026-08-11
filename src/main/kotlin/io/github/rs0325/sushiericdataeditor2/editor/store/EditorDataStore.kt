package io.github.rs0325.sushiericdataeditor2.editor.store

import io.github.rs0325.common.data.core.ManagedData

interface EditorDataStore {
    val kind: EditorDataStoreKind
    val identity: String
    val isAvailable: Boolean

    fun <T : ManagedData<T, *>> list(
        descriptor: EditorDataDescriptor<T>
    ): StoreResult<List<StoreResource>>

    fun <T : ManagedData<T, *>> load(
        descriptor: EditorDataDescriptor<T>,
        id: String
    ): StoreResult<T>

    fun <T : ManagedData<T, *>> save(
        descriptor: EditorDataDescriptor<T>,
        id: String,
        data: T
    ): StoreResult<Unit>

    fun <T : ManagedData<T, *>> rename(
        descriptor: EditorDataDescriptor<T>,
        oldId: String,
        newId: String
    ): StoreResult<Unit>

    fun <T : ManagedData<T, *>> delete(
        descriptor: EditorDataDescriptor<T>,
        id: String
    ): StoreResult<Unit>
}
