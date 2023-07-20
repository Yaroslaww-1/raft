package ucu.edu.proto

import kotlinx.serialization.Serializable

object AppendCommand {
    @Serializable
    data class Request(
        val command: String,
        val depth: Int,
    )
}
