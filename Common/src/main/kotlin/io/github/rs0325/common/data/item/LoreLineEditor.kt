package io.github.rs0325.common.data.item

import io.github.rs0325.common.data.item.data.CustomComponentLoreSection
import io.github.rs0325.common.data.item.data.DisplayInfo
import io.github.rs0325.common.data.item.data.LoreSection
import io.github.rs0325.common.data.item.data.LoreSectionType
import io.github.rs0325.common.data.item.data.PlainTextLoreSection
import io.github.rs0325.common.data.item.data.StatLoreSection
import net.kyori.adventure.text.JoinConfiguration

/**
 * アイテムのLore（説明文）における特定の行（Index）に焦点を当て、
 * 行自体の操作（追加・削除・移動）および、行内部の複数セクション（フラット結合されたテキスト要素）の
 * 精密な編集を行うためのエディタクラスです。
 *
 * 各種操作メソッドにおける配列範囲外アクセス（[IndexOutOfBoundsException]）に対する安全ガードを内蔵しています。
 *
 * @property info 編集対象のデータモデル（[DisplayInfo]）
 * @property currentIndex 現在エディタが焦点を当てているLoreの行番号（0始まり）
 */
class LoreLineEditor(
    private val info: DisplayInfo,
    var currentIndex: Int
) {
    /**
     * 操作対象のLore（ComponentのMutableList）へのダイレクト参照ヘルパー。
     */
    private val lore: MutableList<MutableList<LoreSection>> get() = info.lore

    /**
     * [currentIndex] が現在存在するLoreの有効なインデックス範囲（0 〜 size-1）に収まっているかを取得します。
     * 行に対する削除や編集、セクションへのアクセスを行う前の安全チェックに使用されます。
     */
    val isValidIndex: Boolean get() = currentIndex in 0..<lore.size

    /**
     * 現在の [currentIndex] が指す位置に、新しく初期化された空の行（Component）を挿入します。
     *
     * #### 仕様:
     * - 外部からComponentを受け取らず、関数内で `Component.text("")`（初期値：白文字・装飾なし）を自動生成します。
     * - インデックスが有効範囲（0 〜 リストサイズ）内であれば、その位置へ割り込ませます（以降の既存行は下にズレます）。
     * - インデックスが範囲外（例: リストサイズより大きい、またはマイナス値）の場合は、安全弁として**末尾**に追加されます。
     * - 範囲外追加が発生した場合、[currentIndex] は自動的に新しく追加された末尾の行を指すように更新されます。
     */
    fun add(type: LoreSectionType = LoreSectionType.PLAIN_TEXT) {
        val newLine = mutableListOf(
            createSection(type)
        )

        if (currentIndex in 0..lore.size) {
            lore.add(currentIndex, newLine)
        } else {
            lore.add(newLine)
            currentIndex = lore.size - 1
        }
    }

    /**
     * [currentIndex] が指す現在の行をLoreから削除します。
     *
     * #### 仕様:
     * - 削除が成功すると、以降の行のインデックスは自動的に1つずつ詰まります。
     * - 削除した結果、[currentIndex] が新しいリストの範囲外（末尾削除によりサイズを超過）になった場合、
     *   エディタのクラッシュを防ぐため、自動的に現在の新たな末尾（または要素が空なら0）へと安全に丸め込まれます。
     *
     * @return 削除に成功した場合は `true`。現在のインデックスが範囲外で削除が行われなかった場合は `false`。
     */
    fun remove(): Boolean {
        if (!isValidIndex) return false
        lore.removeAt(currentIndex)
        if (currentIndex >= lore.size) {
            currentIndex = (lore.size - 1).coerceAtLeast(0)
        }
        return true
    }

    /**
     * [currentIndex] が指す現在の行を、指定された新しい行番号（[toIndex]）の位置へ移動（並び替え）させます。
     *
     * #### 仕様:
     * - 元あった要素を一度抜き取り、指定位置へ差し込むため、周囲の要素は自動的に詰まり・割り込み処理が行われます。
     * - 移動先（[toIndex]）が現在のリスト範囲外を指している場合、安全ガードにより自動的に「先頭（0）」または「末尾」へとクランプされます。
     * - 移動の成功に合わせて、エディタが保持する [currentIndex] 自体も自動的に移動先（追従先）のインデックスへと更新されます。
     * - 移動前と移動後のインデックスが同一の場合は、無駄な再描画を防ぐため処理を早期リターンします。
     *
     * @param toIndex 移動先の行番号
     */
    fun moveTo(toIndex: Int) {
        if (!isValidIndex) return
        val maxTargetIndex = lore.size - 1
        val targetIndex = toIndex.coerceIn(0, maxTargetIndex)
        if (currentIndex == targetIndex) return

        val removed = lore.removeAt(currentIndex)
        lore.add(targetIndex, removed)
        currentIndex = targetIndex
    }

    /**
     * 現在の行の特定セクションを操作するための、セクション専用エディタインスタンス（[SectionEditor]）を取得します。
     *
     * 外側から勝手にインスタンス化されるのを防ぐため、このメソッドがセクションエディタへの唯一のアクセス窓口となります。
     * UI（JavaFXのリスト等）で行とセクションが選択された際に、連鎖的に呼び出す用途に適しています。
     * （例： `lineEditor.section(2).editText("新テキスト")`）
     *
     * @param sectionIndex 操作ターゲットとするセクションのインデックス（0始まり）
     * @return 指定されたセクションインデックスの状態を保持する [SectionEditor]
     */
    fun section(sectionIndex: Int): SectionEditor {
        return SectionEditor(sectionIndex)
    }

    /**
     * 現在の行（[currentIndex]）を構成しているすべての階層付きセクションをスキャンし、
     * ネスト（入れ子）のない「フラットな単一コンポーネントのリスト」へと完全に分解・抽出します。
     *
     * #### なぜこの処理が必要か:
     * Adventure APIで `.append()` や `Component.join()` を行うと、内部データは「親」の下に複数の「子（children）」が
     * ぶら下がるツリー構造になります。これをそのままインデックスで操作すると深さの管理でバグの原因になるため、
     * このメソッドで一度すべての要素を「同列の並び（兄弟リスト）」に引き剥がし（アンパック）、安全な配列操作を可能にします。
     *
     * @return 分解されたセクション（Component）の可変リスト。行インデックスが無効な場合は空のリストを返します。
     */
    fun getAllSections(): MutableList<LoreSection> {
        if (!isValidIndex) return mutableListOf()
        return lore[currentIndex]
    }

    /**
     * このLore行に含まれているセクション数を取得する。
     *
     * `getAllSections().size` を直接呼ばずに、セクション数だけを取得したい場合に使用する。
     *
     * @return 現在のLore行に存在するセクション数
     */
    fun getSectionSize(): Int = getAllSections().size

    /**
     * Lore行数を取得する。
     *
     * 外部から `info.lore.size` を直接参照せずに、
     * 現在のLore全体の行数だけを取得したい場合に使用する。
     *
     * @return 現在のLore行数
     */
    fun getLineSize(): Int = lore.size

    /**
     * 指定されたLoreセクション種別に対応する新規セクションインスタンスを生成します。
     *
     * #### 仕様:
     * - [LoreSectionType.PLAIN_TEXT] の場合は [PlainTextLoreSection] を生成します。
     * - [LoreSectionType.STATS] の場合は [StatLoreSection] を生成します。
     * - [LoreSectionType.CUSTOM_COMPONENT] の場合は [CustomComponentLoreSection] を生成します。
     * - 生成される各セクションは、それぞれのデフォルト値を持った初期状態になります。
     *
     * @param type 生成したいLoreセクションの種類。
     * @return [type] に対応する新しい [LoreSection]。
     */
    private fun createSection(type: LoreSectionType): LoreSection {
        return when (type) {
            LoreSectionType.PLAIN_TEXT -> PlainTextLoreSection()
            LoreSectionType.STATS -> StatLoreSection()
            LoreSectionType.CUSTOM_COMPONENT -> CustomComponentLoreSection()
        }
    }

    /**
     * 行内の「特定のセクション（文字・色・装飾の1ブロック）」に状態を拘束させ、
     * 引数なし、あるいは単一のプロパティ指定のみで精密な部分編集や並び替えを行うための内部クラスです。
     *
     * `inner` 指定により、親クラス（[LoreLineEditor]）の [currentIndex] や [lore] の状態に直接アクセス・同期できます。
     *
     * @property currentSectionIndex 現在このインスタンスが焦点を当てて操作しているセクションのインデックス
     */
    inner class SectionEditor internal constructor(
        var currentSectionIndex: Int
    ) {
        /**
         * 現在この [SectionEditor] が指しているセクションを安全に取得します。
         *
         * #### 仕様:
         * - 親のLore行インデックスが無効な場合は `null` を返します。
         * - [currentSectionIndex] が現在のセクション一覧の範囲外の場合も `null` を返します。
         *
         * @return 現在の対象セクション。[LoreSection] が存在しない場合は `null`。
         */
        fun currentSectionOrNull(): LoreSection? {
            if (!isValidIndex) return null
            return lore[currentIndex].getOrNull(currentSectionIndex)
        }

        /**
         * 現在保持している [currentSectionIndex] の位置に、新しく初期化された空のセクションを割り込ませます。
         *
         * #### 仕様:
         * - 外部からComponentを受け取らず、関数内で `Component.text("")`（初期値：白文字・装飾なし）を自動生成します。
         * - 位置は自動的に安全な範囲（0 〜 現在のセクション総数）に制限（クランプ）して挿入されます。
         * - 挿入後、分解されていた全セクションは [JoinConfiguration] を用いた結合処理によって
         *   隙間なくフラットな親子関係に再結合され、元の行へ上書きされます。
         */
        fun add(type: LoreSectionType = LoreSectionType.PLAIN_TEXT) {
            if (!isValidIndex) return

            val sections = lore[currentIndex]

            val targetIdx = currentSectionIndex.coerceIn(0, sections.size)
            sections.add(
                targetIdx,
                createSection(type)
            )
        }

        /**
         * 現在保持している [currentSectionIndex] のセクションを、行内部からピンポイントで削除します。
         *
         * #### 仕様:
         * - 削除後、残った他のセクションは自動的に詰められ、[JoinConfiguration] を用いた結合処理によって再結合されます。
         * - **【自動連動システム】**: セクションを削除した結果、その行のすべてのセクションが消滅（空行化）した場合、
         *   データに透明なゴミが残るのを防ぐため、親クラスの [LoreLineEditor.remove] を自動呼び出しし、**行ごとLoreから完全消滅**させます。
         * - 行が残った場合、[currentSectionIndex] が削除によって配列範囲外にならないよう、新しい末尾へと安全に丸め込まれます。
         *
         * @return 削除に成功し、まだ行に他のセクションが残っている場合は `true`。
         *         最後のセクションが削除され、行自体が消滅した場合は `false`。
         */
        fun remove(): Boolean {
            if (!isValidIndex) return false

            val sections = lore[currentIndex]
            if (currentSectionIndex !in sections.indices) return false

            sections.removeAt(currentSectionIndex)

            return if (sections.isEmpty()) {
                this@LoreLineEditor.remove()
                false
            } else {
                if (currentSectionIndex >= sections.size) {
                    currentSectionIndex = (sections.size - 1).coerceAtLeast(0)
                }
                true
            }
        }

        /**
         * 現在保持しているセクションを、同一の行内にある別のセクション位置（[toSectionIndex]）へ移動（並び替え）させます。
         *
         * #### 仕様:
         * - 元あった要素を一度抜き取り、指定位置へ差し込むため、前後のセクションは自動的に詰まり・割り込み処理が行われます。
         * - 移動先（[toSectionIndex]）が現在の行のセクション範囲外を指している場合、自動的に安全な限界値へとクランプされます。
         * - 移動の成功に合わせて、このオブジェクトが保持する [currentSectionIndex] 自体も移動先のインデックスへと自動追従します。
         *
         * @param toSectionIndex 同一行内での移動先セクションインデックス
         */
        fun moveTo(toSectionIndex: Int) {
            if (!isValidIndex) return

            val sections = lore[currentIndex]
            if (currentSectionIndex !in sections.indices) return

            val maxTarget = sections.size - 1
            val targetIdx = toSectionIndex.coerceIn(0, maxTarget)

            if (currentSectionIndex == targetIdx) return

            val removed = sections.removeAt(currentSectionIndex)
            sections.add(targetIdx, removed)

            currentSectionIndex = targetIdx
        }

        fun replaceCurrentSection(type: LoreSectionType) {
            replaceCurrentSection(createSection(type))
        }

        fun replaceCurrentSection(section: LoreSection) {
            if (!isValidIndex) return

            val sections = lore[currentIndex]
            if (currentSectionIndex !in sections.indices) return

            sections[currentSectionIndex] = section
        }
    }
}