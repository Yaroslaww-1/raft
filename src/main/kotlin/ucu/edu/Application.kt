package ucu.edu

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import ucu.edu.clients.Client
import ucu.edu.clients.WebClient
import ucu.edu.node.Node
import ucu.edu.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
//    val port = environment.config.propertyOrNull("ktor.deployment.port")?.getString()!!

    install(ContentNegotiation) {
        json()
    }

    val nodeId = environment.config.propertyOrNull("ktor.deployment.port")?.getString()!!.toInt()
    val clientsHosts = environment.config.propertyOrNull("ktor.raft.clients")?.getString()!!.split(',')

    val clients = clientsHosts.map { WebClient(
        it.split(':')[1].toInt(),
        it
    ) }
    val node = Node(nodeId, clients)

    configureRouting(node)
}