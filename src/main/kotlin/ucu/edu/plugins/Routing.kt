package ucu.edu.plugins

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import ucu.edu.proto.RequestVote

fun Application.configureRouting() {
    routing {
        post("/votes") {
            val request = call.receive<RequestVote.Request>()
            call.respond(RequestVote.Response(request.term, true))
        }
    }
}
