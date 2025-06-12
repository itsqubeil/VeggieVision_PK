package com.veggievision.lokatani.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.veggievision.lokatani.NLP.NLPProcessor
import com.veggievision.lokatani.NLP.QuestionTemplateHelper
import com.veggievision.lokatani.NLP.SayurDataManager
import com.veggievision.lokatani.NLP.VegetableData
import com.veggievision.lokatani.databinding.FragmentNlpBinding
import com.xwray.groupie.GroupieAdapter
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
    private val questionTemplateHelper = QuestionTemplateHelper()

    private val groupieAdapter = GroupieAdapter()

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

    private var isDataLoaded = false //To check import status

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

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = groupieAdapter

        if (!isDataLoaded) {
            val initialMessage = "Impor file excel terlebih dahulu sebelum memulai percakapan \uD83D\uDCCE"
            groupieAdapter.add(BotMessageItem(initialMessage))
        }

        binding.btnImportExcel.setOnClickListener {
            checkPermissionsAndOpenFilePicker()
        }

        binding.btnAsk.setOnClickListener {
            processUserQuery(binding.etQuery.text.toString().trim())
        }

        setupTemplateButtons()
    }

    private fun setupTemplateButtons() {
        refreshTemplateButtons()

        binding.btnTemplate1.setOnClickListener {
            processUserQuery(binding.btnTemplate1.text.toString())
            refreshTemplateButtons()
        }

        binding.btnTemplate2.setOnClickListener {
            processUserQuery(binding.btnTemplate2.text.toString())
            refreshTemplateButtons()
        }

        binding.btnTemplate3.setOnClickListener {
            processUserQuery(binding.btnTemplate3.text.toString())
            refreshTemplateButtons()
        }
    }

    private fun refreshTemplateButtons() {
        val templates = questionTemplateHelper.getRandomTemplates(3)

        if (templates.size >= 1) binding.btnTemplate1.text = templates[0]
        if (templates.size >= 2) binding.btnTemplate2.text = templates[1]
        if (templates.size >= 3) binding.btnTemplate3.text = templates[2]
    }

    private fun processUserQuery(query: String) {
        if (!isDataLoaded) {
            val warningMessage = "Impor file excel terlebih dahulu sebelum memulai percakapan \uD83D\uDCCE"
            groupieAdapter.add(BotMessageItem(warningMessage))
            binding.rvHistory.scrollToPosition(groupieAdapter.itemCount - 1)
            return
        }

        if (query.isNotEmpty()) {
            // Add user message to the adapter
            groupieAdapter.add(UserMessageItem(query))
            // Scroll to the bottom to see the new message
            binding.rvHistory.scrollToPosition(groupieAdapter.itemCount - 1)


            // Process query and get the response
            val response = nlpProcessor.processQuery(query)

            // Add bot response to the adapter
            groupieAdapter.add(BotMessageItem(response))
            // Scroll to the bottom again to see the response
            binding.rvHistory.scrollToPosition(groupieAdapter.itemCount - 1)

            binding.etQuery.text.clear()
        }
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

                isDataLoaded = true

                // Give feedback in the chat UI
                val successMessage = "Impor data Excel berhasil. ${dataManager.getVegetableData().size} entri dimuat."
                groupieAdapter.add(BotMessageItem(successMessage))
                binding.rvHistory.scrollToPosition(groupieAdapter.itemCount - 1)

                workbook.close()
            }
        } catch (e: Exception) {
            val errorMessage = "Error importing Excel file: ${e.message}"
            groupieAdapter.add(BotMessageItem(errorMessage))
            binding.rvHistory.scrollToPosition(groupieAdapter.itemCount - 1)
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