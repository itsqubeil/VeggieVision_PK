package com.veggievision.lokatani.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.veggievision.lokatani.R
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupListeners()
        showLoading(true)
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.buttonExport.setOnClickListener {
            exportToExcel()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val entries = withContext(Dispatchers.IO) {
                    VeggieDatabase.getDatabase(this@HistoryActivity).veggieDao().getAll()
                }
                Log.d("HistoryActivity", "Data loaded: ${entries.size} items")
                adapter.submitList(entries)
            } catch (e: Exception) {
                Log.e("HistoryActivity", "Error loading data", e)
                Toast.makeText(this@HistoryActivity, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun exportToExcel() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val selectedItems = adapter.getSelectedItems()
                if (selectedItems.isEmpty()) {
                    Toast.makeText(this@HistoryActivity, "Tidak ada data yang dipilih", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    val workbook = XSSFWorkbook()
                    val sheet = workbook.createSheet("Sayuran")
                    val header = sheet.createRow(0)
                    header.createCell(0).setCellValue("Nama Sayuran")
                    header.createCell(1).setCellValue("Berat")
                    header.createCell(2).setCellValue("Waktu")

                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    selectedItems.forEachIndexed { index, item ->
                        val row = sheet.createRow(index + 1)
                        row.createCell(0).setCellValue(item.name)
                        row.createCell(1).setCellValue(item.weight)

                        val timestamp = try {
                            formatter.format(Date(item.timestamp.toLong()))
                        } catch (e: Exception) {
                            item.timestamp
                        }
                        row.createCell(2).setCellValue(timestamp)
                    }

                    val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "riwayat_sayuran.xlsx")
                    FileOutputStream(file).use { workbook.write(it) }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@HistoryActivity, "Diekspor ke ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("HistoryActivity", "Error exporting to Excel", e)
                Toast.makeText(this@HistoryActivity, "Gagal mengekspor: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }
}