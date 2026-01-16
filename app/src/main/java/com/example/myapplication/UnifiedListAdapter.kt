package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.model.UnifiedList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UnifiedListAdapter(
    private var lists: List<UnifiedList>,
    private val onItemClick: (UnifiedList) -> Unit,
    private val onDeleteClick: (UnifiedList) -> Unit
) : RecyclerView.Adapter<UnifiedListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val listIcon: TextView = view.findViewById(R.id.listIcon)
        val listName: TextView = view.findViewById(R.id.listName)
        val listInfo: TextView = view.findViewById(R.id.listInfo)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_unified_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lists[position]
        
        holder.listName.text = item.name
        
        when (item) {
            is UnifiedList.Vocabulary -> {
                holder.listIcon.text = "📖"
                holder.listInfo.text = "${item.itemCount} palabras • ${getTimeAgo(item.createdAt)}"
            }
            is UnifiedList.Verbs -> {
                holder.listIcon.text = "🔤"
                holder.listInfo.text = "${item.itemCount} verbos • ${getTimeAgo(item.createdAt)}"
            }
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = lists.size

    fun updateLists(newLists: List<UnifiedList>) {
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
