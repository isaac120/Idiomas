package com.example.myapplication

import android.os.Bundle
import android.view.View
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
    }

    private lateinit var headersRow: LinearLayout
    private lateinit var rowsContainer: LinearLayout
    private lateinit var addRowButton: Button
    private lateinit var saveButton: Button
    private lateinit var rowCountText: TextView

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val rows = mutableListOf<List<EditText>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bulk_add_flexible)

        if (listId == -1L || columnHeaders.isEmpty()) {
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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
                setTextColor(getColor(R.color.primary_blue))
                setPadding(16, 8, 16, 8)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            headersRow.addView(headerView)
        }

        // Add space for delete button
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(40, 1)
        }
        headersRow.addView(spacer)
    }

    private fun addInitialRows() {
        repeat(5) { addRow() }
    }

    private fun addRow() {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

        val editTexts = mutableListOf<EditText>()

        columnHeaders.forEachIndexed { index, header ->
            val editText = EditText(this).apply {
                hint = header
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply { marginEnd = 4 }
                setBackgroundResource(R.drawable.input_background)
                setPadding(16, 12, 16, 12)
            }
            editTexts.add(editText)
            rowLayout.addView(editText)
        }

        val deleteButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundResource(android.R.color.transparent)
            layoutParams = LinearLayout.LayoutParams(40, 40).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
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
