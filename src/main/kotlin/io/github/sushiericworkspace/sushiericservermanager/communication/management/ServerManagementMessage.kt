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

    /**
     * 監視情報の購読要求です。
     *
     * 既に購読している場合は更新間隔を変更します。
     *
     * @property intervalTicks 更新間隔のtick数。省略するとサーバー側の既定値を使用します。
     * @property nonce 応答と対応付けるための識別子。
     */
    @Serializable
    @SerialName("monitor_subscribe")
    data class MonitorSubscribe(
        val intervalTicks: Int? = null,
        val nonce: String? = null
    ) : ServerManagementRequest

    /**
     * 監視情報の購読停止要求です。
     *
     * @property nonce 応答と対応付けるための識別子。
     */
    @Serializable
    @SerialName("monitor_unsubscribe")
    data class MonitorUnsubscribe(
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

    /**
     * 購読の受付結果です。
     *
     * @property subscribed 購読中であるか。
     * @property intervalTicks サーバーが適用した更新間隔。停止時はnull。
     * @property nonce 要求に含めた識別子。
     */
    @Serializable
    @SerialName("monitor_subscription")
    data class MonitorSubscription(
        val subscribed: Boolean,
        val intervalTicks: Int? = null,
        val nonce: String? = null
    ) : ServerManagementResponse

    /**
     * 購読中に一定間隔で届く監視情報です。
     *
     * 要求への応答ではないため識別子を持ちません。
     */
    @Serializable
    @SerialName("monitor_update")
    data class MonitorUpdate(
        val server: MinecraftServerStatus,
        val jvm: JvmStatus
    ) : ServerManagementResponse
}

/**
 * Minecraft内部の状態です。
 *
 * Management API由来の情報だけを持ちます。
 * CPU使用率やシステムメモリなどOSレベルの情報は含みません。
 *
 * @property ticksPerSecond 直近の平均TPS。
 * @property millisPerTick 直近の平均MSPT。
 * @property currentTick サーバー起動からの経過tick。
 * @property uptimeSeconds サーバー起動からの経過秒数。
 * @property onlinePlayerCount 接続中のプレイヤー数。
 * @property maxPlayerCount 最大プレイヤー数。
 * @property worlds ワールドごとの状態。
 */
@Serializable
data class MinecraftServerStatus(
    val ticksPerSecond: Double,
    val millisPerTick: Double,
    val currentTick: Int,
    val uptimeSeconds: Long,
    val onlinePlayerCount: Int,
    val maxPlayerCount: Int,
    val worlds: List<MinecraftWorldStatus> = emptyList()
)

/**
 * ワールド1つの状態です。
 *
 * @property id ワールドの識別子。
 * @property entityCount 読み込み済みEntity数。
 * @property loadedChunkCount 読み込み済みChunk数。
 * @property playerCount このワールドにいるプレイヤー数。
 */
@Serializable
data class MinecraftWorldStatus(
    val id: String,
    val entityCount: Int,
    val loadedChunkCount: Int,
    val playerCount: Int
)

/**
 * サーバーのJVM状態です。
 *
 * サーバー側で取得できない値はnullになります。
 *
 * @property heapUsedBytes Heapの使用量。
 * @property heapCommittedBytes Heapの確保済み量。
 * @property heapMaxBytes Heapの最大値。
 * @property nonHeapUsedBytes Heap以外の使用量。
 * @property totalMemoryBytes JVMが確保しているメモリ量。
 * @property freeMemoryBytes 確保済みメモリのうち未使用の量。
 * @property maxMemoryBytes JVMが使用できるメモリの上限。
 * @property threadCount 生存中のスレッド数。
 * @property gc GCごとの実行回数と累計時間。
 */
@Serializable
data class JvmStatus(
    val heapUsedBytes: Long,
    val heapCommittedBytes: Long,
    val heapMaxBytes: Long? = null,
    val nonHeapUsedBytes: Long,
    val totalMemoryBytes: Long,
    val freeMemoryBytes: Long,
    val maxMemoryBytes: Long? = null,
    val threadCount: Int,
    val gc: List<GcStatus> = emptyList()
)

/**
 * GC1つの状態です。
 *
 * @property name GCの名前。
 * @property count 実行回数。
 * @property totalTimeMillis 累計実行時間。
 */
@Serializable
data class GcStatus(
    val name: String,
    val count: Long? = null,
    val totalTimeMillis: Long? = null
)
