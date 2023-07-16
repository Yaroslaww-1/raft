package ucu.edu.clients

import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

interface Client {
    suspend fun requestVote(req: RequestVote.Request): RequestVote.Response?
    suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response?
    fun disable()
    fun reEnable()

    fun nodeId(): Int
}