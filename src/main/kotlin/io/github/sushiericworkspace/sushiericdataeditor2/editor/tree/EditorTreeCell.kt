package io.github.sushiericworkspace.sushiericdataeditor2.editor.tree

import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.input.ClipboardContent
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode

/**
 * エディタ用 TreeView の共通セル。
 *
 * - Folder 行は text として表示
 * - Editor 行は EditorGraphicFactory に Node 生成を委譲
 * - ドラッグ可否は TreeDragValidator に委譲
 * - 実データの移動は TreeMoveHandler に委譲
 */
open class EditorTreeCell<R : EditorTreeRow>(
    private val graphicFactory: EditorGraphicFactory<R>,
    private val dragValidator: TreeDragValidator<R>,
    private val moveHandler: TreeMoveHandler<R>,
    private val onRefresh: (R) -> Unit,
    private val folderGraphicFactory: EditorFolderGraphicFactory<R>? = null,
    private val contextMenuFactory: EditorContextMenuFactory<R>? = null
) : TreeCell<R>() {

    companion object {
        private var draggedTreeItem: TreeItem<*>? = null
    }

    init {
        if (!styleClass.contains("custom-tree-cell")) {
            styleClass.add("custom-tree-cell")
        }

        addEventFilter(MouseEvent.MOUSE_PRESSED) {
            treeView?.selectionModel?.clearSelection()
        }

        addEventFilter(MouseEvent.MOUSE_RELEASED) {
            treeView?.selectionModel?.clearSelection()
        }

        setOnDragDetected { event ->
            val currentItem = treeItem ?: return@setOnDragDetected
            if (currentItem.value.kind == EditorTreeRow.Kind.Folder) {
                draggedTreeItem = currentItem
                startDragAndDrop(TransferMode.MOVE).setContent(
                    ClipboardContent().apply { putString("") }
                )
                event.consume()
            }
        }

        setOnDragOver { event ->
            val source = castDraggedTreeItem()
            val target = treeItem

            if (dragValidator.canDrop(source, target)) {
                event.acceptTransferModes(TransferMode.MOVE)
            }
            event.consume()
        }

        setOnDragDropped { event ->
            val source = castDraggedTreeItem()
            val target = treeItem

            val success = if (source != null && target != null) {
                try {
                    val moved = moveHandler.move(source, target)
                    if (moved) {
                        target.parent?.value?.let(onRefresh)
                    }
                    moved
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else {
                false
            }

            event.isDropCompleted = success
            event.consume()
        }

        setOnDragDone { event ->
            draggedTreeItem = null
            event.consume()
        }
    }

    override fun updateItem(row: R?, empty: Boolean) {
        super.updateItem(row, empty)

        styleClass.removeAll("folder-cell", "item-cell")

        if (empty || row == null) {
            text = null
            graphic = null
            contextMenu = null
            style = ""
            return
        }

        contextMenu = contextMenuFactory?.createContextMenu(row)

        when (row.kind) {
            EditorTreeRow.Kind.Folder -> {
                styleClass.add("folder-cell")

                val folderGraphic = folderGraphicFactory?.createFolderGraphic(row)

                if (folderGraphic != null) {
                    text = null
                    graphic = folderGraphic
                } else {
                    text = row.label
                    graphic = null
                }
            }

            EditorTreeRow.Kind.Editor -> {
                styleClass.add("item-cell")
                text = null
                graphic = graphicFactory.createGraphic(row)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun castDraggedTreeItem(): TreeItem<R>? {
        return draggedTreeItem as? TreeItem<R>
    }
}
