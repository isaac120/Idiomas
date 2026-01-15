package com.example.myapplication

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView

data class WordRow(
    var sourceWord: String = "",
    var targetWord: String = ""
)

class WordRowAdapter(
    private val rows: MutableList<WordRow>,
    private val onRowChanged: () -> Unit,
    private val onDeleteRow: (Int) -> Unit
) : RecyclerView.Adapter<WordRowAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sourceInput: EditText = view.findViewById(R.id.sourceWordInput)
        val targetInput: EditText = view.findViewById(R.id.targetWordInput)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteRowButton)

        private var sourceWatcher: TextWatcher? = null
        private var targetWatcher: TextWatcher? = null

        fun bind(row: WordRow, position: Int) {
            // Remove previous watchers
            sourceWatcher?.let { sourceInput.removeTextChangedListener(it) }
            targetWatcher?.let { targetInput.removeTextChangedListener(it) }

            // Set text
            sourceInput.setText(row.sourceWord)
            targetInput.setText(row.targetWord)

            // Add new watchers
            sourceWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        rows[adapterPosition].sourceWord = s.toString()
                        onRowChanged()
                    }
                }
            }

            targetWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        rows[adapterPosition].targetWord = s.toString()
                        onRowChanged()
                    }
                }
            }

            sourceInput.addTextChangedListener(sourceWatcher)
            targetInput.addTextChangedListener(targetWatcher)

            deleteButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteRow(adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rows[position], position)
    }

    override fun getItemCount() = rows.size

    fun addRow() {
        rows.add(WordRow())
        notifyItemInserted(rows.size - 1)
        onRowChanged()
    }

    fun removeRow(position: Int) {
        if (position in rows.indices) {
            rows.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, rows.size - position)
            onRowChanged()
        }
    }

    fun getValidWords(): List<Pair<String, String>> {
        return rows
            .filter { it.sourceWord.isNotBlank() && it.targetWord.isNotBlank() }
            .map { it.sourceWord.trim() to it.targetWord.trim() }
    }
}
