package io.github.toumokorosi01.sushiericdataeditor2.config

import kotlinx.serialization.Serializable

/**
 * サーバー設定全体を管理するデータモデル。
 * @property list 登録されている [ServerProfile] のリスト
 */
@Serializable
data class ServerConfig(
    val list: List<ServerProfile>
)

/**
 * 個別のサーバー接続情報を保持するデータモデル。
 * SFTP接続やSSH認証に必要なパラメータを格納します。
 * @property name サーバーの識別名（表示用）
 * @property host 接続先のホスト名またはIPアドレス
 * @property port 接続ポート番号（通常は "22"）
 * @property user ログインユーザー名
 * @property path サーバー上の対象ディレクトリパス
 * @property key 認証に使用する秘密鍵（Private Key）のローカルパス
 */
@Serializable
data class ServerProfile(
    val name: String,
    val host: String,
    val port: Int,
    val user: String,
    val path: String,
    val key: String
)

/**
 * サーバー設定ファイル（profiles）の読み書きを専門に行うマネージャー。
 * [JsonFileHandler] を継承しており、追加の実装なしで永続化が可能です。
 * * 基本的な使い方：
 * ```kotlin
 * val config = SettingConfigManager.load()
 * SettingConfigManager.save(newConfig)
 * ```
 */
object SettingConfigManager : JsonFileHandler<ServerConfig>(
    FilePath.SERVER_PROFILES,
    { ServerConfig(emptyList()) },
    ServerConfig.serializer()
)