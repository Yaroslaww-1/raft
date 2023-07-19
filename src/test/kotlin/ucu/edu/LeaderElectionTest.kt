package ucu.edu

import junit.framework.TestCase.assertFalse
import org.assertj.core.api.Assertions.assertThat
import ucu.edu.utils.Cluster
import ucu.edu.utils.repeatedTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LeaderElectionTest {
    @Test
    fun clusterIsStableAndLeaderIsElected() = repeatedTest(5) {
        val cluster = Cluster.ofThree()
        cluster.startAll()

        cluster.waitForElectionToFinish()

        val leaders = cluster.nodes.filter { it.isLeader() }
        val followers = cluster.nodes.filter { it.isFollower() }

        assertEquals(1, leaders.count())
        assertEquals(2, followers.count())

        cluster.stopAll()
    }

    @Test
    fun leaderCanBeIsolatedFromClusterAndCatchUpAfter() = repeatedTest(5) {
        val cluster = Cluster.ofThree()
        cluster.startAll()

        cluster.waitForElectionToFinish()

        val oldLeader = cluster.nodes.find { it.isLeader() }!!
        val oldTerm = oldLeader.term

        cluster.isolate(oldLeader)
        cluster.waitForElectionToFinish()
        cluster.reEnable(oldLeader)
        cluster.waitForElectionToFinish()

        val newLeader = cluster.nodes.find { it.isLeader() }!!
        val newTerm = newLeader.term

        assertThat(newTerm).isGreaterThan(oldTerm)

        cluster.stopAll()
    }

    @Test
    fun differentLeaderSelectedAfterMessageSend() = repeatedTest(5) {
        val cluster = Cluster.ofThree()
        cluster.startAll()

        cluster.waitForElectionToFinish()

        val oldLeader = cluster.nodes.find { it.isLeader() }!!
        val oldFollower = cluster.nodes.find { it.isFollower() }!!

        cluster.isolate(oldLeader)
        cluster.waitForElectionToFinish()

        oldFollower.appendCommand("1")

        cluster.reEnable(oldLeader)
        cluster.waitForElectionToFinish()

        assertFalse(oldLeader.isLeader())

        cluster.stopAll()
    }
}