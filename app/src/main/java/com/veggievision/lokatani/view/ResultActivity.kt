package com.veggievision.lokatani.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.veggievision.lokatani.data.VeggieDatabase
import com.veggievision.lokatani.data.VeggieEntity
import com.veggievision.lokatani.databinding.ActivityResultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ArrayAdapter

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var detectedClasses = arrayListOf<String>()
    private var confidenceValues = floatArrayOf()
    private var recognizedText: String? = null
    private var imageUri: Uri? = null
    private var imageFileToDelete: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vegetableOptions = arrayOf("bayam", "kangkung", "pakcoy")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, vegetableOptions)
        binding.autoCompleteTextViewClassName.setAdapter(adapter)

        val capturedImageUri = intent.getStringExtra("captured_image_uri")
        detectedClasses = intent.getStringArrayListExtra("detected_classes") ?: arrayListOf()
        confidenceValues = intent.getFloatArrayExtra("confidence_values") ?: floatArrayOf()
        recognizedText = intent.getStringExtra("recognized_text")

        capturedImageUri?.let {
            val uri = Uri.parse(it)
            imageUri = uri
            uri.path?.let { path ->
                imageFileToDelete = File(path)
            }
            val bitmap = getBitmapFromUri(uri)
            binding.imageView.setImageBitmap(bitmap)
        }
        if (detectedClasses.isNotEmpty()) {
            val className = detectedClasses[0]
            if (className != "berat") {
                binding.autoCompleteTextViewClassName.setText(className)
            }
        }
        val weighter = recognizedText?.replace(Regex("[^0-9]"), "")
//        binding.editTextRecognizedText.setText(weighter ?: "")
        val weighterCleaned = if (!weighter.isNullOrEmpty()) {
            try {
                weighter.toLong().toString()
            } catch (e: NumberFormatException) {
                weighter
            }
        } else {
            ""
        }
        binding.editTextRecognizedText.setText(weighterCleaned)

//        binding.editTextRecognizedText.setText(recognizedText ?: "")

        if (detectedClasses.isNotEmpty() && confidenceValues.isNotEmpty()) {
            val confidencePercent = String.format("%.2f", confidenceValues[0] * 100)
            binding.textViewConfidence.text = "Confidence: $confidencePercent%"
//            binding.textViewConfidence.visibility = View.VISIBLE
        } else {
            binding.textViewConfidence.visibility = View.GONE
        }

        binding.buttonSave.setOnClickListener {
            saveToDatabase()
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveToDatabase() {
        val veggieName = binding.autoCompleteTextViewClassName.text.toString().trim()
        val veggieWeight = binding.editTextRecognizedText.text.toString().trim()
        val timestamp = getCurrentDateTime()

        if (veggieName.isEmpty() || veggieWeight.isEmpty()) {
            Toast.makeText(this, "Nama dan berat harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val entry = VeggieEntity(
            name = veggieName,
            weight = veggieWeight,
            timestamp = timestamp,
        )

        lifecycleScope.launch(Dispatchers.IO) {
            VeggieDatabase.getDatabase(applicationContext).veggieDao().insert(entry)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResultActivity, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        // Jika imageFileToDelete sudah diinisialisasi, gunakan itu
        val fileToLoad = imageFileToDelete ?: File(uri.path!!)
        return BitmapFactory.decodeStream(FileInputStream(fileToLoad))
    }


    override fun onDestroy() {
        super.onDestroy()
        // Hapus file dari cache saat activity dihancurkan
        imageFileToDelete?.let { file ->
            if (file.exists()) {
                if (file.delete()) {
                    android.util.Log.d("ResultActivity", "Cache file deleted: ${file.absolutePath}")
                } else {
                    android.util.Log.e("ResultActivity", "Failed to delete cache file: ${file.absolutePath}")
                }
            }
        }
    }
}
