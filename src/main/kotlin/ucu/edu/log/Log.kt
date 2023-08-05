package ucu.edu.log

import ucu.edu.proto.AppendEntries
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min


class Log(val nodeId: Int) {
    private var entries = CopyOnWriteArrayList<LogEntry>()

    var commitIndex: Int = 0
        private set

    fun nextIndex() = entries.size + 1
    fun prevIndex() = entries.size
    fun lastTerm(): Int = entries.lastOrNull()?.term ?: 0
    fun isNotEmpty(): Boolean = entries.isNotEmpty()

    operator fun get(index: Int) = entries.getOrNull(index - 1)

    fun append(term: Int, command: String) {
        entries.add(LogEntry(term, command))
    }

    @Synchronized fun tryAppend(req: AppendEntries.Request): Boolean {
        if (req.prevLogIndex != 0 && this[req.prevLogIndex - 1]?.term != req.prevLogTerm) return false

        if (req.entries.isEmpty()) return true

        println("[$nodeId] Before insert $entries $req")
        insertStartingFrom(req.prevLogIndex + 1 - 1, req.entries.map { LogEntry(it.term, it.command) }) // -1 because log is 1-indexed, +1 because it's PREV index
        println("[$nodeId] After insert $entries $req")

        if (req.leaderCommit > commitIndex) {
            commit(req.leaderCommit)
        }

        return true
    }

    fun startingFrom(index: Int): List<LogEntry> {
        return entries.filterIndexed { i, _ -> i >= index - 1 }
    }

    fun getValues(): List<String> {
        return entries.map { it.command }
    }

    private fun appendAll(entries: List<LogEntry>) {
        entries.forEach { append(it.term, it.command) }
    }

    fun commit(index: Int) {
        commitIndex = min(prevIndex(), index)
    }

    private fun insertStartingFrom(index: Int, newEntries: List<LogEntry>) {
        if (index > prevIndex()) {
            appendAll(newEntries)
        } else {
            println("$index > ${prevIndex()}")
            var insertIndex = index
            for (entry in newEntries) {
                val conflict = this[insertIndex]?.term != entry.term
                if (conflict) {
                    removeStartingFrom(insertIndex)
                }
                if (insertIndex >= entries.size) entries.add(entry)
                entries[insertIndex] = entry
                insertIndex++
            }
        }
    }

    private fun removeStartingFrom(index: Int) {
        entries.subList(index, entries.size).clear()
    }
}