package io.github.sushiericworkspace.sushiericdataeditor2.config

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val list: List<ServerProfile> = emptyList()
)

/**
 * 個別のサーバー接続情報。
 *
 * 従来のname、host、port、user、path、keyは変更しません。
 * 追加項目はすべてデフォルト値を持つため、既存のprofiles.jsonをそのまま読み込めます。
 */
@Serializable
data class ServerProfile(
    val name: String,
    val host: String,
    val port: Int,
    val user: String,
    val path: String,
    val key: String,
    val authenticationType: String = AuthenticationType.EXISTING_PRIVATE_KEY.storedValue,
    val generatedKey: Boolean = false,
    val keyFormat: String? = null,
    val remoteOperatingSystem: String = RemoteOperatingSystem.UBUNTU_SERVER.storedValue
) {
    fun resolvedAuthenticationType(): AuthenticationType =
        AuthenticationType.fromStoredValue(authenticationType)

    fun resolvedRemoteOperatingSystem(): RemoteOperatingSystem =
        RemoteOperatingSystem.fromStoredValue(remoteOperatingSystem)
}

object SettingConfigManager : JsonFileHandler<ServerConfig>(
    FilePath.SERVER_PROFILES,
    { ServerConfig() },
    ServerConfig.serializer()
) {
    /** 保存後に再読み込みし、書き込んだ内容と一致することを確認します。 */
    fun saveAndVerify(config: ServerConfig): Boolean {
        save(config)
        return load() == config
    }
}
