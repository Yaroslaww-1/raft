package ucu.edu

import ucu.edu.utils.Cluster
import ucu.edu.utils.repeatedTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LogReplicationTest {
    @Test
    fun commandSuccessfullyReplicated() = repeatedTest(10) {
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
    fun followerCatchUp() = repeatedTest(10) {
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
    fun appendCommandToFollowerSuccessfullyReplicated() = repeatedTest(10) {
        val cluster = Cluster.ofThree()
        cluster.startAll()

        cluster.waitForElectionToFinish()

        val follower = cluster.nodes.filter { it.isFollower() }.first()

        follower.appendCommand("1")
        follower.appendCommand("3")
        follower.appendCommand("2")

        cluster.waitForReplicationToFinish()

        val expected = listOf("1", "3", "2")

        for (node in cluster.nodes) {
            assertEquals(expected, node.getCommands())
        }

        cluster.stopAll()
    }
}