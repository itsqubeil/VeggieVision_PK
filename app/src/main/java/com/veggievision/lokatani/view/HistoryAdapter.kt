package com.veggievision.lokatani.view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.veggievision.lokatani.data.VeggieEntity
import com.veggievision.lokatani.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<VeggieEntity, HistoryAdapter.VeggieViewHolder>(DIFF_CALLBACK) {

    private val selectedItemIds = mutableSetOf<Int>()
    var onSelectionChanged: ((Int) -> Unit)? = null

    inner class VeggieViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VeggieEntity, isSelected: Boolean) {
            binding.textName.text = item.name
            binding.textWeight.text = "${item.weight} gram"
            binding.textTimestamp.text = item.timestamp
            binding.checkbox.isChecked = isSelected

            binding.root.setOnClickListener {
                toggleSelection(item.id)
            }

            binding.checkbox.setOnClickListener {
                toggleSelection(item.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VeggieViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VeggieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VeggieViewHolder, position: Int) {
        val item = getItem(position)
        val isSelected = selectedItemIds.contains(item.id)
        holder.bind(item, isSelected)
    }

    fun toggleSelection(id: Int) {
        if (selectedItemIds.contains(id)) {
            selectedItemIds.remove(id)
        } else {
            selectedItemIds.add(id)
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedItemIds.size)
    }

    fun clearSelection() {
        selectedItemIds.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedItemIds.size)
    }

    fun selectAll() {
        currentList.forEach { selectedItemIds.add(it.id) }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedItemIds.size)
    }

    fun getSelectedItems(): List<VeggieEntity> {
        return currentList.filter { selectedItemIds.contains(it.id) }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VeggieEntity>() {
            override fun areItemsTheSame(oldItem: VeggieEntity, newItem: VeggieEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: VeggieEntity, newItem: VeggieEntity) =
                oldItem == newItem
        }
    }
}
