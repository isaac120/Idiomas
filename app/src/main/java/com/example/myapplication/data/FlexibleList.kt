package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lista flexible con columnas personalizables.
 * Reemplaza VocabularyList y VerbList con un sistema unificado.
 */
@Entity(tableName = "flexible_lists")
data class FlexibleList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val columnCount: Int,           // Número de columnas (2-5)
    val columnHeaders: String,      // JSON array: ["Español", "Inglés", "Participle"]
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 5
    }
}
