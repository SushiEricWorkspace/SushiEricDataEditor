package io.github.sushiericworkspace.sushiericdataeditor2.editor.session

import io.github.sushiericworkspace.sushiericdataeditor2.communication.SshManager
import io.github.sushiericworkspace.sushiericdataeditor2.app.AppMode
import io.github.sushiericworkspace.sushiericdataeditor2.editor.service.EditorDataService
import io.github.sushiericworkspace.sushiericdataeditor2.editor.store.EditorDataStore
import kotlin.properties.ReadOnlyProperty

object EditorSession {
    var sshManager = SshManager()
    var dataService: EditorDataService? = null
        private set

    var mode: AppMode? = null
        private set

    fun prepareOnlineMode() {
        mode = AppMode.ONLINE
        dataService = null
    }

    fun startOnlineSession() {
        mode = AppMode.ONLINE
        dataService = EditorDataService(sshManager)
    }

    fun startOfflineSession(store: EditorDataStore) {
        mode = AppMode.OFFLINE
        dataService = EditorDataService(store)
    }

    fun disconnect() {
        if (mode != AppMode.OFFLINE) {
            sshManager.disconnect()
        }
        dataService = null
    }

    fun resetMode() {
        disconnect()
        mode = null
    }
}

fun <T : Any> sessionValue(provider: () -> T?): ReadOnlyProperty<Any, T?> =
    ReadOnlyProperty { _, _ ->
        provider() // null ならそのまま null を返す
    }
