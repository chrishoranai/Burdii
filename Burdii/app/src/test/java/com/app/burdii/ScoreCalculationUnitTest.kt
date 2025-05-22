package com.app.burdii

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreCalculationUnitTest {

    // Test data
    private val playerNames = listOf("Alice", "Bob", "Charlie")

    // Test for calculating total scores from hole-by-hole scores
    @Test
    fun testCalculateTotalScoresByHole() {
        val scoresByHole = arrayOf(
            intArrayOf(3, 4, 5, 3, 4, 5), // Alice's scores
            intArrayOf(4, 4, 4, 4, 4, 4), // Bob's scores
            intArrayOf(5, 3, 5, 3, 5, 3)  // Charlie's scores
        )

        val calculatedScores = mutableListOf<Pair<String, Int>>()

        playerNames.forEachIndexed { playerIndex, name ->
            val totalScore = scoresByHole[playerIndex].sum()
            calculatedScores.add(Pair(name, totalScore))
        }

        assertEquals(3, calculatedScores.size)
        assertEquals(Pair("Alice", 24), calculatedScores[0])
        assertEquals(Pair("Bob", 24), calculatedScores[1])
        assertEquals(Pair("Charlie", 24), calculatedScores[2])
    }

    // Test for using provided final scores
    @Test
    fun testUseFinalScores() {
        val finalScores = intArrayOf(75, 80, 72)

        val calculatedScores = mutableListOf<Pair<String, Int>>()

        playerNames.forEachIndexed { playerIndex, name ->
            calculatedScores.add(Pair(name, finalScores[playerIndex]))
        }

        assertEquals(3, calculatedScores.size)
        assertEquals(Pair("Alice", 75), calculatedScores[0])
        assertEquals(Pair("Bob", 80), calculatedScores[1])
        assertEquals(Pair("Charlie", 72), calculatedScores[2])
    }

    // Test for sorting players by total score (lowest first)
    @Test
    fun testSortPlayersByScore() {
        val unsortedScores = listOf(
            Pair("Bob", 80),
            Pair("Alice", 75),
            Pair("Charlie", 72)
        )

        val sortedScores = unsortedScores.sortedBy { it.second }

        assertEquals(3, sortedScores.size)
        assertEquals(Pair("Charlie", 72), sortedScores[0])
        assertEquals(Pair("Alice", 75), sortedScores[1])
        assertEquals(Pair("Bob", 80), sortedScores[2])
    }

    // Test for handling edge case: zero scores
    @Test
    fun testHandleZeroScores() {
        val scoresByHole = arrayOf(
            intArrayOf(0, 0, 0),
            intArrayOf(0, 0, 0)
        )
        val testPlayerNames = listOf("Player1", "Player2")

        val calculatedScores = mutableListOf<Pair<String, Int>>()

        testPlayerNames.forEachIndexed { playerIndex, name ->
            val totalScore = scoresByHole[playerIndex].sum()
            calculatedScores.add(Pair(name, totalScore))
        }

        assertEquals(2, calculatedScores.size)
        assertEquals(Pair("Player1", 0), calculatedScores[0])
        assertEquals(Pair("Player2", 0), calculatedScores[1])
    }

    // Test for handling edge case: empty input (assuming empty scores array)
    @Test
    fun testHandleEmptyInput() {
        val scoresByHole = arrayOf<IntArray>()
        val testPlayerNames = listOf<String>()

        val calculatedScores = mutableListOf<Pair<String, Int>>()

        testPlayerNames.forEachIndexed { playerIndex, name ->
            // This block should not be executed for empty lists
            val totalScore = scoresByHole.getOrNull(playerIndex)?.sum() ?: 0
            calculatedScores.add(Pair(name, totalScore))
        }

        assertEquals(0, calculatedScores.size)
    }

    // Test for handling edge case: mixed positive and negative scores (if applicable, though golf is usually positive)
    @Test
    fun testHandleMixedScores() {
        val scoresByHole = arrayOf(
            intArrayOf(5, -2, 3),
            intArrayOf(1, 1, -1)
        )
        val testPlayerNames = listOf("PlayerA", "PlayerB")

        val calculatedScores = mutableListOf<Pair<String, Int>>()

        testPlayerNames.forEachIndexed { playerIndex, name ->
            val totalScore = scoresByHole[playerIndex].sum()
            calculatedScores.add(Pair(name, totalScore))
        }

        assertEquals(2, calculatedScores.size)
        assertEquals(Pair("PlayerA", 6), calculatedScores[0])
        assertEquals(Pair("PlayerB", 1), calculatedScores[1])
    }
}