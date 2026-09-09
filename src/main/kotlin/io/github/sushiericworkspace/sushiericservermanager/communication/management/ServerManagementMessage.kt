package io.github.sushiericworkspace.sushiericservermanager.communication.management

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Management APIとやり取りするメッセージです。
 *
 * 種別はJSONの`type`で表し、SushiEricServerModの定義と対応させます。
 * 送信方向ごとに型を分け、受信専用の種別を送信できないようにします。
 *
 * 接続確認とコンソールログ配信に必要な種別を定義します。
 * コマンド実行や監視の種別は各機能のIssueで追加します。
 */
sealed interface ServerManagementMessage

/**
 * Managerからサーバーへ送るメッセージです。
 */
@Serializable
sealed interface ServerManagementRequest : ServerManagementMessage {

    /**
     * 接続確認の要求です。
     *
     * @property nonce 応答と対応付けるための識別子。
     */
    @Serializable
    @SerialName("ping")
    data class Ping(
        val nonce: String? = null
    ) : ServerManagementRequest

    /** コンソールログの購読を開始する要求です。 */
    @Serializable
    @SerialName("console_subscribe")
    data object ConsoleSubscribe : ServerManagementRequest

    /** コンソールログの購読を停止する要求です。 */
    @Serializable
    @SerialName("console_unsubscribe")
    data object ConsoleUnsubscribe : ServerManagementRequest
}

/**
 * サーバーからManagerへ届くメッセージです。
 */
@Serializable
sealed interface ServerManagementResponse : ServerManagementMessage {

    /**
     * 接続確認への応答です。
     *
     * @property nonce 要求に含めた識別子。
     */
    @Serializable
    @SerialName("pong")
    data class Pong(
        val nonce: String? = null
    ) : ServerManagementResponse

    /**
     * サーバーから配信されたコンソールログです。
     *
     * @property timestamp ログ生成時刻のISO-8601文字列。
     * @property level ログレベル。
     * @property message ログ本文。例外発生時はスタックトレースを含む場合があります。
     */
    @Serializable
    @SerialName("console_log")
    data class ConsoleLog(
        val timestamp: String,
        val level: String,
        val message: String
    ) : ServerManagementResponse

    /**
     * 受け付けられなかったメッセージへの応答です。
     *
     * サーバーはこの応答のあと接続を閉じます。
     *
     * @property reason 拒否した理由。
     * @property detail 原因の補足。
     */
    @Serializable
    @SerialName("error")
    data class Error(
        val reason: String,
        val detail: String? = null
    ) : ServerManagementResponse
}
