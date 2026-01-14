package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity para almacenar historial de sesiones de práctica.
 */
@Entity(tableName = "practice_sessions")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val correctCount: Int,
    val totalCount: Int,
    val listName: String = ""
)
