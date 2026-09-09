package io.github.sushiericworkspace.sushiericservermanager.feature.console

/**
 * WebSocketから受信したログを、JavaFXスレッドへ渡すまで一時保持します。
 *
 * 容量を超えた場合は古いログから破棄し、受信処理を停止させません。
 */
class ConsoleLogBuffer(private val capacity: Int) {
    private val entries = ArrayDeque<ConsoleLogEntry>()

    init {
        require(capacity > 0) { "capacityは1以上である必要があります。" }
    }

    /** 現在保持している件数です。 */
    val size: Int
        @Synchronized get() = entries.size

    /** ログを末尾へ追加します。 */
    @Synchronized
    fun offer(entry: ConsoleLogEntry) {
        while (entries.size >= capacity) entries.removeFirst()
        entries.addLast(entry)
    }

    /** 最大[maxCount]件を古い順に取り出します。 */
    @Synchronized
    fun drain(maxCount: Int): List<ConsoleLogEntry> {
        require(maxCount > 0) { "maxCountは1以上である必要があります。" }
        val count = minOf(maxCount, entries.size)
        return buildList(count) {
            repeat(count) { add(entries.removeFirst()) }
        }
    }

    /** 保持中のログを破棄します。 */
    @Synchronized
    fun clear() = entries.clear()
}

internal fun <T> appendConsoleLogs(
    target: MutableList<T>,
    added: Collection<T>,
    limit: Int
) {
    require(limit > 0) { "limitは1以上である必要があります。" }
    target.addAll(added)
    val overflow = target.size - limit
    if (overflow > 0) target.subList(0, overflow).clear()
}
