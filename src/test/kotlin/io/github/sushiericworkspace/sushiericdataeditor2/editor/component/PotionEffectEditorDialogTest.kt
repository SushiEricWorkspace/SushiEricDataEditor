package io.github.sushiericworkspace.sushiericdataeditor2.editor.component

import io.github.sushiericworkspace.common.data.effect.model.MutableLeveledTimedEffectData
import io.github.sushiericworkspace.common.data.effect.model.SushiEricEffectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PotionEffectEditorDialogTest {

    @Test
    fun `LevelとTimeの4分類に応じた入力項目を返す`() {
        assertEquals(
            setOf(EffectEditorField.LEVEL, EffectEditorField.TIME),
            visibleEffectEditorFields(hasLevel = true, hasTime = true)
        )
        assertEquals(
            setOf(EffectEditorField.LEVEL),
            visibleEffectEditorFields(hasLevel = true, hasTime = false)
        )
        assertEquals(
            setOf(EffectEditorField.TIME),
            visibleEffectEditorFields(hasLevel = false, hasTime = true)
        )
        assertEquals(
            emptySet(),
            visibleEffectEditorFields(hasLevel = false, hasTime = false)
        )
    }

    @Test
    fun `入力項目はCommonのmarker interfaceから決定する`() {
        assertEquals(
            setOf(EffectEditorField.LEVEL, EffectEditorField.TIME),
            visibleEffectEditorFields(SushiEricEffectType.POISON)
        )
    }

    @Test
    fun `EffectTypeに対応するデータへ必要な入力値を設定する`() {
        val effect = createEditorEffectData(
            type = SushiEricEffectType.REGENERATION,
            level = 3,
            time = 120L
        )

        val leveledTimed = assertIs<MutableLeveledTimedEffectData>(effect)
        assertEquals(SushiEricEffectType.REGENERATION, leveledTimed.type)
        assertEquals(3, leveledTimed.level)
        assertEquals(120L, leveledTimed.time)
    }
}
