package io.github.sushiericworkspace.sushiericservermanager.config

import kotlinx.serialization.Serializable

/**
 * サーバープロセスを操作するコマンドの組です。
 *
 * 空の項目は未設定として扱い、その操作を行いません。
 *
 * @property startCommand 起動コマンド。
 * @property stopCommand 停止コマンド。
 * @property restartCommand 再起動コマンド。
 */
@Serializable
data class ServerControlCommandSet(
    val startCommand: String = "",
    val stopCommand: String = "",
    val restartCommand: String = ""
) {
    /** いずれかのコマンドが登録されているかを返します。 */
    val hasAnyCommand: Boolean
        get() = listOf(startCommand, stopCommand, restartCommand).any { it.isNotBlank() }

    /** 前後の空白を取り除いた組を返します。 */
    fun normalized(): ServerControlCommandSet =
        ServerControlCommandSet(
            startCommand = startCommand.trim(),
            stopCommand = stopCommand.trim(),
            restartCommand = restartCommand.trim()
        )

    companion object {
        /** 何も登録されていない状態です。 */
        val EMPTY = ServerControlCommandSet()
    }
}

/**
 * プロファイルごとの操作コマンドです。
 *
 * 接続情報とは別のファイルへ保存します。ホストや鍵のパスを含まないため、
 * コマンドの定義だけを他の利用者へ渡せます。
 *
 * @property commands プロファイル名をキーにしたコマンドの組。
 */
@Serializable
data class ServerControlCommandsConfig(
    val commands: Map<String, ServerControlCommandSet> = emptyMap()
)

/**
 * 受け渡し用のコマンド定義です。
 *
 * 適用先は受け取った側が決めるため、プロファイル名を含めません。
 *
 * @property formatVersion 形式の版。読み込み時に想定した形式かを確認します。
 */
@Serializable
data class ServerControlCommandsExport(
    val formatVersion: Int = FORMAT_VERSION,
    val startCommand: String = "",
    val stopCommand: String = "",
    val restartCommand: String = ""
) {
    /** 保存用のコマンドの組へ変換します。 */
    fun toCommandSet(): ServerControlCommandSet =
        ServerControlCommandSet(
            startCommand = startCommand,
            stopCommand = stopCommand,
            restartCommand = restartCommand
        ).normalized()

    companion object {
        /** 現在の形式の版です。 */
        const val FORMAT_VERSION: Int = 1

        /** コマンドの組から受け渡し用の定義を作ります。 */
        fun from(commandSet: ServerControlCommandSet): ServerControlCommandsExport {
            val normalized = commandSet.normalized()

            return ServerControlCommandsExport(
                formatVersion = FORMAT_VERSION,
                startCommand = normalized.startCommand,
                stopCommand = normalized.stopCommand,
                restartCommand = normalized.restartCommand
            )
        }
    }
}

/**
 * サーバー操作コマンドの保存と読み込みを行います。
 *
 * 保存先は接続情報とは別のファイルであり、プロファイル名で対応付けます。
 */
object ServerControlCommandsManager : JsonFileHandler<ServerControlCommandsConfig>(
    FilePath.SERVER_CONTROL_COMMANDS,
    { ServerControlCommandsConfig() },
    ServerControlCommandsConfig.serializer()
) {

    /**
     * プロファイルへ対応するコマンドを読み込みます。
     *
     * 登録が無い場合は空の組を返します。
     */
    fun loadFor(profileName: String?): ServerControlCommandSet {
        if (profileName.isNullOrBlank()) {
            return ServerControlCommandSet.EMPTY
        }

        return load().commands[profileName] ?: ServerControlCommandSet.EMPTY
    }

    /**
     * プロファイルへ対応するコマンドを保存します。
     *
     * 他のプロファイルの登録は変更しません。
     * すべて空の場合は登録を削除し、不要な項目を残しません。
     */
    fun saveFor(
        profileName: String,
        commandSet: ServerControlCommandSet
    ) {
        val normalized = commandSet.normalized()
        val current = load()

        val updated =
            if (normalized.hasAnyCommand) {
                current.commands + (profileName to normalized)
            } else {
                current.commands - profileName
            }

        save(current.copy(commands = updated))
    }
}
