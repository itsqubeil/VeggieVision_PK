package com.veggievision.lokatani.view

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.veggievision.lokatani.databinding.ActivityResultBinding
import java.io.File
import java.io.FileInputStream

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var detectedClasses = arrayListOf<String>()
    private var confidenceValues = floatArrayOf()
    private var recognizedText: String? = null

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
            val bitmap = getBitmapFromUri(uri)
            binding.imageView.setImageBitmap(bitmap)
        }

        if (detectedClasses.isNotEmpty()) {
            val className = detectedClasses[0]
            if (className != "berat") {
                binding.editTextClassName.setText(className)
            } else {
                binding.editTextClassName.setText("")
            }
        } else {
            binding.editTextClassName.setText("")
        }

        if (detectedClasses.isNotEmpty() && confidenceValues.isNotEmpty()) {
            val confidencePercent = String.format("%.2f", confidenceValues[0] * 100)
            binding.textViewConfidence.text = "Akurasi: $confidencePercent%"
            binding.textViewConfidence.visibility = View.VISIBLE
        } else {
            binding.textViewConfidence.visibility = View.GONE
        }

        binding.editTextRecognizedText.setText(recognizedText)

    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val file = File(uri.path!!)
        val bitmap = BitmapFactory.decodeStream(FileInputStream(file))
        return bitmap
    }
}