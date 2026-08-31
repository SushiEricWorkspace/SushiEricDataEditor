package io.github.sushiericworkspace.sushiericdataeditor2.editor.view

/**
 * サイドバーに表示するデータの状態を、互いに独立したフラグとして保持します。
 */
internal data class SidebarDataState(
    val selected: Boolean,
    val modified: Boolean,
    val invalid: Boolean
) {
    val styleClasses: List<String>
        get() = buildList {
            if (selected) add(SELECTED_STYLE_CLASS)
            if (modified) add(MODIFIED_STYLE_CLASS)
            if (invalid) add(INVALID_STYLE_CLASS)
        }

    fun displayText(name: String): String = buildString {
        if (invalid) append("⚠ ")
        append(name)
        if (modified) append("  ●")
    }

    fun description(): String? {
        val states = buildList {
            if (selected) add("選択中")
            if (modified) add("未保存の変更あり")
            if (invalid) add("入力内容に問題あり")
        }
        return states.takeIf { it.isNotEmpty() }?.joinToString(" / ")
    }

    companion object {
        val STYLE_CLASSES = listOf(
            SELECTED_STYLE_CLASS,
            MODIFIED_STYLE_CLASS,
            INVALID_STYLE_CLASS
        )

        private const val SELECTED_STYLE_CLASS = "button-selected"
        private const val MODIFIED_STYLE_CLASS = "button-modified"
        private const val INVALID_STYLE_CLASS = "button-invalid"
    }
}
