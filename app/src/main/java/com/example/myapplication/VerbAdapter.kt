package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.IrregularVerb

class VerbAdapter(
    private var verbs: List<IrregularVerb>,
    private val onItemClick: (IrregularVerb) -> Unit,
    private val onDeleteClick: (IrregularVerb) -> Unit
) : RecyclerView.Adapter<VerbAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val baseForm: TextView = view.findViewById(R.id.baseForm)
        val simplePast: TextView = view.findViewById(R.id.simplePast)
        val pastParticiple: TextView = view.findViewById(R.id.pastParticiple)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteVerbButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verb, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val verb = verbs[position]
        
        holder.baseForm.text = verb.baseForm
        holder.simplePast.text = verb.simplePast
        holder.pastParticiple.text = verb.pastParticiple
        
        holder.itemView.setOnClickListener {
            onItemClick(verb)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(verb)
        }
    }

    override fun getItemCount() = verbs.size

    fun updateVerbs(newVerbs: List<IrregularVerb>) {
        verbs = newVerbs
        notifyDataSetChanged()
    }
}
