package com.veggievision.lokatani.view

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.veggievision.lokatani.R
import com.veggievision.lokatani.data.VeggieDatabase
import com.veggievision.lokatani.data.VeggieEntity
import com.veggievision.lokatani.databinding.ActivityHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private lateinit var database: VeggieDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = VeggieDatabase.getDatabase(this)
        adapter = HistoryAdapter()

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
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
                Toast.makeText(this, "Tidak ada data yang dipilih", Toast.LENGTH_SHORT).show()
            } else {
                exportToExcel(selectedItems)
            }
        }
    }

    private fun exportToExcel(data: List<VeggieEntity>) {
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

        val fileName = "data_sayuran.xlsx"
        val file = File(getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()

        Toast.makeText(this, "File disimpan di: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
