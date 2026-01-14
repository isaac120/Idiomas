package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa una palabra dentro de una lista de vocabulario.
 */
@Entity(
    tableName = "vocabulary_words",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class VocabularyWord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val sourceWord: String,
    val targetWord: String
)
