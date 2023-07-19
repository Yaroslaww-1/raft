package ucu.edu.node

import kotlinx.coroutines.*
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote
import java.time.Instant
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timerTask

class Leader(val node: Node) : State {
    private var heartbeatTimer: Timer? = null
    private val nextIndex = node.clients.associateWith { _ -> node.log.nextIndex() }.toMutableMap()
    private val matchIndex = node.clients.associateWith { _ -> 0 }.toMutableMap()

    override fun start() {
        heartbeatTimer = fixedRateTimer("Leader heartbeat", initialDelay = 0, period = 50) {
            println("[${Instant.now()}] leader ${node.id} STARTING TO SEND $node ${node.clients.filter { it.isConnected() }.map { it.destinationId() }} connected")
            runBlocking {
                yield()
                node.clients
                    .map {
                        val prevLogIndex = nextIndex[it]!! - 1
                        val prevLogTerm = node.log[prevLogIndex]?.term ?: -1

                        val entries = node.log.startingFrom(prevLogIndex + 1).map { e -> AppendEntries.LogEntry(e.term, e.command) }

                        val request = AppendEntries.Request(
                            node.term,
                            node.id,
                            prevLogIndex,
                            prevLogTerm,
                            entries,
                            node.log.commitIndex
                        )

                        yield()

                        async {
                            val response = it.appendEntries(request)
                            if (response == null) null else Triple(it, response, entries)
                        }
                    }
                    .mapNotNull { withTimeoutOrNull(30) { it.await() } }
                    .forEach { (client, response, entries) ->
                        yield()

                        if (response.term > node.term) {
                            node.term = response.term
                            node.votedFor = null
                            println("transit 1")
                            node.transitTo(Follower(node))
                            return@runBlocking
                        }

                        if (response.success) {
                            nextIndex[client] = nextIndex[client]!! + entries.size
                            matchIndex[client] = matchIndex[client]!! + entries.size
                        } else {
                            nextIndex[client] = nextIndex[client]!! - 1
                        }
                    }
            }

            val newCommit = ((node.log.commitIndex + 1)..Int.MAX_VALUE)
                .takeWhile { newCommit ->
                    val clusterApprove = matchIndex.entries.filter { it.value >= newCommit }.count() + 1 >= node.majority
                    val logLastTermMatch = node.log[newCommit]?.term == node.term
                    clusterApprove && logLastTermMatch
                }
                .lastOrNull()
            if (newCommit != null) {
                println("[${Instant.now()}] leader ${node.id} COMMIT $newCommit")
                node.log.commit(newCommit)
            }
        }
    }

    override fun stop() {
        heartbeatTimer!!.cancel()
        heartbeatTimer!!.purge()
        println("leader ${node} cancelled ${Instant.now()}")
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        val granted = node.canVote(req)

        if (granted) {
            node.term = req.term
            node.votedFor = req.candidateId
            println("transit 2")
            node.transitTo(Follower(node))
        }

        return RequestVote.Response(node.term, granted)
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        if (req.term < node.term) return AppendEntries.Response(node.term, false)

        if (req.term > node.term) {
            node.term = req.term
            node.votedFor = null
            println("transit 3")
            node.transitTo(Follower(node))
        }

        val appended = node.log.tryAppend(req);

        if (req.leaderId != node.id && appended) {
            println("Multiple leaders $req")
            println("transit 4")
            node.transitTo(Follower(node))
        }

        return AppendEntries.Response(node.term, appended)
    }

    fun appendCommand(command: String) {
        node.log.append(node.term, command)
    }
}