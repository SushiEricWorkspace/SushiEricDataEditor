package io.github.sushiericworkspace.sushiericdataeditor2.editor.store

import io.github.sushiericworkspace.common.data.item.model.ItemBaseData
import io.github.sushiericworkspace.common.data.item.model.PlainTextLoreSection
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
                assertIs<StoreResult.Success<ItemBaseData>>(
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

    @Test
    fun `internalIdがない既存Itemを保存すると生成した値を維持する`() {
        val root = createTempDirectory("offline-store-legacy-item").toFile()
        try {
            val store = LocalEditorDataStore(root)
            val descriptor = EditorDataDescriptors.item
            val item = validItem("legacy_sword")
            assertIs<StoreResult.Success<Unit>>(store.save(descriptor, "legacy_sword", item))

            val file = root.resolve("item_data/stats/legacy_sword.yml")
            file.writeText(
                file.readLines()
                    .filterNot { it.trimStart().startsWith("internal-id:") }
                    .joinToString(System.lineSeparator(), postfix = System.lineSeparator())
            )

            val migrated = assertIs<StoreResult.Success<ItemBaseData>>(
                store.load(descriptor, "legacy_sword")
            ).value
            assertIs<StoreResult.Success<Unit>>(store.save(descriptor, "legacy_sword", migrated))
            val reloaded = assertIs<StoreResult.Success<ItemBaseData>>(
                store.load(descriptor, "legacy_sword")
            ).value

            assertEquals(migrated.internalId, reloaded.internalId)
            assertTrue(file.readText().lineSequence().any { it.startsWith("internal-id:") })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `複製したItemのLoreを保存して再読込できる`() {
        val root = createTempDirectory("offline-store-duplicated-item").toFile()
        try {
            val store = LocalEditorDataStore(root)
            val descriptor = EditorDataDescriptors.item
            val source = validItem("source").apply {
                display.lore.add(mutableListOf(PlainTextLoreSection("複製対象のLore")))
            }
            val duplicate = descriptor.duplicateAsNew(source, "duplicate")

            assertIs<StoreResult.Success<Unit>>(store.save(descriptor, "duplicate", duplicate))
            val reloaded = assertIs<StoreResult.Success<ItemBaseData>>(
                store.load(descriptor, "duplicate")
            ).value

            assertEquals(
                "複製対象のLore",
                (reloaded.display.lore.single().single() as PlainTextLoreSection).text
            )
        } finally {
            root.deleteRecursively()
        }
    }

    private fun validItem(id: String): ItemBaseData = ItemBaseData(id = id).apply {
        display.displayName = "Sword"
    }
}
