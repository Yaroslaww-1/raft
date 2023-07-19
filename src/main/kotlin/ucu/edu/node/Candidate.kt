package ucu.edu.node

import kotlinx.coroutines.*
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

class Candidate(val node: Node) : State {
    private var election: Job? = null

    override fun start() {
        election = GlobalScope.launch {
            delay(50)
            while (true) {
                yield()
                startLeaderElection()
            }
        }
    }

    override fun stop() {
        election?.cancel()
    }

    private suspend fun startLeaderElection() {
        println("node ${node} ${node.id} START LEADER ELECTION")
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
                    node.log.prevIndex(),
                    node.log.lastTerm()
                )

                // TODO: add retry here with election timeout
                coroutineScope {
                    async {
                        val response = it.requestVote(request)
                        if (response == null) null else it to response
                    }
                }
            }
            .mapNotNull { withTimeoutOrNull(30) { it.await() } }
            .forEach { (client, response) ->
                if (response.term > node.term) {
                    node.transitTo(Follower(node))
                    return@startLeaderElection
                }

                if (response.voteGranted) {
                    votes.add(client.destinationId())
                }

                if (votes.size >= requiredVotes) {
                    node.transitTo(Leader(node))
                    return@startLeaderElection
                }
            }

        delay((150L..300L).random())
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        val granted = node.canVote(req)

        if (granted) {
            node.term = req.term
            node.votedFor = req.candidateId
            node.transitTo(Follower(node))
        }

        return RequestVote.Response(node.term, granted)
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        if (req.term < node.term) return AppendEntries.Response(node.term, false)

        if (req.leaderId != node.id) {
            node.transitTo(Follower(node))
        }

        node.term = req.term
        node.votedFor = null
        node.transitTo(Follower(node))
        return AppendEntries.Response(node.term, node.log.tryAppend(req))
    }
}