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

    override fun start() {
        heartbeatTimer = fixedRateTimer("Leader heartbeat", initialDelay = 0, period = 50) {
            println("leader STARTING TO SEND $node ${Instant.now()}")
            runBlocking {
                node.clients
                    .map {
                        val request = AppendEntries.Request(
                            node.term,
                            node.id,
                        )

                        yield()

                        async {
                            println("leader sends $node-${node.id} -> ${it.nodeId()} ${Instant.now()}")
                            val response = it.appendEntries(request)
                            if (response == null) null else it to response
                        }
                    }
                    .mapNotNull { withTimeoutOrNull(30) { it.await() } }
                    .forEach { (client, response) ->
                        when {
                            response.success -> {

                            }
                            !response.success -> {

                            }
                        }
                    }
            }
        }
    }

    override fun stop() {
        println("leader ${node} trying to cancel ${Instant.now()}")
        heartbeatTimer!!.cancel()
        println("leader ${node} cancelled ${Instant.now()}")
        heartbeatTimer!!.purge()
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        val granted = node.canVote(req.term, req.candidateId)

        if (granted) {
            node.term = req.term
            node.votedFor = req.candidateId
            node.transitTo(Follower(node))
        }

        return RequestVote.Response(node.term, granted)
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        if (req.term > node.term) {
            node.term = req.term
            node.votedFor = null
            node.transitTo(Follower(node))
        }

        return AppendEntries.Response(node.term, true)
    }
}