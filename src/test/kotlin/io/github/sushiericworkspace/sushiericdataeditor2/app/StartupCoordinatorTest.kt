package io.github.sushiericworkspace.sushiericdataeditor2.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StartupCoordinatorTest {
    @Test
    fun `オフラインでは更新確認を呼ばない`() {
        var updateChecks = 0
        val coordinator = StartupCoordinator {
            updateChecks++
            null
        }

        assertIs<StartupPreparationResult.Ready>(coordinator.prepare(AppMode.OFFLINE))
        assertEquals(0, updateChecks)
    }

    @Test
    fun `オンラインでは更新確認後に準備完了する`() {
        var updateChecks = 0
        val coordinator = StartupCoordinator {
            updateChecks++
            null
        }

        assertIs<StartupPreparationResult.Ready>(coordinator.prepare(AppMode.ONLINE))
        assertEquals(1, updateChecks)
    }

    @Test
    fun `オンライン更新確認失敗を区別する`() {
        val coordinator = StartupCoordinator {
            error("network")
        }

        assertIs<StartupPreparationResult.Failure>(coordinator.prepare(AppMode.ONLINE))
    }
}
