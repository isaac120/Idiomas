package com.example.myapplication

import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.PracticeSession
import com.example.myapplication.data.StreakManager
import com.example.myapplication.data.WordStats
import com.example.myapplication.model.WordPair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class StudyActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        // Words passed from MainActivity
        var wordsToStudy: MutableList<WordPair> = mutableListOf()
    }

    // UI Elements
    private lateinit var progressText: TextView
    private lateinit var scoreText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var languageLabel: TextView
    private lateinit var wordText: TextView
    private lateinit var answerPrompt: TextView
    private lateinit var answerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var feedbackCard: CardView
    private lateinit var feedbackContent: LinearLayout
    private lateinit var feedbackEmoji: TextView
    private lateinit var feedbackText: TextView
    private lateinit var correctAnswerText: TextView
    private lateinit var nextButton: Button
    private lateinit var completeOverlay: LinearLayout
    private lateinit var finalScoreText: TextView
    private lateinit var finishButton: Button
    private lateinit var answerSection: LinearLayout
    private lateinit var speakerButton: ImageButton

    // Audio
    private lateinit var tts: TextToSpeech
    private var ttsInitialized = false
    private var correctSound: MediaPlayer? = null
    private var incorrectSound: MediaPlayer? = null

    // Study session state
    private var pendingWords: MutableList<WordPair> = mutableListOf()
    private var currentWord: WordPair? = null
    private var correctCount = 0
    private var totalWords = 0
    private var currentIndex = 0

    // Streak
    private val streakManager by lazy { StreakManager(this) }
    
    // Database
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val failedWords = mutableSetOf<String>()
    private val correctWords = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_study)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.progressHeader)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                wordsToStudy.clear()
                finish()
            }
        })

        // Initialize TTS
        tts = TextToSpeech(this, this)

        // Initialize sound effects
        correctSound = MediaPlayer.create(this, R.raw.correct)
        incorrectSound = MediaPlayer.create(this, R.raw.incorrect)

        initViews()
        setupListeners()
        startStudySession()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsInitialized = true
            // Set default language
            tts.language = Locale("es", "ES")
        }
    }

    private fun initViews() {
        progressText = findViewById(R.id.progressText)
        scoreText = findViewById(R.id.scoreText)
        progressBar = findViewById(R.id.progressBar)
        languageLabel = findViewById(R.id.languageLabel)
        wordText = findViewById(R.id.wordText)
        answerPrompt = findViewById(R.id.answerPrompt)
        answerInput = findViewById(R.id.answerInput)
        submitButton = findViewById(R.id.submitButton)
        feedbackCard = findViewById(R.id.feedbackCard)
        feedbackContent = findViewById(R.id.feedbackContent)
        feedbackEmoji = findViewById(R.id.feedbackEmoji)
        feedbackText = findViewById(R.id.feedbackText)
        correctAnswerText = findViewById(R.id.correctAnswerText)
        nextButton = findViewById(R.id.nextButton)
        completeOverlay = findViewById(R.id.completeOverlay)
        finalScoreText = findViewById(R.id.finalScoreText)
        finishButton = findViewById(R.id.finishButton)
        answerSection = findViewById(R.id.answerSection)
        speakerButton = findViewById(R.id.speakerButton)
    }

    private fun setupListeners() {
        submitButton.setOnClickListener {
            checkAnswer()
        }

        answerInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkAnswer()
                true
            } else {
                false
            }
        }

        nextButton.setOnClickListener {
            showNextWord()
        }

        finishButton.setOnClickListener {
            finish()
        }

        speakerButton.setOnClickListener {
            speakCurrentWord()
        }
    }

    private fun speakCurrentWord() {
        if (!ttsInitialized) return

        currentWord?.let { word ->
            val textToSpeak = word.getDisplayWord()
            val locale = if (word.showSourceFirst) {
                Locale("es", "ES") // Spanish
            } else {
                Locale.US // English
            }
            tts.language = locale
            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "word")
        }
    }

    private fun speakWord(text: String, isSpanish: Boolean) {
        if (!ttsInitialized) return

        val locale = if (isSpanish) {
            Locale("es", "ES")
        } else {
            Locale.US
        }
        tts.language = locale
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "answer")
    }

    private fun playCorrectSound() {
        correctSound?.let {
            if (it.isPlaying) {
                it.seekTo(0)
            }
            it.start()
        }
    }

    private fun playIncorrectSound() {
        incorrectSound?.let {
            if (it.isPlaying) {
                it.seekTo(0)
            }
            it.start()
        }
    }

    private fun startStudySession() {
        pendingWords = wordsToStudy.toMutableList()
        totalWords = pendingWords.size
        correctCount = 0
        currentIndex = 0

        if (pendingWords.isEmpty()) {
            finish()
            return
        }

        updateProgress()
        showNextWord()
    }

    private fun showNextWord() {
        // Hide feedback
        feedbackCard.visibility = View.GONE
        answerSection.visibility = View.VISIBLE
        answerInput.isEnabled = true
        submitButton.isEnabled = true
        answerInput.text.clear()

        if (pendingWords.isEmpty()) {
            showSessionComplete()
            return
        }

        // Get next random word
        currentWord = pendingWords.random()
        currentIndex++

        currentWord?.let { word ->
            // Display the word
            wordText.text = word.getDisplayWord()
            
            // Set language labels based on direction
            if (word.showSourceFirst) {
                languageLabel.text = "ESPAÑOL"
                answerPrompt.text = "Escribe en INGLÉS:"
            } else {
                languageLabel.text = "ENGLISH"
                answerPrompt.text = "Escribe en ESPAÑOL:"
            }
        }

        updateProgress()
        answerInput.requestFocus()
    }

    private fun checkAnswer() {
        val userAnswer = answerInput.text.toString().trim()
        
        if (userAnswer.isEmpty()) {
            answerInput.error = "Escribe tu respuesta"
            return
        }

        currentWord?.let { word ->
            val isCorrect = word.checkAnswer(userAnswer)

            // Disable input
            answerInput.isEnabled = false
            submitButton.isEnabled = false

            // Play sound effect
            if (isCorrect) {
                playCorrectSound()
            } else {
                playIncorrectSound()
                // Speak the correct answer after a short delay
                android.os.Handler(mainLooper).postDelayed({
                    val isSpanish = !word.showSourceFirst // If showing English, answer is Spanish
                    speakWord(word.getExpectedAnswer(), isSpanish)
                }, 500)
            }

            // Show feedback
            showFeedback(isCorrect, word.getExpectedAnswer())

            if (isCorrect) {
                // Remove word from pending list
                pendingWords.remove(word)
                correctCount++
                scoreText.text = "✓ $correctCount"
                correctWords.add(word.getExpectedAnswer())
            } else {
                // Word stays in the list for retry
                // Optionally move to end of list
                pendingWords.remove(word)
                pendingWords.add(word)
                failedWords.add(word.getExpectedAnswer())
            }

            updateProgress()
        }
    }

    private fun showFeedback(isCorrect: Boolean, correctAnswer: String) {
        feedbackCard.visibility = View.VISIBLE
        answerSection.visibility = View.GONE

        if (isCorrect) {
            feedbackEmoji.text = "✓"
            feedbackText.text = "¡Correcto!"
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            feedbackContent.setBackgroundColor(ContextCompat.getColor(this, R.color.success_green_light))
            correctAnswerText.visibility = View.GONE
        } else {
            feedbackEmoji.text = "✗"
            feedbackText.text = "Incorrecto"
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
            feedbackContent.setBackgroundColor(ContextCompat.getColor(this, R.color.error_red_light))
            correctAnswerText.visibility = View.VISIBLE
            correctAnswerText.text = "Respuesta correcta: $correctAnswer"
            correctAnswerText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }

        // Update button text based on remaining words
        if (pendingWords.isEmpty()) {
            nextButton.text = "Ver resultados"
        } else {
            nextButton.text = "Siguiente →"
        }
    }

    private fun updateProgress() {
        val completed = totalWords - pendingWords.size
        val progressPercent = if (totalWords > 0) (completed * 100) / totalWords else 0
        
        progressBar.progress = progressPercent
        progressText.text = "Palabra ${minOf(currentIndex, totalWords)} de $totalWords"
    }

    private fun showSessionComplete() {
        completeOverlay.visibility = View.VISIBLE
        finalScoreText.text = "Completaste $correctCount de $totalWords palabras"
        
        // Record practice for streak
        val newStreak = streakManager.recordPractice()
        if (newStreak > 0) {
            android.widget.Toast.makeText(
                this,
                "🔥 Racha: $newStreak días",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        
        // Save session to database
        saveSessionStats()
    }

    private fun saveSessionStats() {
        CoroutineScope(Dispatchers.IO).launch {
            val statsDao = database.statsDao()
            
            // Save session
            statsDao.insertSession(
                PracticeSession(
                    correctCount = correctCount,
                    totalCount = totalWords
                )
            )
            
            // Update word stats for failed words
            failedWords.forEach { word ->
                val existing = statsDao.getWordStats(word)
                if (existing != null) {
                    statsDao.incrementIncorrect(word)
                } else {
                    statsDao.insertWordStats(WordStats(word = word, incorrectCount = 1))
                }
            }
            
            // Update word stats for correct words
            correctWords.forEach { word ->
                val existing = statsDao.getWordStats(word)
                if (existing != null) {
                    statsDao.incrementCorrect(word)
                } else {
                    statsDao.insertWordStats(WordStats(word = word, correctCount = 1))
                }
            }
        }
    }

    override fun onDestroy() {
        // Release TTS
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        // Release MediaPlayers
        correctSound?.release()
        incorrectSound?.release()
        correctSound = null
        incorrectSound = null

        super.onDestroy()
    }
}
