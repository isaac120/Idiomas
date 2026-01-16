package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa una lista de verbos irregulares guardada.
 */
@Entity(tableName = "verb_lists")
data class VerbList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val verbCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
