package com.veggievision.lokatani.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.veggievision.lokatani.NLP.NLPProcessor
import com.veggievision.lokatani.NLP.SayurDataManager
import com.veggievision.lokatani.NLP.VegetableData
import com.veggievision.lokatani.databinding.FragmentNlpBinding
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class NLPFragment : Fragment() {

    private var _binding: FragmentNlpBinding? = null
    private val binding get() = _binding!!

    private val dataManager = SayurDataManager()
    private val nlpProcessor = NLPProcessor(dataManager)
    private val queryHistory = mutableListOf<Pair<String, String>>()
    private lateinit var historyAdapter: NLPHistoryAdapter

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    requireActivity().contentResolver.takePersistableUriPermission(uri, takeFlags)
                    importExcelFile(uri)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNlpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyAdapter = NLPHistoryAdapter(queryHistory)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext()).apply {
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

//        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
//            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
//
//            Log.d("NLPFragment", "Keyboard visible? $imeVisible")
//
//            binding.tvQueryLabel.visibility = if (imeVisible) View.GONE else View.VISIBLE
//            binding.tvResponseLabel.visibility = if (imeVisible) View.GONE else View.VISIBLE
//            binding.btnImportExcel.visibility = if (imeVisible) View.GONE else View.VISIBLE
//
//            insets
//        }
//        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun checkPermissionsAndOpenFilePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openFilePicker()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
        }
        filePickerLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openFilePicker()
        } else {
            Toast.makeText(requireContext(), "mohon izin akses penyimoanan", Toast.LENGTH_LONG).show()
        }
    }

    private fun importExcelFile(uri: Uri) {
        try {
            val inputStream: InputStream? = requireActivity().contentResolver.openInputStream(uri)
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
                        dataManager.addVegetableData(VegetableData(id, vegetableType, weight, timestamp))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 102
    }
}
