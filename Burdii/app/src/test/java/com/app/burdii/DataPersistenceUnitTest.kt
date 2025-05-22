package com.app.burdii

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.jvm.Throws
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class DataPersistenceUnitTest {

    private lateinit var roundDao: RoundDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build() // Allow main thread for testing, avoid in production
        roundDao = db.roundDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testInsertAndGetRound() = runBlocking {
        val round = Round(
            id = 1, // Assuming ID is generated or provided
            name = "Test Round 1",
            players = listOf("Player A", "Player B"),
            scores = mapOf("Player A" to listOf(3, 4, 5), "Player B" to listOf(4, 4, 4)),
            isComplete = true,
            date = System.currentTimeMillis()
        )
        roundDao.insertRound(round)
        val byId = roundDao.getRoundById(1)
        assertEquals(byId?.name, round.name)
        assertEquals(byId?.players, round.players)
        assertEquals(byId?.scores, round.scores)
        assertEquals(byId?.isComplete, round.isComplete)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAllRounds() = runBlocking {
        val round1 = Round(1, "Round 1", listOf("A"), mapOf("A" to listOf(10)), true, System.currentTimeMillis())
        val round2 = Round(2, "Round 2", listOf("B"), mapOf("B" to listOf(20)), true, System.currentTimeMillis())
        roundDao.insertRound(round1)
        roundDao.insertRound(round2)
        val allRounds = roundDao.getAllRounds()
        assertEquals(2, allRounds.size)
        assertTrue(allRounds.any { it.name == "Round 1" })
        assertTrue(allRounds.any { it.name == "Round 2" })
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteRound() = runBlocking {
        val round = Round(1, "To Delete", listOf("X"), mapOf("X" to listOf(5)), true, System.currentTimeMillis())
        roundDao.insertRound(round)
        var byId = roundDao.getRoundById(1)
        assertNotNull(byId)

        roundDao.deleteRound(round)
        byId = roundDao.getRoundById(1)
        assertNull(byId)
    }

    @Test
    @Throws(Exception::class)
    fun testGetNonexistentRound() = runBlocking {
        val round = roundDao.getRoundById(100)
        assertNull(round)
    }

    // Note: Handling database errors like conflicts or malformed data often requires
    // more complex setup or integration tests. Unit tests typically focus on the DAO logic itself.
    // For example, testing primary key conflicts might involve inserting an entity with an
    // existing ID and checking for an exception or specific return value depending on the
    // DAO function's OnConflictStrategy.
}