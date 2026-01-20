package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO para estadísticas de práctica.
 */
@Dao
interface StatsDao {

    // Practice Sessions
    @Insert
    suspend fun insertSession(session: PracticeSession)

    @Query("SELECT * FROM practice_sessions ORDER BY date DESC LIMIT 10")
    suspend fun getRecentSessions(): List<PracticeSession>

    @Query("SELECT SUM(correctCount) FROM practice_sessions")
    suspend fun getTotalCorrect(): Int?

    @Query("SELECT SUM(totalCount) FROM practice_sessions")
    suspend fun getTotalAnswered(): Int?

    @Query("SELECT COUNT(*) FROM practice_sessions")
    suspend fun getSessionCount(): Int

    // Get sessions from last 7 days for weekly chart
    @Query("SELECT * FROM practice_sessions WHERE date >= :startDate ORDER BY date ASC")
    suspend fun getSessionsSince(startDate: Long): List<PracticeSession>

    // Get stats grouped by list name
    @Query("""
        SELECT listName, 
               SUM(correctCount) as totalCorrect, 
               SUM(totalCount) as totalAnswered,
               COUNT(*) as sessionCount
        FROM practice_sessions 
        WHERE listName != ''
        GROUP BY listName 
        ORDER BY sessionCount DESC
        LIMIT 10
    """)
    suspend fun getStatsByList(): List<ListStats>

    // Word Stats
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordStats(wordStats: WordStats)

    @Query("SELECT * FROM word_stats WHERE word = :word")
    suspend fun getWordStats(word: String): WordStats?

    @Query("SELECT * FROM word_stats ORDER BY incorrectCount DESC LIMIT 10")
    suspend fun getDifficultWords(): List<WordStats>

    @Query("UPDATE word_stats SET incorrectCount = incorrectCount + 1, lastSeenDate = :date WHERE word = :word")
    suspend fun incrementIncorrect(word: String, date: Long = System.currentTimeMillis())

    @Query("UPDATE word_stats SET correctCount = correctCount + 1, lastSeenDate = :date WHERE word = :word")
    suspend fun incrementCorrect(word: String, date: Long = System.currentTimeMillis())
}

/**
 * Data class for list statistics aggregation.
 */
data class ListStats(
    val listName: String,
    val totalCorrect: Int,
    val totalAnswered: Int,
    val sessionCount: Int
)
