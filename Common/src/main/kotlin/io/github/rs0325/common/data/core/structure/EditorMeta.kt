package io.github.rs0325.common.data.core.structure

import io.github.rs0325.common.data.core.DeepCopyable
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

/**
 * エディター専用の構造
 * */
@ConfigSerializable
data class EditorMeta(
    /** コメントアウト */
    @Setting("comment")
    var comment: MutableList<String> = mutableListOf()
) : DeepCopyable<EditorMeta> {
    override fun deepCopy(): EditorMeta {
        return this.copy(
            comment = comment.toMutableList()
        )
    }
}