package com.veggievision.lokatani.view

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.veggievision.lokatani.data.VeggieDatabase
import com.veggievision.lokatani.data.VeggieEntity
import com.veggievision.lokatani.databinding.FragmentHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HistoryAdapter
    private lateinit var database: VeggieDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = VeggieDatabase.getDatabase(requireContext())
        adapter = HistoryAdapter()

//        adapter.onSelectionChanged = { selectedCount ->
//            if (selectedCount > 0) {
//                binding.buttonSelectAll.visibility = View.GONE
//                binding.buttonClearSelection.visibility = View.VISIBLE
//            } else {
//                binding.buttonSelectAll.visibility = View.VISIBLE
//                binding.buttonClearSelection.visibility = View.GONE
//            }
//        }

        adapter.onSelectionChanged = { selectedCount ->
            val hasSelection = selectedCount > 0
            binding.buttonSelectAll.fadeToVisibility(!hasSelection)
            binding.buttonClearSelection.fadeToVisibility(hasSelection)
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        loadHistory()
        setupListeners()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val history = withContext(Dispatchers.IO) {
                database.veggieDao().getAll()
            }
            adapter.submitList(history)
        }
    }

    private fun setupListeners() {
        binding.buttonSelectAll.setOnClickListener {
            adapter.selectAll()
        }

        binding.buttonClearSelection.setOnClickListener {
            adapter.clearSelection()
        }

        binding.buttonExport.setOnClickListener {
            val selectedItems = adapter.getSelectedItems()
            if (selectedItems.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada data yang dipilih", Toast.LENGTH_SHORT).show()
            } else {
                exportToExcel(selectedItems)
            }
        }

        binding.buttonDeleteSelected.setOnClickListener {
            val selectedItems = adapter.getSelectedItems()

            if (selectedItems.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada data yang dipilih", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Confirm before deleting (optional)
            deleteSelectedItems(selectedItems)
        }

    }

    private fun exportToExcel(data: List<VeggieEntity>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Data Sayuran")

                val header = sheet.createRow(0)
                header.createCell(0).setCellValue("ID")
                header.createCell(1).setCellValue("Jenis Sayur")
                header.createCell(2).setCellValue("Berat")
                header.createCell(3).setCellValue("Timestamp")

                data.forEachIndexed { index, item ->
                    val row = sheet.createRow(index + 1)
                    row.createCell(0).setCellValue(index + 1.toDouble())
                    row.createCell(1).setCellValue(item.name)
                    row.createCell(2).setCellValue(item.weight.toDouble())
                    row.createCell(3).setCellValue(item.timestamp)
                }

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "VeggieVision_Exported_Data_$timeStamp.xlsx"

                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val veggieVisionDir = File(downloadDir, "veggievision")

                if (!veggieVisionDir.exists()) {
                    veggieVisionDir.mkdirs()
                }

                val file = File(veggieVisionDir, fileName)
                FileOutputStream(file).use {
                    workbook.write(it)
                }
                workbook.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "File disimpan di: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Gagal menyimpan file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun deleteSelectedItems(items: List<VeggieEntity>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                database.veggieDao().deleteItems(items)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                    adapter.clearSelection()
                    loadHistory()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun View.fadeToVisibility(visible: Boolean, duration: Long = 200) {
    animate()
        .alpha(if (visible) 1f else 0f)
        .setDuration(duration)
        .withStartAction {
            if (visible) this.visibility = View.VISIBLE
        }
        .withEndAction {
            if (!visible) this.visibility = View.GONE
        }
}
