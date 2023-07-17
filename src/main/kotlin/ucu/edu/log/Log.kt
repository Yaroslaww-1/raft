package ucu.edu.log

import ucu.edu.proto.AppendEntries
import kotlin.math.max
import kotlin.math.min


class Log {
    private var entries = mutableListOf<LogEntry>()

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

    fun tryAppend(req: AppendEntries.Request): Boolean {
        println("tryAppend $req")

        if (req.prevLogIndex != 0 && this[req.prevLogIndex]?.term != req.prevLogTerm) return false

        if (req.entries.isEmpty()) return true

        insertStartingFrom(req.prevLogIndex + 1, req.entries.map { LogEntry(it.term, it.command) })

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
            println(index)
            var insertIndex = index
            for (entry in newEntries) {
                val conflict = this[insertIndex]?.term != entry.term
                if (conflict) {
                    removeStartingFrom(insertIndex)
                }
                entries[insertIndex - 1] = entry
                insertIndex++
            }
        }
    }

    private fun removeStartingFrom(index: Int) {
        entries.subList(max(index - 1, 0), entries.size).clear()
    }
}