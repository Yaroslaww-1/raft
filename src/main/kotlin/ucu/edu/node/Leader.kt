package ucu.edu.node

import kotlinx.coroutines.*
import kotlinx.coroutines.time.withTimeoutOrNull
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote
import java.time.Duration
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask

class Leader(val context: Context) : State {
    private var heartbeatTimer: Timer = Timer()

    override fun start() {
        heartbeatTimer.schedule(timerTask {
            runBlocking {
                context.clients
                    .map {
                        val request = AppendEntries.Request(
                            context.term,
                            context.nodeId,
                        )

                        async {
                            val response = it.appendEntries(request)
                            if (response == null) null else it to response
                        }
                    }
                    .mapNotNull { withTimeoutOrNull(50 * context.config.timeScale) { it.await() } }
                    .forEach { (client, response) ->
                        when {
                            response.success -> {

                            }
                            !response.success -> {

                            }
                        }
                    }
            }
        }, 100 * context.config.timeScale, 100 * context.config.timeScale)
    }

    override fun stop() {
        heartbeatTimer.cancel()
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        println("leader ${context.nodeId} requestVote")
        val granted = context.canVote(req.term, req.candidateId)

        if (granted) {
            context.term = req.term
            context.votedFor = req.candidateId
            transitToFollower()
        }

        return RequestVote.Response(context.term, granted)
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        println("leader ${context.nodeId} appendEntries")
        if (req.term > context.term) {
            context.term = req.term
            context.votedFor = null
            transitToFollower()
        }

        return AppendEntries.Response(context.term, true)
    }

    fun transitToFollower() {
        println("leader ${context.nodeId} to follower")
        context.setState(Follower(context))
    }
}