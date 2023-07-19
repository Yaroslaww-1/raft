package ucu.edu

import ucu.edu.utils.Cluster
import ucu.edu.utils.repeatedTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogReplicationTest {
    @Test
    fun commandSuccessfullyReplicated() = repeatedTest(5) {
        val cluster = Cluster.ofThree()
        cluster.startAll()

        cluster.waitForElectionToFinish()

        val leader = cluster.nodes.filter { it.isLeader() }.first()

        leader.appendCommand("1")
        leader.appendCommand("3")
        leader.appendCommand("2")

        cluster.waitForReplicationToFinish()

        val expected = listOf("1", "3", "2")

        for (node in cluster.nodes) {
            assertEquals(expected, node.getCommands())
        }

        cluster.stopAll()
    }

    @Test
    fun followerCatchUp() = repeatedTest(5) {
        val cluster = Cluster.ofThree()
        cluster.startAll()

        cluster.waitForElectionToFinish()

        val leader = cluster.nodes.filter { it.isLeader() }.first()
        val follower = cluster.nodes.filter { it.isFollower() }.first()

        cluster.isolate(follower)

        leader.appendCommand("1")
        leader.appendCommand("3")
        leader.appendCommand("2")

        cluster.waitForReplicationToFinish()

        cluster.reEnable(follower)

        cluster.waitForReplicationToFinish()

        val expected = listOf("1", "3", "2")

        for (node in cluster.nodes) {
            assertEquals(expected, node.getCommands())
        }

        cluster.stopAll()
    }

    @Test
    fun appendCommandToFollowerSuccessfullyReplicated() = repeatedTest(5) {
        val cluster = Cluster.ofThree()
        cluster.startAll()

        cluster.waitForElectionToFinish()

        val follower = cluster.nodes.filter { it.isFollower() }.first()

        follower.appendCommand("1")
        follower.appendCommand("3")
        follower.appendCommand("2")

        cluster.waitForReplicationToFinish()

        cluster.assertAllRunningNodesHaveCommand(listOf("1", "3", "2"))

        cluster.stopAll()
    }

    @Test
    fun selfTest() = repeatedTest(1) {
        // Step 1 - Start 2 nodes
        val cluster = Cluster.ofThree()
        cluster.nodes[0].start()
        cluster.nodes[1].start()
        cluster.waitForElectionToFinish()
        // the Leader should be elected
        cluster.assertSingleLeaderPresent()

        // Step 2 - Post msg1, msg2
        cluster.nodes.first().appendCommand("1")
        cluster.nodes.first().appendCommand("2")
        cluster.waitForReplicationToFinish()
        //  messages should be replicated and committed
        cluster.assertAllRunningNodesHaveCommand(listOf("1", "2"))

        // Step 3 - Start 3-rd node
        cluster.nodes[2].start()
        cluster.waitForReplicationToFinish()
        // messages should be replicated on the 3-rd node
        cluster.assertAllRunningNodesHaveCommand(listOf("1", "2"))

        // Step 4 - Partition a Leader - (OldLeader)
        val oldLeader = cluster.nodes.filter { it.isLeader() }.first()
        cluster.isolate(oldLeader)
        cluster.waitForElectionToFinish()

        // Step 5 - Post msg3, msg4
        cluster.nodes.first().appendCommand("3")
        cluster.nodes.first().appendCommand("4")
        cluster.waitForReplicationToFinish()
        //  messages should be replicated and committed
        for (node in cluster.nodes.filter { it.id != oldLeader.id }) {
            assertEquals(listOf("1", "2", "3", "4"), node.getCommands())
        }

        // Step 6 - Post msg5 via OldLeader
        oldLeader.appendCommand("5")
        cluster.waitForReplicationToFinish()

        // Step 7 - Join cluster
        cluster.reEnable(oldLeader)
        cluster.waitForReplicationToFinish()
        // message should not be committed
        cluster.assertAllRunningNodesHaveCommand(listOf("1", "2", "3", "4"))

        cluster.stopAll()
    }
}