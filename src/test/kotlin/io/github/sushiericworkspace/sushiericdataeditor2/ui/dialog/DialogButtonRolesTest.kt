package io.github.sushiericworkspace.sushiericdataeditor2.ui.dialog

import kotlin.test.Test
import kotlin.test.assertTrue

class DialogButtonRolesTest {
    @Test
    fun `肯定操作をdefaultにしてキャンセル操作をcancelにする`() {
        var defaultButton = false
        var cancelButton = false

        DialogButtonRoles.applyRoles(
            setDefaultButton = { defaultButton = it },
            setCancelButton = { cancelButton = it }
        )

        assertTrue(defaultButton)
        assertTrue(cancelButton)
    }
}
