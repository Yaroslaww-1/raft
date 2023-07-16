package ucu.edu.node

import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote
import ucu.edu.utils.RandomisedTimer
import java.time.Instant

class Follower(val context: Context) : State {
    private val heartbeatTimer = RandomisedTimer(150 * context.config.timeScale, 300 * context.config.timeScale) {
        transitToCandidate()
    }

    override fun start() {
        heartbeatTimer.start()
    }

    override fun stop() {
        heartbeatTimer.cancel()
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        println("follower ${context.nodeId} requestVote")
        val granted = context.canVote(req.term, req.candidateId)

        if (granted) {
            context.term = req.term
            context.votedFor = req.candidateId
        }

        return RequestVote.Response(context.term, granted)
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        println("follower ${context.nodeId} appendEntries")
        if (req.term > context.term) {
            context.term = req.term
            context.votedFor = null
        }

        heartbeatTimer.restart()

        return AppendEntries.Response(context.term, true)
    }

    private fun transitToCandidate() {
        println("follower ${context.nodeId} to candidate ${Instant.now()}")
        context.setState(Candidate(context))
    }
}