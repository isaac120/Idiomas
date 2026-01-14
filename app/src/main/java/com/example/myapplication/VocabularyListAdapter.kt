package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.VocabularyList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class VocabularyListAdapter(
    private var lists: List<VocabularyList>,
    private val onItemClick: (VocabularyList) -> Unit,
    private val onDeleteClick: (VocabularyList) -> Unit
) : RecyclerView.Adapter<VocabularyListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val listName: TextView = view.findViewById(R.id.listName)
        val listInfo: TextView = view.findViewById(R.id.listInfo)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vocabulary_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val list = lists[position]
        
        holder.listName.text = list.name
        holder.listInfo.text = "${list.wordCount} palabras • ${getTimeAgo(list.createdAt)}"
        
        holder.itemView.setOnClickListener {
            onItemClick(list)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(list)
        }
    }

    override fun getItemCount() = lists.size

    fun updateLists(newLists: List<VocabularyList>) {
        lists = newLists
        notifyDataSetChanged()
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Ahora"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
                "Hace $mins min"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "Hace $hours h"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "Hace $days días"
            }
            else -> {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
