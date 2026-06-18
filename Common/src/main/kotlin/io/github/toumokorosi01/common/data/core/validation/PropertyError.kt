package io.github.toumokorosi01.common.data.core.validation

import kotlin.reflect.KProperty

/**
 * 検証エラーを保持する専用のクラス（Mapのキー対応版）
 */
data class PropertyError(
    /** 変数そのものへの参照（this::stats など） */
    val property: KProperty<*>,
    /** ユーザーへの警告メッセージ */
    val message: String,
    /** Mapのキー（StatsTypeなど）を特定するためのオプショナルな情報 */
    val key: Any? = null
)