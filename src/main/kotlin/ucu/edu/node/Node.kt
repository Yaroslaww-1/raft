package ucu.edu.node

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import ucu.edu.clients.Client
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

class Node(clients: List<Client>) {
    var isLeader = false
        private set

    suspend fun requestVote(req: RequestVote.Request): RequestVote.Response = runBlocking {
        delay(10)
        RequestVote.Response(1, true)
    }

    suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response = runBlocking {
        delay(10)
        AppendEntries.Response(1, true)
    }
}