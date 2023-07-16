package ucu.edu.node

import ucu.edu.clients.Client
import ucu.edu.config.Config
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote
import java.time.Instant
import java.time.temporal.ChronoUnit

class Node(
    val id: Int,
    val clients: List<Client>,
) {
    val majority = (1 + clients.size) / 2 + 1

    var term: Int = 1
    var votedFor: Int? = null
    var running = true

    private var state: State = Follower(this)

    fun isLeader() = this.state is Leader
    fun isFollower() = this.state is Follower
    fun isCandidate() = this.state is Candidate

    fun stateName() = state.javaClass.simpleName

    fun canVote(candidateTerm: Int, candidateId: Int): Boolean {
        if (candidateTerm > term) return true
        if (candidateTerm == term && (votedFor == null || votedFor == candidateId)) return true
        // TODO add logIndex checks
        return false
    }

    fun start() {
        running = true
        state.start()
    }

    fun stop() {
        running = false
        state.stop()
    }

    fun transitTo(state: State) {
        println("[${Instant.now()}] node $id to ${state.javaClass.simpleName}")
        this.state.stop()
        this.state = state
        if (running) this.state.start()
    }

    suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        println("[${Instant.now()}] ${stateName()} ${id} requestVote from ${req.candidateId}")
        return this.state.requestVote(req)
    }

    suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        println("[${Instant.now()}] ${stateName()} ${id} appendEntries ${req.leaderId}")
        return this.state.appendEntries(req)
    }
}