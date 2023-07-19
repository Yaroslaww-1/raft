package ucu.edu.clients

import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

interface Client {
    suspend fun requestVote(req: RequestVote.Request): RequestVote.Response?
    suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response?
    suspend fun appendCommand(command: String, depth: Int)

    fun isolate()
    fun connect()

    fun isConnected(): Boolean
    fun sourceId(): Int
    fun destinationId(): Int
}