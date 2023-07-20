package ucu.edu.clients

import kotlinx.coroutines.delay
import ucu.edu.node.Node
import ucu.edu.proto.AppendCommand
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

class LocalClient : Client {
    private var sourceId: Int = -1
    private lateinit var destination: Node
    private var connected = true

    fun sourceId(): Int {
        return sourceId
    }

    override fun destinationId(): Int {
        return destination.id
    }

    fun isConnected(): Boolean {
        return connected
    }

    fun initialize(sourceId: Int, backend: Node) {
        this.sourceId = sourceId
        this.destination = backend
    }

    fun isolate() {
        connected = false
    }

    fun connect() {
        connected = true
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response? {
        delay((5..10).random().toLong())
        return if (connected) destination.requestVote(req) else null
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response? {
        delay((5..10).random().toLong())
        return if (connected) destination.appendEntries(req) else null
    }

    override suspend fun appendCommand(req: AppendCommand.Request) {
        destination.appendCommand(req)
    }
}