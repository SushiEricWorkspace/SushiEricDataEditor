package io.github.toumokorosi01.sushiericdataeditor2.editor.session

import io.github.toumokorosi01.sushiericdataeditor2.communication.SshManager
import io.github.toumokorosi01.sushiericdataeditor2.editor.service.EditorDataService
import kotlin.properties.ReadOnlyProperty

object EditorSession {
    var sshManager = SshManager()
    var dataService: EditorDataService? = null

    fun disconnect() {
        // nullチェックを入れて、接続が生きている時だけ切断する
        sshManager.disconnect()
        dataService = null
    }
}

fun <T : Any> sessionValue(provider: () -> T?): ReadOnlyProperty<Any, T?> =
    ReadOnlyProperty { _, _ ->
        provider() // null ならそのまま null を返す
    }