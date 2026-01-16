package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
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
import com.example.myapplication.data.VerbList
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerbListsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var fabAddList: FloatingActionButton
    private lateinit var adapter: VerbListAdapter

    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verb_lists)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top + v.paddingTop, v.paddingRight, v.paddingBottom)
            insets
        }

        initViews()
        setupRecyclerView()
        setupListeners()
        loadLists()
    }

    override fun onResume() {
        super.onResume()
        loadLists()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.verbListsRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        fabAddList = findViewById(R.id.fabAddVerbList)
    }

    private fun setupRecyclerView() {
        adapter = VerbListAdapter(
            lists = emptyList(),
            onItemClick = { list -> openList(list) },
            onDeleteClick = { list -> confirmDeleteList(list) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        fabAddList.setOnClickListener {
            showCreateListDialog()
        }
    }

    private fun loadLists() {
        lifecycleScope.launch {
            val lists = withContext(Dispatchers.IO) {
                database.verbDao().getAllLists()
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_list, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.listNameInput)

        AlertDialog.Builder(this)
            .setTitle("🔤 Nueva lista de verbos")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    createList(name)
                } else {
                    Toast.makeText(this, "Ingresa un nombre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createList(name: String) {
        lifecycleScope.launch {
            val listId = withContext(Dispatchers.IO) {
                database.verbDao().createEmptyList(name)
            }
            Toast.makeText(this@VerbListsActivity, "✓ Lista creada", Toast.LENGTH_SHORT).show()
            loadLists()
            
            // Open the new list
            val intent = Intent(this@VerbListsActivity, EditVerbListActivity::class.java).apply {
                putExtra(EditVerbListActivity.EXTRA_LIST_ID, listId)
                putExtra(EditVerbListActivity.EXTRA_LIST_NAME, name)
            }
            startActivity(intent)
        }
    }

    private fun openList(list: VerbList) {
        val intent = Intent(this, EditVerbListActivity::class.java).apply {
            putExtra(EditVerbListActivity.EXTRA_LIST_ID, list.id)
            putExtra(EditVerbListActivity.EXTRA_LIST_NAME, list.name)
        }
        startActivity(intent)
    }

    private fun confirmDeleteList(list: VerbList) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar lista")
            .setMessage("¿Eliminar \"${list.name}\" y todos sus verbos?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteList(list)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteList(list: VerbList) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.verbDao().deleteList(list.id)
            }
            loadLists()
        }
    }
}
