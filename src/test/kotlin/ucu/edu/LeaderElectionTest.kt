package ucu.edu

import org.assertj.core.api.Assertions.assertThat
import ucu.edu.utils.Cluster
import ucu.edu.utils.repeatedTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
    }

    @Test
    fun leaderCanBeIsolatedFromClusterAndCatchUpAfter() = repeatedTest(5) {
        val cluster = Cluster.ofThree()
        cluster.startAll()

        cluster.waitForElectionToFinish()

        val oldLeader = cluster.nodes.find { it.isLeader() }!!
        val oldTerm = oldLeader.term

        oldLeader.stop()
        cluster.waitForElectionToFinish()
        oldLeader.start()
        cluster.waitForElectionToFinish()

        val newLeader = cluster.nodes.find { it.isLeader() }!!
        val newTerm = newLeader.term

        assertThat(newTerm).isGreaterThan(oldTerm)
        assertFalse(oldLeader.isLeader())
    }
}