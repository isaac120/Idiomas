package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.model.WordPair
import com.example.myapplication.util.FileParser

class MainActivity : AppCompatActivity() {

    private lateinit var selectFileButton: Button
    private lateinit var startButton: Button
    private lateinit var wordsCountText: TextView
    private lateinit var uploadCard: CardView

    private var loadedWords: List<WordPair> = emptyList()

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // Persist permission for the file
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            // Parse the CSV file
            loadedWords = FileParser.parseCSV(this, it)
            
            if (loadedWords.isNotEmpty()) {
                showLoadedWords()
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
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        selectFileButton = findViewById(R.id.selectFileButton)
        startButton = findViewById(R.id.startButton)
        wordsCountText = findViewById(R.id.wordsCountText)
        uploadCard = findViewById(R.id.uploadCard)
    }

    private fun setupListeners() {
        selectFileButton.setOnClickListener {
            openFilePicker()
        }

        uploadCard.setOnClickListener {
            openFilePicker()
        }

        startButton.setOnClickListener {
            startStudySession()
        }
    }

    private fun openFilePicker() {
        filePickerLauncher.launch(arrayOf(
            "text/csv",
            "text/comma-separated-values",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "*/*"  // Fallback for devices that don't recognize CSV mime types
        ))
    }

    private fun showLoadedWords() {
        wordsCountText.visibility = View.VISIBLE
        wordsCountText.text = "✓ ${loadedWords.size} palabras cargadas"
        
        startButton.visibility = View.VISIBLE
        
        // Show preview of first few words
        val preview = loadedWords.take(3).joinToString("\n") { 
            "${it.sourceWord} → ${it.targetWord}" 
        }
        if (loadedWords.size > 3) {
            wordsCountText.text = "✓ ${loadedWords.size} palabras cargadas\n\n$preview\n..."
        } else {
            wordsCountText.text = "✓ ${loadedWords.size} palabras cargadas\n\n$preview"
        }
    }

    private fun startStudySession() {
        if (loadedWords.isEmpty()) {
            Toast.makeText(this, "Por favor, carga un archivo primero", Toast.LENGTH_SHORT).show()
            return
        }

        // Start study activity with the words
        val intent = Intent(this, StudyActivity::class.java)
        StudyActivity.wordsToStudy = FileParser.shuffleAndRandomizeDirection(loadedWords)
        startActivity(intent)
    }
}