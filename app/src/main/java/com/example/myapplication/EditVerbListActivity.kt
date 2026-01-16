package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
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
import com.example.myapplication.data.IrregularVerb
import androidx.core.content.FileProvider
import com.example.myapplication.util.FileParser
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditVerbListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LIST_ID = "list_id"
        const val EXTRA_LIST_NAME = "list_name"
    }

    private lateinit var listTitle: TextView
    private lateinit var verbCountText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var practiceButton: Button
    private lateinit var fabAddVerb: FloatingActionButton
    private lateinit var exportButton: ImageButton
    private lateinit var adapter: VerbAdapter

    private var listId: Long = -1
    private var listName: String = ""

    private val database by lazy { AppDatabase.getDatabase(this) }

    private val csvImportLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { importFromCSV(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_verb_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top + v.paddingTop, v.paddingRight, v.paddingBottom)
            insets
        }

        listId = intent.getLongExtra(EXTRA_LIST_ID, -1)
        listName = intent.getStringExtra(EXTRA_LIST_NAME) ?: "Verbos"

        if (listId == -1L) {
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupListeners()
        loadVerbs()
    }

    override fun onResume() {
        super.onResume()
        loadVerbs()
    }

    private fun initViews() {
        listTitle = findViewById(R.id.listTitle)
        verbCountText = findViewById(R.id.verbCountText)
        recyclerView = findViewById(R.id.verbsRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        practiceButton = findViewById(R.id.practiceButton)
        fabAddVerb = findViewById(R.id.fabAddVerb)
        exportButton = findViewById(R.id.exportButton)

        listTitle.text = listName
    }

    private fun setupRecyclerView() {
        adapter = VerbAdapter(
            verbs = emptyList(),
            onItemClick = { verb -> showEditVerbDialog(verb) },
            onDeleteClick = { verb -> confirmDeleteVerb(verb) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        fabAddVerb.setOnClickListener {
            showAddOptionsDialog()
        }

        practiceButton.setOnClickListener {
            startPractice()
        }

        exportButton.setOnClickListener {
            exportList()
        }
    }

    private fun loadVerbs() {
        lifecycleScope.launch {
            val verbs = withContext(Dispatchers.IO) {
                database.verbDao().getVerbsForList(listId)
            }

            adapter.updateVerbs(verbs)
            verbCountText.text = "${verbs.size} verbos"

            if (verbs.isEmpty()) {
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

    private fun showAddOptionsDialog() {
        val options = arrayOf("➕ Agregar un verbo", "📝 Agregar varios verbos", "📂 Cargar desde CSV")

        AlertDialog.Builder(this)
            .setTitle("Agregar verbos")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddVerbDialog()
                    1 -> openBulkAdd()
                    2 -> csvImportLauncher.launch("text/*")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openBulkAdd() {
        val intent = Intent(this, BulkAddVerbsActivity::class.java).apply {
            putExtra(BulkAddVerbsActivity.EXTRA_LIST_ID, listId)
            putExtra(BulkAddVerbsActivity.EXTRA_LIST_NAME, listName)
        }
        startActivity(intent)
    }

    private fun showAddVerbDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_verb, null)
        val baseInput = dialogView.findViewById<TextInputEditText>(R.id.baseFormInput)
        val pastInput = dialogView.findViewById<TextInputEditText>(R.id.simplePastInput)
        val participleInput = dialogView.findViewById<TextInputEditText>(R.id.pastParticipleInput)

        AlertDialog.Builder(this)
            .setTitle("➕ Agregar verbo")
            .setView(dialogView)
            .setPositiveButton("Agregar") { _, _ ->
                val base = baseInput.text.toString().trim()
                val past = pastInput.text.toString().trim()
                val participle = participleInput.text.toString().trim()

                if (base.isNotEmpty() && past.isNotEmpty() && participle.isNotEmpty()) {
                    addVerb(base, past, participle)
                } else {
                    Toast.makeText(this, "Completa los 3 campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditVerbDialog(verb: IrregularVerb) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_verb, null)
        val baseInput = dialogView.findViewById<TextInputEditText>(R.id.baseFormInput)
        val pastInput = dialogView.findViewById<TextInputEditText>(R.id.simplePastInput)
        val participleInput = dialogView.findViewById<TextInputEditText>(R.id.pastParticipleInput)

        baseInput.setText(verb.baseForm)
        pastInput.setText(verb.simplePast)
        participleInput.setText(verb.pastParticiple)

        AlertDialog.Builder(this)
            .setTitle("✏️ Editar verbo")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val base = baseInput.text.toString().trim()
                val past = pastInput.text.toString().trim()
                val participle = participleInput.text.toString().trim()

                if (base.isNotEmpty() && past.isNotEmpty() && participle.isNotEmpty()) {
                    updateVerb(verb, base, past, participle)
                } else {
                    Toast.makeText(this, "Completa los 3 campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addVerb(base: String, past: String, participle: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.verbDao().addVerbToList(listId, base, past, participle)
            }
            Toast.makeText(this@EditVerbListActivity, "✓ Verbo agregado", Toast.LENGTH_SHORT).show()
            loadVerbs()
        }
    }

    private fun updateVerb(verb: IrregularVerb, base: String, past: String, participle: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val updated = verb.copy(baseForm = base, simplePast = past, pastParticiple = participle)
                database.verbDao().updateVerb(updated)
            }
            Toast.makeText(this@EditVerbListActivity, "✓ Verbo actualizado", Toast.LENGTH_SHORT).show()
            loadVerbs()
        }
    }

    private fun confirmDeleteVerb(verb: IrregularVerb) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar verbo")
            .setMessage("¿Eliminar \"${verb.baseForm}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteVerb(verb)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteVerb(verb: IrregularVerb) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.verbDao().removeVerbFromList(listId, verb.id)
            }
            loadVerbs()
        }
    }

    private fun importFromCSV(uri: Uri) {
        lifecycleScope.launch {
            try {
                val words = withContext(Dispatchers.IO) {
                    FileParser.parseCSV(this@EditVerbListActivity, uri)
                }

                // For verb CSV, we expect 3 columns
                var imported = 0
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                        lines.forEachIndexed { index, line ->
                            val parts = line.split(",", ";", "\t").map { it.trim() }
                            if (parts.size >= 3 && parts[0].isNotEmpty()) {
                                // Skip header line (first line with header words)
                                if (index == 0 && FileParser.isHeaderLine(line.lowercase())) {
                                    return@forEachIndexed
                                }
                                database.verbDao().addVerbToList(listId, parts[0], parts[1], parts[2])
                                imported++
                            }
                        }
                    }
                }

                if (imported > 0) {
                    Toast.makeText(this@EditVerbListActivity, "✅ $imported verbos importados", Toast.LENGTH_SHORT).show()
                    loadVerbs()
                } else {
                    Toast.makeText(this@EditVerbListActivity, "⚠️ No se encontraron verbos válidos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@EditVerbListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPractice() {
        lifecycleScope.launch {
            val verbs = withContext(Dispatchers.IO) {
                database.verbDao().getVerbsForList(listId)
            }

            if (verbs.isEmpty()) {
                Toast.makeText(this@EditVerbListActivity, "Agrega verbos primero", Toast.LENGTH_SHORT).show()
                return@launch
            }

            VerbStudyActivity.verbsToStudy = verbs.shuffled().toMutableList()
            startActivity(Intent(this@EditVerbListActivity, VerbStudyActivity::class.java))
        }
    }

    private fun exportList() {
        lifecycleScope.launch {
            val verbs = withContext(Dispatchers.IO) {
                database.verbDao().getVerbsForList(listId)
            }

            if (verbs.isEmpty()) {
                Toast.makeText(this@EditVerbListActivity, "La lista está vacía", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                // Generate CSV content
                val csvContent = StringBuilder()
                csvContent.appendLine("Base,Past,Participle")
                verbs.forEach { verb ->
                    csvContent.appendLine("${verb.baseForm},${verb.simplePast},${verb.pastParticiple}")
                }

                // Write to file
                val fileName = "${listName.replace(" ", "_")}_verbos.csv"
                val file = java.io.File(cacheDir, fileName)
                file.writeText(csvContent.toString())

                // Share via FileProvider
                val uri = FileProvider.getUriForFile(
                    this@EditVerbListActivity,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Lista de verbos: $listName")
                    putExtra(Intent.EXTRA_TITLE, fileName)
                    putExtra(Intent.EXTRA_TEXT, "Lista de verbos: $listName")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Compartir lista"))

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@EditVerbListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
