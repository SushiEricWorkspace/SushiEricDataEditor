package io.github.rs0325.sushiericdataeditor2.ui.shortcut

import javafx.scene.Scene
import org.slf4j.LoggerFactory

/**
 * Sceneに対してショートカットキーを登録、解除するための管理クラス。
 *
 * JavaFXのショートカットはScene単位で登録されるため、
 * 親画面、モーダル画面など、使いたいSceneごとに登録する必要がある。
 */
object ShortcutManager {
    private val logger = LoggerFactory.getLogger(ShortcutManager::class.java)

    /**
     * 指定したSceneにショートカットキーを登録する。
     *
     * 同じキーのショートカットが既に登録されている場合は上書きされる。
     *
     * @param scene 登録先のScene。
     * @param shortcut 登録するショートカット種別。
     * @param action ショートカット押下時に実行する処理。
     */
    fun register(scene: Scene, shortcut: EditorShortcut, action: () -> Unit) {
        scene.accelerators[shortcut.combination] = Runnable {
            logger.info("ショートカットキーが検出されました: ${shortcut.displayName}")
            action()
        }
    }

    /**
     * 指定したSceneからショートカットキーを解除する。
     *
     * @param scene 解除対象のScene。nullの場合は何もしない。
     * @param shortcut 解除するショートカット種別。
     */
    fun unregister(scene: Scene?, shortcut: EditorShortcut) {
        scene?.accelerators?.remove(shortcut.combination)
    }

    /**
     * 指定したSceneからエディタ用ショートカットをすべて解除する。
     *
     * 今は保存のみだが、EditorShortcutに項目が増えた場合もまとめて解除できる。
     *
     * @param scene 解除対象のScene。nullの場合は何もしない。
     */
    fun unregisterAll(scene: Scene?) {
        if (scene == null) return

        EditorShortcut.entries.forEach { shortcut ->
            scene.accelerators.remove(shortcut.combination)
        }
    }
}