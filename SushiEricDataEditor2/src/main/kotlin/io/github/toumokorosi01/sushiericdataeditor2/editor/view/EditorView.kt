package io.github.toumokorosi01.sushiericdataeditor2.editor.view

import io.github.toumokorosi01.sushiericdataeditor2.editor.controller.MainController
import io.github.toumokorosi01.sushiericdataeditor2.editor.service.EditorDataService
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

/**
 * 各種データエディタ画面の基盤となる 抽象クラス（ベースビュー）です。
 *
 * このクラスは、サイドバー、トップアクションバー、メインコンテンツエリアを持つ
 * エディタウィンドウの共通レイアウトとライフサイクルを定義します。
 * 新しいデータ型のエディタを実装する場合は、このクラスを継承して各抽象メソッドを実装してください。
 *
 * @property main 画面遷移やダイアログ表示などの共通UI制御を行うメインコントローラー
 * @property dataService データのロード、保存、およびリモートリソースの管理を行うデータサービス
 */
abstract class EditorView(
    protected val main: MainController,
    protected val dataService: EditorDataService
) {
    var openCancelled: Boolean = false
        private set

    protected fun cancelOpen() {
        openCancelled = true
    }

    /**
     * サイドバー内（コンテナ）のコンポーネント（主にアイテム選択ボタンなど）を構築します。
     * 必要に応じて、初期表示時に対象となるタブ（リソース）を自動で選択する処理もここに記述します。
     *
     * @param container ボタン群を配置するサイドバーの垂直レイアウトコンテナ
     * @param selectId 初期表示時に選択させたいアイテムのID。省略時（null）はデフォルトの挙動（先頭の要素を選択など）となります。
     */
    abstract fun setupSidebar(container: VBox, selectId: String? = null)

    /**
     * トップバー（コンテナ）に配置する、エディタ固有の共通アクションボタン（「保存」「新規作成」など）を構築します。
     *
     * @param container アクションボタンを水平に並べるためのトップレイアウトコンテナ
     */
    abstract fun setupActions(container: HBox)

    /**
     * 指定された一意の識別子（IDやファイル名など）に対応するタブ（アイテム）を選択状態にします。
     * 内部的には、データのロード、メインコンテンツエリアの再描画、およびサイドバーボタンのハイライト更新などを行います。
     *
     * @param targetId 選択対象となるリソースの識別子（ID）
     */
    abstract fun selectTab(targetId: String)

    /**
     * 現在編集中のデータを確定し、[dataService] を介して永続化（保存）する処理を実行します。
     * 必要に応じて、サーバー上のデータとの競合チェックや、上書き確認ダイアログの表示などもここで行います。
     */
    abstract fun onSave(targetItemId: String? = null)

    /**
     * エディタ（ウィンドウ）が閉じられる直前に呼び出される ライフサイクル関数です。
     *
     * このメソッドは、ユーザーがウィンドウの「×」ボタンを押した際や、システムによって
     * ウィンドウが閉じられる要求が発生した際にトリガーされます。
     * 子クラスでオーバーライドすることで、未保存チェックによる閉じる動作のキャンセルや、
     * メモリ解放のためのキャッシュクリアなどの後処理を実装できます。
     *
     * @return ウィンドウをそのまま閉じてよい場合は `true`、
     *         未保存データがあるなどの理由で閉じる動作を中断（キャンセル）したい場合は `false`。
     */
    open fun onClose(): Boolean = true
}