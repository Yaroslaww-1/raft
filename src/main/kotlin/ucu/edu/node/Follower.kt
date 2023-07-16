package ucu.edu.node

import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote
import ucu.edu.utils.RandomisedTimer

class Follower(val node: Node) : State {
    private var heartbeatTimer = RandomisedTimer(150, 300) {
        node.transitTo(Candidate(node))
    }

    override fun start() {
        heartbeatTimer.start()
    }

    override fun stop() {
        heartbeatTimer.cancel()
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        val granted = node.canVote(req.term, req.candidateId)

        if (granted) {
            node.term = req.term
            node.votedFor = req.candidateId
            heartbeatTimer.restart()
        }

        return RequestVote.Response(node.term, granted)
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        if (req.term > node.term) {
            node.term = req.term
            node.votedFor = null
        }

        heartbeatTimer.restart()

        return AppendEntries.Response(node.term, true)
    }
}