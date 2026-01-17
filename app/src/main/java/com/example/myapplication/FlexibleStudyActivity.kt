package com.example.myapplication

import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.data.ListItem
import com.google.android.material.textfield.TextInputEditText
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

    private var pendingQuestions: MutableList<FlexibleQuestion> = mutableListOf()
    private var currentQuestion: FlexibleQuestion? = null
    private var totalQuestions = 0
    private var answeredCorrectly = 0
    private var isShowingFeedback = false

    // Audio
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var correctSound: MediaPlayer? = null
    private var incorrectSound: MediaPlayer? = null

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
    }

    private fun showNextQuestion() {
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
    }

    private fun checkAnswer() {
        val question = currentQuestion ?: return
        val userAnswer = answerInput.text.toString().trim().lowercase()
        val correctAnswer = question.askedValue.lowercase()

        answerInput.isEnabled = false

        if (userAnswer == correctAnswer) {
            answeredCorrectly++
            scoreText.text = "✓ $answeredCorrectly"
            progressText.text = "Pregunta ${answeredCorrectly + 1} de $totalQuestions"
            progressBar.progress = (answeredCorrectly * 100) / totalQuestions.coerceAtLeast(1)
            feedbackText.text = "✅ ¡Correcto!"
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            playCorrectSound()
        } else {
            feedbackText.text = "❌ ${question.askedValue}"
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
            playIncorrectSound()
            
            // Add question back to end of queue to practice again
            pendingQuestions.add(question)
        }

        feedbackText.visibility = View.VISIBLE
        checkButton.text = if (pendingQuestions.isNotEmpty()) "Siguiente →" else "Ver resultados"
        isShowingFeedback = true
    }

    private fun showResults() {
        val percentage = if (totalQuestions > 0) 100 else 0
        val emoji = "🏆"

        AlertDialog.Builder(this)
            .setTitle("$emoji ¡Completado!")
            .setMessage("Has respondido correctamente las $totalQuestions preguntas.")
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
