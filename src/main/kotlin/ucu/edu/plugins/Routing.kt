package ucu.edu.plugins

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import ucu.edu.node.Context
import ucu.edu.proto.RequestVote

fun Application.configureRouting(node: Context) {
    routing {
        post("/votes") {
            val request = call.receive<RequestVote.Request>()
            val response = node.requestVote(request)
            call.respond(response)
        }
    }
}
