package io.github.toumokorosi01.sushiericdataeditor2.config

import java.io.File

/**
 * アプリケーションで使用する設定ファイルとディレクトリのパスを一元管理する列挙型。
 *
 * 既存の保存先と列挙値は維持し、SSH用の管理ディレクトリとknown_hostsだけを追加します。
 */
enum class FilePath {
    SERVER_PROFILES,
    SETTINGS,
    AUTOSAVE_DIR,
    LOCK,
    SSH_DIR,
    KNOWN_HOSTS;

    val path: String
        get() = when (this) {
            SERVER_PROFILES -> buildPath("profiles.json")
            SETTINGS -> buildPath("config.json")
            AUTOSAVE_DIR -> buildPath("autosave")
            LOCK -> buildPath("lock")
            SSH_DIR -> buildPath("ssh")
            KNOWN_HOSTS -> buildPath("ssh${File.separator}known_hosts")
        }

    private fun buildPath(subPath: String): String {
        val baseDir = "${OS.dataConfigBase}${File.separator}SushiEricDataEditor2"
        return "$baseDir${File.separator}$subPath"
    }

    fun toFile(): File = File(path)
}
