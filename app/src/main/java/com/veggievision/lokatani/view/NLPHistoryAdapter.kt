package com.veggievision.lokatani.view
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.veggievision.lokatani.R

class NLPHistoryAdapter (private val history: List<Pair<String, String>>) : RecyclerView.Adapter<NLPHistoryAdapter.ViewHolder>(){

        class ViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view) {
            val tvQuery: TextView = view.findViewById(R.id.tvHistoryQuery)
            val tvResponse: TextView = view.findViewById(R.id.tvHistoryResponse)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_nlp_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (query, response) = history[position]
            holder.tvQuery.text = query
            holder.tvResponse.text = response
        }

        override fun getItemCount() = history.size
}