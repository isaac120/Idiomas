package com.example.myapplication

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.AchievementManager
import com.example.myapplication.data.StreakManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private lateinit var streakText: TextView
    private lateinit var bestStreakText: TextView
    private lateinit var sessionCountText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var totalWordsText: TextView
    private lateinit var correctWordsText: TextView
    private lateinit var difficultWordsList: LinearLayout
    private lateinit var sessionsList: LinearLayout
    private lateinit var noDifficultWords: TextView
    private lateinit var noSessions: TextView
    private lateinit var weeklyChartContainer: LinearLayout
    private lateinit var weeklyLabelsContainer: LinearLayout
    private lateinit var weeklySessionsText: TextView
    private lateinit var listStatsList: LinearLayout
    private lateinit var noListStats: TextView
    private lateinit var achievementsCount: TextView
    private lateinit var achievementsGrid: android.widget.GridLayout
    
    private val achievementManager by lazy { AchievementManager(this) }

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val streakManager by lazy { StreakManager(this) }
    private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stats)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContent)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        loadStats()
    }

    private fun initViews() {
        streakText = findViewById(R.id.streakText)
        bestStreakText = findViewById(R.id.bestStreakText)
        sessionCountText = findViewById(R.id.sessionCountText)
        accuracyText = findViewById(R.id.accuracyText)
        totalWordsText = findViewById(R.id.totalWordsText)
        correctWordsText = findViewById(R.id.correctWordsText)
        difficultWordsList = findViewById(R.id.difficultWordsList)
        sessionsList = findViewById(R.id.sessionsList)
        noDifficultWords = findViewById(R.id.noDifficultWords)
        noSessions = findViewById(R.id.noSessions)
        weeklyChartContainer = findViewById(R.id.weeklyChartContainer)
        weeklyLabelsContainer = findViewById(R.id.weeklyLabelsContainer)
        weeklySessionsText = findViewById(R.id.weeklySessionsText)
        listStatsList = findViewById(R.id.listStatsList)
        noListStats = findViewById(R.id.noListStats)
        achievementsCount = findViewById(R.id.achievementsCount)
        achievementsGrid = findViewById(R.id.achievementsGrid)
    }

    private fun loadStats() {
        // Load streak (synchronous - SharedPreferences)
        val currentStreak = streakManager.getStreak()
        val bestStreak = streakManager.getBestStreak()
        
        streakText.text = if (currentStreak == 1) "1 día" else "$currentStreak días"
        bestStreakText.text = bestStreak.toString()
        
        // Load achievements
        loadAchievements()

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

            // Load weekly chart
            loadWeeklyChart()

            // Load stats by list
            loadListStats()

            // Load difficult words
            val difficultWords = withContext(Dispatchers.IO) { statsDao.getDifficultWords() }
            
            if (difficultWords.isNotEmpty()) {
                noDifficultWords.visibility = View.GONE
                difficultWords.filter { it.incorrectCount > 0 }.take(10).forEach { word ->
                    val accuracy = if (word.correctCount + word.incorrectCount > 0) {
                        (word.correctCount * 100) / (word.correctCount + word.incorrectCount)
                    } else 0
                    addDifficultWordItem(word.word, word.incorrectCount, accuracy)
                }
            }

            // Load recent sessions
            val sessions = withContext(Dispatchers.IO) { statsDao.getRecentSessions() }
            
            if (sessions.isNotEmpty()) {
                noSessions.visibility = View.GONE
                sessions.forEach { session ->
                    addSessionItem(session.date, session.correctCount, session.totalCount, session.listName)
                }
            }
        }
    }

    private suspend fun loadWeeklyChart() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis

        val sessions = withContext(Dispatchers.IO) {
            database.statsDao().getSessionsSince(startOfWeek)
        }

        // Group sessions by day
        val dayCounts = mutableMapOf<String, Int>()
        sessions.forEach { session ->
            val dayKey = dayFormat.format(Date(session.date))
            dayCounts[dayKey] = (dayCounts[dayKey] ?: 0) + 1
        }

        val maxCount = dayCounts.values.maxOrNull() ?: 1
        val dayNames = listOf("L", "M", "X", "J", "V", "S", "D")

        weeklyChartContainer.removeAllViews()
        weeklyLabelsContainer.removeAllViews()

        // Create bars for each day
        for (i in 0..6) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, i - 6)
            val dayKey = dayFormat.format(cal.time)
            val count = dayCounts[dayKey] ?: 0
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val dayLabel = when (dayOfWeek) {
                Calendar.MONDAY -> "L"
                Calendar.TUESDAY -> "M"
                Calendar.WEDNESDAY -> "X"
                Calendar.THURSDAY -> "J"
                Calendar.FRIDAY -> "V"
                Calendar.SATURDAY -> "S"
                Calendar.SUNDAY -> "D"
                else -> ""
            }

            // Create bar
            val barContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }

            val barHeight = if (maxCount > 0 && count > 0) {
                (count.toFloat() / maxCount * 80).toInt().coerceAtLeast(8)
            } else 4

            val bar = View(this).apply {
                val drawable = GradientDrawable().apply {
                    cornerRadius = 8f
                    setColor(if (count > 0) 
                        ContextCompat.getColor(context, R.color.primary_blue)
                    else 
                        ContextCompat.getColor(context, R.color.divider_color))
                }
                background = drawable
                layoutParams = LinearLayout.LayoutParams(
                    resources.displayMetrics.density.toInt() * 24,
                    resources.displayMetrics.density.toInt() * barHeight
                ).apply {
                    bottomMargin = 4
                }
            }

            barContainer.addView(bar)
            weeklyChartContainer.addView(barContainer)

            // Create label
            val label = TextView(this).apply {
                text = dayLabel
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            weeklyLabelsContainer.addView(label)
        }

        val totalWeekSessions = sessions.size
        weeklySessionsText.text = if (totalWeekSessions == 1) 
            "1 sesión esta semana" 
        else 
            "$totalWeekSessions sesiones esta semana"
    }

    private suspend fun loadListStats() {
        val listStats = withContext(Dispatchers.IO) {
            database.statsDao().getStatsByList()
        }

        if (listStats.isNotEmpty()) {
            noListStats.visibility = View.GONE
            listStats.forEach { stats ->
                addListStatsItem(stats.listName, stats.totalCorrect, stats.totalAnswered, stats.sessionCount)
            }
        }
    }

    private fun addListStatsItem(listName: String, correct: Int, total: Int, sessions: Int) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val percentage = if (total > 0) (correct * 100) / total else 0

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nameText = TextView(this).apply {
            text = listName
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val percentText = TextView(this).apply {
            text = "$percentage%"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, 
                if (percentage >= 80) R.color.success_green 
                else if (percentage >= 50) R.color.primary_blue 
                else R.color.error_red))
        }

        topRow.addView(nameText)
        topRow.addView(percentText)

        // Progress bar
        val progressContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (4 * resources.displayMetrics.density).toInt()
            ).apply { topMargin = 4 }
            
            val bgDrawable = GradientDrawable().apply {
                cornerRadius = 4f
                setColor(ContextCompat.getColor(context, R.color.divider_color))
            }
            background = bgDrawable
        }

        val progress = View(this).apply {
            val progressDrawable = GradientDrawable().apply {
                cornerRadius = 4f
                setColor(ContextCompat.getColor(context, 
                    if (percentage >= 80) R.color.success_green 
                    else if (percentage >= 50) R.color.primary_blue 
                    else R.color.error_red))
            }
            background = progressDrawable
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = percentage.toFloat() / 100
            }
        }

        progressContainer.addView(progress)

        val sessionsText = TextView(this).apply {
            text = "$sessions sesiones"
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.text_hint))
        }

        item.addView(topRow)
        item.addView(progressContainer)
        item.addView(sessionsText)
        listStatsList.addView(item)
    }

    private fun addDifficultWordItem(word: String, errorCount: Int, accuracy: Int) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 10, 0, 10)
        }

        val wordText = TextView(this).apply {
            text = word
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val statsText = TextView(this).apply {
            text = "❌$errorCount  ($accuracy%)"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.error_red))
        }

        item.addView(wordText)
        item.addView(statsText)
        difficultWordsList.addView(item)
    }

    private fun addSessionItem(date: Long, correct: Int, total: Int, listName: String) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 10, 0, 10)
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val dateText = TextView(this).apply {
            text = dateFormat.format(Date(date))
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val scoreText = TextView(this).apply {
            val percentage = if (total > 0) (correct * 100) / total else 0
            text = "✓ $correct/$total ($percentage%)"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.success_green))
        }

        topRow.addView(dateText)
        topRow.addView(scoreText)

        val listText = TextView(this).apply {
            text = if (listName.isNotEmpty()) "📋 $listName" else ""
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.text_hint))
            visibility = if (listName.isNotEmpty()) View.VISIBLE else View.GONE
        }

        item.addView(topRow)
        item.addView(listText)
        sessionsList.addView(item)
    }
    
    private fun loadAchievements() {
        val unlocked = achievementManager.getUnlockedCount()
        val total = AchievementManager.ALL_ACHIEVEMENTS.size
        achievementsCount.text = "$unlocked de $total desbloqueados"
        
        achievementsGrid.removeAllViews()
        
        val density = resources.displayMetrics.density
        val badgeSize = (48 * density).toInt()
        val margin = (4 * density).toInt()
        
        AchievementManager.ALL_ACHIEVEMENTS.forEach { achievement ->
            val isUnlocked = achievementManager.isUnlocked(achievement)
            
            val badge = TextView(this).apply {
                text = if (isUnlocked) achievement.icon else "🔒"
                textSize = 24f
                gravity = Gravity.CENTER
                
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = badgeSize
                    height = badgeSize
                    setMargins(margin, margin, margin, margin)
                }
                
                val bgDrawable = GradientDrawable().apply {
                    cornerRadius = 12 * density
                    if (isUnlocked) {
                        setColor(ContextCompat.getColor(context, R.color.success_green_light))
                    } else {
                        setColor(ContextCompat.getColor(context, R.color.divider_color))
                    }
                }
                background = bgDrawable
                
                alpha = if (isUnlocked) 1f else 0.5f
                
                setOnClickListener {
                    val status = if (isUnlocked) "✅ Desbloqueado" else "🔒 Bloqueado"
                    androidx.appcompat.app.AlertDialog.Builder(this@StatsActivity)
                        .setTitle("${achievement.icon} ${achievement.name}")
                        .setMessage("${achievement.description}\n\n$status")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            
            achievementsGrid.addView(badge)
        }
    }
}
