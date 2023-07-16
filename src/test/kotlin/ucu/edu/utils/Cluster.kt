package ucu.edu.utils

import kotlinx.coroutines.delay
import ucu.edu.clients.LocalClient
import ucu.edu.node.Node

class Cluster(
    val nodes: List<Node>
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

            return Cluster(listOf(node1, node2, node3))
        }
    }

    fun startAll() {
        nodes.forEach { it.start() }
    }

    suspend fun waitForElectionToFinish() {
        delay(1000)
    }
}