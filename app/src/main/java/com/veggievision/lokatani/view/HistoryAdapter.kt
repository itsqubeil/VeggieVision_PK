package com.veggievision.lokatani.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.veggievision.lokatani.data.VeggieEntity
import com.veggievision.lokatani.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<VeggieEntity, HistoryAdapter.VeggieViewHolder>(VeggieDiffCallback()) {

    private val selectedItems = mutableSetOf<VeggieEntity>()

    class VeggieViewHolder(private val binding: ItemHistoryBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VeggieEntity, isSelected: Boolean) {
            binding.textName.text = item.name
            binding.textWeight.text = "${item.weight} gr"

            try {
                val timestamp = item.timestamp.toLongOrNull()
                val date = if (timestamp != null) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
                } else {
                    item.timestamp
                }
                binding.textTimestamp.text = date
            } catch (e: Exception) {
                binding.textTimestamp.text = item.timestamp
            }
            binding.root.isSelected = isSelected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VeggieViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VeggieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VeggieViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, selectedItems.contains(item))

        holder.itemView.setOnClickListener {
            toggleSelection(item)
            notifyItemChanged(position)
        }
    }

    private fun toggleSelection(item: VeggieEntity) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
    }

    fun getSelectedItems(): List<VeggieEntity> {
        return selectedItems.toList()
    }
}

class VeggieDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<VeggieEntity>() {
    override fun areItemsTheSame(oldItem: VeggieEntity, newItem: VeggieEntity) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: VeggieEntity, newItem: VeggieEntity) = oldItem == newItem
}