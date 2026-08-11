package io.github.rs0325.sushiericdataeditor2.editor.store

import io.github.rs0325.common.data.item.data.ItemData
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LocalEditorDataStoreTest {
    @Test
    fun `初回作成とCRUDを実行する`() {
        val root = createTempDirectory("offline-store").toFile()
        try {
            val store = LocalEditorDataStore(root)
            assertIs<StoreResult.Success<Unit>>(store.ensureDirectories())
            val item = validItem("sword")

            assertIs<StoreResult.Success<Unit>>(store.save(EditorDataDescriptors.item, "sword", item))
            assertTrue(root.resolve("item_data/stats/sword.yml").isFile)
            assertEquals(
                "Sword",
                assertIs<StoreResult.Success<ItemData>>(
                    store.load(EditorDataDescriptors.item, "sword")
                ).value.display.displayName
            )
            assertIs<StoreResult.Success<Unit>>(
                store.rename(EditorDataDescriptors.item, "sword", "renamed_sword")
            )
            assertFalse(root.resolve("item_data/stats/sword.yml").exists())
            assertTrue(root.resolve("item_data/stats/renamed_sword.yml").isFile)
            assertIs<StoreResult.Success<Unit>>(
                store.delete(EditorDataDescriptors.item, "renamed_sword")
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `パストラバーサルを拒否する`() {
        val root = createTempDirectory("offline-store-invalid").toFile()
        try {
            val result = LocalEditorDataStore(root).save(
                EditorDataDescriptors.item,
                "../outside",
                validItem("../outside")
            )
            assertEquals(StoreErrorCode.INVALID_ID, assertIs<StoreResult.Failure>(result).error.code)
            assertFalse(root.parentFile.resolve("outside.yml").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun validItem(id: String): ItemData = ItemData(id = id).apply {
        display.displayName = "Sword"
    }
}
