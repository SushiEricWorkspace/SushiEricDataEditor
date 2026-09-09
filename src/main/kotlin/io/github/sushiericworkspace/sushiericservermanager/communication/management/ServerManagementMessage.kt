package io.github.sushiericworkspace.sushiericservermanager.communication.management

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Management APIとやり取りするメッセージです。
 *
 * 種別はJSONの`type`で表し、SushiEricServerModの定義と対応させます。
 * 送信方向ごとに型を分け、受信専用の種別を送信できないようにします。
 *
 * 本Issueでは接続確認に必要な種別だけを定義します。
 * コマンド実行、ログ配信、監視の種別は各機能のIssueで追加します。
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
