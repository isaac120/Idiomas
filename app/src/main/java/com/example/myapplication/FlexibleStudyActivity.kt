package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.Button
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

data class FlexibleQuestion(
    val item: ListItem,
    val givenColumnIndex: Int,
    val askedColumnIndex: Int,
    val givenValue: String,
    val askedValue: String,
    val givenHeader: String,
    val askedHeader: String
)

class FlexibleStudyActivity : AppCompatActivity() {

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

    private var questions: List<FlexibleQuestion> = emptyList()
    private var currentIndex = 0
    private var correctCount = 0
    private var isShowingFeedback = false

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
        generateQuestions()
        showCurrentQuestion()
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
    }

    private fun setupListeners() {
        checkButton.setOnClickListener {
            if (isShowingFeedback) {
                nextQuestion()
            } else {
                checkAnswer()
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
                
                // Generate 2 questions per item with different column combinations
                repeat(2) {
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        questions = allQuestions.shuffled()
    }

    private fun showCurrentQuestion() {
        if (questions.isEmpty()) {
            showResults()
            return
        }

        val question = questions[currentIndex]

        progressText.text = "Pregunta ${currentIndex + 1} de ${questions.size}"
        progressBar.progress = ((currentIndex + 1) * 100) / questions.size
        scoreText.text = "✓ $correctCount"

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
        val question = questions[currentIndex]
        val userAnswer = answerInput.text.toString().trim().lowercase()
        val correctAnswer = question.askedValue.lowercase()

        answerInput.isEnabled = false

        if (userAnswer == correctAnswer) {
            correctCount++
            scoreText.text = "✓ $correctCount"
            feedbackText.text = "✅ ¡Correcto!"
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
        } else {
            feedbackText.text = "❌ ${question.askedValue}"
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
        }

        feedbackText.visibility = View.VISIBLE
        checkButton.text = if (currentIndex < questions.size - 1) "Siguiente →" else "Ver resultados"
        isShowingFeedback = true
    }

    private fun nextQuestion() {
        currentIndex++

        if (currentIndex >= questions.size) {
            showResults()
        } else {
            showCurrentQuestion()
        }
    }

    private fun showResults() {
        val total = questions.size.coerceAtLeast(1)
        val percentage = (correctCount * 100) / total
        val emoji = when {
            percentage >= 90 -> "🏆"
            percentage >= 70 -> "🎉"
            percentage >= 50 -> "👍"
            else -> "📚"
        }

        AlertDialog.Builder(this)
            .setTitle("$emoji Resultados")
            .setMessage("Correctos: $correctCount/$total\nPrecisión: $percentage%")
            .setPositiveButton("Terminar") { _, _ ->
                finish()
            }
            .setNeutralButton("Practicar de nuevo") { _, _ ->
                currentIndex = 0
                correctCount = 0
                generateQuestions()
                showCurrentQuestion()
            }
            .setCancelable(false)
            .show()
    }
}
