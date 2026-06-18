package io.github.toumokorosi01.sushiericdataeditor2.config

/**
 * 実行環境のオペレーティングシステム（OS）を判別し、
 * OS固有のパスやサポート状況を管理するユーティリティオブジェクト。
 */
object OS {
    /** 小文字に変換されたシステムプロパティ "os.name" */
    val name: String = System.getProperty("os.name").lowercase()

    /** 実行環境が Windows であるか */
    val isWindows = name.contains("win")

    /** 実行環境が macOS であるか */
    val isMac = name.contains("mac")

    /**
     * アプリケーションの設定ファイルやデータを保存するためのベースディレクトリパス。
     * はじめてアクセスされた際に、OSに応じて以下の適切なパスを決定します。
     *
     * - Windows: `LOCALAPPDATA` 環境変数、または `~/AppData/Local`
     * - macOS: `~/Library/Application Support`
     * - その他: ユーザーホームディレクトリ(サポート外)
     */
    val dataConfigBase: String by lazy {
        when {
            isWindows -> System.getenv("LOCALAPPDATA") ?: "${System.getProperty("user.home")}/AppData/Local"
            isMac -> "${System.getProperty("user.home")}/Library/Application Support"
            // サポート外のOSでも、プログラムが落ちないようにユーザーホームを返しておく
            else -> System.getProperty("user.home")
        }
    }

    /**
     * 現在のOSが本アプリケーションの公式サポート対象であるかを判定します。
     * 現時点では Windows および macOS をサポート対象としています。
     *
     * @return サポート対象のOSであれば true
     */
    fun isSupportedOs(): Boolean {
        // メモ: プロパティの name を利用しても良いですが、常に最新のシステム値を参照します
        val os = System.getProperty("os.name").lowercase()
        return os.contains("win") || os.contains("mac")
    }
}