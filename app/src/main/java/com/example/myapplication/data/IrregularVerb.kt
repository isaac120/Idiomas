package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa un verbo irregular con sus 3 formas.
 */
@Entity(
    tableName = "irregular_verbs",
    foreignKeys = [
        ForeignKey(
            entity = VerbList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class IrregularVerb(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val baseForm: String,
    val simplePast: String,
    val pastParticiple: String
)
