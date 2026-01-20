package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Data class representing an achievement/badge.
 */
data class Achievement(
    val id: String,
    val icon: String,
    val name: String,
    val description: String,
    val requirement: Int = 0
)

/**
 * Manager for tracking and checking achievements.
 */
class AchievementManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
    
    companion object {
        // Streak achievements
        val STREAK_1 = Achievement("streak_1", "🔥", "Primer paso", "1 día de racha", 1)
        val STREAK_7 = Achievement("streak_7", "🔥", "Una semana", "7 días seguidos", 7)
        val STREAK_30 = Achievement("streak_30", "🔥", "Un mes", "30 días seguidos", 30)
        
        // Words achievements
        val WORDS_50 = Achievement("words_50", "📖", "Principiante", "50 palabras practicadas", 50)
        val WORDS_200 = Achievement("words_200", "📚", "Estudiante", "200 palabras practicadas", 200)
        val WORDS_500 = Achievement("words_500", "🎓", "Experto", "500 palabras practicadas", 500)
        val WORDS_1000 = Achievement("words_1000", "🏆", "Maestro", "1000 palabras practicadas", 1000)
        
        // Accuracy achievements
        val PERFECT_SESSION = Achievement("perfect", "✨", "Perfecto", "100% en una sesión (mínimo 10)")
        val CONSISTENT = Achievement("consistent", "💎", "Consistente", "90%+ promedio general")
        
        // Speed achievements
        val SPEED_5S = Achievement("speed_5s", "⚡", "Rayo", "Completar sesión en modo 5s")
        val SPEED_2S = Achievement("speed_2s", "💀", "Imposible", "Completar sesión en modo 2s")
        
        val ALL_ACHIEVEMENTS = listOf(
            STREAK_1, STREAK_7, STREAK_30,
            WORDS_50, WORDS_200, WORDS_500, WORDS_1000,
            PERFECT_SESSION, CONSISTENT,
            SPEED_5S, SPEED_2S
        )
    }
    
    /**
     * Check if an achievement is unlocked.
     */
    fun isUnlocked(achievement: Achievement): Boolean {
        return prefs.getBoolean(achievement.id, false)
    }
    
    /**
     * Unlock an achievement.
     */
    fun unlock(achievement: Achievement): Boolean {
        if (!isUnlocked(achievement)) {
            prefs.edit().putBoolean(achievement.id, true).apply()
            return true // Newly unlocked
        }
        return false // Already unlocked
    }
    
    /**
     * Get all unlocked achievements.
     */
    fun getUnlockedAchievements(): List<Achievement> {
        return ALL_ACHIEVEMENTS.filter { isUnlocked(it) }
    }
    
    /**
     * Get count of unlocked achievements.
     */
    fun getUnlockedCount(): Int {
        return ALL_ACHIEVEMENTS.count { isUnlocked(it) }
    }
    
    /**
     * Check streak achievements based on current streak.
     * Returns list of newly unlocked achievements.
     */
    fun checkStreakAchievements(currentStreak: Int): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()
        
        if (currentStreak >= STREAK_1.requirement && unlock(STREAK_1)) {
            newlyUnlocked.add(STREAK_1)
        }
        if (currentStreak >= STREAK_7.requirement && unlock(STREAK_7)) {
            newlyUnlocked.add(STREAK_7)
        }
        if (currentStreak >= STREAK_30.requirement && unlock(STREAK_30)) {
            newlyUnlocked.add(STREAK_30)
        }
        
        return newlyUnlocked
    }
    
    /**
     * Check word count achievements.
     * Returns list of newly unlocked achievements.
     */
    fun checkWordAchievements(totalWords: Int): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()
        
        if (totalWords >= WORDS_50.requirement && unlock(WORDS_50)) {
            newlyUnlocked.add(WORDS_50)
        }
        if (totalWords >= WORDS_200.requirement && unlock(WORDS_200)) {
            newlyUnlocked.add(WORDS_200)
        }
        if (totalWords >= WORDS_500.requirement && unlock(WORDS_500)) {
            newlyUnlocked.add(WORDS_500)
        }
        if (totalWords >= WORDS_1000.requirement && unlock(WORDS_1000)) {
            newlyUnlocked.add(WORDS_1000)
        }
        
        return newlyUnlocked
    }
    
    /**
     * Check accuracy achievement.
     * Returns newly unlocked achievement or null.
     */
    fun checkPerfectSession(correct: Int, total: Int): Achievement? {
        if (total >= 10 && correct == total) {
            if (unlock(PERFECT_SESSION)) {
                return PERFECT_SESSION
            }
        }
        return null
    }
    
    /**
     * Check consistent accuracy achievement.
     */
    fun checkConsistentAccuracy(overallAccuracy: Int): Achievement? {
        if (overallAccuracy >= 90) {
            if (unlock(CONSISTENT)) {
                return CONSISTENT
            }
        }
        return null
    }
    
    /**
     * Check speed achievements based on time limit used.
     * Returns newly unlocked achievement or null.
     */
    fun checkSpeedAchievement(timeLimitMs: Long, completed: Boolean): Achievement? {
        if (!completed) return null
        
        return when {
            timeLimitMs == 2000L && unlock(SPEED_2S) -> SPEED_2S
            timeLimitMs == 5000L && unlock(SPEED_5S) -> SPEED_5S
            else -> null
        }
    }
}
