package ucu.edu.clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import ucu.edu.proto.AppendCommand
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

class WebClient(val destinationId: Int, val destinationHost: String) : Client {
    private val client = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.INFO
        }

        install(ContentNegotiation) {
            json()
        }
    }

    override fun destinationId(): Int = destinationId

    override suspend fun requestVote(req: RequestVote.Request): RequestVote.Response? {
        return client.post("$destinationHost/requestVote") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()
    }

    override suspend fun appendEntries(req: AppendEntries.Request): AppendEntries.Response? {
        return client.post("$destinationHost/appendEntries") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()
    }

    override suspend fun appendCommand(req: AppendCommand.Request) {
        return client.post("$destinationHost/appendCommand") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()
    }
}