package com.example.myapplication.util

import android.content.Context
import android.net.Uri
import com.example.myapplication.model.WordPair
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.io.FileOutputStream

/**
 * Utilidad para parsear archivos CSV con pares de palabras.
 * Formato esperado: palabra_origen,palabra_destino (una línea por par)
 */
object FileParser {

    /**
     * Parsea un archivo CSV desde un URI.
     * @param context Contexto de la aplicación
     * @param uri URI del archivo a parsear
     * @return Lista de WordPair parseados
     */
    fun parseCSV(context: Context, uri: Uri): List<WordPair> {
        val wordPairs = mutableListOf<WordPair>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    var isFirstLine = true
                    
                    while (reader.readLine().also { line = it } != null) {
                        // Saltar línea de encabezado si existe
                        if (isFirstLine) {
                            isFirstLine = false
                            val firstLine = line?.lowercase() ?: ""
                            if (firstLine.contains("español") || 
                                firstLine.contains("ingles") ||
                                firstLine.contains("english") ||
                                firstLine.contains("spanish")) {
                                continue
                            }
                        }
                        
                        line?.let { currentLine ->
                            val parts = currentLine.split(",", ";", "\t")
                            if (parts.size >= 2) {
                                val sourceWord = parts[0].trim()
                                val targetWord = parts[1].trim()
                                
                                if (sourceWord.isNotEmpty() && targetWord.isNotEmpty()) {
                                    wordPairs.add(WordPair(sourceWord, targetWord))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return wordPairs
    }

    /**
     * Mezcla las palabras aleatoriamente y asigna direcciones aleatorias.
     * @param wordPairs Lista de pares de palabras
     * @return Lista mezclada con direcciones aleatorias
     */
    fun shuffleAndRandomizeDirection(wordPairs: List<WordPair>): MutableList<WordPair> {
        return wordPairs.map { pair ->
            pair.copy(showSourceFirst = listOf(true, false).random())
        }.shuffled().toMutableList()
    }

    /**
     * Genera un archivo CSV a partir de una lista de pares de palabras.
     * @param context Contexto de la aplicación
     * @param fileName Nombre del archivo a crear (sin extensión)
     * @param words Lista de pares de palabras
     * @return Archivo generado
     */
    fun generateCSV(context: Context, fileName: String, words: List<WordPair>): File {
        // Crear directorio de caché si no existe
        val cachePath = File(context.cacheDir, "lists")
        cachePath.mkdirs()
        
        // Crear archivo
        val file = File(cachePath, "$fileName.csv")
        
        FileOutputStream(file).use { stream ->
            // Escribir BOM para Excel (UTF-8)
            stream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            
            // Escribir encabezado
            stream.write("Español,Inglés\n".toByteArray())
            
            // Escribir palabras
            words.forEach { word ->
                val line = "${word.sourceWord},${word.targetWord}\n"
                stream.write(line.toByteArray())
            }
        }
        
        return file
    }
}
