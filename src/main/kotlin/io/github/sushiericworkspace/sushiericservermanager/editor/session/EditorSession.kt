package io.github.sushiericworkspace.sushiericservermanager.editor.session

import io.github.sushiericworkspace.sushiericservermanager.communication.SshManager
import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementClient
import io.github.sushiericworkspace.sushiericservermanager.app.AppMode
import io.github.sushiericworkspace.sushiericservermanager.editor.service.EditorDataService
import io.github.sushiericworkspace.sushiericservermanager.editor.store.EditorDataStore
import kotlin.properties.ReadOnlyProperty

object EditorSession {
    var sshManager = SshManager()

    /**
     * Management APIのクライアントです。
     *
     * SSH接続の確立後に非同期で接続します。接続できなくてもSSHとSFTPは
     * 利用できるため、失敗しても以降の処理を止めません。
     */
    val managementClient = ServerManagementClient()
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

        connectManagementApi()
    }

    /**
     * Management APIへ非同期で接続します。
     *
     * 接続には待ち時間があるため、UIスレッドを止めないよう別スレッドで行います。
     * 結果は[ServerManagementClient.state]と状態リスナーで受け取ります。
     */
    private fun connectManagementApi() {
        val client = sshManager.sshClient
            ?: return

        Thread({
            managementClient.connect(client)
        }, "management-api-connect").apply {
            isDaemon = true
            start()
        }
    }

    fun startOfflineSession(store: EditorDataStore) {
        mode = AppMode.OFFLINE
        dataService = EditorDataService(store)
    }

    fun disconnect() {
        if (mode != AppMode.OFFLINE) {
            /*
             * SSHを切るとTunnelも使用できなくなるため、
             * Management APIを先に閉じてTunnelを解放する。
             */
            managementClient.disconnect()
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
