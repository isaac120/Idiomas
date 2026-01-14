package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.VocabularyWord

class WordAdapter(
    private var words: List<VocabularyWord>,
    private val onDeleteClick: (VocabularyWord) -> Unit
) : RecyclerView.Adapter<WordAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sourceWord: TextView = view.findViewById(R.id.sourceWord)
        val targetWord: TextView = view.findViewById(R.id.targetWord)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteWordButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val word = words[position]
        
        holder.sourceWord.text = word.sourceWord
        holder.targetWord.text = word.targetWord
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(word)
        }
    }

    override fun getItemCount() = words.size

    fun updateWords(newWords: List<VocabularyWord>) {
        words = newWords
        notifyDataSetChanged()
    }
}
