package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/**
 * DAO para operaciones de verbos irregulares.
 */
@Dao
interface VerbDao {

    // ===== Verb Lists =====
    @Query("SELECT * FROM verb_lists ORDER BY createdAt DESC")
    suspend fun getAllLists(): List<VerbList>

    @Insert
    suspend fun insertList(list: VerbList): Long

    @Query("DELETE FROM verb_lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    @Query("SELECT * FROM verb_lists WHERE id = :listId")
    suspend fun getListById(listId: Long): VerbList?

    @Query("UPDATE verb_lists SET verbCount = :count WHERE id = :listId")
    suspend fun updateVerbCount(listId: Long, count: Int)

    // ===== Verbs =====
    @Query("SELECT * FROM irregular_verbs WHERE listId = :listId")
    suspend fun getVerbsForList(listId: Long): List<IrregularVerb>

    @Insert
    suspend fun insertVerbs(verbs: List<IrregularVerb>)

    @Insert
    suspend fun insertVerb(verb: IrregularVerb): Long

    @Update
    suspend fun updateVerb(verb: IrregularVerb)

    @Query("DELETE FROM irregular_verbs WHERE id = :verbId")
    suspend fun deleteVerb(verbId: Long)

    @Query("DELETE FROM irregular_verbs WHERE listId = :listId")
    suspend fun deleteVerbsForList(listId: Long)

    @Query("SELECT COUNT(*) FROM irregular_verbs WHERE listId = :listId")
    suspend fun getVerbCountForList(listId: Long): Int

    // ===== Transactions =====
    @Transaction
    suspend fun createEmptyList(name: String): Long {
        return insertList(VerbList(name = name, verbCount = 0))
    }

    @Transaction
    suspend fun addVerbToList(listId: Long, baseForm: String, simplePast: String, pastParticiple: String) {
        insertVerb(IrregularVerb(listId = listId, baseForm = baseForm, simplePast = simplePast, pastParticiple = pastParticiple))
        val newCount = getVerbCountForList(listId)
        updateVerbCount(listId, newCount)
    }

    @Transaction
    suspend fun removeVerbFromList(listId: Long, verbId: Long) {
        deleteVerb(verbId)
        val newCount = getVerbCountForList(listId)
        updateVerbCount(listId, newCount)
    }

    @Transaction
    suspend fun insertListWithVerbs(name: String, verbs: List<Triple<String, String, String>>): Long {
        val listId = insertList(VerbList(name = name, verbCount = verbs.size))
        val irregularVerbs = verbs.map { (base, past, participle) ->
            IrregularVerb(listId = listId, baseForm = base, simplePast = past, pastParticiple = participle)
        }
        insertVerbs(irregularVerbs)
        return listId
    }
}
