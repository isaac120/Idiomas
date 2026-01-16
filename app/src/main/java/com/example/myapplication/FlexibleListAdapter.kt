package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.FlexibleList
import org.json.JSONArray

class FlexibleListAdapter(
    private var lists: List<FlexibleList>,
    private val onItemClick: (FlexibleList) -> Unit,
    private val onDeleteClick: (FlexibleList) -> Unit
) : RecyclerView.Adapter<FlexibleListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.listName)
        private val countText: TextView = itemView.findViewById(R.id.itemCount)
        private val columnsText: TextView = itemView.findViewById(R.id.columnsInfo)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(list: FlexibleList) {
            nameText.text = list.name
            countText.text = "${list.itemCount} items"
            
            // Parse column headers from JSON
            try {
                val headers = JSONArray(list.columnHeaders)
                val headerNames = (0 until headers.length()).map { headers.getString(it) }
                columnsText.text = headerNames.joinToString(" • ")
            } catch (e: Exception) {
                columnsText.text = "${list.columnCount} columnas"
            }

            itemView.setOnClickListener { onItemClick(list) }
            deleteButton.setOnClickListener { onDeleteClick(list) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flexible_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lists[position])
    }

    override fun getItemCount() = lists.size

    fun updateLists(newLists: List<FlexibleList>) {
        lists = newLists
        notifyDataSetChanged()
    }
}
