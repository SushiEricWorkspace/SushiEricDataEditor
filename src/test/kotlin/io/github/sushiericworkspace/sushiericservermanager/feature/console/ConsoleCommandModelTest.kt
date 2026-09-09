package io.github.sushiericworkspace.sushiericservermanager.feature.console

import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementCommandSuggestion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConsoleCommandModelTest {
    @Test
    fun `サーバーが返した範囲だけを補完候補で置換する`() {
        val result = ConsoleCommandModel().applySuggestion(
            command = "gamemode cre Steve",
            suggestion = ServerManagementCommandSuggestion(
                text = "creative",
                start = 9,
                end = 12
            )
        )

        assertEquals(
            ConsoleCompletionApplication(
                text = "gamemode creative Steve",
                caretPosition = 17
            ),
            result
        )
    }

    @Test
    fun `不正な置換範囲は適用しない`() {
        val model = ConsoleCommandModel()

        assertNull(
            model.applySuggestion(
                "say hello",
                ServerManagementCommandSuggestion("world", -1, 3)
            )
        )
        assertNull(
            model.applySuggestion(
                "say hello",
                ServerManagementCommandSuggestion("world", 4, 99)
            )
        )
    }

    @Test
    fun `上下操作で履歴と操作前の入力を往復する`() {
        val model = ConsoleCommandModel()
        model.record("say first")
        model.record("say second")

        assertEquals("say second", model.previous("say draft"))
        assertEquals("say first", model.previous("say second"))
        assertEquals("say second", model.next())
        assertEquals("say draft", model.next())
        assertNull(model.next())
    }

    @Test
    fun `連続して同じコマンドを実行しても履歴は重複しない`() {
        val model = ConsoleCommandModel()
        model.record("list")
        model.record("list")

        assertEquals("list", model.previous(""))
        assertEquals("", model.next())
    }
}
