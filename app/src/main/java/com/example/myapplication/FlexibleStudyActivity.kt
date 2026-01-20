package com.example.myapplication

import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ListItem
import com.example.myapplication.data.PracticeSession
import com.example.myapplication.data.StreakManager
import com.example.myapplication.data.WordStats
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale

data class FlexibleQuestion(
    val item: ListItem,
    val givenColumnIndex: Int,
    val askedColumnIndex: Int,
    val givenValue: String,
    val askedValue: String,
    val givenHeader: String,
    val askedHeader: String
)

class FlexibleStudyActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        var itemsToStudy: MutableList<ListItem> = mutableListOf()
        var columnHeaders: List<String> = emptyList()
        var listName: String = ""
        var timeLimitMs: Long = 0 // 0 = no limit
    }

    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var scoreText: TextView
    private lateinit var givenFormLabel: TextView
    private lateinit var givenFormText: TextView
    private lateinit var askedFormLabel: TextView
    private lateinit var answerInput: TextInputEditText
    private lateinit var feedbackText: TextView
    private lateinit var checkButton: Button
    private lateinit var speakerButton: ImageButton
    private lateinit var timerSection: LinearLayout
    private lateinit var timerBar: ProgressBar
    private lateinit var timerText: TextView

    private var pendingQuestions: MutableList<FlexibleQuestion> = mutableListOf()
    private var currentQuestion: FlexibleQuestion? = null
    private var totalQuestions = 0
    private var answeredCorrectly = 0
    private var totalAnswered = 0
    private var isShowingFeedback = false

    // Audio
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var correctSound: MediaPlayer? = null
    private var incorrectSound: MediaPlayer? = null

    // Database & Streak
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val streakManager by lazy { StreakManager(this) }
    
    // Timer
    private var countDownTimer: android.os.CountDownTimer? = null
    private var isTimedMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_flexible_study)

        if (itemsToStudy.isEmpty() || columnHeaders.size < 2) {
            finish()
            return
        }

        initViews()
        setupWindowInsets()
        setupListeners()
        setupAudio()
        generateQuestions()
        showNextQuestion()
    }

    private fun setupAudio() {
        tts = TextToSpeech(this, this)
        correctSound = MediaPlayer.create(this, R.raw.correct)
        incorrectSound = MediaPlayer.create(this, R.raw.incorrect)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onDestroy() {
        stopTimer()
        tts?.stop()
        tts?.shutdown()
        correctSound?.release()
        incorrectSound?.release()
        super.onDestroy()
    }

    private fun speakWord(word: String) {
        if (isTtsReady) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word")
        }
    }

    private fun playCorrectSound() {
        correctSound?.seekTo(0)
        correctSound?.start()
    }

    private fun playIncorrectSound() {
        incorrectSound?.seekTo(0)
        incorrectSound?.start()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top + 24, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    private fun initViews() {
        progressText = findViewById(R.id.progressText)
        progressBar = findViewById(R.id.progressBar)
        scoreText = findViewById(R.id.scoreText)
        givenFormLabel = findViewById(R.id.givenFormLabel)
        givenFormText = findViewById(R.id.givenFormText)
        askedFormLabel = findViewById(R.id.askedFormLabel)
        answerInput = findViewById(R.id.answerInput)
        feedbackText = findViewById(R.id.feedbackText)
        checkButton = findViewById(R.id.checkButton)
        speakerButton = findViewById(R.id.speakerButton)
        timerSection = findViewById(R.id.timerSection)
        timerBar = findViewById(R.id.timerBar)
        timerText = findViewById(R.id.timerText)
        
        // Setup timed mode if active
        isTimedMode = timeLimitMs > 0
        timerSection.visibility = if (isTimedMode) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        checkButton.setOnClickListener {
            if (isShowingFeedback) {
                showNextQuestion()
            } else {
                checkAnswer()
            }
        }

        speakerButton.setOnClickListener {
            currentQuestion?.let { speakWord(it.givenValue) }
        }
        
        // Handle Enter key on keyboard
        answerInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                if (isShowingFeedback) {
                    showNextQuestion()
                } else {
                    checkAnswer()
                }
                true
            } else {
                false
            }
        }
    }

    private fun generateQuestions() {
        val allQuestions = mutableListOf<FlexibleQuestion>()
        val columnIndices = columnHeaders.indices.toList()

        itemsToStudy.forEach { item ->
            try {
                val values = JSONArray(item.values)
                val valuesList = (0 until values.length()).map { values.getString(it) }
                
                // Generate 1 question per item with random column combination
                val givenIndex = columnIndices.random()
                val askedIndex = columnIndices.filter { it != givenIndex }.random()
                
                val givenValue = valuesList.getOrElse(givenIndex) { "" }
                val askedValue = valuesList.getOrElse(askedIndex) { "" }
                
                if (givenValue.isNotEmpty() && askedValue.isNotEmpty()) {
                    allQuestions.add(
                        FlexibleQuestion(
                            item = item,
                            givenColumnIndex = givenIndex,
                            askedColumnIndex = askedIndex,
                            givenValue = givenValue,
                            askedValue = askedValue,
                            givenHeader = columnHeaders.getOrElse(givenIndex) { "Columna ${givenIndex + 1}" },
                            askedHeader = columnHeaders.getOrElse(askedIndex) { "Columna ${askedIndex + 1}" }
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        pendingQuestions = allQuestions.shuffled().toMutableList()
        totalQuestions = pendingQuestions.size
        answeredCorrectly = 0
        totalAnswered = 0
    }

    private fun showNextQuestion() {
        // Stop any existing timer
        stopTimer()
        
        if (pendingQuestions.isEmpty()) {
            showResults()
            return
        }

        currentQuestion = pendingQuestions.removeAt(0)
        val question = currentQuestion!!

        // Progress shows how many have been correctly answered out of total
        progressText.text = "Pregunta ${answeredCorrectly + 1} de $totalQuestions"
        progressBar.progress = (answeredCorrectly * 100) / totalQuestions.coerceAtLeast(1)
        scoreText.text = "✓ $answeredCorrectly"

        givenFormText.text = question.givenValue
        givenFormLabel.text = "(${question.givenHeader})"
        askedFormLabel.text = question.askedHeader.uppercase()

        answerInput.setText("")
        answerInput.isEnabled = true

        feedbackText.visibility = View.GONE
        checkButton.text = "✓ Verificar"
        isShowingFeedback = false

        answerInput.requestFocus()
        
        // Start timer if in timed mode
        if (isTimedMode) {
            startTimer()
        }
    }
    
    private fun startTimer() {
        timerBar.progress = 100
        timerBar.progressTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.primary_blue)
        )
        
        countDownTimer = object : android.os.CountDownTimer(timeLimitMs, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = (millisUntilFinished * 100 / timeLimitMs).toInt()
                timerBar.progress = progress
                
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                timerText.text = "⏱️ ${secondsLeft}s"
                
                // Change color when low on time
                if (secondsLeft <= 3) {
                    timerBar.progressTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this@FlexibleStudyActivity, R.color.error_red)
                    )
                    timerText.setTextColor(ContextCompat.getColor(this@FlexibleStudyActivity, R.color.error_red))
                } else {
                    timerText.setTextColor(ContextCompat.getColor(this@FlexibleStudyActivity, R.color.text_secondary))
                }
            }
            
            override fun onFinish() {
                timerBar.progress = 0
                timerText.text = "⏱️ 0s"
                // Time's up! Count as incorrect
                handleTimeUp()
            }
        }.start()
    }
    
    private fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }
    
    private fun handleTimeUp() {
        if (isShowingFeedback) return
        
        val question = currentQuestion ?: return
        
        answerInput.isEnabled = false
        totalAnswered++
        
        feedbackText.text = "⏱️ ¡Tiempo! → ${question.askedValue}"
        feedbackText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
        feedbackText.visibility = View.VISIBLE
        playIncorrectSound()
        
        // Track incorrect answer
        trackWordResult(question.askedValue, isCorrect = false)
        
        // Add question back to queue
        pendingQuestions.add(question)
        
        checkButton.text = if (pendingQuestions.isNotEmpty()) "Siguiente →" else "Ver resultados"
        isShowingFeedback = true
    }

    private fun checkAnswer() {
        // Stop timer when user submits answer
        stopTimer()
        
        val question = currentQuestion ?: return
        val userAnswer = answerInput.text.toString().trim().lowercase()
        val correctAnswer = question.askedValue.lowercase()

        answerInput.isEnabled = false
        totalAnswered++

        if (userAnswer == correctAnswer) {
            answeredCorrectly++
            scoreText.text = "✓ $answeredCorrectly"
            progressText.text = "Pregunta ${answeredCorrectly + 1} de $totalQuestions"
            progressBar.progress = (answeredCorrectly * 100) / totalQuestions.coerceAtLeast(1)
            feedbackText.text = "✅ ¡Correcto!"
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            playCorrectSound()
            
            // Track correct answer
            trackWordResult(question.askedValue, isCorrect = true)
        } else {
            feedbackText.text = "❌ ${question.askedValue}"
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
            playIncorrectSound()
            
            // Track incorrect answer
            trackWordResult(question.askedValue, isCorrect = false)
            
            // Add question back to end of queue to practice again
            pendingQuestions.add(question)
        }

        feedbackText.visibility = View.VISIBLE
        checkButton.text = if (pendingQuestions.isNotEmpty()) "Siguiente →" else "Ver resultados"
        isShowingFeedback = true
    }

    private fun trackWordResult(word: String, isCorrect: Boolean) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val statsDao = database.statsDao()
                
                // Check if word exists
                val existingStats = statsDao.getWordStats(word)
                if (existingStats == null) {
                    // Create new word stats
                    statsDao.insertWordStats(
                        WordStats(
                            word = word,
                            incorrectCount = if (isCorrect) 0 else 1,
                            correctCount = if (isCorrect) 1 else 0
                        )
                    )
                } else {
                    // Update existing
                    if (isCorrect) {
                        statsDao.incrementCorrect(word)
                    } else {
                        statsDao.incrementIncorrect(word)
                    }
                }
            }
        }
    }

    private fun saveSession() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.statsDao().insertSession(
                    PracticeSession(
                        correctCount = answeredCorrectly,
                        totalCount = totalAnswered,
                        listName = listName
                    )
                )
            }
        }
    }

    private fun showResults() {
        // Save the session and record streak
        saveSession()
        val streak = streakManager.recordPractice()
        
        // Check for new achievements
        val achievementManager = com.example.myapplication.data.AchievementManager(this)
        val newlyUnlocked = mutableListOf<com.example.myapplication.data.Achievement>()
        
        // Check streak achievements
        newlyUnlocked.addAll(achievementManager.checkStreakAchievements(streak))
        
        // Check word achievements (get total words from database)
        lifecycleScope.launch {
            val totalWords = withContext(Dispatchers.IO) {
                database.statsDao().getTotalAnswered() ?: 0
            }
            newlyUnlocked.addAll(achievementManager.checkWordAchievements(totalWords))
            
            // Check perfect session
            achievementManager.checkPerfectSession(answeredCorrectly, totalAnswered)?.let {
                newlyUnlocked.add(it)
            }
            
            // Check speed achievement
            achievementManager.checkSpeedAchievement(timeLimitMs, true)?.let {
                newlyUnlocked.add(it)
            }
            
            // Check consistent accuracy
            val totalCorrect = withContext(Dispatchers.IO) {
                database.statsDao().getTotalCorrect() ?: 0
            }
            val overallAccuracy = if (totalWords > 0) (totalCorrect * 100 / totalWords) else 0
            achievementManager.checkConsistentAccuracy(overallAccuracy)?.let {
                newlyUnlocked.add(it)
            }
            
            // Show results dialog
            showResultsDialog(streak, newlyUnlocked)
        }
    }
    
    private fun showResultsDialog(streak: Int, newlyUnlocked: List<com.example.myapplication.data.Achievement>) {
        val streakMessage = if (streak > 1) "🔥 Racha: $streak días seguidos" else "🔥 ¡Empezaste tu racha!"
        
        val achievementMessage = if (newlyUnlocked.isNotEmpty()) {
            val badges = newlyUnlocked.joinToString(" ") { it.icon }
            val names = newlyUnlocked.joinToString(", ") { it.name }
            "\n\n🏆 ¡Nuevos logros!\n$badges\n$names"
        } else ""
        
        AlertDialog.Builder(this)
            .setTitle("🏆 ¡Completado!")
            .setMessage("Has respondido correctamente las $totalQuestions preguntas.\n\n$streakMessage$achievementMessage")
            .setPositiveButton("Terminar") { _, _ ->
                finish()
            }
            .setNeutralButton("Practicar de nuevo") { _, _ ->
                generateQuestions()
                showNextQuestion()
            }
            .setCancelable(false)
            .show()
    }
}
