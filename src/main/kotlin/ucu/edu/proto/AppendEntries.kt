package ucu.edu.proto

import kotlinx.serialization.Serializable

object AppendEntries {
    @Serializable
    data class LogEntry(
        val term: Int,
        val command: String
    )

    @Serializable
    data class Request(
        val term: Int,
        val leaderId: Int,
        val prevLogIndex: Int,
        val prevLogTerm: Int,
        val entries: List<LogEntry>,
        val leaderCommit: Int
    )

    @Serializable
    data class Response(
        val term: Int,
        val success: Boolean
    )
}
