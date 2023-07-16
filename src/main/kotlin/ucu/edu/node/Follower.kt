package ucu.edu.node

import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote
import ucu.edu.utils.RandomisedTimer

class Follower(val node: Node) : State {
    private val heartbeatTimer = RandomisedTimer(node.scaledTime(150), node.scaledTime(300)) {
        node.transitTo(Candidate(node))
    }

    override fun start() {
        heartbeatTimer.start()
    }

    override fun stop() {
        heartbeatTimer.cancel()
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        println("follower ${node.id} requestVote")
        val granted = node.canVote(req.term, req.candidateId)

        if (granted) {
            node.term = req.term
            node.votedFor = req.candidateId
        }

        return RequestVote.Response(node.term, granted)
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        println("follower ${node.id} appendEntries")
        if (req.term > node.term) {
            node.term = req.term
            node.votedFor = null
        }

        heartbeatTimer.restart()

        return AppendEntries.Response(node.term, true)
    }
}