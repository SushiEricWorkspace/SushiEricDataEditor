package io.github.sushiericworkspace.sushiericdataeditor2.editor.main.item.tree

import io.github.sushiericworkspace.common.data.item.data.ItemData
import io.github.sushiericworkspace.sushiericdataeditor2.editor.main.item.ItemEditorFactory
import io.github.sushiericworkspace.sushiericdataeditor2.editor.tree.EditorContextMenuFactory
import io.github.sushiericworkspace.sushiericdataeditor2.editor.tree.EditorFolderGraphicFactory
import io.github.sushiericworkspace.sushiericdataeditor2.editor.tree.EditorTreeCell

/**
 * Itemエディタ用TreeCell。
 *
 * 実装本体は editor.tree.EditorTreeCell に共通化し、
 * Item固有の Graphic / Drag判定 / Move処理だけを注入する。
 */
class LoreDragDropTreeCell(
    itemData: ItemData,
    refreshButtonVisual: (String) -> Unit,
    onRefresh: (TreeRow) -> Unit,
    loreTreeUiIdMemory: LoreTreeUiIdMemory,
    folderGraphicFactory: EditorFolderGraphicFactory<TreeRow>? = null,
    contextMenuFactory: EditorContextMenuFactory<TreeRow>? = null
) : EditorTreeCell<TreeRow>(
    graphicFactory = ItemEditorFactory(itemData, refreshButtonVisual),
    dragValidator = ItemTreeDragValidator,
    moveHandler = ItemTreeMoveHandler(itemData, refreshButtonVisual, loreTreeUiIdMemory),
    onRefresh = onRefresh,
    folderGraphicFactory = folderGraphicFactory,
    contextMenuFactory = contextMenuFactory
)
