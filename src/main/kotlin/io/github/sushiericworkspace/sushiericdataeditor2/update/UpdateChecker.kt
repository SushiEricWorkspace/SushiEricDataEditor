package io.github.sushiericworkspace.sushiericdataeditor2.update

import kotlinx.serialization.json.Json
import java.net.URI

class UpdateChecker(
    private val updateJsonUrl: String
) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun check(): UpdateInfo? {
        val text = readUpdateJson()

        val updateInfo = json.decodeFromString<UpdateInfo>(text)

        return if (isNewer(updateInfo.version, AppVersion.CURRENT)) {
            updateInfo
        } else {
            null
        }
    }

    private fun readUpdateJson(): String {
        val connection = URI(updateJsonUrl)
            .toURL()
            .openConnection()
            .apply {
                connectTimeout = 5000
                readTimeout = 5000
            }

        return connection.getInputStream().bufferedReader().use { reader ->
            reader.readText()
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

        val maxSize = maxOf(remoteParts.size, currentParts.size)

        for (index in 0 until maxSize) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }

            if (remotePart > currentPart) {
                return true
            }

            if (remotePart < currentPart) {
                return false
            }
        }

        return false
    }
}