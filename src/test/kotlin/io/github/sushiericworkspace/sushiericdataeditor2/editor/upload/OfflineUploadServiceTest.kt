package io.github.sushiericworkspace.sushiericdataeditor2.editor.upload

import io.github.sushiericworkspace.common.data.core.ManagedData
import io.github.sushiericworkspace.common.data.item.model.mutable.MutableItemBaseData
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.EditorDataDescriptor
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.EditorDataDescriptors
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.EditorDataStore
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.EditorDataStoreKind
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.InMemoryEditorDataStore
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.LocalEditorDataStore
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.StoreError
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.StoreErrorCode
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.StoreResource
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.StoreResult
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OfflineUploadServiceTest {
    @Test
    fun `選択データだけを正規化後にアップロードしてローカルを保持する`() {
        val root = createTempDirectory("offline-upload").toFile()
        try {
            val local = LocalEditorDataStore(root)
            local.save(EditorDataDescriptors.item, "one", validItem("one"))
            local.save(EditorDataDescriptors.item, "two", validItem("two"))
            val remote = InMemoryEditorDataStore("server")
            remote.save(EditorDataDescriptors.item, "one", validItem("one"))
            val service = OfflineUploadService(root, remote)

            val scan = assertIs<UploadScanResult.Success>(service.scan())
            assertEquals(
                UploadCandidateState.OVERWRITE,
                scan.candidates.single { it.key.id == "one" }.state
            )

            val two = UploadKey(UploadDataCategory.ITEM, "two")
            val result = service.upload(setOf(two), emptySet())

            assertEquals(listOf(two), result.succeeded)
            assertTrue(result.failed.isEmpty())
            assertIs<StoreResult.Success<MutableItemBaseData>>(
                remote.load(EditorDataDescriptors.item, "two")
            )
            assertIs<StoreResult.Success<MutableItemBaseData>>(
                local.load(EditorDataDescriptors.item, "two")
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `上書き承認なしでは既存リモートを変更しない`() {
        val root = createTempDirectory("offline-upload-conflict").toFile()
        try {
            val local = LocalEditorDataStore(root)
            local.save(EditorDataDescriptors.item, "same", validItem("same", "Local"))
            val remote = InMemoryEditorDataStore("server")
            remote.save(EditorDataDescriptors.item, "same", validItem("same", "Remote"))
            val key = UploadKey(UploadDataCategory.ITEM, "same")

            val result = OfflineUploadService(root, remote).upload(setOf(key), emptySet())

            assertEquals(
                StoreErrorCode.ALREADY_EXISTS,
                result.failed.single().error.code
            )
            assertEquals(
                "Remote",
                assertIs<StoreResult.Success<MutableItemBaseData>>(
                    remote.load(EditorDataDescriptors.item, "same")
                ).value.display.displayName
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `リモート一覧を取得できない場合はアップロード対象を選択不可にする`() {
        val root = createTempDirectory("offline-upload-unavailable").toFile()
        try {
            LocalEditorDataStore(root).save(
                EditorDataDescriptors.item,
                "one",
                validItem("one")
            )

            val scan = assertIs<UploadScanResult.Success>(
                OfflineUploadService(root, ListUnavailableStore()).scan()
            )

            val candidate = scan.candidates.single { it.key.id == "one" }
            assertEquals(UploadCandidateState.UNAVAILABLE, candidate.state)
            assertEquals(StoreErrorCode.STORE_UNAVAILABLE, candidate.error?.code)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun validItem(id: String, name: String = "Sword"): MutableItemBaseData =
        MutableItemBaseData(id = id).apply { display.displayName = name }

    private class ListUnavailableStore : EditorDataStore {
        private val delegate = InMemoryEditorDataStore("unavailable")

        override val kind = EditorDataStoreKind.IN_MEMORY
        override val identity = "unavailable"
        override val isAvailable = false

        override fun <T : ManagedData<T, *>> list(
            descriptor: EditorDataDescriptor<T>
        ): StoreResult<List<StoreResource>> =
            StoreResult.Failure(StoreError(StoreErrorCode.STORE_UNAVAILABLE))

        override fun <T : ManagedData<T, *>> load(
            descriptor: EditorDataDescriptor<T>,
            id: String
        ): StoreResult<T> = delegate.load(descriptor, id)

        override fun <T : ManagedData<T, *>> save(
            descriptor: EditorDataDescriptor<T>,
            id: String,
            data: T
        ): StoreResult<Unit> = delegate.save(descriptor, id, data)

        override fun <T : ManagedData<T, *>> rename(
            descriptor: EditorDataDescriptor<T>,
            oldId: String,
            newId: String
        ): StoreResult<Unit> = delegate.rename(descriptor, oldId, newId)

        override fun <T : ManagedData<T, *>> delete(
            descriptor: EditorDataDescriptor<T>,
            id: String
        ): StoreResult<Unit> = delegate.delete(descriptor, id)
    }
}
