package ucu.edu

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import ucu.edu.clients.LocalClient
import ucu.edu.node.Node
import kotlin.test.Test
import kotlin.test.assertTrue

class LeaderElectionTest {
    @Test
    fun testSerialisation() = runBlocking {
        val client1 = LocalClient()
        val client2 = LocalClient()
        val client3 = LocalClient()

        val node1 = Node(listOf(client2, client3))
        val node2 = Node(listOf(client1, client3))
        val node3 = Node(listOf(client1, client2))

        client1.initialize(node1)
        client2.initialize(node2)
        client3.initialize(node3)

        delay(1000)

        assertTrue { node1.isLeader || node2.isLeader || node3.isLeader }
    }
}