package ucu.edu.node

import kotlinx.coroutines.*
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote
import java.util.*
import kotlin.concurrent.timerTask

class Leader(val node: Node) : State {
    private var heartbeatTimer: Timer = Timer()

    override fun start() {
        heartbeatTimer.schedule(timerTask {
            runBlocking {
                node.clients
                    .map {
                        val request = AppendEntries.Request(
                            node.term,
                            node.id,
                        )

                        async {
                            val response = it.appendEntries(request)
                            if (response == null) null else it to response
                        }
                    }
                    .mapNotNull { withTimeoutOrNull(node.scaledTime(50)) { it.await() } }
                    .forEach { (client, response) ->
                        when {
                            response.success -> {

                            }
                            !response.success -> {

                            }
                        }
                    }
            }
        }, node.scaledTime(100), node.scaledTime(100))
    }

    override fun stop() {
        heartbeatTimer.cancel()
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