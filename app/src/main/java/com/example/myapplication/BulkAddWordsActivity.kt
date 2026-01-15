package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.VocabularyWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BulkAddWordsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LIST_ID = "list_id"
        const val EXTRA_LIST_NAME = "list_name"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var addRowButton: Button
    private lateinit var saveButton: Button
    private lateinit var listNameText: TextView
    private lateinit var adapter: WordRowAdapter

    private val database by lazy { AppDatabase.getDatabase(this) }
    private var listId: Long = -1
    private var listName: String = ""
    private val rows = mutableListOf<WordRow>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bulk_add)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top + v.paddingTop, v.paddingRight, v.paddingBottom)
            insets
        }

        listId = intent.getLongExtra(EXTRA_LIST_ID, -1)
        listName = intent.getStringExtra(EXTRA_LIST_NAME) ?: ""

        if (listId == -1L) {
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupListeners()

        // Start with 5 empty rows
        repeat(5) { rows.add(WordRow()) }
        adapter.notifyDataSetChanged()
        updateSaveButton()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.wordsRecyclerView)
        addRowButton = findViewById(R.id.addRowButton)
        saveButton = findViewById(R.id.saveButton)
        listNameText = findViewById(R.id.listNameText)

        listNameText.text = "Lista: $listName"
    }

    private fun setupRecyclerView() {
        adapter = WordRowAdapter(
            rows = rows,
            onRowChanged = { updateSaveButton() },
            onDeleteRow = { position -> adapter.removeRow(position) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        addRowButton.setOnClickListener {
            adapter.addRow()
            recyclerView.smoothScrollToPosition(rows.size - 1)
        }

        saveButton.setOnClickListener {
            saveWords()
        }
    }

    private fun updateSaveButton() {
        val validCount = adapter.getValidWords().size
        saveButton.text = "💾 Guardar ($validCount palabras)"
        saveButton.isEnabled = validCount > 0
    }

    private fun saveWords() {
        val validWords = adapter.getValidWords()

        if (validWords.isEmpty()) {
            Toast.makeText(this, "No hay palabras válidas para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    validWords.forEach { (source, target) ->
                        database.vocabularyDao().insertWord(
                            VocabularyWord(
                                listId = listId,
                                sourceWord = source,
                                targetWord = target
                            )
                        )
                    }
                }

                Toast.makeText(
                    this@BulkAddWordsActivity,
                    "✓ ${validWords.size} palabras guardadas",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@BulkAddWordsActivity,
                    "Error al guardar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
