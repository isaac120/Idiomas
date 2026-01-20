package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.FlexibleList
import org.json.JSONArray

/**
 * Data class to hold list info with stats for display.
 */
data class ListDisplayInfo(
    val list: FlexibleList,
    val itemCount: Int,
    val accuracy: Int? = null
)

/**
 * Adapter for displaying lists in the practice selection dialog.
 */
class ListCardAdapter(
    private val lists: List<ListDisplayInfo>,
    private val onListClick: (FlexibleList) -> Unit
) : RecyclerView.Adapter<ListCardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val listIcon: TextView = view.findViewById(R.id.listIcon)
        val listName: TextView = view.findViewById(R.id.listName)
        val listColumns: TextView = view.findViewById(R.id.listColumns)
        val listStats: TextView = view.findViewById(R.id.listStats)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = lists[position]
        val list = info.list
        
        // Icon based on list type (using emoji variation)
        val icons = listOf("📚", "📖", "📝", "📋", "🔤", "💬", "📕", "📗")
        holder.listIcon.text = icons[position % icons.size]
        
        // List name
        holder.listName.text = list.name
        
        // Column headers
        val columns = try {
            val headers = JSONArray(list.columnHeaders)
            (0 until headers.length()).map { headers.getString(it) }.joinToString(" • ")
        } catch (e: Exception) {
            "Columnas: ${list.columnCount}"
        }
        holder.listColumns.text = columns
        
        // Stats
        val statsText = buildString {
            append("${info.itemCount} items")
            info.accuracy?.let { acc ->
                if (acc > 0) append(" · $acc% precisión")
            }
        }
        holder.listStats.text = statsText
        
        // Click listener
        holder.itemView.setOnClickListener {
            onListClick(list)
        }
    }

    override fun getItemCount() = lists.size
}
