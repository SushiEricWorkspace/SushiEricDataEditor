package io.github.toumokorosi01.sushiericdataeditor2.editor.item.diff

import io.github.toumokorosi01.common.StatsType

/**
 * 差分を検出する対象フィールドの定義
 */
enum class DiffField(val categoryName: String) {
    RARITY("レアリティ"),
    DISPLAY_NAME("表示名"),
    LORE("Lore"),
    STATS("ステータス"),
    COMMENT("コメントアウト"),
    DETAIL("詳細データ")
}

/**
 * どのフィールドの、具体的にどの要素（行数やStatsのキー）に差分があるかを一意に特定するための識別子
 */
data class DiffId(
    val field: DiffField,
    val index: Int? = null,       // Lore や説明文の「行インデックス (0始まり)」用
    val statsType: StatsType? = null // Stats の「キー」用
)