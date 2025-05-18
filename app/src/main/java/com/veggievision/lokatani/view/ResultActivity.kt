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

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var detectedClasses = arrayListOf<String>()
    private var confidenceValues = floatArrayOf()
    private var recognizedText: String? = null
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val capturedImageUri = intent.getStringExtra("captured_image_uri")
        detectedClasses = intent.getStringArrayListExtra("detected_classes") ?: arrayListOf()
        confidenceValues = intent.getFloatArrayExtra("confidence_values") ?: floatArrayOf()
        recognizedText = intent.getStringExtra("recognized_text")

        capturedImageUri?.let {
            val uri = Uri.parse(it)
            imageUri = uri
            val bitmap = getBitmapFromUri(uri)
            binding.imageView.setImageBitmap(bitmap)
        }
        if (detectedClasses.isNotEmpty()) {
            val className = detectedClasses[0]
            if (className != "berat") {
                binding.editTextClassName.setText(className)
            }
        }

        binding.editTextRecognizedText.setText(recognizedText ?: "")

        if (detectedClasses.isNotEmpty() && confidenceValues.isNotEmpty()) {
            val confidencePercent = String.format("%.2f", confidenceValues[0] * 100)
            binding.textViewConfidence.text = "Confidence: $confidencePercent%"
            binding.textViewConfidence.visibility = View.VISIBLE
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
        val veggieName = binding.editTextClassName.text.toString().trim()
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

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val file = File(uri.path!!)
        return BitmapFactory.decodeStream(FileInputStream(file))
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
