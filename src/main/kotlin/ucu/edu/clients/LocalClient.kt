package ucu.edu.clients

import kotlinx.coroutines.delay
import ucu.edu.node.Node
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

class LocalClient : Client {
    private lateinit var backend: Node
    private var enabled = true

    fun initialize(backend: Node) {
        this.backend = backend
    }

    fun enable() {
        enabled = true
    }

    fun disable() {
        enabled = false
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response? {
        delay((10..20).random().toLong())
        return if (enabled) backend.requestVote(req) else null
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response? {
        delay((10..20).random().toLong())
        return if (enabled) backend.appendEntries(req) else null
    }

    override fun nodeId(): Int {
        return backend.id
    }
}