package com.example.myapplication

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private lateinit var sessionCountText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var totalWordsText: TextView
    private lateinit var correctWordsText: TextView
    private lateinit var difficultWordsList: LinearLayout
    private lateinit var sessionsList: LinearLayout
    private lateinit var noDifficultWords: TextView
    private lateinit var noSessions: TextView

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stats)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContent)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top + v.paddingTop, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        loadStats()
    }

    private fun initViews() {
        sessionCountText = findViewById(R.id.sessionCountText)
        accuracyText = findViewById(R.id.accuracyText)
        totalWordsText = findViewById(R.id.totalWordsText)
        correctWordsText = findViewById(R.id.correctWordsText)
        difficultWordsList = findViewById(R.id.difficultWordsList)
        sessionsList = findViewById(R.id.sessionsList)
        noDifficultWords = findViewById(R.id.noDifficultWords)
        noSessions = findViewById(R.id.noSessions)
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val statsDao = database.statsDao()

            // Load general stats
            val sessionCount = withContext(Dispatchers.IO) { statsDao.getSessionCount() }
            val totalCorrect = withContext(Dispatchers.IO) { statsDao.getTotalCorrect() ?: 0 }
            val totalAnswered = withContext(Dispatchers.IO) { statsDao.getTotalAnswered() ?: 0 }

            sessionCountText.text = sessionCount.toString()
            
            val accuracy = if (totalAnswered > 0) (totalCorrect * 100) / totalAnswered else 0
            accuracyText.text = "$accuracy%"
            
            totalWordsText.text = "$totalAnswered palabras practicadas"
            correctWordsText.text = "$totalCorrect correctas"

            // Load difficult words
            val difficultWords = withContext(Dispatchers.IO) { statsDao.getDifficultWords() }
            
            if (difficultWords.isNotEmpty()) {
                noDifficultWords.visibility = android.view.View.GONE
                difficultWords.forEach { word ->
                    addDifficultWordItem(word.word, word.incorrectCount)
                }
            }

            // Load recent sessions
            val sessions = withContext(Dispatchers.IO) { statsDao.getRecentSessions() }
            
            if (sessions.isNotEmpty()) {
                noSessions.visibility = android.view.View.GONE
                sessions.forEach { session ->
                    addSessionItem(session.date, session.correctCount, session.totalCount)
                }
            }
        }
    }

    private fun addDifficultWordItem(word: String, errorCount: Int) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val wordText = TextView(this).apply {
            text = word
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val countText = TextView(this).apply {
            text = "❌ $errorCount"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.error_red))
        }

        item.addView(wordText)
        item.addView(countText)
        difficultWordsList.addView(item)
    }

    private fun addSessionItem(date: Long, correct: Int, total: Int) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val dateText = TextView(this).apply {
            text = dateFormat.format(Date(date))
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val scoreText = TextView(this).apply {
            text = "✓ $correct/$total"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.success_green))
        }

        item.addView(dateText)
        item.addView(scoreText)
        sessionsList.addView(item)
    }
}
