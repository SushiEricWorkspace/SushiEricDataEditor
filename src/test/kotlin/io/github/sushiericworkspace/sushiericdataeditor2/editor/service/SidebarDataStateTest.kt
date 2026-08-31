package io.github.sushiericworkspace.sushiericdataeditor2.editor.service

import io.github.sushiericworkspace.sushiericdataeditor2.editor.view.SidebarDataState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SidebarDataStateTest {
    @Test
    fun `通常状態では装飾を追加しない`() {
        val state = SidebarDataState(selected = false, modified = false, invalid = false)

        assertEquals(emptyList(), state.styleClasses)
        assertEquals("sample", state.displayText("sample"))
        assertNull(state.description())
    }

    @Test
    fun `選択と変更と不正の状態を同時に保持する`() {
        val state = SidebarDataState(selected = true, modified = true, invalid = true)

        assertEquals(
            listOf("button-selected", "button-modified", "button-invalid"),
            state.styleClasses
        )
        assertEquals("⚠ sample  ●", state.displayText("sample"))
        assertEquals("選択中 / 未保存の変更あり / 入力内容に問題あり", state.description())
    }

    @Test
    fun `不正状態は変更状態がなくても表示する`() {
        val state = SidebarDataState(selected = false, modified = false, invalid = true)

        assertEquals(listOf("button-invalid"), state.styleClasses)
        assertEquals("⚠ sample", state.displayText("sample"))
        assertEquals("入力内容に問題あり", state.description())
    }

    @Test
    fun `各状態の組み合わせで該当する装飾だけを保持する`() {
        val combinations = listOf(
            SidebarDataState(false, false, false) to emptyList(),
            SidebarDataState(true, false, false) to listOf("button-selected"),
            SidebarDataState(false, true, false) to listOf("button-modified"),
            SidebarDataState(false, false, true) to listOf("button-invalid"),
            SidebarDataState(true, true, false) to listOf("button-selected", "button-modified"),
            SidebarDataState(true, false, true) to listOf("button-selected", "button-invalid"),
            SidebarDataState(false, true, true) to listOf("button-modified", "button-invalid"),
            SidebarDataState(true, true, true) to
                listOf("button-selected", "button-modified", "button-invalid")
        )

        combinations.forEach { (state, expected) ->
            assertEquals(expected, state.styleClasses)
        }
    }
}
