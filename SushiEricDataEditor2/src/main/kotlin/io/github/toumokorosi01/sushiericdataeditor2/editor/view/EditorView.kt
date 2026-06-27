package io.github.toumokorosi01.sushiericdataeditor2.editor.view

import io.github.toumokorosi01.common.data.core.ManagedData
import io.github.toumokorosi01.common.data.ore.data.OreData
import io.github.toumokorosi01.common.data.mob.data.MobData
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.sushiericdataeditor2.editor.controller.MainController
import io.github.toumokorosi01.sushiericdataeditor2.editor.service.EditorDataService
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 各種データエディタ画面の基盤となる抽象クラスです。
 *
 * このクラスは、サイドバー、トップアクションバー、メインコンテンツエリアを持つ
 * エディタウィンドウの共通レイアウトとライフサイクルを定義します。
 *
 * また、扱うデータ型[T]をジェネリックとして受け取ることで、
 * [ItemData]、[MobData]、[OreData]などの管理データを共通処理として扱えるようにします。
 *
 * 新しいデータ型のエディタを実装する場合は、このクラスを継承し、
 * [T]に対象データ型を指定して、各抽象メソッドを実装してください。
 *
 * @param T このエディタが扱う管理データ型。[ManagedData]を実装している必要があります。
 * @property main 画面遷移やダイアログ表示などの共通UI制御を行うメインコントローラー。
 * @property dataService データのロード、保存、およびリモートリソースの管理を行うデータサービス。
 * @property dataAccess このエディタが扱うデータ種別に対応するデータ操作アクセサ。
 */
abstract class EditorView<T : ManagedData<T, *>>(
    protected val main: MainController,
    protected val dataService: EditorDataService,
    protected val dataAccess: EditorDataService.DataAccess<T>
) {
    var openCancelled: Boolean = false
        private set

    /** サイドバーに並んでいるボタンをIDで即座に引き出せるようにするプロパティ */
    protected val sidebarButtons: MutableMap<String, Button> = mutableMapOf()

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    protected fun cancelOpen() {
        openCancelled = true
    }

    /**
     * ネットワーク切断など、アプリ側からエディタを強制終了して選択画面へ戻す安全な処理
     */
    protected fun handleForceBackToSelect() {
        logger.warn("ネットワーク切断または不正な状態を検知したため、エディタを強制終了します。")
        cancelOpen()
        dataService.forceBackToSelect()
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