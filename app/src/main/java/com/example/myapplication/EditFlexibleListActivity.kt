package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.FlexibleList
import com.example.myapplication.data.ListItem
import com.example.myapplication.util.FileParser
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class EditFlexibleListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LIST_ID = "list_id"
        const val EXTRA_LIST_NAME = "list_name"
    }

    private lateinit var listTitle: TextView
    private lateinit var itemCount: TextView
    private lateinit var headersContainer: LinearLayout
    private lateinit var addColumnButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var practiceButton: android.widget.Button
    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var exportButton: ImageButton

    private lateinit var adapter: FlexibleItemAdapter
    private val database by lazy { AppDatabase.getDatabase(this) }
    
    private var listId: Long = -1
    private var listName: String = ""
    private var columnHeaders: MutableList<String> = mutableListOf()
    private var items: List<ListItem> = emptyList()

    // File picker for CSV import
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            importFromCSV(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_flexible_list)

        listId = intent.getLongExtra(EXTRA_LIST_ID, -1)
        listName = intent.getStringExtra(EXTRA_LIST_NAME) ?: ""

        if (listId == -1L) {
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top + v.paddingTop, v.paddingRight, v.paddingBottom)
            insets
        }

        initViews()
        setupRecyclerView()
        setupListeners()
        loadList()
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun initViews() {
        listTitle = findViewById(R.id.listTitle)
        itemCount = findViewById(R.id.itemCount)
        headersContainer = findViewById(R.id.headersContainer)
        addColumnButton = findViewById(R.id.addColumnButton)
        recyclerView = findViewById(R.id.itemsRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        practiceButton = findViewById(R.id.practiceButton)
        fabAddItem = findViewById(R.id.fabAddItem)
        exportButton = findViewById(R.id.exportButton)

        listTitle.text = listName
    }

    private fun setupRecyclerView() {
        adapter = FlexibleItemAdapter(
            items = emptyList(),
            columnCount = 2,
            onDeleteClick = { item -> deleteItem(item) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        fabAddItem.setOnClickListener { showAddOptionsDialog() }
        addColumnButton.setOnClickListener { addColumn() }
        practiceButton.setOnClickListener { startPractice() }
        exportButton.setOnClickListener { exportList() }
    }

    private fun showAddOptionsDialog() {
        val options = arrayOf(
            "➕ Agregar un item",
            "📝 Agregar varios items",
            "📂 Importar CSV"
        )

        AlertDialog.Builder(this)
            .setTitle("Agregar items")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddItemDialog()
                    1 -> openBulkAdd()
                    2 -> openFilePicker()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openBulkAdd() {
        BulkAddFlexibleActivity.listId = listId
        BulkAddFlexibleActivity.columnHeaders = columnHeaders.toList()
        startActivity(Intent(this, BulkAddFlexibleActivity::class.java))
    }

    private fun openFilePicker() {
        filePickerLauncher.launch(arrayOf(
            "text/csv",
            "text/comma-separated-values",
            "application/vnd.ms-excel",
            "*/*"
        ))
    }

    private fun importFromCSV(uri: Uri) {
        lifecycleScope.launch {
            try {
                var imported = 0
                val expectedColumns = columnHeaders.size

                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var isFirstLine = true
                            var line: String?

                            while (reader.readLine().also { line = it } != null) {
                                // Skip header line if detected
                                if (isFirstLine) {
                                    isFirstLine = false
                                    if (line != null && FileParser.isHeaderLine(line!!.lowercase())) {
                                        continue
                                    }
                                }

                                line?.let { currentLine ->
                                    val parts = currentLine.split(",", ";", "\t").map { it.trim() }
                                    if (parts.isNotEmpty() && parts.any { it.isNotEmpty() }) {
                                        // Pad or trim to match expected columns
                                        val values = if (parts.size >= expectedColumns) {
                                            parts.take(expectedColumns)
                                        } else {
                                            parts + List(expectedColumns - parts.size) { "" }
                                        }
                                        database.flexibleDao().addItemToList(listId, values)
                                        imported++
                                    }
                                }
                            }
                        }
                    }
                }

                if (imported > 0) {
                    Toast.makeText(this@EditFlexibleListActivity, "✅ $imported items importados", Toast.LENGTH_SHORT).show()
                    loadItems()
                } else {
                    Toast.makeText(this@EditFlexibleListActivity, "No se encontraron items válidos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@EditFlexibleListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadList() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                database.flexibleDao().getListById(listId)
            }

            list?.let {
                // Parse column headers
                try {
                    val headers = JSONArray(it.columnHeaders)
                    columnHeaders = (0 until headers.length()).map { i -> headers.getString(i) }.toMutableList()
                } catch (e: Exception) {
                    columnHeaders = mutableListOf("Columna 1", "Columna 2")
                }

                updateHeadersUI()
                loadItems()
            }
        }
    }

    private fun loadItems() {
        lifecycleScope.launch {
            items = withContext(Dispatchers.IO) {
                database.flexibleDao().getItemsForList(listId)
            }

            adapter.updateItems(items, columnHeaders.size)
            itemCount.text = "${items.size} items"

            if (items.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun updateHeadersUI() {
        // Remove all views except the add button
        headersContainer.removeViews(0, headersContainer.childCount - 1)

        columnHeaders.forEachIndexed { index, header ->
            val headerView = layoutInflater.inflate(R.layout.item_column_header, headersContainer, false)
            val headerText = headerView.findViewById<TextView>(R.id.headerText)
            headerText.text = header

            headerView.setOnClickListener {
                showEditHeaderDialog(index, header)
            }

            headersContainer.addView(headerView, index)
        }

        // Show/hide add button based on max columns
        addColumnButton.visibility = if (columnHeaders.size < FlexibleList.MAX_COLUMNS) View.VISIBLE else View.GONE
    }

    private fun showEditHeaderDialog(index: Int, currentHeader: String) {
        val editText = EditText(this).apply {
            setText(currentHeader)
            hint = "Nombre del encabezado"
            setPadding(48, 32, 48, 32)
            selectAll()
        }

        AlertDialog.Builder(this)
            .setTitle("✏️ Editar encabezado")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newHeader = editText.text.toString().trim()
                if (newHeader.isNotEmpty()) {
                    columnHeaders[index] = newHeader
                    saveHeaders()
                    updateHeadersUI()
                }
            }
            .setNegativeButton("Cancelar", null)
            .apply {
                if (columnHeaders.size > FlexibleList.MIN_COLUMNS) {
                    setNeutralButton("🗑️ Eliminar") { _, _ ->
                        confirmDeleteColumn(index)
                    }
                }
            }
            .show()
    }

    private fun confirmDeleteColumn(index: Int) {
        if (items.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar columna")
                .setMessage("Esta columna tiene datos. ¿Eliminar la columna \"${columnHeaders[index]}\" y sus datos?")
                .setPositiveButton("Eliminar") { _, _ ->
                    deleteColumn(index)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            deleteColumn(index)
        }
    }

    private fun deleteColumn(index: Int) {
        columnHeaders.removeAt(index)
        saveHeaders()
        
        // Update items to remove the column data
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                items.forEach { item ->
                    try {
                        val values = JSONArray(item.values)
                        val newValues = mutableListOf<String>()
                        for (i in 0 until values.length()) {
                            if (i != index) newValues.add(values.getString(i))
                        }
                        val newValuesJson = newValues.joinToString(",", "[", "]") { "\"${it.replace("\"", "\\\"")}\"" }
                        database.flexibleDao().updateItem(item.copy(values = newValuesJson))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                database.flexibleDao().updateColumns(listId, columnHeaders.size, headersToJson())
            }
            updateHeadersUI()
            loadItems()
        }
    }

    private fun addColumn() {
        if (columnHeaders.size >= FlexibleList.MAX_COLUMNS) {
            Toast.makeText(this, "Máximo ${FlexibleList.MAX_COLUMNS} columnas", Toast.LENGTH_SHORT).show()
            return
        }

        val editText = EditText(this).apply {
            hint = "Nombre del encabezado"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("➕ Nueva columna")
            .setView(editText)
            .setPositiveButton("Agregar") { _, _ ->
                val header = editText.text.toString().trim()
                if (header.isNotEmpty()) {
                    columnHeaders.add(header)
                    saveHeaders()
                    updateHeadersUI()
                    
                    // Update existing items with empty value for new column
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            items.forEach { item ->
                                try {
                                    val values = JSONArray(item.values)
                                    val newValues = mutableListOf<String>()
                                    for (i in 0 until values.length()) {
                                        newValues.add(values.getString(i))
                                    }
                                    newValues.add("") // Add empty value for new column
                                    val newValuesJson = newValues.joinToString(",", "[", "]") { "\"${it.replace("\"", "\\\"")}\"" }
                                    database.flexibleDao().updateItem(item.copy(values = newValuesJson))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        loadItems()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveHeaders() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.flexibleDao().updateColumns(listId, columnHeaders.size, headersToJson())
            }
        }
    }

    private fun headersToJson(): String {
        return columnHeaders.joinToString(",", "[", "]") { "\"${it.replace("\"", "\\\"")}\"" }
    }

    private fun showAddItemDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val inputs = mutableListOf<EditText>()
        columnHeaders.forEach { header ->
            val editText = EditText(this).apply {
                hint = header
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
            }
            inputs.add(editText)
            container.addView(editText)
        }

        AlertDialog.Builder(this)
            .setTitle("➕ Agregar item")
            .setView(container)
            .setPositiveButton("Agregar") { _, _ ->
                val values = inputs.map { it.text.toString().trim() }
                if (values.any { it.isNotEmpty() }) {
                    addItem(values)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addItem(values: List<String>) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.flexibleDao().addItemToList(listId, values)
            }
            loadItems()
        }
    }

    private fun deleteItem(item: ListItem) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.flexibleDao().removeItemFromList(listId, item.id)
            }
            loadItems()
        }
    }

    private fun startPractice() {
        if (items.isEmpty()) {
            Toast.makeText(this, "Agrega items primero", Toast.LENGTH_SHORT).show()
            return
        }

        FlexibleStudyActivity.itemsToStudy = items.toMutableList()
        FlexibleStudyActivity.columnHeaders = columnHeaders.toList()
        startActivity(Intent(this, FlexibleStudyActivity::class.java))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun exportList() {
        lifecycleScope.launch {
            if (items.isEmpty()) {
                Toast.makeText(this@EditFlexibleListActivity, "La lista está vacía", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                val csvContent = StringBuilder()
                csvContent.appendLine(columnHeaders.joinToString(","))
                
                items.forEach { item ->
                    try {
                        val values = JSONArray(item.values)
                        val row = (0 until values.length()).map { values.getString(it) }
                        csvContent.appendLine(row.joinToString(","))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val fileName = "${listName.replace(" ", "_")}.csv"
                val file = java.io.File(cacheDir, fileName)
                file.writeText(csvContent.toString())

                val uri = FileProvider.getUriForFile(
                    this@EditFlexibleListActivity,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Lista: $listName")
                    putExtra(Intent.EXTRA_TITLE, fileName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Compartir lista"))

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@EditFlexibleListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
