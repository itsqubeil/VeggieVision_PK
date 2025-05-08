package com.veggievision.lokatani.view

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraXThreads.TAG
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.veggievision.lokatani.R
import com.veggievision.lokatani.databinding.ActivityMainBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.json.JSONObject
import java.io.InputStream
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var barChart: BarChart
    private lateinit var tvTitle: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("main", "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
//        clearTemporaryCapturedFiles()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        barChart = binding.barChart
        tvTitle = binding.tvTitle
        binding.button.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent) }

        binding.buttonNlp.setOnClickListener {
            val intent = Intent(this, NLPActivity::class.java)
            startActivity(intent) }

        val predictionData = loadPredictionData()

        if (predictionData.isNotEmpty()) {
            showChart(predictionData)
        } else {
            Toast.makeText(this, "Data minggu ini tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }
    private fun clearTemporaryCapturedFiles() {
        val cacheDir = cacheDir
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("captured_image_uri")) {
                file.delete()
            }
        }
    }

    private fun loadPredictionData(): List<Pair<String, Float>> {
        val resultList = mutableListOf<Pair<String, Float>>()

        try {
            val inputStream: InputStream = assets.open("vegetable_predictions_lookup.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            val listId = getCurrentWeekId()

            if (jsonObject.has(listId)) {
                val sayurArray = jsonObject.getJSONArray(listId)

                for (i in 0 until sayurArray.length()) {
                    val item = sayurArray.getJSONObject(i)
                    val name = item.getString("sayur_name")
                    val prediction = item.getDouble("prediction").toFloat()

                    resultList.add(Pair(name, prediction))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
        }

        return resultList
    }

    private fun getCurrentWeekId(): String {
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_MONTH)

        return when (currentWeek) {
            1 -> "1"
            2 -> "2"
            3 -> "3"
            else -> "4"
        }
    }

    private fun showChart(data: List<Pair<String, Float>>) {
        val entries = data.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second)
        }

        val dataSet = BarDataSet(entries, "Prediksi Sayuran")
        dataSet.colors = getColors(data.size)

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f

        barChart.data = barData
        barChart.setFitBars(true)

        // Customize X-Axis
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = -45f

        // Customize Y-Axis
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f // start y-axis from zero

        // Set description
        val description = Description()
        description.text = "Data Minggu Ini"
        description.textSize = 12f
        barChart.description = description

        // Animate chart
        barChart.animateY(1500)

        // Refresh
        barChart.invalidate()
    }

    private fun getColors(size: Int): List<Int> {
        val colors = listOf(
            Color.parseColor("#FF5722"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#8BC34A"),
            Color.parseColor("#3F51B5"),
            Color.parseColor("#FF9800")
        )
        return List(size) { colors[it % colors.size] }
    }
}
