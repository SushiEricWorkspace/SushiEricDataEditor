package io.github.sushiericworkspace.sushiericdataeditor2.editor.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorDataDescriptorTest {
    @Test
    fun `Itemの初期表示名にはIDが設定される`() {
        val data = EditorDataDescriptors.item.createDefault("new_item")

        assertEquals("new_item", data.display.displayName)
        assertTrue(EditorDataDescriptors.item.validate(data, emptySet()).isEmpty())
    }

    @Test
    fun `Itemの表示名を空にするとバリデーションエラーになる`() {
        val data = EditorDataDescriptors.item.createDefault("new_item").apply {
            display.displayName = " "
        }

        assertTrue(EditorDataDescriptors.item.validate(data, emptySet()).isNotEmpty())
    }

    @Test
    fun `管理対象はItemとOreだけである`() {
        assertEquals(
            listOf(EditorDataDescriptors.item, EditorDataDescriptors.ore),
            EditorDataDescriptors.all
        )
    }
}
