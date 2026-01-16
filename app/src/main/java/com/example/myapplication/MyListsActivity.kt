package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
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
import com.example.myapplication.data.FlexibleList
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyListsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var fabAddList: FloatingActionButton
    private lateinit var adapter: FlexibleListAdapter
    
    private val database by lazy { AppDatabase.getDatabase(this) }

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
            showCreateListDialog()
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
                database.flexibleDao().createEmptyList(name, 2) // Start with 2 columns
            }
            
            Toast.makeText(this@MyListsActivity, "✓ Lista creada", Toast.LENGTH_SHORT).show()
            
            val intent = Intent(this@MyListsActivity, EditFlexibleListActivity::class.java).apply {
                putExtra(EditFlexibleListActivity.EXTRA_LIST_ID, listId)
                putExtra(EditFlexibleListActivity.EXTRA_LIST_NAME, name)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    private fun setupRecyclerView() {
        adapter = FlexibleListAdapter(
            lists = emptyList(),
            onItemClick = { list -> openList(list) },
            onDeleteClick = { list -> confirmDeleteList(list) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadLists() {
        lifecycleScope.launch {
            val lists = withContext(Dispatchers.IO) {
                database.flexibleDao().getAllLists()
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

    private fun openList(list: FlexibleList) {
        val intent = Intent(this, EditFlexibleListActivity::class.java).apply {
            putExtra(EditFlexibleListActivity.EXTRA_LIST_ID, list.id)
            putExtra(EditFlexibleListActivity.EXTRA_LIST_NAME, list.name)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun confirmDeleteList(list: FlexibleList) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar lista")
            .setMessage("¿Eliminar \"${list.name}\"?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteList(list)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteList(list: FlexibleList) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.flexibleDao().deleteList(list.id)
            }
            loadLists()
        }
    }
}
