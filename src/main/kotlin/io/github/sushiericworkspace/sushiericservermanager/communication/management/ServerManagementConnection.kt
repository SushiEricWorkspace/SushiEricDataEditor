package io.github.sushiericworkspace.sushiericservermanager.communication.management

import io.github.sushiericworkspace.sushiericservermanager.communication.management.codec.ServerManagementDecodeResult
import io.github.sushiericworkspace.sushiericservermanager.communication.management.codec.ServerManagementMessageCodec
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

/**
 * Management APIとのWebSocket接続1本です。
 *
 * 接続はSSH Tunnelのローカル側へ張ります。転送先はサーバーのループバックへ
 * 固定されているため、この接続が外部ホストへ向くことはありません。
 *
 * 受信したメッセージは[listener]へ渡します。復号できないメッセージは
 * 記録するだけで接続を維持します。サーバー側が新しい種別を送っても
 * 接続が落ちないようにするためです。
 */
class ServerManagementConnection internal constructor(
    private val listener: ServerManagementListener
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val buffer = StringBuilder()

    @Volatile
    private var webSocket: WebSocket? = null

    /** 接続が生存しているかを返します。 */
    val isOpen: Boolean
        get() = webSocket?.isInputClosed == false

    /**
     * 指定したローカルポートのManagement APIへ接続します。
     *
     * @param localPort SSH Tunnelのローカル側ポート。
     * @param timeout 接続の待ち時間。
     * @throws java.io.IOException 接続に失敗した場合。
     */
    internal fun connect(
        localPort: Int,
        timeout: Duration
    ) {
        val uri =
            URI.create("ws://$LOOPBACK:$localPort$PATH")

        webSocket =
            HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(timeout)
                .buildAsync(uri, Adapter())
                .get(timeout.toMillis(), TimeUnit.MILLISECONDS)
    }

    /**
     * メッセージを送信します。
     *
     * @param request 送信するメッセージ。
     * @return 送信できた場合は`true`。接続していない場合は`false`。
     */
    fun send(
        request: ServerManagementRequest
    ): Boolean {
        val socket =
            webSocket
                ?: return false

        return try {
            socket.sendText(
                ServerManagementMessageCodec.encode(request),
                true
            )

            true
        } catch (exception: Exception) {
            logger.warn("Management APIへの送信に失敗しました。", exception)
            false
        }
    }

    /** 接続を閉じます。 */
    fun close() {
        val socket =
            webSocket
                ?: return

        webSocket = null

        runCatching {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "closed by manager")
        }

        runCatching { socket.abort() }
    }

    /**
     * JDKのWebSocketコールバックを[ServerManagementListener]へ橋渡しします。
     */
    private inner class Adapter : WebSocket.Listener {

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(Long.MAX_VALUE)
        }

        override fun onText(
            webSocket: WebSocket,
            data: CharSequence,
            last: Boolean
        ): CompletionStage<*>? {
            buffer.append(data)

            if (!last) {
                return null
            }

            val text = buffer.toString()
            buffer.setLength(0)

            when (val result = ServerManagementMessageCodec.decode(text)) {
                is ServerManagementDecodeResult.Success ->
                    listener.onMessage(result.message)

                is ServerManagementDecodeResult.Failure ->
                    logger.warn(
                        "Management APIからのメッセージを解釈できません: {} {}",
                        result.reason,
                        result.detail
                    )
            }

            return null
        }

        override fun onClose(
            webSocket: WebSocket,
            statusCode: Int,
            reason: String
        ): CompletionStage<*>? {
            listener.onClosed(statusCode, reason)
            return null
        }

        override fun onError(
            webSocket: WebSocket,
            error: Throwable
        ) {
            listener.onError(error)
        }
    }

    private companion object {
        const val LOOPBACK = "127.0.0.1"
        const val PATH = "/management"
    }
}

/**
 * Management API接続の受信通知先です。
 *
 * 通知はWebSocketのスレッドから呼ばれます。
 * UIを更新する場合は呼び出し側でJavaFXスレッドへ移してください。
 */
interface ServerManagementListener {

    /** メッセージを受信したときに呼ばれます。 */
    fun onMessage(message: ServerManagementResponse)

    /** 接続が閉じたときに呼ばれます。 */
    fun onClosed(statusCode: Int, reason: String)

    /** 通信エラーが発生したときに呼ばれます。 */
    fun onError(error: Throwable)
}
