package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.StreakManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var streakCount: TextView
    private lateinit var streakLabel: TextView
    private lateinit var streakCard: CardView
    private lateinit var myListsCard: CardView
    private lateinit var statsCard: CardView
    private lateinit var practiceButton: Button
    private lateinit var motivationalText: TextView
    private lateinit var listsCountText: TextView
    private lateinit var darkModeSwitch: com.google.android.material.switchmaterial.SwitchMaterial

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val streakManager by lazy { StreakManager(this) }

    private val motivationalMessages = listOf(
        "¡Tú puedes! 💪",
        "¡Sigue así! 🌟",
        "¡Un día más! 🎯",
        "¡Excelente trabajo! 🏆",
        "¡No te rindas! 🔥",
        "¡Vamos a aprender! 📚",
        "¡Cada día cuenta! ⭐"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
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
        playEnterAnimations()
    }

    override fun onResume() {
        super.onResume()
        updateStreakDisplay()
        updateListsCount()
        updateMotivationalMessage()
    }

    private fun initViews() {
        streakCount = findViewById(R.id.streakCount)
        streakLabel = findViewById(R.id.streakLabel)
        streakCard = findViewById(R.id.streakCard)
        myListsCard = findViewById(R.id.myListsCard)
        statsCard = findViewById(R.id.statsCard)
        practiceButton = findViewById(R.id.practiceButton)
        motivationalText = findViewById(R.id.motivationalText)
        listsCountText = findViewById(R.id.listsCountText)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)
        
        // Set switch state
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        darkModeSwitch.isChecked = isDarkMode
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun playEnterAnimations() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        streakCard.startAnimation(scaleIn)
        
        slideUp.startOffset = 100
        myListsCard.startAnimation(slideUp)
        
        val slideUp2 = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        slideUp2.startOffset = 200
        statsCard.startAnimation(slideUp2)
        
        fadeIn.startOffset = 300
        practiceButton.startAnimation(fadeIn)
    }

    private fun updateStreakDisplay() {
        val streak = streakManager.getStreak()
        streakCount.text = streak.toString()
        
        streakLabel.text = when {
            streak == 0 -> "días"
            streak == 1 -> "día"
            else -> "días"
        }

        // Pulse animation if streak > 0
        if (streak > 0) {
            val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
            streakCard.startAnimation(pulse)
        }
    }

    private fun updateListsCount() {
        lifecycleScope.launch {
            val vocabCount = withContext(Dispatchers.IO) {
                database.vocabularyDao().getAllLists().size
            }
            val verbCount = withContext(Dispatchers.IO) {
                database.verbDao().getAllLists().size
            }
            val total = vocabCount + verbCount
            listsCountText.text = "$total listas"
        }
    }

    private fun updateMotivationalMessage() {
        motivationalText.text = motivationalMessages.random()
    }

    private fun setupListeners() {
        myListsCard.setOnClickListener {
            startActivity(Intent(this, MyListsActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        statsCard.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        practiceButton.setOnClickListener {
            startPractice()
        }

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            toggleDarkMode(isChecked)
        }
    }

    private fun startPractice() {
        lifecycleScope.launch {
            val vocabLists = withContext(Dispatchers.IO) {
                database.vocabularyDao().getAllLists()
            }
            val verbLists = withContext(Dispatchers.IO) {
                database.verbDao().getAllLists()
            }

            if (vocabLists.isEmpty() && verbLists.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "¡Primero agrega listas en 📚 Mis Listas!",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(this@MainActivity, MyListsActivity::class.java))
                return@launch
            }

            // Combine lists with type indicators
            val allItems = mutableListOf<Pair<String, Any>>()
            vocabLists.forEach { allItems.add("📖 ${it.name}" to it) }
            verbLists.forEach { allItems.add("🔤 ${it.name}" to it) }

            val listNames = allItems.map { it.first }.toTypedArray()
            AlertDialog.Builder(this@MainActivity)
                .setTitle("📚 Selecciona una lista")
                .setItems(listNames) { _, which ->
                    val selected = allItems[which].second
                    when (selected) {
                        is com.example.myapplication.data.VocabularyList -> startStudyWithList(selected.id)
                        is com.example.myapplication.data.VerbList -> startVerbStudy(selected.id)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun startStudyWithList(listId: Long) {
        lifecycleScope.launch {
            val words = withContext(Dispatchers.IO) {
                database.vocabularyDao().getWordsForList(listId)
            }

            if (words.isEmpty()) {
                Toast.makeText(this@MainActivity, "Esta lista está vacía", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Convert to WordPair and start study
            val wordPairs = words.map { 
                com.example.myapplication.model.WordPair(it.sourceWord, it.targetWord) 
            }.toMutableList()

            // Show difficulty selection dialog
            showDifficultyDialog(wordPairs)
        }
    }

    private fun showDifficultyDialog(wordPairs: MutableList<com.example.myapplication.model.WordPair>) {
        val difficulties = arrayOf(
            "🐢 Sin timer (relajado)",
            "🟢 Fácil (15 segundos)",
            "🟡 Normal (10 segundos)",
            "🔴 Difícil (5 segundos)"
        )
        val timerValues = arrayOf(0, 15, 10, 5)

        AlertDialog.Builder(this)
            .setTitle("⏱️ Selecciona dificultad")
            .setItems(difficulties) { _, which ->
                StudyActivity.timerSeconds = timerValues[which]
                StudyActivity.wordsToStudy = com.example.myapplication.util.FileParser.shuffleAndRandomizeDirection(wordPairs)
                startActivity(Intent(this, StudyActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun startVerbStudy(listId: Long) {
        lifecycleScope.launch {
            val verbs = withContext(Dispatchers.IO) {
                database.verbDao().getVerbsForList(listId)
            }

            if (verbs.isEmpty()) {
                Toast.makeText(this@MainActivity, "Esta lista está vacía", Toast.LENGTH_SHORT).show()
                return@launch
            }

            VerbStudyActivity.verbsToStudy = verbs.shuffled().toMutableList()
            startActivity(Intent(this@MainActivity, VerbStudyActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    private fun toggleDarkMode(enableDark: Boolean) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("dark_mode", enableDark)
            .apply()
        
        AppCompatDelegate.setDefaultNightMode(
            if (enableDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}