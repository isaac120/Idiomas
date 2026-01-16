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
import com.example.myapplication.data.IrregularVerb
import com.google.android.material.textfield.TextInputEditText

enum class VerbForm {
    BASE, PAST, PARTICIPLE
}

data class VerbQuestion(
    val verb: IrregularVerb,
    val givenForm: VerbForm,
    val askedForm: VerbForm
) {
    fun getGivenText(): String = when (givenForm) {
        VerbForm.BASE -> verb.baseForm
        VerbForm.PAST -> verb.simplePast
        VerbForm.PARTICIPLE -> verb.pastParticiple
    }

    fun getGivenLabel(): String = when (givenForm) {
        VerbForm.BASE -> "Base form"
        VerbForm.PAST -> "Simple past"
        VerbForm.PARTICIPLE -> "Past participle"
    }

    fun getAskedLabel(): String = when (askedForm) {
        VerbForm.BASE -> "BASE FORM"
        VerbForm.PAST -> "SIMPLE PAST"
        VerbForm.PARTICIPLE -> "PAST PARTICIPLE"
    }

    fun getCorrectAnswer(): String = when (askedForm) {
        VerbForm.BASE -> verb.baseForm
        VerbForm.PAST -> verb.simplePast
        VerbForm.PARTICIPLE -> verb.pastParticiple
    }
}

class VerbStudyActivity : AppCompatActivity() {

    companion object {
        var verbsToStudy: MutableList<IrregularVerb> = mutableListOf()
    }

    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var givenFormLabel: TextView
    private lateinit var givenFormText: TextView
    private lateinit var askedFormLabel: TextView
    private lateinit var answerInput: TextInputEditText
    private lateinit var feedbackText: TextView
    private lateinit var checkButton: Button
    private lateinit var scoreText: TextView

    private var questions: List<VerbQuestion> = emptyList()
    private var currentIndex = 0
    private var correctCount = 0
    private var isShowingFeedback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verb_study)

        if (verbsToStudy.isEmpty()) {
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
        givenFormLabel = findViewById(R.id.givenFormLabel)
        givenFormText = findViewById(R.id.givenFormText)
        askedFormLabel = findViewById(R.id.askedFormLabel)
        answerInput = findViewById(R.id.answerInput)
        feedbackText = findViewById(R.id.feedbackText)
        checkButton = findViewById(R.id.checkButton)
        scoreText = findViewById(R.id.scoreText)
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
        val allQuestions = mutableListOf<VerbQuestion>()
        val forms = listOf(VerbForm.BASE, VerbForm.PAST, VerbForm.PARTICIPLE)

        verbsToStudy.forEach { verb ->
            // Generate 2 different questions per verb
            val usedCombinations = mutableSetOf<Pair<VerbForm, VerbForm>>()
            
            repeat(2) {
                var givenForm: VerbForm
                var askedForm: VerbForm
                
                do {
                    givenForm = forms.random()
                    askedForm = forms.filter { it != givenForm }.random()
                } while (usedCombinations.contains(givenForm to askedForm))
                
                usedCombinations.add(givenForm to askedForm)
                allQuestions.add(VerbQuestion(verb, givenForm, askedForm))
            }
        }

        questions = allQuestions.shuffled()
    }

    private fun showCurrentQuestion() {
        val question = questions[currentIndex]

        // Update progress
        progressText.text = "Pregunta ${currentIndex + 1} de ${questions.size}"
        progressBar.progress = ((currentIndex + 1) * 100) / questions.size
        scoreText.text = "✓ $correctCount"

        // Show question
        givenFormText.text = question.getGivenText()
        givenFormLabel.text = "(${question.getGivenLabel()})"
        askedFormLabel.text = question.getAskedLabel()

        // Clear input
        answerInput.setText("")
        answerInput.isEnabled = true

        // Reset UI
        feedbackText.visibility = View.GONE
        checkButton.text = "✓ Verificar"
        isShowingFeedback = false

        // Focus on input
        answerInput.requestFocus()
    }

    private fun checkAnswer() {
        val question = questions[currentIndex]
        val userAnswer = answerInput.text.toString().trim().lowercase()
        val correctAnswer = question.getCorrectAnswer().lowercase()

        answerInput.isEnabled = false

        if (userAnswer == correctAnswer) {
            correctCount++
            scoreText.text = "✓ $correctCount"
            feedbackText.text = "✅ ¡Correcto!"
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
        } else {
            feedbackText.text = "❌ ${question.getCorrectAnswer()}"
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
        val percentage = (correctCount * 100) / questions.size
        val emoji = when {
            percentage >= 90 -> "🏆"
            percentage >= 70 -> "🎉"
            percentage >= 50 -> "👍"
            else -> "📚"
        }

        AlertDialog.Builder(this)
            .setTitle("$emoji Resultados")
            .setMessage("Correctos: $correctCount/${questions.size}\nPrecisión: $percentage%")
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
