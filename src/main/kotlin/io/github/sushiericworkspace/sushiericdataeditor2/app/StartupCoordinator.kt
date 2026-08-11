package io.github.sushiericworkspace.sushiericdataeditor2.app

import io.github.sushiericworkspace.sushiericdataeditor2.update.UpdateInfo

sealed interface StartupPreparationResult {
    data object Ready : StartupPreparationResult
    data class UpdateRequired(val updateInfo: UpdateInfo) : StartupPreparationResult
    data class Failure(val cause: Throwable) : StartupPreparationResult
}

/**
 * 起動前処理のモード分岐です。オフラインでは更新確認関数を一切呼びません。
 */
class StartupCoordinator(
    private val updateCheck: () -> UpdateInfo?
) {
    fun prepare(mode: AppMode): StartupPreparationResult {
        if (mode == AppMode.OFFLINE) return StartupPreparationResult.Ready

        return try {
            updateCheck()?.let(StartupPreparationResult::UpdateRequired)
                ?: StartupPreparationResult.Ready
        } catch (e: Exception) {
            StartupPreparationResult.Failure(e)
        }
    }
}
