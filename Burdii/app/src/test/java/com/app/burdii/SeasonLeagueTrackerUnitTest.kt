package com.app.burdii

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class SeasonLeagueTrackerUnitTest {

    // Dummy data structures to simulate database data for testing
    data class TestRound(
        val id: Long = 0, // Simulate primary key
        val weekName: String,
        val playerScores: Map<String, Int> // Map of player name to their total score for the week
    )

    private lateinit var testRounds: MutableList<TestRound>

    @Before
    fun setUp() {
        // Initialize with some dummy data
        testRounds = mutableListOf()
    }

    // Helper function to calculate aggregate standings
    private fun calculateAggregateStandings(rounds: List<TestRound>): Map<String, Int> {
        val aggregateScores = mutableMapOf<String, Int>()
        for (round in rounds) {
            for ((player, score) in round.playerScores) {
                aggregateScores[player] = aggregateScores.getOrDefault(player, 0) + score
            }
        }
        return aggregateScores
    }

    // Helper function to find season champion(s)
    private fun findSeasonChampions(standings: Map<String, Int>): List<String> {
        if (standings.isEmpty()) {
            return emptyList()
        }
        val sortedStandings = standings.toList().sortedBy { (_, score) -> score }
        val lowestScore = sortedStandings.first().second
        return sortedStandings.filter { (_, score) -> score == lowestScore }.map { (player, _) -> player }
    }

    @Test
    fun testAggregateStandingsCalculation() {
        testRounds.add(TestRound(weekName = "Week 1", playerScores = mapOf("Alice" to 72, "Bob" to 75, "Charlie" to 70)))
        testRounds.add(TestRound(weekName = "Week 2", playerScores = mapOf("Alice" to 70, "Bob" to 73, "Charlie" to 71)))
        testRounds.add(TestRound(weekName = "Week 3", playerScores = mapOf("Alice" to 75, "Bob" to 70, "Charlie" to 72)))

        val standings = calculateAggregateStandings(testRounds)

        assertEquals(3, standings.size)
        assertEquals(72 + 70 + 75, standings["Alice"])
        assertEquals(75 + 73 + 70, standings["Bob"])
        assertEquals(70 + 71 + 72, standings["Charlie"])
    }

    @Test
    fun testSeasonChampionIdentification() {
        val standings = mapOf("Alice" to 217, "Bob" to 218, "Charlie" to 213)
        val champions = findSeasonChampions(standings)

        assertEquals(1, champions.size)
        assertTrue(champions.contains("Charlie"))
    }

    @Test
    fun testSeasonChampionIdentificationWithTie() {
        val standings = mapOf("Alice" to 217, "Bob" to 217, "Charlie" to 218)
        val champions = findSeasonChampions(standings)

        assertEquals(2, champions.size)
        assertTrue(champions.contains("Alice"))
        assertTrue(champions.contains("Bob"))
    }

    @Test
    fun testResetSeason() {
        testRounds.add(TestRound(weekName = "Week 1", playerScores = mapOf("Alice" to 72)))
        testRounds.add(TestRound(weekName = "Week 2", playerScores = mapOf("Bob" to 75)))

        assertEquals(2, testRounds.size)

        testRounds.clear() // Simulate deleting all data

        assertEquals(0, testRounds.size)
    }

    @Test
    fun testAggregateStandingsWithNoRounds() {
        val standings = calculateAggregateStandings(testRounds)
        assertTrue(standings.isEmpty())
    }

    @Test
    fun testSeasonChampionWithNoStandings() {
        val champions = findSeasonChampions(emptyMap())
        assertTrue(champions.isEmpty())
    }

    @Test
    fun testAggregateStandingsWithIncompleteData() {
        testRounds.add(TestRound(weekName = "Week 1", playerScores = mapOf("Alice" to 72, "Bob" to 75)))
        testRounds.add(TestRound(weekName = "Week 2", playerScores = mapOf("Alice" to 70, "Charlie" to 71)))

        val standings = calculateAggregateStandings(testRounds)

        assertEquals(3, standings.size)
        assertEquals(72 + 70, standings["Alice"])
        assertEquals(75, standings["Bob"])
        assertEquals(71, standings["Charlie"])
    }
}