package ucu.edu

import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import ucu.edu.clients.LocalClient
import ucu.edu.node.Context
import ucu.edu.utils.repeatedTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LeaderElectionTest {
    private fun clusterOfThree(): List<Context> {
        val client1 = LocalClient()
        val client2 = LocalClient()
        val client3 = LocalClient()

        val node1 = Context(1, listOf(client2, client3))
        val node2 = Context(2, listOf(client1, client3))
        val node3 = Context(3, listOf(client1, client2))

        client1.initialize(node1)
        client2.initialize(node2)
        client3.initialize(node3)

       return listOf(node1, node2, node3)
    }

    @Test
    fun clusterIsStableAndLeaderIsElected() = repeatedTest(5) {
        val nodes = clusterOfThree()
        nodes.forEach { it.start() }

        delay(1000)

        val leaders = nodes.filter { it.isLeader() }
        val followers = nodes.filter { it.isFollower() }

        assertEquals(1, leaders.count())
        assertEquals(2, followers.count())
    }

    @Test
    fun leaderCanBeIsolatedFromClusterAndCatchUpAfter() = repeatedTest(5) {
        val nodes = clusterOfThree()
        nodes.forEach { it.start() }

        delay(1000)

        val oldLeader = nodes.find { it.isLeader() }!!
        val oldTerm = oldLeader.term

        oldLeader.stop()
        delay(1000)

        oldLeader.start()
        delay(1000)

        val newLeader = nodes.find { it.isLeader() }!!
        val newTerm = newLeader.term

        assertThat(newTerm).isGreaterThan(oldTerm)
        assertFalse(oldLeader.isLeader())
    }
}