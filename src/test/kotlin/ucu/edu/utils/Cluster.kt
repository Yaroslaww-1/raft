package ucu.edu.utils

import kotlinx.coroutines.delay
import ucu.edu.clients.Client
import ucu.edu.clients.LocalClient
import ucu.edu.node.Node
import java.time.Instant
import kotlin.test.assertEquals

class Cluster(
    val nodes: List<Node>,
    val clients: List<Client>
) {
    companion object {
        fun ofThree(): Cluster {
            val client12 = LocalClient()
            val client13 = LocalClient()

            val client21 = LocalClient()
            val client23 = LocalClient()

            val client31 = LocalClient()
            val client32 = LocalClient()

            val node1 = Node(1, listOf(client12, client13))
            val node2 = Node(2, listOf(client21, client23))
            val node3 = Node(3, listOf(client31, client32))

            client21.initialize(2, node1)
            client31.initialize(3, node1)

            client12.initialize(1, node2)
            client32.initialize(3, node2)

            client13.initialize(1, node3)
            client23.initialize(2, node3)

            return Cluster(
                listOf(node1, node2, node3),
                listOf(client12, client13, client21, client23, client31, client32)
            )
        }
    }

    fun startAll() {
        nodes.forEach { it.start() }
    }

    suspend fun stopAll() {
        nodes.forEach { it.stop() }
        nodes.forEach { println("node ${it} TRYING TO STOP") }
        delay(500)
        nodes.forEach { println("node ${it} STOPPED") }
    }

    fun isolate(node: Node) {
        println("[${Instant.now()}] isolating ${node.id}")
        clients.find { it.destinationId() == node.id }!!.isolate()
        clients.find { it.sourceId() == node.id }!!.isolate()
    }

    fun reEnable(node: Node) {
        println("[${Instant.now()}] reEnabling ${node.id}")
        clients.find { it.destinationId() == node.id }!!.connect()
        clients.find { it.sourceId() == node.id }!!.connect()
    }

    suspend fun waitForElectionToFinish() {
        delay(1000)
    }

    suspend fun waitForReplicationToFinish() {
        delay(1000)
    }

    fun assertSingleLeaderPresent() {
        val leaders = nodes.filter { it.isLeader() }
        assertEquals(1, leaders.size)
    }

    fun assertAllRunningNodesHaveCommand(expected: List<String>) {
        for (node in nodes) {
            if (node.running) assertEquals(expected, node.getCommands())
        }
    }
}