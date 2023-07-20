package ucu.edu

import ucu.edu.proto.AppendCommand
import ucu.edu.utils.Cluster
import ucu.edu.utils.repeatedTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LogReplicationTest {
    @Test
    fun commandSuccessfullyReplicated() = repeatedTest(5) {
        val cluster = Cluster.ofThree()
        cluster.startAll()

        cluster.waitForElectionToFinish()

        val leader = cluster.nodes.filter { it.isLeader() }.first()

        leader.appendCommand(command("1"))
        leader.appendCommand(command("3"))
        leader.appendCommand(command("2"))

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

        leader.appendCommand(command("1"))
        leader.appendCommand(command("3"))
        leader.appendCommand(command("2"))

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

        follower.appendCommand(command("1"))
        follower.appendCommand(command("3"))
        follower.appendCommand(command("2"))

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
        cluster.nodes.first().appendCommand(command("1"))
        cluster.nodes.first().appendCommand(command("2"))
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
        val mainCluster = cluster.nodes.filter { it.id != oldLeader.id }
        mainCluster.first().appendCommand(command("3"))
        mainCluster.first().appendCommand(command("4"))
        cluster.waitForReplicationToFinish()
        //  messages should be replicated and committed
        for (node in mainCluster) {
            assertEquals(listOf("1", "2", "3", "4"), node.getCommands())
        }

        // Step 6 - Post msg5 via OldLeader
        oldLeader.appendCommand(command("5"))
        cluster.waitForReplicationToFinish()

        // Step 7 - Join cluster
        cluster.reEnable(oldLeader)
        cluster.waitForReplicationToFinish()
        // message should not be committed
        cluster.assertAllRunningNodesHaveCommand(listOf("1", "2", "3", "4"))

        cluster.stopAll()
    }

    private fun command(text: String): AppendCommand.Request {
        return AppendCommand.Request(text, 0)
    }
}