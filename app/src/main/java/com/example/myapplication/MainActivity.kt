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
import com.example.myapplication.data.FlexibleList
import com.example.myapplication.data.StreakManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

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
            val count = withContext(Dispatchers.IO) {
                database.flexibleDao().getAllLists().size
            }
            listsCountText.text = "$count listas"
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
            val lists = withContext(Dispatchers.IO) {
                database.flexibleDao().getAllLists()
            }

            if (lists.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "¡Primero agrega listas en 📚 Mis Listas!",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(this@MainActivity, MyListsActivity::class.java))
                return@launch
            }

            // Get item counts for each list
            val listInfos = lists.map { list ->
                val itemCount = withContext(Dispatchers.IO) {
                    database.flexibleDao().getItemsForList(list.id).size
                }
                ListDisplayInfo(list, itemCount)
            }

            showListSelectionDialog(listInfos)
        }
    }

    private fun showListSelectionDialog(listInfos: List<ListDisplayInfo>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_list, null)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.listsRecyclerView)
        val cancelButton = dialogView.findViewById<TextView>(R.id.cancelButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.adapter = ListCardAdapter(listInfos) { selectedList ->
            dialog.dismiss()
            startStudyWithFlexibleList(selectedList)
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startStudyWithFlexibleList(list: FlexibleList) {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                database.flexibleDao().getItemsForList(list.id)
            }

            if (items.isEmpty()) {
                Toast.makeText(this@MainActivity, "Esta lista está vacía", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Parse column headers
            val headers = try {
                val arr = JSONArray(list.columnHeaders)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (e: Exception) {
                (1..list.columnCount).map { "Columna $it" }
            }

            // Show mode selection dialog
            showModeSelectionDialog(items, headers, list.name)
        }
    }
    
    private fun showModeSelectionDialog(items: List<com.example.myapplication.data.ListItem>, headers: List<String>, listName: String) {
        val modes = arrayOf(
            "♾️ Sin límite de tiempo",
            "💀 2 segundos (Ultra Hard)",
            "⚡ 5 segundos",
            "🔥 10 segundos",
            "🎯 15 segundos",
            "🐢 20 segundos"
        )
        
        AlertDialog.Builder(this)
            .setTitle("⏱️ Modo de práctica")
            .setItems(modes) { _, which ->
                val timeLimit = when (which) {
                    1 -> 2000L
                    2 -> 5000L
                    3 -> 10000L
                    4 -> 15000L
                    5 -> 20000L
                    else -> 0L
                }
                
                FlexibleStudyActivity.itemsToStudy = items.shuffled().toMutableList()
                FlexibleStudyActivity.columnHeaders = headers
                FlexibleStudyActivity.listName = listName
                FlexibleStudyActivity.timeLimitMs = timeLimit
                startActivity(Intent(this@MainActivity, FlexibleStudyActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            .setNegativeButton("Cancelar", null)
            .show()
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