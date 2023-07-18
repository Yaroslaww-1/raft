package ucu.edu.node

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ucu.edu.clients.Client
import ucu.edu.config.Config
import ucu.edu.log.Log
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
    var log = Log()

    private var state: State = Follower(this)

    fun isLeader() = this.state is Leader
    fun isFollower() = this.state is Follower
    fun isCandidate() = this.state is Candidate

    fun stateName() = state.javaClass.simpleName

    fun canVote(candidate: RequestVote.Request): Boolean {
        if (candidate.term < term) return false

        if (candidate.term == term) return (votedFor == null || votedFor == candidate.candidateId)

        if (candidate.term > term) {
            if (log.isNotEmpty() && candidate.lastLogTerm < log.lastTerm()) return false
            if (log.isNotEmpty() && candidate.lastLogTerm == log.lastTerm() && candidate.lastLogIndex < log.prevIndex()) return false
        }

        return true
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
        println("[${Instant.now()}] ${stateName()} ${id} appendEntries ${req.leaderId} ${req.entries}")
        return this.state.appendEntries(req)
    }

    suspend fun appendCommand(command: String, depth: Int = 0) {
        if (depth > 1) return

        if (!isLeader()) {
            clients
                .map {
                    coroutineScope {
                        async {
                            it.appendCommand(command, depth + 1)
                        }
                    }
                }
                .map { it.await() }
        } else {
            (state as Leader).appendCommand(command)
        }
    }

    fun getCommands(): List<String> {
        return log.getValues()
    }
}