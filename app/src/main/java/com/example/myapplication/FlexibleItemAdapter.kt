package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.ListItem
import org.json.JSONArray

class FlexibleItemAdapter(
    private var items: List<ListItem>,
    private var columnCount: Int,
    private val onDeleteClick: (ListItem) -> Unit
) : RecyclerView.Adapter<FlexibleItemAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val valuesContainer: LinearLayout = itemView.findViewById(R.id.valuesContainer)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(item: ListItem) {
            valuesContainer.removeAllViews()

            try {
                val values = JSONArray(item.values)
                for (i in 0 until values.length()) {
                    val textView = TextView(itemView.context).apply {
                        text = values.getString(i)
                        textSize = 14f
                        setTextColor(itemView.context.getColor(R.color.text_primary))
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        ).apply {
                            marginEnd = 8
                        }
                    }
                    valuesContainer.addView(textView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            deleteButton.setOnClickListener { onDeleteClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flexible_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ListItem>, newColumnCount: Int) {
        items = newItems
        columnCount = newColumnCount
        notifyDataSetChanged()
    }
}
