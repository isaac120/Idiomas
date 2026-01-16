package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/**
 * DAO para operaciones con listas flexibles.
 */
@Dao
interface FlexibleDao {

    // ========== OPERACIONES CON LISTAS ==========

    @Query("SELECT * FROM flexible_lists ORDER BY createdAt DESC")
    suspend fun getAllLists(): List<FlexibleList>

    @Insert
    suspend fun insertList(list: FlexibleList): Long

    @Update
    suspend fun updateList(list: FlexibleList)

    @Query("DELETE FROM flexible_lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    @Query("SELECT * FROM flexible_lists WHERE id = :listId")
    suspend fun getListById(listId: Long): FlexibleList?

    @Query("UPDATE flexible_lists SET itemCount = :count WHERE id = :listId")
    suspend fun updateItemCount(listId: Long, count: Int)

    @Query("UPDATE flexible_lists SET columnHeaders = :headers WHERE id = :listId")
    suspend fun updateColumnHeaders(listId: Long, headers: String)

    @Query("UPDATE flexible_lists SET columnCount = :count, columnHeaders = :headers WHERE id = :listId")
    suspend fun updateColumns(listId: Long, count: Int, headers: String)

    // ========== OPERACIONES CON ITEMS ==========

    @Query("SELECT * FROM list_items WHERE listId = :listId")
    suspend fun getItemsForList(listId: Long): List<ListItem>

    @Insert
    suspend fun insertItem(item: ListItem): Long

    @Insert
    suspend fun insertItems(items: List<ListItem>)

    @Update
    suspend fun updateItem(item: ListItem)

    @Query("DELETE FROM list_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("DELETE FROM list_items WHERE listId = :listId")
    suspend fun deleteItemsForList(listId: Long)

    @Query("SELECT COUNT(*) FROM list_items WHERE listId = :listId")
    suspend fun getItemCountForList(listId: Long): Int

    // ========== TRANSACCIONES ==========

    @Transaction
    suspend fun createEmptyList(name: String, columnCount: Int = 2): Long {
        val headers = (1..columnCount).map { "Columna $it" }
        val headersJson = headers.joinToString(",", "[", "]") { "\"$it\"" }
        return insertList(
            FlexibleList(
                name = name,
                columnCount = columnCount,
                columnHeaders = headersJson,
                itemCount = 0
            )
        )
    }

    @Transaction
    suspend fun addItemToList(listId: Long, values: List<String>) {
        val valuesJson = values.joinToString(",", "[", "]") { "\"${it.replace("\"", "\\\"")}\"" }
        insertItem(ListItem(listId = listId, values = valuesJson))
        val newCount = getItemCountForList(listId)
        updateItemCount(listId, newCount)
    }

    @Transaction
    suspend fun removeItemFromList(listId: Long, itemId: Long) {
        deleteItem(itemId)
        val newCount = getItemCountForList(listId)
        updateItemCount(listId, newCount)
    }
}
