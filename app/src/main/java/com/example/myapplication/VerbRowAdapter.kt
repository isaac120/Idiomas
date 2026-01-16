package com.example.myapplication

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView

data class VerbRow(
    var baseForm: String = "",
    var pastForm: String = "",
    var participleForm: String = ""
)

class VerbRowAdapter(
    private val rows: MutableList<VerbRow>,
    private val onRowChanged: () -> Unit,
    private val onDeleteRow: (Int) -> Unit
) : RecyclerView.Adapter<VerbRowAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val baseInput: EditText = view.findViewById(R.id.baseFormInput)
        val pastInput: EditText = view.findViewById(R.id.pastFormInput)
        val participleInput: EditText = view.findViewById(R.id.participleFormInput)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteRowButton)

        private var baseWatcher: TextWatcher? = null
        private var pastWatcher: TextWatcher? = null
        private var participleWatcher: TextWatcher? = null

        fun bind(row: VerbRow, position: Int) {
            // Remove previous watchers
            baseWatcher?.let { baseInput.removeTextChangedListener(it) }
            pastWatcher?.let { pastInput.removeTextChangedListener(it) }
            participleWatcher?.let { participleInput.removeTextChangedListener(it) }

            // Set text
            baseInput.setText(row.baseForm)
            pastInput.setText(row.pastForm)
            participleInput.setText(row.participleForm)

            // Add new watchers
            baseWatcher = createWatcher { rows[bindingAdapterPosition].baseForm = it }
            pastWatcher = createWatcher { rows[bindingAdapterPosition].pastForm = it }
            participleWatcher = createWatcher { rows[bindingAdapterPosition].participleForm = it }

            baseInput.addTextChangedListener(baseWatcher)
            pastInput.addTextChangedListener(pastWatcher)
            participleInput.addTextChangedListener(participleWatcher)

            deleteButton.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteRow(bindingAdapterPosition)
                }
            }
        }

        private fun createWatcher(onChanged: (String) -> Unit): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        onChanged(s.toString())
                        onRowChanged()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verb_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rows[position], position)
    }

    override fun getItemCount() = rows.size

    fun addRow() {
        rows.add(VerbRow())
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

    fun getValidVerbs(): List<Triple<String, String, String>> {
        return rows
            .filter { it.baseForm.isNotBlank() && it.pastForm.isNotBlank() && it.participleForm.isNotBlank() }
            .map { Triple(it.baseForm.trim(), it.pastForm.trim(), it.participleForm.trim()) }
    }
}
