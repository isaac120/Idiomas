package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity para rastrear palabras difíciles (más falladas).
 */
@Entity(tableName = "word_stats")
data class WordStats(
    @PrimaryKey
    val word: String,
    val incorrectCount: Int = 0,
    val correctCount: Int = 0,
    val lastSeenDate: Long = System.currentTimeMillis()
)
