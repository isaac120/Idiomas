package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages daily practice streak using SharedPreferences.
 */
class StreakManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "streak_prefs"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_LAST_PRACTICE_DATE = "last_practice_date"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Get the current streak count.
     */
    fun getStreak(): Int {
        checkStreakValidity()
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }

    /**
     * Get the best streak ever achieved.
     */
    fun getBestStreak(): Int {
        return prefs.getInt(KEY_BEST_STREAK, 0)
    }

    /**
     * Record a practice session. Should be called when user completes a study session.
     * Returns the new streak count.
     */
    fun recordPractice(): Int {
        val today = getTodayDate()
        val lastPracticeDate = prefs.getString(KEY_LAST_PRACTICE_DATE, null)
        var currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        var bestStreak = prefs.getInt(KEY_BEST_STREAK, 0)

        when {
            // First time practicing or same day - don't change streak if same day
            lastPracticeDate == null -> {
                currentStreak = 1
            }
            lastPracticeDate == today -> {
                // Already practiced today, streak stays the same
                return currentStreak
            }
            isYesterday(lastPracticeDate) -> {
                // Practiced yesterday, increment streak
                currentStreak++
            }
            else -> {
                // Missed days, reset streak
                currentStreak = 1
            }
        }

        // Update best streak if current is higher
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak
        }

        // Save updated values
        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putInt(KEY_BEST_STREAK, bestStreak)
            .putString(KEY_LAST_PRACTICE_DATE, today)
            .apply()

        return currentStreak
    }

    /**
     * Check if streak is still valid (practiced yesterday or today).
     * If not, reset the streak.
     */
    private fun checkStreakValidity() {
        val lastPracticeDate = prefs.getString(KEY_LAST_PRACTICE_DATE, null) ?: return
        val today = getTodayDate()

        // If last practice was today or yesterday, streak is valid
        if (lastPracticeDate == today || isYesterday(lastPracticeDate)) {
            return
        }

        // Streak broken - reset to 0
        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, 0)
            .apply()
    }

    /**
     * Check if a date string is yesterday.
     */
    private fun isYesterday(dateString: String): Boolean {
        return try {
            val date = dateFormat.parse(dateString) ?: return false
            val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            dateFormat.format(date) == dateFormat.format(yesterday)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get today's date as a string.
     */
    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    /**
     * Check if user has practiced today.
     */
    fun hasPracticedToday(): Boolean {
        val lastPracticeDate = prefs.getString(KEY_LAST_PRACTICE_DATE, null)
        return lastPracticeDate == getTodayDate()
    }
}
