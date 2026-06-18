package io.github.toumokorosi01.sushiericdataeditor2.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val notes: List<String> = emptyList()
)