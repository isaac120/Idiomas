package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa una lista de vocabulario guardada.
 */
@Entity(tableName = "vocabulary_lists")
data class VocabularyList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val wordCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)
