package ucu.edu.utils

import kotlinx.coroutines.delay
import ucu.edu.clients.Client
import ucu.edu.clients.LocalClient
import ucu.edu.node.Node
import java.time.Instant

class Cluster(
    val nodes: List<Node>,
    val clients: List<Client>
) {
    companion object {
        fun ofThree(): Cluster {
            val client1 = LocalClient()
            val client2 = LocalClient()
            val client3 = LocalClient()

            val node1 = Node(1, listOf(client2, client3))
            val node2 = Node(2, listOf(client1, client3))
            val node3 = Node(3, listOf(client1, client2))

            client1.initialize(node1)
            client2.initialize(node2)
            client3.initialize(node3)

            return Cluster(
                listOf(node1, node2, node3),
                listOf(client1, client2, client3)
            )
        }
    }

    fun startAll() {
        nodes.forEach { it.start() }
    }

    suspend fun stopAll() {
        nodes.forEach { it.stop() }
        nodes.forEach { println("node ${it} TRYING TO STOP") }
        delay(1000)
        nodes.forEach { println("node ${it} STOPPED") }
    }

    fun isolate(node: Node) {
        println("[${Instant.now()}] isolating ${node.id}")
        clients.find { it.nodeId() == node.id }!!.disable()
        node.stop()
    }

    fun reEnable(node: Node) {
        clients.find { it.nodeId() == node.id }!!.reEnable()
    }

    suspend fun waitForElectionToFinish() {
        delay(1000)
    }

    suspend fun waitForReplicationToFinish() {
        delay(1000)
    }
}