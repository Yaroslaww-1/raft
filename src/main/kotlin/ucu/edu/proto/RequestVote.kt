package ucu.edu.proto

import kotlinx.serialization.Serializable

object RequestVote {
    @Serializable
    data class Request(
        val term: Int,
        val candidateId: Int,
//        val lastLogIndex: Int,
//        val lastLogTerm: Int
    )

    @Serializable
    data class Response(
        val term: Int,
        val voteGranted: Boolean
    )
}
