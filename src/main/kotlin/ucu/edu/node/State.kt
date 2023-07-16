package ucu.edu.node

import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

interface State {
    fun start()
    fun stop()
    suspend fun requestVote(req: RequestVote.Request): RequestVote.Response
    suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response
}