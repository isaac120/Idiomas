package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/**
 * DAO para operaciones de base de datos de vocabulario.
 */
@Dao
interface VocabularyDao {

    // Lists operations
    @Query("SELECT * FROM vocabulary_lists ORDER BY createdAt DESC")
    suspend fun getAllLists(): List<VocabularyList>

    @Insert
    suspend fun insertList(list: VocabularyList): Long

    @Query("DELETE FROM vocabulary_lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    @Query("SELECT * FROM vocabulary_lists WHERE id = :listId")
    suspend fun getListById(listId: Long): VocabularyList?

    @Query("UPDATE vocabulary_lists SET wordCount = :count WHERE id = :listId")
    suspend fun updateWordCount(listId: Long, count: Int)

    // Words operations
    @Query("SELECT * FROM vocabulary_words WHERE listId = :listId")
    suspend fun getWordsForList(listId: Long): List<VocabularyWord>

    @Insert
    suspend fun insertWords(words: List<VocabularyWord>)

    @Insert
    suspend fun insertWord(word: VocabularyWord): Long

    @Query("DELETE FROM vocabulary_words WHERE id = :wordId")
    suspend fun deleteWord(wordId: Long)

    @Query("DELETE FROM vocabulary_words WHERE listId = :listId")
    suspend fun deleteWordsForList(listId: Long)

    @Query("SELECT COUNT(*) FROM vocabulary_words WHERE listId = :listId")
    suspend fun getWordCountForList(listId: Long): Int

    @androidx.room.Update
    suspend fun updateWord(word: VocabularyWord)

    // Transaction to insert list with words
    @Transaction
    suspend fun insertListWithWords(name: String, words: List<Pair<String, String>>): Long {
        val listId = insertList(VocabularyList(name = name, wordCount = words.size))
        val vocabularyWords = words.map { (source, target) ->
            VocabularyWord(listId = listId, sourceWord = source, targetWord = target)
        }
        insertWords(vocabularyWords)
        return listId
    }

    // Transaction to create empty list
    @Transaction
    suspend fun createEmptyList(name: String): Long {
        return insertList(VocabularyList(name = name, wordCount = 0))
    }

    // Transaction to add word and update count
    @Transaction
    suspend fun addWordToList(listId: Long, sourceWord: String, targetWord: String) {
        insertWord(VocabularyWord(listId = listId, sourceWord = sourceWord, targetWord = targetWord))
        val newCount = getWordCountForList(listId)
        updateWordCount(listId, newCount)
    }

    // Transaction to remove word and update count
    @Transaction
    suspend fun removeWordFromList(listId: Long, wordId: Long) {
        deleteWord(wordId)
        val newCount = getWordCountForList(listId)
        updateWordCount(listId, newCount)
    }
}
