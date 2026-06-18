package io.github.toumokorosi01.sushiericdataeditor2.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val version: String,
    val windowsDownloadUrl: String,
    val macDownloadUrl: String,
    val notes: List<String> = emptyList()
) {
    fun downloadUrlForCurrentOs(): String {
        val osName = System.getProperty("os.name").lowercase()

        return when {
            osName.contains("win") -> windowsDownloadUrl
            osName.contains("mac") -> macDownloadUrl
            else -> windowsDownloadUrl
        }
    }
}