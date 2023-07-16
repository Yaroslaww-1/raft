package ucu.edu.node

import kotlinx.coroutines.*
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

class Candidate(val node: Node) : State {
    private var election: Job? = null

    override fun start() {
        election = GlobalScope.launch {
            while (true) {
                startLeaderElection()
            }
        }
    }

    override fun stop() {
        election?.cancel()
    }

    private suspend fun startLeaderElection() {
        node.term++

        val votes = mutableSetOf<Int>()
        node.votedFor = node.id
        votes.add(node.id)
        val requiredVotes = node.majority

        node.clients
            .map {
                val request = RequestVote.Request(
                    node.term,
                    node.id,
                )

                // TODO: add retry here with election timeout
                coroutineScope {
                    async {
                        val response = it.requestVote(request)
                        if (response == null) null else it to response
                    }
                }
            }
            .mapNotNull { withTimeoutOrNull(node.scaledTime(50)) { it.await() } }
            .forEach { (client, response) ->
                if (response.term > node.term) {
                    node.transitTo(Follower(node))
                    return@startLeaderElection
                }

                if (response.voteGranted) {
                    votes.add(client.nodeId())
                }

                if (votes.size >= requiredVotes) {
                    node.transitTo(Leader(node))
                    return@startLeaderElection
                }
            }
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        println("candidate ${node.id} requestVote")
        val granted = node.canVote(req.term, req.candidateId)

        if (granted) {
            node.term = req.term
            node.votedFor = req.candidateId
            node.transitTo(Follower(node))
        }

        return RequestVote.Response(node.term, granted)
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        println("candidate ${node.id} appendEntries")
        if (req.term > node.term) {
            node.term = req.term
            node.votedFor = null
            node.transitTo(Follower(node))
            return AppendEntries.Response(node.term, true)
        }

        return AppendEntries.Response(node.term, false)
    }
}