package io.github.sushiericworkspace.sushiericservermanager.feature.console

import io.github.sushiericworkspace.sushiericservermanager.app.AppScreen
import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementCommandSuggestion
import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementRequest
import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementResponse
import io.github.sushiericworkspace.sushiericservermanager.communication.management.ServerManagementState
import io.github.sushiericworkspace.sushiericservermanager.editor.session.EditorSession
import javafx.animation.AnimationTimer
import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Bounds
import javafx.geometry.Orientation
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ScrollBar
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.stage.Popup
import javafx.util.Duration
import java.net.URL
import java.util.ResourceBundle
import java.util.UUID

/** Management APIを使用するMinecraftコンソール画面を管理します。 */
class ConsoleController : Initializable {
    @FXML private lateinit var rootPane: BorderPane
    @FXML private lateinit var connectionLabel: Label
    @FXML private lateinit var outputListView: ListView<ConsoleOutputEntry>
    @FXML private lateinit var commandField: TextField

    private val client = EditorSession.managementClient
    private val commandModel = ConsoleCommandModel()
    private val completionDelay = PauseTransition(Duration.millis(COMPLETION_DELAY_MILLIS))
    private val suggestionPopup = Popup()
    private val suggestionList = ListView<ServerManagementCommandSuggestion>()
    private val pendingCommands = mutableMapOf<String, String>()
    private val incomingLogs = ConsoleLogBuffer(INCOMING_LOG_LIMIT)
    private val autoScrollPolicy = ConsoleAutoScrollPolicy()
    private var pendingCompletion: PendingCompletion? = null
    private var suppressInputListener = false
    private var subscribedToLogs = false
    private var verticalScrollBar: ScrollBar? = null

    private val logDrainTimer = object : AnimationTimer() {
        override fun handle(now: Long) {
            val logs = incomingLogs.drain(LOGS_PER_FRAME)
            if (logs.isEmpty()) return
            appendEntries(
                logs.map { log ->
                    ConsoleOutputEntry(
                        text = log.displayText,
                        styleClass = levelStyleClass(log.normalizedLevel)
                    )
                }
            )
        }
    }

    private val stateListener: (ServerManagementState) -> Unit = { state ->
        runOnFxThread { applyConnectionState(state) }
    }
    private val messageListener: (ServerManagementResponse) -> Unit = { message ->
        if (message is ServerManagementResponse.ConsoleLog) {
            incomingLogs.offer(ConsoleLogEntry.from(message))
        } else {
            runOnFxThread { receive(message) }
        }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        configureOutputList()
        configureSuggestionPopup()
        configureCommandInput()
        client.addStateListener(stateListener)
        client.addMessageListener(messageListener)
        applyConnectionState(client.state)
        logDrainTimer.start()
    }

    private fun configureOutputList() {
        outputListView.setCellFactory {
            object : ListCell<ConsoleOutputEntry>() {
                override fun updateItem(item: ConsoleOutputEntry?, empty: Boolean) {
                    super.updateItem(item, empty)
                    styleClass.removeAll(DISPLAY_STYLE_CLASSES)
                    text = if (empty || item == null) null else item.text
                    item?.styleClass?.let(styleClass::add)
                }
            }
        }
        Platform.runLater {
            verticalScrollBar = outputListView.lookupAll(".scroll-bar")
                .filterIsInstance<ScrollBar>()
                .firstOrNull { it.orientation == Orientation.VERTICAL }
                ?.also { scrollBar ->
                    scrollBar.valueProperty().addListener { _, _, value ->
                        autoScrollPolicy.update(
                            current = value.toDouble(),
                            maximum = scrollBar.max,
                            scrollBarVisible = scrollBar.isVisible
                        )
                    }
                    scrollBar.visibleProperty().addListener { _, _, visible ->
                        autoScrollPolicy.update(
                            current = scrollBar.value,
                            maximum = scrollBar.max,
                            scrollBarVisible = visible
                        )
                    }
                }
        }
    }

    private fun configureSuggestionPopup() {
        suggestionList.styleClass.add("console-suggestion-list")
        suggestionList.stylesheets.add(
            requireNotNull(javaClass.getResource(AppScreen.CONSOLE.css)).toExternalForm()
        )
        suggestionList.setCellFactory {
            object : ListCell<ServerManagementCommandSuggestion>() {
                override fun updateItem(item: ServerManagementCommandSuggestion?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else item.text
                    tooltip = item?.tooltip?.takeIf(String::isNotBlank)?.let(::Tooltip)
                }
            }
        }
        suggestionList.setOnMouseClicked { event ->
            if (event.button == MouseButton.PRIMARY && !suggestionList.selectionModel.isEmpty) {
                applySelectedSuggestion()
            }
        }
        suggestionPopup.isAutoHide = true
        suggestionPopup.isHideOnEscape = true
        suggestionPopup.content.add(suggestionList)
        suggestionPopup.scene.addEventFilter(KeyEvent.KEY_PRESSED, ::handlePopupKeyPressed)
    }

    private fun configureCommandInput() {
        completionDelay.setOnFinished { requestCompletion() }
        commandField.textProperty().addListener { _, _, text ->
            if (suppressInputListener) return@addListener
            commandModel.resetHistoryNavigation()
            scheduleCompletion(text)
        }
        commandField.addEventFilter(KeyEvent.KEY_PRESSED, ::handleKeyPressed)
    }

    private fun handleKeyPressed(event: KeyEvent) {
        when (event.code) {
            KeyCode.ENTER -> executeCommand()
            KeyCode.TAB -> {
                if (suggestionPopup.isShowing && !suggestionList.items.isEmpty()) {
                    applySelectedSuggestion()
                } else {
                    completionDelay.stop()
                    requestCompletion()
                }
            }
            KeyCode.UP -> {
                if (suggestionPopup.isShowing) {
                    moveSuggestionSelection(-1)
                } else {
                    commandModel.previous(commandField.text)?.let(::replaceCommandText)
                }
            }
            KeyCode.DOWN -> {
                if (suggestionPopup.isShowing) {
                    moveSuggestionSelection(1)
                } else {
                    commandModel.next()?.let(::replaceCommandText)
                }
            }
            KeyCode.ESCAPE -> cancelCompletion()
            KeyCode.LEFT,
            KeyCode.RIGHT,
            KeyCode.HOME,
            KeyCode.END -> {
                cancelCompletion()
                return
            }
            else -> return
        }
        event.consume()
    }

    private fun handlePopupKeyPressed(event: KeyEvent) {
        when (event.code) {
            KeyCode.ENTER -> executeCommand()
            KeyCode.TAB -> applySelectedSuggestion()
            KeyCode.UP -> moveSuggestionSelection(-1)
            KeyCode.DOWN -> moveSuggestionSelection(1)
            KeyCode.ESCAPE -> cancelCompletion()
            KeyCode.LEFT,
            KeyCode.RIGHT,
            KeyCode.HOME,
            KeyCode.END -> moveCaretFromPopup(event.code)
            else -> return
        }
        event.consume()
    }

    private fun moveCaretFromPopup(keyCode: KeyCode) {
        cancelCompletion()
        commandField.requestFocus()
        val current = commandField.caretPosition
        val destination = when (keyCode) {
            KeyCode.LEFT -> current - 1
            KeyCode.RIGHT -> current + 1
            KeyCode.HOME -> 0
            KeyCode.END -> commandField.text.length
            else -> current
        }
        commandField.positionCaret(destination.coerceIn(0, commandField.text.length))
    }

    private fun executeCommand() {
        hideSuggestions()
        completionDelay.stop()
        val command = commandField.text.trim()
        if (command.isEmpty()) return
        if (!client.isConnected) {
            appendOutput("Management APIへ接続していないため実行できません。", COMMAND_ERROR_STYLE)
            applyConnectionState(client.state)
            return
        }

        val nonce = UUID.randomUUID().toString()
        val sent = client.send(ServerManagementRequest.CommandExecute(command, nonce))
        if (!sent) {
            appendOutput("コマンドを送信できませんでした。", COMMAND_ERROR_STYLE)
            applyConnectionState(client.state)
            return
        }

        pendingCommands[nonce] = command
        appendOutput("> $command", COMMAND_STYLE)
        commandModel.record(command)
        replaceCommandText("")
    }

    private fun scheduleCompletion(text: String) {
        pendingCompletion = null
        if (text.isBlank() || !client.isConnected) {
            completionDelay.stop()
            hideSuggestions()
            return
        }
        completionDelay.playFromStart()
    }

    private fun requestCompletion() {
        val command = commandField.text
        if (command.isBlank() || !client.isConnected) {
            hideSuggestions()
            return
        }

        val cursor = commandField.caretPosition
        val nonce = UUID.randomUUID().toString()
        val request = PendingCompletion(nonce, command, cursor)
        pendingCompletion = request
        if (!client.send(ServerManagementRequest.CommandComplete(command, cursor, nonce))) {
            pendingCompletion = null
            hideSuggestions()
            applyConnectionState(client.state)
        }
    }

    private fun receive(message: ServerManagementResponse) {
        when (message) {
            is ServerManagementResponse.CommandResult -> showCommandResult(message)
            is ServerManagementResponse.CommandCompleteResult -> showCompletionResult(message)
            is ServerManagementResponse.ConsoleLog -> Unit
            is ServerManagementResponse.Error -> {
                appendOutput(
                    "Management APIエラー: ${message.reason}${message.detail?.let { " ($it)" }.orEmpty()}",
                    COMMAND_ERROR_STYLE
                )
                hideSuggestions()
            }
            is ServerManagementResponse.Pong -> Unit
        }
    }

    private fun showCommandResult(result: ServerManagementResponse.CommandResult) {
        val nonce = result.nonce ?: return
        if (pendingCommands.remove(nonce) == null) return

        if (result.output.isEmpty()) {
            val status = if (result.success) "成功" else "失敗"
            val returnValue = result.returnValue?.let { "（戻り値: $it）" }.orEmpty()
            appendOutput(
                "$status$returnValue",
                if (result.success) COMMAND_SUCCESS_STYLE else COMMAND_ERROR_STYLE
            )
        } else {
            val styleClass = if (result.success) COMMAND_OUTPUT_STYLE else COMMAND_ERROR_STYLE
            result.output.forEach { appendOutput(it, styleClass) }
        }
    }

    private fun showCompletionResult(result: ServerManagementResponse.CommandCompleteResult) {
        val request = pendingCompletion ?: return
        if (result.nonce != request.nonce) return
        pendingCompletion = null
        if (commandField.text != request.command || commandField.caretPosition != request.cursor) return

        suggestionList.items.setAll(result.suggestions)
        if (result.suggestions.isEmpty()) {
            hideSuggestions()
            return
        }
        suggestionList.selectionModel.selectFirst()
        showSuggestions()
    }

    private fun showSuggestions() {
        val bounds: Bounds = commandField.localToScreen(commandField.boundsInLocal) ?: return
        suggestionList.prefWidth = commandField.width.coerceAtLeast(MINIMUM_POPUP_WIDTH)
        suggestionList.prefHeight =
            (suggestionList.items.size.coerceAtMost(MAXIMUM_VISIBLE_SUGGESTIONS) * SUGGESTION_ROW_HEIGHT)
                .coerceAtLeast(SUGGESTION_ROW_HEIGHT)
        if (suggestionPopup.isShowing) {
            suggestionPopup.x = bounds.minX
            suggestionPopup.y = bounds.maxY
        } else {
            suggestionPopup.show(commandField, bounds.minX, bounds.maxY)
        }
        if (suggestionList.selectionModel.isEmpty) {
            suggestionList.selectionModel.selectFirst()
        }
    }

    private fun hideSuggestions() {
        suggestionPopup.hide()
        suggestionList.items.clear()
    }

    private fun cancelCompletion() {
        completionDelay.stop()
        pendingCompletion = null
        hideSuggestions()
    }

    private fun moveSuggestionSelection(delta: Int) {
        val size = suggestionList.items.size
        if (size == 0) return
        val current = suggestionList.selectionModel.selectedIndex.coerceAtLeast(0)
        suggestionList.selectionModel.select((current + delta).coerceIn(0, size - 1))
        suggestionList.scrollTo(suggestionList.selectionModel.selectedIndex)
    }

    private fun applySelectedSuggestion() {
        val suggestion = commandModel.selectSuggestion(
            suggestionList.items,
            suggestionList.selectionModel.selectedIndex
        ) ?: return
        val applied = commandModel.applySuggestion(commandField.text, suggestion) ?: return
        hideSuggestions()
        replaceCommandText(applied.text, applied.caretPosition)
        commandField.requestFocus()
        scheduleCompletion(applied.text)
    }

    private fun replaceCommandText(text: String, caretPosition: Int = text.length) {
        suppressInputListener = true
        try {
            commandField.text = text
            commandField.positionCaret(caretPosition.coerceIn(0, text.length))
        } finally {
            suppressInputListener = false
        }
    }

    private fun appendOutput(text: String, styleClass: String = COMMAND_OUTPUT_STYLE) {
        appendEntries(listOf(ConsoleOutputEntry(text, styleClass)))
    }

    private fun appendEntries(entries: Collection<ConsoleOutputEntry>) {
        val shouldScroll = autoScrollPolicy.isEnabled
        appendConsoleLogs(outputListView.items, entries, MAXIMUM_OUTPUT_LINES)
        if (shouldScroll && outputListView.items.isNotEmpty()) {
            outputListView.scrollTo(outputListView.items.lastIndex)
        }
    }

    private fun applyConnectionState(state: ServerManagementState) {
        val connected = state is ServerManagementState.Connected
        commandField.isDisable = !connected
        commandField.promptText = if (connected) "コマンドを入力" else "Management APIへ接続していません"
        connectionLabel.text = when (state) {
            ServerManagementState.Disconnected -> "Management API：未接続"
            ServerManagementState.Connecting -> "Management API：接続中..."
            is ServerManagementState.Connected -> "Management API：接続済み"
            is ServerManagementState.Failed -> "Management API：未接続（${state.reason}）"
        }
        if (connected) {
            subscribeToLogs()
        } else {
            subscribedToLogs = false
            incomingLogs.clear()
            pendingCommands.clear()
            pendingCompletion = null
            completionDelay.stop()
            hideSuggestions()
        }
    }

    private fun subscribeToLogs() {
        if (!subscribedToLogs && client.send(ServerManagementRequest.ConsoleSubscribe)) {
            subscribedToLogs = true
        }
    }

    /** 画面が閉じられたとき、登録したリスナーとPopupを解放します。 */
    fun dispose() {
        if (subscribedToLogs && client.isConnected) {
            client.send(ServerManagementRequest.ConsoleUnsubscribe)
        }
        subscribedToLogs = false
        logDrainTimer.stop()
        incomingLogs.clear()
        completionDelay.stop()
        hideSuggestions()
        client.removeStateListener(stateListener)
        client.removeMessageListener(messageListener)
        pendingCommands.clear()
        pendingCompletion = null
    }

    private fun runOnFxThread(action: () -> Unit) {
        if (Platform.isFxApplicationThread()) action() else Platform.runLater(action)
    }

    private data class PendingCompletion(
        val nonce: String,
        val command: String,
        val cursor: Int
    )

    private data class ConsoleOutputEntry(
        val text: String,
        val styleClass: String
    )

    companion object {
        private const val COMPLETION_DELAY_MILLIS = 150.0
        private const val MINIMUM_POPUP_WIDTH = 320.0
        private const val MAXIMUM_VISIBLE_SUGGESTIONS = 8
        private const val SUGGESTION_ROW_HEIGHT = 28.0
        private const val MAXIMUM_OUTPUT_LINES = 1_000
        private const val INCOMING_LOG_LIMIT = 2_000
        private const val LOGS_PER_FRAME = 200

        private const val COMMAND_STYLE = "console-line-command"
        private const val COMMAND_OUTPUT_STYLE = "console-line-output"
        private const val COMMAND_SUCCESS_STYLE = "console-line-success"
        private const val COMMAND_ERROR_STYLE = "console-line-error"
        private val DISPLAY_STYLE_CLASSES = setOf(
            COMMAND_STYLE,
            COMMAND_OUTPUT_STYLE,
            COMMAND_SUCCESS_STYLE,
            COMMAND_ERROR_STYLE,
            "console-log-trace",
            "console-log-debug",
            "console-log-info",
            "console-log-warn",
            "console-log-error"
        )

        /** ログレベルに対応するCSSクラスを返します。 */
        fun levelStyleClass(level: String): String = when (level.uppercase()) {
            "TRACE" -> "console-log-trace"
            "DEBUG" -> "console-log-debug"
            "WARN" -> "console-log-warn"
            "ERROR", "FATAL" -> "console-log-error"
            else -> "console-log-info"
        }
    }
}
