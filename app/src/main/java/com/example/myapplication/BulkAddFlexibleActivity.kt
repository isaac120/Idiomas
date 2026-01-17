package com.example.myapplication

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BulkAddFlexibleActivity : AppCompatActivity() {

    companion object {
        var listId: Long = -1
        var columnHeaders: List<String> = emptyList()
        private const val COLUMN_WIDTH = 200 // dp per column
    }

    private lateinit var headersRow: LinearLayout
    private lateinit var rowsContainer: LinearLayout
    private lateinit var addRowButton: Button
    private lateinit var saveButton: Button
    private lateinit var rowCountText: TextView

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val rows = mutableListOf<List<EditText>>()
    private var columnWidthPx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bulk_add_flexible)

        if (listId == -1L || columnHeaders.isEmpty()) {
            finish()
            return
        }

        // Calculate column width in pixels
        val density = resources.displayMetrics.density
        columnWidthPx = (COLUMN_WIDTH * density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top + 16, v.paddingRight, v.paddingBottom)
            insets
        }

        initViews()
        setupHeaders()
        addInitialRows()
        setupListeners()
    }

    private fun initViews() {
        headersRow = findViewById(R.id.headersRow)
        rowsContainer = findViewById(R.id.rowsContainer)
        addRowButton = findViewById(R.id.addRowButton)
        saveButton = findViewById(R.id.saveButton)
        rowCountText = findViewById(R.id.rowCountText)
    }

    private fun setupHeaders() {
        headersRow.removeAllViews()
        
        columnHeaders.forEach { header ->
            val headerView = TextView(this).apply {
                text = header
                textSize = 14f
                setTextColor(getColor(R.color.white))
                setPadding(16, 12, 16, 12)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    columnWidthPx,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 8
                }
            }
            headersRow.addView(headerView)
        }

        // Add space for delete button
        val spacer = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(56, 1)
        }
        headersRow.addView(spacer)
    }

    private fun addInitialRows() {
        repeat(5) { addRow() }
    }

    private fun addRow() {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 0, 8, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val editTexts = mutableListOf<EditText>()

        columnHeaders.forEachIndexed { _, header ->
            val editText = EditText(this).apply {
                hint = header
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    columnWidthPx,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 12 }
                setBackgroundResource(R.drawable.input_background)
                setPadding(20, 20, 20, 20)
            }
            editTexts.add(editText)
            rowLayout.addView(editText)
        }

        val deleteButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundResource(android.R.color.transparent)
            layoutParams = LinearLayout.LayoutParams(56, 56)
            setOnClickListener {
                if (rows.size > 1) {
                    val idx = rows.indexOf(editTexts)
                    if (idx != -1) {
                        rows.removeAt(idx)
                        rowsContainer.removeViewAt(idx)
                        updateRowCount()
                    }
                }
            }
        }
        rowLayout.addView(deleteButton)

        rows.add(editTexts)
        rowsContainer.addView(rowLayout)
        updateRowCount()
    }

    private fun updateRowCount() {
        rowCountText.text = "${rows.size} filas"
    }

    private fun setupListeners() {
        addRowButton.setOnClickListener { addRow() }
        saveButton.setOnClickListener { saveItems() }
    }

    private fun saveItems() {
        lifecycleScope.launch {
            var added = 0

            withContext(Dispatchers.IO) {
                rows.forEach { row ->
                    val values = row.map { it.text.toString().trim() }
                    if (values.any { it.isNotEmpty() }) {
                        database.flexibleDao().addItemToList(listId, values)
                        added++
                    }
                }
            }

            if (added > 0) {
                Toast.makeText(this@BulkAddFlexibleActivity, "✅ $added items agregados", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@BulkAddFlexibleActivity, "No hay items para agregar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
