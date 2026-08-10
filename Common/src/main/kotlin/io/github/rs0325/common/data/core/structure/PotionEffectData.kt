package io.github.rs0325.common.data.core.structure

import io.github.rs0325.common.EffectType
import io.github.rs0325.common.data.core.DeepCopyable
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class PotionEffectData(
    @Setting("type")
    var type: EffectType = EffectType.MAX_HEALTH,

    @Setting("level")
    var level: Int = 0,

    @Setting("time")
    var time: Long = 0L
) : DeepCopyable<PotionEffectData> {

    override fun deepCopy(): PotionEffectData {
        return this.copy()
    }
}