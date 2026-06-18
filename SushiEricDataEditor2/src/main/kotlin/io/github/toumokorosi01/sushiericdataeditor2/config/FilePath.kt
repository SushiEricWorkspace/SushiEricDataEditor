package io.github.toumokorosi01.sushiericdataeditor2.config

import java.io.File

/**
 * アプリケーションで使用する設定ファイルのパスを一元管理する列挙型。
 *
 * OSごとの適切なデータ保存ディレクトリ配下に、
 * アプリ専用のディレクトリ（SushiEricDataEditor2）を含めたフルパスを生成します。
 */
enum class FilePath {
    /** サーバー接続情報（プロファイル）を保存するJSONファイルのパス */
    SERVER_PROFILES,
    /** アプリケーション全体の基本設定を保存するJSONファイルのパス */
    SETTINGS,
    /** 自動保存データを格納するルートディレクトリのパス */
    AUTOSAVE_DIR,
    /** 多重起動防止のロックファイルを格納するディレクトリのパス */
    LOCK;

    /**
     * 各列挙値に対応するファイルのフルパスを文字列で取得します。
     *
     * OSに応じたベースディレクトリと、アプリケーション固有のディレクトリ構造を結合します。
     */
    val path: String
        get() = when (this) {
            SERVER_PROFILES -> buildPath("profiles.json")
            SETTINGS -> buildPath("config.json")
            AUTOSAVE_DIR -> buildPath("autosave")
            LOCK -> buildPath("lock")
        }

    /**
     * ベースディレクトリ、アプリケーション名、およびファイル名を結合してパスを構成します。
     *
     * [OS.dataConfigBase] から取得したOS依存のパスに、共通のディレクトリ名を挟みます。
     *
     * @param subPath ファイル名（またはサブディレクトリ名）
     * @return OSに応じたセパレータを含む結合済みのパス文字列
     */
    private fun buildPath(subPath: String): String {
        val baseDir = "${OS.dataConfigBase}${File.separator}SushiEricDataEditor2"
        return "$baseDir${File.separator}$subPath"
    }

    /**
     * 現在のパスを[File]オブジェクトとして取得します。
     *
     * @return このパスを指す [File] インスタンス
     */
    fun toFile(): File = File(path)
}