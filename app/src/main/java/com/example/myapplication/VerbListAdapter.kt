package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.VerbList

class VerbListAdapter(
    private var lists: List<VerbList>,
    private val onItemClick: (VerbList) -> Unit,
    private val onDeleteClick: (VerbList) -> Unit
) : RecyclerView.Adapter<VerbListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val listName: TextView = view.findViewById(R.id.verbListName)
        val verbCount: TextView = view.findViewById(R.id.verbCount)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteVerbListButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verb_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val list = lists[position]
        
        holder.listName.text = list.name
        holder.verbCount.text = "${list.verbCount} verbos"
        
        holder.itemView.setOnClickListener {
            onItemClick(list)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(list)
        }
    }

    override fun getItemCount() = lists.size

    fun updateLists(newLists: List<VerbList>) {
        lists = newLists
        notifyDataSetChanged()
    }
}
