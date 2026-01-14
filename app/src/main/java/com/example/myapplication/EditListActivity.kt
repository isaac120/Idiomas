package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.VocabularyWord
import com.example.myapplication.model.WordPair
import com.example.myapplication.util.FileParser
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LIST_ID = "list_id"
        const val EXTRA_LIST_NAME = "list_name"
    }

    private lateinit var listTitle: TextView
    private lateinit var wordCountText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var practiceButton: Button
    private lateinit var fabAddWord: FloatingActionButton
    private lateinit var adapter: WordAdapter

    private var listId: Long = -1
    private var listName: String = ""
    
    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top + v.paddingTop, v.paddingRight, v.paddingBottom)
            insets
        }

        listId = intent.getLongExtra(EXTRA_LIST_ID, -1)
        listName = intent.getStringExtra(EXTRA_LIST_NAME) ?: "Lista"

        if (listId == -1L) {
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupListeners()
        loadWords()
    }

    override fun onResume() {
        super.onResume()
        loadWords()
    }

    private fun initViews() {
        listTitle = findViewById(R.id.listTitle)
        wordCountText = findViewById(R.id.wordCountText)
        recyclerView = findViewById(R.id.wordsRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        practiceButton = findViewById(R.id.practiceButton)
        fabAddWord = findViewById(R.id.fabAddWord)

        listTitle.text = listName
    }

    private fun setupRecyclerView() {
        adapter = WordAdapter(
            words = emptyList(),
            onDeleteClick = { word -> confirmDeleteWord(word) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        fabAddWord.setOnClickListener {
            showAddWordDialog()
        }

        practiceButton.setOnClickListener {
            startPractice()
        }
    }

    private fun loadWords() {
        lifecycleScope.launch {
            val words = withContext(Dispatchers.IO) {
                database.vocabularyDao().getWordsForList(listId)
            }

            adapter.updateWords(words)
            wordCountText.text = "${words.size} palabras"

            if (words.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                practiceButton.isEnabled = false
                practiceButton.alpha = 0.5f
            } else {
                emptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                practiceButton.isEnabled = true
                practiceButton.alpha = 1f
            }
        }
    }

    private fun showAddWordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_word, null)
        val sourceInput = dialogView.findViewById<TextInputEditText>(R.id.sourceWordInput)
        val targetInput = dialogView.findViewById<TextInputEditText>(R.id.targetWordInput)

        AlertDialog.Builder(this)
            .setTitle("➕ Agregar palabra")
            .setView(dialogView)
            .setPositiveButton("Agregar") { _, _ ->
                val source = sourceInput.text.toString().trim()
                val target = targetInput.text.toString().trim()

                if (source.isNotEmpty() && target.isNotEmpty()) {
                    addWord(source, target)
                } else {
                    Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addWord(sourceWord: String, targetWord: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.vocabularyDao().addWordToList(listId, sourceWord, targetWord)
            }
            Toast.makeText(this@EditListActivity, "✓ Palabra agregada", Toast.LENGTH_SHORT).show()
            loadWords()
        }
    }

    private fun confirmDeleteWord(word: VocabularyWord) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar palabra")
            .setMessage("¿Eliminar \"${word.sourceWord}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteWord(word)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteWord(word: VocabularyWord) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.vocabularyDao().removeWordFromList(listId, word.id)
            }
            loadWords()
        }
    }

    private fun startPractice() {
        lifecycleScope.launch {
            val words = withContext(Dispatchers.IO) {
                database.vocabularyDao().getWordsForList(listId)
            }

            if (words.isEmpty()) {
                Toast.makeText(this@EditListActivity, "Agrega palabras primero", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val wordPairs = words.map { word ->
                WordPair(word.sourceWord, word.targetWord)
            }

            StudyActivity.wordsToStudy = FileParser.shuffleAndRandomizeDirection(wordPairs)
            startActivity(Intent(this@EditListActivity, StudyActivity::class.java))
        }
    }
}
