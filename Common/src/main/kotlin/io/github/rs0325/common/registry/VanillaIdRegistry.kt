package io.github.rs0325.common.registry

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class IdList(
    val items: List<String> = emptyList(),
    val entities: List<String> = emptyList(),
    val blocks: List<String> = emptyList()
)

object VanillaIdRegistry {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private fun loadList(
        resourcePath: String,
        selector: (IdList) -> List<String>
    ): List<String> {
        val stream = VanillaIdRegistry::class.java.getResourceAsStream(resourcePath)
            ?: return emptyList()

        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            val text = reader.readText()
            val list = json.decodeFromString<IdList>(text)

            selector(list)
        }
    }

    private val loadedItems: List<String> by lazy {
        loadList("/item_list.json") { it.items }
    }

    private val loadedEntities: List<String> by lazy {
        loadList("/entity_list.json") { it.entities }
    }

    private val loadedBlocks: List<String> by lazy {
        loadList("/full_block_list.json") { it.blocks }
    }

    val defaultItem: String
        get() = loadedItems[0]

    val defaultEntity: String
        get() = loadedEntities[0]

    val defaultBlock: String
        get() = loadedBlocks[0]

    val allItems: List<String>
        get() = loadedItems

    val allEntities: List<String>
        get() = loadedEntities

    val allBlocks: List<String>
        get() = loadedBlocks

    fun containsItem(id: String): Boolean {
        return allItems.contains(id)
    }

    fun containsEntity(id: String): Boolean {
        return allEntities.contains(id)
    }

    fun containsBlock(id: String): Boolean {
        return allBlocks.contains(id)
    }

    fun searchItems(query: String): List<String> {
        if (query.isBlank()) return allItems
        return allItems.filter { it.contains(query, ignoreCase = true) }
    }

    fun searchEntities(query: String): List<String> {
        if (query.isBlank()) return allEntities
        return allEntities.filter { it.contains(query, ignoreCase = true) }
    }

    fun searchBlocks(query: String): List<String> {
        if (query.isBlank()) return  allBlocks
        return allBlocks.filter { it.contains(query, ignoreCase = true) }
    }
}