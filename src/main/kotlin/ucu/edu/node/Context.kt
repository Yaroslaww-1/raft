package ucu.edu.node

import ucu.edu.clients.Client
import ucu.edu.config.Config
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

class Context(
    val nodeId: Int,
    val clients: List<Client>,
) : State {
    val majority = (1 + clients.size) / 2 + 1
    var _state: State
    var term: Int = 1
    var votedFor: Int? = null
    val config = Config(1)

    init {
        _state = Follower(this)
    }

    fun canVote(candidateTerm: Int, candidateId: Int): Boolean {
        if (candidateTerm > term) return true
        if (candidateTerm == term && (votedFor == null || votedFor == candidateId)) return true
        // TODO add logIndex checks
        return false
    }

    override fun start() {
        _state.start()
    }

    override fun stop() {
        _state.stop()
    }

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response {
        return this._state.requestVote(req);
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response {
        return this._state.appendEntries(req);
    }

    fun setState(state: State) {
        _state.stop()
        _state = state
        println("$nodeId to ${_state.javaClass.canonicalName}")
        _state.start()
    }

    fun isLeader(): Boolean {
        return this._state is Leader
    }

    fun isFollower(): Boolean {
        return this._state is Follower
    }
}