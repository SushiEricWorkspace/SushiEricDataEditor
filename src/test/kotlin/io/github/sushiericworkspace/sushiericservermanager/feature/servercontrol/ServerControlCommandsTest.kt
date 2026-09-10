package io.github.sushiericworkspace.sushiericservermanager.feature.servercontrol

import io.github.sushiericworkspace.sushiericservermanager.config.ServerControlCommandSet
import io.github.sushiericworkspace.sushiericservermanager.config.ServerControlCommandsExport
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * サーバー操作コマンドの取り出しと、受け渡し用ファイルの読み書きを検証する。
 */
class ServerControlCommandsTest {

    @Test
    fun `登録されたコマンドを操作ごとに取り出す`() {
        val commands =
            ServerControlCommandSet(
                startCommand = "systemctl start minecraft",
                stopCommand = "systemctl stop minecraft",
                restartCommand = "systemctl restart minecraft"
            )

        assertEquals(
            "systemctl start minecraft",
            ServerControlCommand.START.commandOf(commands)
        )
        assertEquals(
            "systemctl stop minecraft",
            ServerControlCommand.STOP.commandOf(commands)
        )
        assertEquals(
            "systemctl restart minecraft",
            ServerControlCommand.RESTART.commandOf(commands)
        )
    }

    @Test
    fun `空白だけのコマンドは未設定として扱う`() {
        val commands =
            ServerControlCommandSet(
                startCommand = "   ",
                stopCommand = "",
                restartCommand = "systemctl restart minecraft"
            )

        assertNull(ServerControlCommand.START.commandOf(commands))
        assertFalse(ServerControlCommand.START.isConfigured(commands))

        assertNull(ServerControlCommand.STOP.commandOf(commands))
        assertTrue(ServerControlCommand.RESTART.isConfigured(commands))
    }

    @Test
    fun `プロファイルが無い場合も未設定として扱う`() {
        ServerControlCommand.entries.forEach { control ->
            assertNull(control.commandOf(null))
            assertFalse(control.isConfigured(null))
        }
    }

    @Test
    fun `前後の空白を取り除いて保持する`() {
        val normalized =
            ServerControlCommandSet(
                startCommand = "  start.sh  ",
                stopCommand = "\tstop.sh\n",
                restartCommand = ""
            ).normalized()

        assertEquals("start.sh", normalized.startCommand)
        assertEquals("stop.sh", normalized.stopCommand)
        assertEquals("", normalized.restartCommand)
        assertTrue(normalized.hasAnyCommand)
    }

    @Test
    fun `すべて空の場合はコマンドを持たないと判定する`() {
        assertFalse(ServerControlCommandSet.EMPTY.hasAnyCommand)
        assertFalse(ServerControlCommandSet(startCommand = "  ").normalized().hasAnyCommand)
    }

    @Test
    fun `書き出したファイルを読み込める`() {
        val file = Files.createTempFile("server-control", ".json").toFile()
        file.deleteOnExit()

        val commands =
            ServerControlCommandSet(
                startCommand = "start.sh",
                stopCommand = "stop.sh",
                restartCommand = "restart.sh"
            )

        assertTrue(ServerControlCommandsIo.export(file, commands))

        assertEquals(commands, ServerControlCommandsIo.import(file))
    }

    @Test
    fun `書き出したファイルにプロファイル名を含めない`() {
        val file = Files.createTempFile("server-control", ".json").toFile()
        file.deleteOnExit()

        ServerControlCommandsIo.export(
            file,
            ServerControlCommandSet(startCommand = "start.sh")
        )

        val text = file.readText()

        assertTrue(text.contains("formatVersion"))
        assertTrue(text.contains("startCommand"))
        assertFalse(text.contains("name"))
        assertFalse(text.contains("host"))
    }

    @Test
    fun `対応していない形式は読み込まない`() {
        val file = Files.createTempFile("server-control", ".json").toFile()
        file.deleteOnExit()

        val unsupportedVersion = ServerControlCommandsExport.FORMAT_VERSION + 1
        file.writeText("""{"formatVersion":$unsupportedVersion,"startCommand":"start.sh"}""")

        assertNull(ServerControlCommandsIo.import(file))
    }

    @Test
    fun `壊れたファイルは読み込まない`() {
        val file = Files.createTempFile("server-control", ".json").toFile()
        file.deleteOnExit()

        file.writeText("これはJSONではありません")

        assertNull(ServerControlCommandsIo.import(file))
    }

    @Test
    fun `未設定の項目を含む定義も読み込める`() {
        val file = Files.createTempFile("server-control", ".json").toFile()
        file.deleteOnExit()

        file.writeText(
            """{"formatVersion":${ServerControlCommandsExport.FORMAT_VERSION},"startCommand":"start.sh"}"""
        )

        val imported = ServerControlCommandsIo.import(file)

        assertEquals("start.sh", imported?.startCommand)
        assertEquals("", imported?.stopCommand)
        assertEquals("", imported?.restartCommand)
    }
}
