package io.github.toumokorosi01.common.data.core

import io.github.toumokorosi01.common.Dir
import io.github.toumokorosi01.common.data.item.data.ItemData
import io.github.toumokorosi01.common.data.mob.data.MobData
import io.github.toumokorosi01.common.data.ore.data.OreData

/**
 * 管理データの種類ごとに必要な保存先情報をまとめるデータ種別定義。
 *
 * [Item]、[Ore]、[Mob]のように、扱うデータ型ごとに
 * リモートディレクトリ、カテゴリディレクトリ、ファイルパス生成処理、
 * 新規データ生成処理を保持します。
 *
 * 実際の読み込み・保存処理は、[DataType]がsealedであることを利用して
 * 呼び出し側で`when`分岐し、データ種別ごとに対応するManagerへ委譲します。
 *
 * @param T このデータ種別が扱う管理データ型。[ManagedData]を実装している必要があります。
 * @property categoryDirName ローカルバックアップで使用するカテゴリディレクトリ名。
 * @property displayName 画面表示やログなどで使用するデータ種別名。
 * @property dir 一覧取得時に参照するリモートディレクトリ。
 * @property pathOf ファイル名からリモート上の対象ファイルパスを生成する関数。
 * @property createDefault このデータの新規インスタンスを生成する処理。
 */
sealed class DataType<T : ManagedData<T, *>>(
    val categoryDirName: String,
    val displayName: String,
    val dir: Dir,
    val pathOf: (String) -> io.github.toumokorosi01.common.Path,
    val createDefault: (String) -> T
) {
    /**
     * アイテムデータを表すデータ種別。
     */
    data object Item : DataType<ItemData>(
        categoryDirName = "items",
        displayName = "アイテム",
        dir = Dir.Item.Stats,
        pathOf = { fileName -> Dir.Item.Stats.File(fileName) },
        createDefault = { id -> ItemData(id = id) }
    )

    /**
     * 鉱石データを表すデータ種別。
     */
    data object Ore : DataType<OreData>(
        categoryDirName = "ores",
        displayName = "鉱石",
        dir = Dir.Ore.Ores,
        pathOf = { fileName -> Dir.Ore.Ores.File(fileName) },
        createDefault = { id -> OreData(id = id) }
    )

    /**
     * モブデータを表すデータ種別。
     */
    data object Mob : DataType<MobData>(
        categoryDirName = "mobs",
        displayName = "モブ",
        dir = Dir.Mob.Mobs,
        pathOf = { fileName -> Dir.Mob.Mobs.File(fileName) },
        createDefault = { id -> MobData(id = id) }
    )
}