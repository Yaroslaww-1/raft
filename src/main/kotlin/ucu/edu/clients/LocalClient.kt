package ucu.edu.clients

import kotlinx.coroutines.channels.Channel
import ucu.edu.node.Node
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

class LocalClient : Client {
    private lateinit var backend: Node

    fun initialize(backend: Node) {
        this.backend = backend
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        return backend.requestVote(req)
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        return backend.appendEntries(req)
    }
}