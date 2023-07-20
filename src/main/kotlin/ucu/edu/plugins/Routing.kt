package ucu.edu.plugins

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import ucu.edu.node.Node
import ucu.edu.proto.AppendCommand
import ucu.edu.proto.AppendEntries
import ucu.edu.proto.RequestVote

fun Application.configureRouting(node: Node) {
    routing {
        post("/requestVote") {
            val request = call.receive<RequestVote.Request>()
            val response = node.requestVote(request)
            call.respond(response)
        }

        post("/appendEntries") {
            val request = call.receive<AppendEntries.Request>()
            val response = node.appendEntries(request)
            call.respond(response)
        }

        post("/appendCommand") {
            val request = call.receive<AppendCommand.Request>()
            val response = node.appendCommand(request)
            call.respond(response)
        }

        get("/commands") {
            val response = node.getCommands()
            call.respond(response)
        }
    }
}
