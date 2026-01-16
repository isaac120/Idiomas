package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Item (fila) dentro de una lista flexible.
 * Los valores se almacenan como JSON para soportar número variable de columnas.
 */
@Entity(
    tableName = "list_items",
    foreignKeys = [
        ForeignKey(
            entity = FlexibleList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class ListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val values: String  // JSON array: ["casa", "house"] o ["go", "went", "gone"]
)
