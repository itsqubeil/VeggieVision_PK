package com.veggievision.lokatani.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.veggievision.lokatani.databinding.ActivityResultBinding
import java.io.File
import java.io.FileInputStream

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var detectedClasses = arrayListOf<String>()
    private var confidenceValues = floatArrayOf()
    private var recognizedText: String? = null
    private var rawJsonResult: String? = null
    private var imageUri: Uri? = null

    companion object {
        private const val TAG = "ResultActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val capturedImageUri = intent.getStringExtra("captured_image_uri")
        detectedClasses = intent.getStringArrayListExtra("detected_classes") ?: arrayListOf()
        confidenceValues = intent.getFloatArrayExtra("confidence_values") ?: floatArrayOf()
        recognizedText = intent.getStringExtra("recognized_text")
        rawJsonResult = intent.getStringExtra("raw_json_result")

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

        // Display JSON result
//        binding.jsonResultText.text = formatJsonForDisplay(rawJsonResult)
//
//        // Set up copy button
//        binding.btnCopyJson.setOnClickListener {
//            rawJsonResult?.let { json ->
//                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                val clip = ClipData.newPlainText("JSON Result", json)
//                clipboard.setPrimaryClip(clip)
//                Toast.makeText(this, "JSON copied to clipboard", Toast.LENGTH_SHORT).show()
//            }
//        }
        binding.jsonResultText.visibility = View.GONE
        binding.btnCopyJson.visibility = View.GONE
        binding.buttonSave.setOnClickListener {
            // Add your save functionality here
            Toast.makeText(this, "Data saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val file = File(uri.path!!)
        val bitmap = BitmapFactory.decodeStream(FileInputStream(file))
        return bitmap
    }

    private fun formatJsonForDisplay(jsonStr: String?): String {
        if (jsonStr.isNullOrEmpty()) return "No detection results available"

        return try {
            // Parse JSON string
            val jsonElement = JsonParser.parseString(jsonStr)

            // Pretty print using Gson
            val gson = GsonBuilder().setPrettyPrinting().create()
            gson.toJson(jsonElement)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting JSON: ${e.message}")
            // If formatting fails, return as is
            jsonStr
        }
    }
}