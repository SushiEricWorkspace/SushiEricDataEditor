package io.github.sushiericworkspace.sushiericservermanager.feature.console

import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementRequest
import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementResponse
import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementState
import io.github.sushiericworkspace.sushiericservermanager.editor.session.EditorSession
import javafx.animation.AnimationTimer
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Orientation
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ScrollBar
import javafx.scene.layout.BorderPane
import java.net.URL
import java.util.ResourceBundle

/** Management APIから受信したサーバーログを表示するコンソール画面です。 */
class ConsoleController : Initializable {
    @FXML private lateinit var rootPane: BorderPane
    @FXML private lateinit var connectionLabel: Label
    @FXML private lateinit var logListView: ListView<ConsoleLogEntry>

    private val managementClient = EditorSession.managementClient
    private val incomingLogs = ConsoleLogBuffer(INCOMING_LOG_LIMIT)
    private val displayedLogs = FXCollections.observableArrayList<ConsoleLogEntry>()
    private var subscribed = false
    private var disposed = false
    private val autoScrollPolicy = ConsoleAutoScrollPolicy()
    private var adjustingScroll = false
    private var verticalScrollBar: ScrollBar? = null

    private val stateListener: (ServerManagementState) -> Unit = { state ->
        Platform.runLater { applyConnectionState(state) }
    }
    private val messageListener: (ServerManagementResponse) -> Unit = { response ->
        if (response is ServerManagementResponse.ConsoleLog) {
            incomingLogs.offer(ConsoleLogEntry.from(response))
        }
    }
    private val flushTimer = object : AnimationTimer() {
        override fun handle(now: Long) = flushIncomingLogs()
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        logListView.items = displayedLogs
        logListView.setCellFactory {
            object : ListCell<ConsoleLogEntry>() {
                init {
                    isWrapText = true
                }

                override fun updateItem(item: ConsoleLogEntry?, empty: Boolean) {
                    super.updateItem(item, empty)
                    LEVEL_STYLE_CLASSES.forEach(styleClass::remove)
                    text = if (empty) null else item?.displayText
                    if (!empty && item != null) styleClass.add(levelStyleClass(item.normalizedLevel))
                }
            }
        }

        managementClient.addStateListener(stateListener)
        managementClient.addMessageListener(messageListener)
        applyConnectionState(managementClient.state)
        flushTimer.start()
        Platform.runLater(::attachScrollTracking)
    }

    /** 画面を閉じる際に購読とリスナーを解除します。 */
    fun dispose() {
        if (disposed) return
        disposed = true
        flushTimer.stop()
        if (subscribed && managementClient.isConnected) {
            managementClient.send(ServerManagementRequest.ConsoleUnsubscribe)
        }
        subscribed = false
        managementClient.removeStateListener(stateListener)
        managementClient.removeMessageListener(messageListener)
        incomingLogs.clear()
    }

    private fun applyConnectionState(state: ServerManagementState) {
        if (disposed) return
        when (state) {
            is ServerManagementState.Connected -> {
                if (!subscribed) {
                    subscribed = managementClient.send(ServerManagementRequest.ConsoleSubscribe)
                }
                connectionLabel.text = if (subscribed) {
                    "Management API：ログ購読中"
                } else {
                    "Management API：ログ購読を開始できませんでした"
                }
            }
            is ServerManagementState.Connecting -> {
                subscribed = false
                connectionLabel.text = "Management API：接続中..."
            }
            is ServerManagementState.Disconnected -> {
                subscribed = false
                connectionLabel.text = "Management API：未接続"
            }
            is ServerManagementState.Failed -> {
                subscribed = false
                connectionLabel.text = "Management API：未接続（${state.reason}）"
            }
        }
    }

    private fun flushIncomingLogs() {
        val added = incomingLogs.drain(LOGS_PER_FRAME)
        if (added.isEmpty()) return

        val shouldAutoScroll = autoScrollPolicy.isEnabled
        if (shouldAutoScroll) adjustingScroll = true
        try {
            appendConsoleLogs(displayedLogs, added, DISPLAY_LOG_LIMIT)
            attachScrollTracking()
            if (shouldAutoScroll && displayedLogs.isNotEmpty()) {
                logListView.scrollTo(displayedLogs.lastIndex)
            }
        } finally {
            adjustingScroll = false
        }
    }

    private fun attachScrollTracking() {
        if (verticalScrollBar != null) return
        val scrollBar = logListView.lookupAll(".scroll-bar")
            .filterIsInstance<ScrollBar>()
            .firstOrNull { it.orientation == Orientation.VERTICAL }
            ?: return

        verticalScrollBar = scrollBar
        scrollBar.valueProperty().addListener { _, _, value ->
            if (!adjustingScroll) {
                autoScrollPolicy.update(
                    current = value.toDouble(),
                    maximum = scrollBar.max,
                    scrollBarVisible = scrollBar.isVisible
                )
            }
        }
    }

    companion object {
        /** 画面上に保持するログの最大件数です。 */
        const val DISPLAY_LOG_LIMIT = 1_000
        internal const val INCOMING_LOG_LIMIT = 2_000
        internal const val LOGS_PER_FRAME = 200
        private val LEVEL_STYLE_CLASSES = setOf(
            "console-log-trace", "console-log-debug", "console-log-info",
            "console-log-warn", "console-log-error"
        )

        internal fun levelStyleClass(level: String): String = when (level) {
            "TRACE" -> "console-log-trace"
            "DEBUG" -> "console-log-debug"
            "WARN" -> "console-log-warn"
            "ERROR", "FATAL" -> "console-log-error"
            else -> "console-log-info"
        }
    }
}
