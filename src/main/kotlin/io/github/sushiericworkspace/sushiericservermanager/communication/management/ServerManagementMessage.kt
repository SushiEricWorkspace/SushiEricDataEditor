package io.github.sushiericworkspace.sushiericservermanager.communication.management

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Management APIとやり取りするメッセージです。
 *
 * 種別はJSONの`type`で表し、SushiEricServerModの定義と対応させます。
 * 送信方向ごとに型を分け、受信専用の種別を送信できないようにします。
 *
 * 機能ごとに必要な種別を追加します。
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

    /**
     * コマンドの実行要求です。
     *
     * @property command 実行するコマンド。先頭のスラッシュは任意です。
     * @property nonce 応答と対応付けるための識別子。
     */
    @Serializable
    @SerialName("command_execute")
    data class CommandExecute(
        val command: String,
        val nonce: String? = null
    ) : ServerManagementRequest

    /**
     * Brigadierによる補完候補の要求です。
     *
     * @property command 補完対象のコマンド。
     * @property cursor 補完位置。省略時はコマンド末尾です。
     * @property nonce 応答と対応付けるための識別子。
     */
    @Serializable
    @SerialName("command_complete")
    data class CommandComplete(
        val command: String,
        val cursor: Int? = null,
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
     * コマンド実行の結果です。
     *
     * @property success コマンドが成功したか。
     * @property returnValue コマンドの戻り値。
     * @property output 実行中に出力されたメッセージ。
     * @property nonce 要求に含めた識別子。
     */
    @Serializable
    @SerialName("command_result")
    data class CommandResult(
        val success: Boolean,
        val returnValue: Int? = null,
        val output: List<String> = emptyList(),
        val nonce: String? = null
    ) : ServerManagementResponse

    /**
     * Brigadierによる補完候補です。
     *
     * @property suggestions 置換範囲を含む補完候補。
     * @property nonce 要求に含めた識別子。
     */
    @Serializable
    @SerialName("command_complete_result")
    data class CommandCompleteResult(
        val suggestions: List<ServerManagementCommandSuggestion> = emptyList(),
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

/**
 * サーバーのBrigadierが返した補完候補です。
 *
 * @property text 置換する文字列。
 * @property start 置換範囲の開始位置。
 * @property end 置換範囲の終了位置。
 * @property tooltip 候補の説明。
 */
@Serializable
data class ServerManagementCommandSuggestion(
    val text: String,
    val start: Int,
    val end: Int,
    val tooltip: String? = null
)
