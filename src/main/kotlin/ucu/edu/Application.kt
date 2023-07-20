package ucu.edu

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.event.*
import ucu.edu.clients.WebClient
import ucu.edu.node.Node
import ucu.edu.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
    }

    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    val nodeId = environment.config.propertyOrNull("ktor.deployment.port")?.getString()!!.toInt()
    val clientsHosts = environment.config.propertyOrNull("ktor.raft.clients")?.getString()!!.split(',')

    val clients = clientsHosts.map {
        WebClient(
            it.split(':')[1].toInt(),
            it
        )
    }
    val node = Node(nodeId, clients)
    node.start()

    (environment as ApplicationEngineEnvironment).connectors.forEach { connector ->
        println("Running ${connector.host}:${connector.port}")
    }

    configureRouting(node)
}