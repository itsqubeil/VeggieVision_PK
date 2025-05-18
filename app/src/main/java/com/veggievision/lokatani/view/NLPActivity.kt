package com.veggievision.lokatani.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.veggievision.lokatani.NLP.NLPProcessor
import com.veggievision.lokatani.NLP.SayurDataManager
import com.veggievision.lokatani.NLP.VegetableData
import com.veggievision.lokatani.databinding.ActivityNlpBinding
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type

class NLPActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNlpBinding
    private val REQUEST_CODE_PICK_EXCEL = 101
    private val PERMISSION_REQUEST_CODE = 102
    private val dataManager = SayurDataManager()
    private val nlpProcessor = NLPProcessor(dataManager)
    private val queryHistory = mutableListOf<Pair<String, String>>()
    private lateinit var historyAdapter: NLPHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNlpBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        historyAdapter = NLPHistoryAdapter(queryHistory)
        binding.rvHistory.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = false
            reverseLayout = true
        }
        binding.rvHistory.adapter = historyAdapter

        binding.btnImportExcel.setOnClickListener {
            checkPermissionsAndOpenFilePicker()
        }

        binding.btnAsk.setOnClickListener {
            val query = binding.etQuery.text.toString().trim()
            if (query.isNotEmpty()) {
                val response = nlpProcessor.processQuery(query)
                binding.tvResponse.text = response

                queryHistory.add(0, Pair(query, response))
                historyAdapter.notifyItemInserted(0)

                binding.rvHistory.scrollToPosition(0)

                binding.etQuery.text.clear()
            }
        }

        // Fix layout when keyboard visible
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.setPadding(0, 0, 0, maxOf(imeHeight, navBarHeight))
            val imeVisible = insets.isVisible(Type.ime())

            binding.tvQueryLabel.visibility = if (imeVisible) View.GONE else View.VISIBLE
            binding.tvResponseLabel.visibility = if (imeVisible) View.GONE else View.VISIBLE
            binding.btnImportExcel.visibility = if (imeVisible) View.GONE else View.VISIBLE

            insets
        }
    }

    private fun checkPermissionsAndOpenFilePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openFilePicker()
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            setType("*/*")
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_EXCEL)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker()
            } else {
                Toast.makeText(this, "Izin akses penyimpanan diperlukan untuk mengimpor file Excel", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_EXCEL && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                importExcelFile(uri)
            }
        }
    }

    private fun importExcelFile(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val sheet = workbook.getSheetAt(0)
                dataManager.clearData()

                var skipFirstRow = true
                for (row in sheet) {
                    if (skipFirstRow) {
                        skipFirstRow = false
                        continue
                    }

                    val id = row.getCell(0)?.takeIf { it.cellType == CellType.NUMERIC }?.numericCellValue?.toInt()
                    val vegetableType = row.getCell(1)?.let { cell ->
                        when (cell.cellType) {
                            CellType.STRING -> cell.stringCellValue
                            CellType.NUMERIC -> cell.numericCellValue.toString()
                            else -> null
                        }
                    }
                    val weight = row.getCell(2)?.takeIf { it.cellType == CellType.NUMERIC }?.numericCellValue
                    val timestamp = row.getCell(3)?.let { cell ->
                        when (cell.cellType) {
                            CellType.NUMERIC -> cell.dateCellValue
                            CellType.STRING -> {
                                try {
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        .parse(cell.stringCellValue)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            else -> null
                        }
                    }

                    if (id != null && vegetableType != null && weight != null && timestamp != null) {
                        dataManager.addVegetableData(
                            VegetableData(id, vegetableType, weight, timestamp)
                        )
                    }
                }

                binding.tvResponse.text = "Excel data imported successfully. ${dataManager.getVegetableData().size} entries loaded."
                workbook.close()
            }
        } catch (e: Exception) {
            binding.tvResponse.text = "Error importing Excel file: ${e.message}"
            e.printStackTrace()
        }
    }
}