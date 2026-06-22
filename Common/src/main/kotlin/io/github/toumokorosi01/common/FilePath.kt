package io.github.toumokorosi01.common

import java.io.File

/**
 * ファイルパス構築の基底クラス。
 * 指定されたディレクトリとファイル名を結合し、物理ファイルへの解決を行います。
 */
abstract class Path(
    private val dir: Dir,
    private val fileName: String
) {
    /**
     * ファイルを含めたフルパスの文字列を返します (例: "item_data/stats/my_item.yml")
     */
    fun getRawPath(separator: String = "/"): String =
        "${dir.getRawPath(separator)}$separator$fileName"

    /**
     * ベースディレクトリ（プラグインのデータフォルダ等）を元に、最終的な [File] オブジェクトを返します。
     */
    fun resolve(base: File): File =
        File(dir.resolve(base), fileName)
}

/**
 * YAMLファイルに特化したパス。自動的に拡張子 `.yml` を付与します。
 */
abstract class YmlPath(
    dir: Dir,
    name: String
) : Path(dir, "$name.yml")

/**
 * ディレクトリ構造を定義する sealed class。
 * 親ディレクトリへの参照を保持することで、再帰的にパスを解決します。
 */
sealed class Dir(val name: String, val parent: Dir? = null) {

    companion object {
        const val BASE_ROOT = "config/${Const.MOD_NAME}"
    }

    /**
     * ルートからの相対パス文字列を返します (例: "item_data/stats")
     */
    fun getRawPath(separator: String = "/"): String =
        parent?.let { "${it.getRawPath(separator)}$separator$name" } ?: name

    /**
     * ベースディレクトリからこのディレクトリまでのフルパスを解決します。
     */
    fun resolve(base: File): File = File(parent?.resolve(base) ?: base, name)

    /**
     * このディレクトリが Player ディレクトリの子孫であるか判定します。
     */
    fun Dir.isPlayerData(): Boolean {
        var current: Dir? = this
        while (current != null) {
            if (current is Player) return true
            current = current.parent
        }
        return false
    }

    /**
     * アイテムに関する共通データディレクトリ (`root/item_data/`)。
     */
    object Item : Dir("item_data") {
        /** アイテムのステータス定義フォルダ (`root/item_data/stats/`) */
        object Stats : Dir("stats", Item) {
            /**
             * 特定のアイテムIDに対応する設定ファイル。
             * @param id アイテムの識別子（ファイル名になります）
             */
            class File(id: String) : YmlPath(Stats, id)
        }
    }

    /**
     * 鉱石に関する共通データディレクトリ (`root/ore_data/`)。
     */
    object Ore : Dir("ore_data") {
        /** 鉱石定義フォルダ (`root/ore_data/ores/`) */
        object Ores : Dir("ores", Ore) {
            /**
             * 特定の鉱石IDに対応する設定ファイル。
             * @param id 鉱石の識別子（ファイル名になります）
             */
            class File(id: String) : YmlPath(Ores, id)
        }
    }

    /**
     * Mobに関する共通データディレクトリ (`root/mob_data/`)。
     */
    object Mob : Dir("mob_data") {
        /** Mob定義フォルダ (`root/mob_data/mobs/`) */
        object Mobs : Dir("mobs", Mob) {
            /**
             * 特定のMob IDに対応する設定ファイル。
             * @param id Mobの識別子（ファイル名になります）
             */
            class File(id: String) : YmlPath(Mobs, id)
        }
    }

    /**
     * プレイヤー個別のデータディレクトリ (`root/player_data/`)。
     */
    object Player : Dir("player_data") {

        /**
         * 特定のプレイヤー専用のディレクトリ。
         *
         * ### 使用例:
         * ```kotlin
         * val userDir = Dir.Player.User(uuid.toString())
         * val statsFile = userDir.stats.resolve(plugin.dataFolder)
         * ```
         * @param uuid プレイヤーのUUID文字列
         */
        class User(uuid: String) : Dir(uuid, Player) {

            /** プレイヤーの基本統計データファイル (`stats.yml`) */
            val stats = StatsPath(this)

            /** ストレージ関連のディレクトリ */
            val storage = Storage(this)

            /** プレイヤーディレクトリ直下のファイルを定義するクラス */
            class StatsPath(user: User) : YmlPath(user, "stats")

            /**
             * プレイヤーのストレージディレクトリ (`.../uuid/storage/`)。
             */
            class Storage(user: User) : Dir("storage", user) {
                /** インベントリ保存用ファイル (`inventory.yml`) */
                val inventory = InventoryPath(this)

                /** バックパック用ディレクトリ */
                val backpack = Backpack(this)

                class InventoryPath(storage: Storage) : YmlPath(storage, "inventory")

                /**
                 * バックパックディレクトリ (`.../uuid/storage/backpack/`)。
                 */
                class Backpack(storage: Storage) : Dir("backpack", storage) {

                    /**
                     * 定数
                     * */
                    companion object {
                        const val MIN_SLOT = 0
                        const val MAX_SLOT = 17
                    }

                    /**
                     * 特定のインデックスに対応するスロットデータを取得します。
                     * [MIN_SLOT]未満は[MIN_SLOT]に、[MAX_SLOT]より大きい場合は[MAX_SLOT]に丸められます。
                     * @param index スロット番号
                     */
                    fun slot(index: Int): SlotPath {
                        // indexを 0..17 の範囲内に収める
                        val clampedIndex = index.coerceIn(MIN_SLOT, MAX_SLOT)
                        return SlotPath(this, clampedIndex)
                    }

                    /**
                     * バックパック内の各スロットに対応するファイル定義。
                     */
                    class SlotPath(backpack: Backpack, index: Int) : YmlPath(backpack, index.toString())
                }

                /**
                 * エンダーチェストディレクトリ (`.../uuid/storage/ender_chest/`)
                 * */
                class EnderChest(storage: Storage) : Dir("ender_chest", storage) {

                    /**
                     * 定数
                     * */
                    companion object {
                        const val MIN_SLOT = 0
                        const val MAX_SLOT = 8
                    }

                    /**
                     * 特定のインデックスに対応するスロットデータを取得します。
                     * [MIN_SLOT]未満は[MIN_SLOT]に、[MAX_SLOT]より大きい場合は[MAX_SLOT]に丸められます。
                     * @param index スロット番号
                     */
                    fun slot(index: Int): SlotPath {
                        // indexを 0..17 の範囲内に収める
                        val clampedIndex = index.coerceIn(MIN_SLOT, MAX_SLOT)
                        return SlotPath(this, clampedIndex)
                    }

                    /**
                     * バックパック内の各スロットに対応するファイル定義。
                     */
                    class SlotPath(enderChest: EnderChest, index: Int) : YmlPath(enderChest, index.toString())
                }
            }
        }
    }
}