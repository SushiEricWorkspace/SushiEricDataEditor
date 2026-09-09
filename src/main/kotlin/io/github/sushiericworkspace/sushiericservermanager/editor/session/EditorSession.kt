package io.github.sushiericworkspace.sushiericservermanager.editor.session

import io.github.sushiericworkspace.sushiericservermanager.communication.SshManager
import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementClient
import io.github.sushiericworkspace.sushiericservermanager.config.AppSettingsManager
import io.github.sushiericworkspace.sushiericservermanager.config.ServerProfile
import io.github.sushiericworkspace.sushiericservermanager.monitor.ServerMonitor
import io.github.sushiericworkspace.sushiericservermanager.app.AppMode
import io.github.sushiericworkspace.sushiericservermanager.editor.service.EditorDataService
import io.github.sushiericworkspace.sushiericservermanager.editor.store.EditorDataStore
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty

object EditorSession {
    private val logger = LoggerFactory.getLogger(EditorSession::class.java)

    var sshManager = SshManager()

    /**
     * Management APIのクライアントです。
     *
     * SSH接続の確立後に非同期で接続します。接続できなくてもSSHとSFTPは
     * 利用できるため、失敗しても以降の処理を止めません。
     */
    val managementClient = ServerManagementClient()

    /**
     * 監視情報の購読と保持を行います。
     *
     * Management APIへ接続できた場合だけ購読を開始します。
     */
    val serverMonitor = ServerMonitor(managementClient)
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

        val profile = sshManager.currentProfile

        val remotePort =
            profile
                ?.resolvedManagementPort()
                ?: ServerProfile.DEFAULT_MANAGEMENT_PORT

        logger.info(
            "Management APIへ接続します: profile={} managementPort={}",
            profile?.name,
            remotePort
        )

        Thread({
            if (managementClient.connect(client, remotePort)) {
                serverMonitor.start(
                    AppSettingsManager.load().resolvedMonitorIntervalTicks()
                )
            }
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
            /*
             * 購読を止めてから接続を閉じる。
             * 接続を先に閉じるとunsubscribeを送れなくなる。
             */
            serverMonitor.stop()
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
