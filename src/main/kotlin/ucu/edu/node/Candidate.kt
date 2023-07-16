package ucu.edu.node

import kotlinx.coroutines.*
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

class Candidate(val context: Context) : State {
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
        context.term++

        val votes = mutableSetOf<Int>()
        context.votedFor = context.nodeId
        votes.add(context.nodeId)
        val requiredVotes = context.majority

        context.clients
            .map {
                val request = RequestVote.Request(
                    context.term,
                    context.nodeId,
                )

                // TODO: add retry here with election timeout
                coroutineScope {
                    async {
                        val response = it.requestVote(request)
                        if (response == null) null else it to response
                    }
                }
            }
            .mapNotNull { withTimeoutOrNull(50 * context.config.timeScale) { it.await() } }
            .forEach { (client, response) ->
                if (response.term > context.term) {
                    transitToFollower()
                    return@startLeaderElection
                }

                if (response.voteGranted) {
                    votes.add(client.nodeId())
                }

                if (votes.size >= requiredVotes) {
                    transitToLeader()
                    return@startLeaderElection
                }
            }
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        println("candidate ${context.nodeId} requestVote")
        val granted = context.canVote(req.term, req.candidateId)

        if (granted) {
            context.term = req.term
            context.votedFor = req.candidateId
            transitToFollower()
        }

        return RequestVote.Response(context.term, granted)
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        println("candidate ${context.nodeId} appendEntries")
        if (req.term > context.term) {
            context.term = req.term
            context.votedFor = null
            transitToFollower()
            return AppendEntries.Response(context.term, true)
        }

        return AppendEntries.Response(context.term, false)
    }

    fun transitToFollower() {
        println("candidate ${context.nodeId} to follower")
        context.setState(Follower(context))
    }

    fun transitToLeader() {
        println("candidate ${context.nodeId} to leader")
        context.setState(Leader(context))
    }
}