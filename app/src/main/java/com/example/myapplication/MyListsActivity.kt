package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.VocabularyList
import com.example.myapplication.model.WordPair
import com.example.myapplication.util.FileParser
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyListsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var fabAddList: FloatingActionButton
    private lateinit var adapter: VocabularyListAdapter
    
    private val database by lazy { AppDatabase.getDatabase(this) }

    private var loadedWords: List<WordPair> = emptyList()

    // File picker launcher for CSV import
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            loadedWords = FileParser.parseCSV(this, it)
            
            if (loadedWords.isNotEmpty()) {
                showSaveImportedListDialog()
            } else {
                Toast.makeText(
                    this,
                    "No se encontraron palabras. Verifica el formato del archivo.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_lists)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top + v.paddingTop, v.paddingRight, v.paddingBottom)
            insets
        }

        initViews()
        setupRecyclerView()
        loadLists()
        playEnterAnimation()
    }

    override fun onResume() {
        super.onResume()
        loadLists()
    }

    private fun playEnterAnimation() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        recyclerView.startAnimation(slideUp)
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.listsRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        fabAddList = findViewById(R.id.fabAddList)

        fabAddList.setOnClickListener {
            showAddOptionsDialog()
        }
    }

    private fun showAddOptionsDialog() {
        val options = arrayOf("📂 Importar CSV", "✏️ Crear lista vacía")
        
        AlertDialog.Builder(this)
            .setTitle("➕ Agregar lista")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openFilePicker()
                    1 -> showCreateListDialog()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openFilePicker() {
        filePickerLauncher.launch(arrayOf(
            "text/csv",
            "text/comma-separated-values",
            "application/vnd.ms-excel",
            "*/*"
        ))
    }

    private fun showSaveImportedListDialog() {
        val editText = EditText(this).apply {
            hint = "Nombre de la lista"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("💾 Guardar lista importada")
            .setMessage("${loadedWords.size} palabras encontradas.\nDale un nombre:")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveImportedList(name)
                } else {
                    Toast.makeText(this, "Ingresa un nombre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveImportedList(name: String) {
        lifecycleScope.launch {
            try {
                val wordPairs = loadedWords.map { it.sourceWord to it.targetWord }
                
                withContext(Dispatchers.IO) {
                    database.vocabularyDao().insertListWithWords(name, wordPairs)
                }
                
                Toast.makeText(
                    this@MyListsActivity,
                    "✓ Lista \"$name\" guardada con ${loadedWords.size} palabras",
                    Toast.LENGTH_SHORT
                ).show()
                
                loadedWords = emptyList()
                loadLists()
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@MyListsActivity,
                    "Error al guardar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = VocabularyListAdapter(
            lists = emptyList(),
            onItemClick = { list -> openEditList(list) },
            onDeleteClick = { list -> confirmDeleteList(list) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadLists() {
        lifecycleScope.launch {
            val lists = withContext(Dispatchers.IO) {
                database.vocabularyDao().getAllLists()
            }
            
            adapter.updateLists(lists)
            
            if (lists.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun showCreateListDialog() {
        val editText = EditText(this).apply {
            hint = "Nombre de la lista"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("📝 Nueva lista")
            .setMessage("Dale un nombre a tu nueva lista:")
            .setView(editText)
            .setPositiveButton("Crear") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    createEmptyList(name)
                } else {
                    Toast.makeText(this, "Ingresa un nombre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createEmptyList(name: String) {
        lifecycleScope.launch {
            val listId = withContext(Dispatchers.IO) {
                database.vocabularyDao().createEmptyList(name)
            }
            
            Toast.makeText(this@MyListsActivity, "✓ Lista creada", Toast.LENGTH_SHORT).show()
            
            val intent = Intent(this@MyListsActivity, EditListActivity::class.java).apply {
                putExtra(EditListActivity.EXTRA_LIST_ID, listId)
                putExtra(EditListActivity.EXTRA_LIST_NAME, name)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    private fun openEditList(list: VocabularyList) {
        val intent = Intent(this, EditListActivity::class.java).apply {
            putExtra(EditListActivity.EXTRA_LIST_ID, list.id)
            putExtra(EditListActivity.EXTRA_LIST_NAME, list.name)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun confirmDeleteList(list: VocabularyList) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar lista")
            .setMessage("¿Estás seguro de eliminar \"${list.name}\"?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteList(list)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteList(list: VocabularyList) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.vocabularyDao().deleteList(list.id)
            }
            loadLists()
        }
    }
}
