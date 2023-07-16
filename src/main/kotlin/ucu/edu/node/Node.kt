package ucu.edu.node

import ucu.edu.clients.Client
import ucu.edu.config.Config
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

class Node(
    val id: Int,
    val clients: List<Client>,
) {
    val majority = (1 + clients.size) / 2 + 1

    var term: Int = 1
    var votedFor: Int? = null

    private val config = Config(1)
    private var state: State = Follower(this)

    fun isLeader() = this.state is Leader
    fun isFollower() = this.state is Follower
    fun isCandidate() = this.state is Candidate

    fun canVote(candidateTerm: Int, candidateId: Int): Boolean {
        if (candidateTerm > term) return true
        if (candidateTerm == term && (votedFor == null || votedFor == candidateId)) return true
        // TODO add logIndex checks
        return false
    }

    fun start() {
        state.start()
    }

    fun stop() {
        state.stop()
    }

    fun scaledTime(base: Long): Long {
        return base * config.timeScale
    }

    fun transitTo(state: State) {
        this.state.stop()
        println("node $id to ${state.javaClass.canonicalName}")
        this.state = state
        this.state.start()
    }

    suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        return this.state.requestVote(req);
    }

    suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        return this.state.appendEntries(req);
    }
}