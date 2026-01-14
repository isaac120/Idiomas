package com.example.myapplication.model

/**
 * Representa un par de palabras en dos idiomas.
 * @param sourceWord La palabra en el idioma de origen (ej: español)
 * @param targetWord La palabra en el idioma destino (ej: inglés)
 * @param showSourceFirst Si true, muestra sourceWord y pide targetWord; si false, al revés
 */
data class WordPair(
    val sourceWord: String,
    val targetWord: String,
    var showSourceFirst: Boolean = true
) {
    /**
     * Obtiene la palabra que se muestra al usuario
     */
    fun getDisplayWord(): String = if (showSourceFirst) sourceWord else targetWord

    /**
     * Obtiene la palabra que el usuario debe escribir
     */
    fun getExpectedAnswer(): String = if (showSourceFirst) targetWord else sourceWord

    /**
     * Verifica si la respuesta es correcta (ignorando mayúsculas/minúsculas y acentos)
     */
    fun checkAnswer(userAnswer: String): Boolean {
        val normalizedExpected = normalizeString(getExpectedAnswer())
        val normalizedAnswer = normalizeString(userAnswer)
        return normalizedExpected == normalizedAnswer
    }

    private fun normalizeString(text: String): String {
        return text.trim()
            .lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ü", "u")
            .replace("ñ", "n")
    }
}
