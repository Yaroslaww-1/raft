package ucu.edu

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import ucu.edu.clients.Client
import ucu.edu.node.Node
import ucu.edu.plugins.configureRouting
import ucu.edu.proto.RequestVote
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testSerialisation() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }

            val clients = listOf<Client>()
            val node = Node(1, clients)

            configureRouting(node)
        }

        val response = client.post("/votes") {
            contentType(ContentType.Application.Json)
            setBody(RequestVote.Request(1, 1))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(RequestVote.Response(1, true), response.body())
    }
}
