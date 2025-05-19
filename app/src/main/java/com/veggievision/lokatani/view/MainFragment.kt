package com.veggievision.lokatani.view

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.veggievision.lokatani.R
//import com.veggievision.lokatani.databinding.ActivityMainBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.veggievision.lokatani.databinding.FragmentMainBinding
import org.json.JSONObject
import java.io.InputStream
import java.util.Calendar


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var barChart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        barChart = binding.barChart

//        binding.button.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, CameraFragment())
//                .addToBackStack(null)
//                .commit()
//        }
//
//        binding.buttonNlp.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, NLPFragment())
//                .addToBackStack(null)
//                .commit()
//        }
//
//        binding.buttonRiwayat.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, HistoryFragment())
//                .addToBackStack(null)
//                .commit()
//        }

        val predictionData = loadPredictionData()

        if (predictionData.isNotEmpty()) {
            showChart(predictionData)
        } else {
            Toast.makeText(requireContext(), "Data minggu ini tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPredictionData(): List<Pair<String, Float>> {
        val resultList = mutableListOf<Pair<String, Float>>()
        try {
            val inputStream = requireContext().assets.open("vegetable_predictions_lookup.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            val listId = getCurrentWeekId()

            if (jsonObject.has(listId)) {
                val sayurArray = jsonObject.getJSONArray(listId)
                for (i in 0 until sayurArray.length()) {
                    val item = sayurArray.getJSONObject(i)
                    val name = item.getString("sayur_name")
                    val prediction = item.getDouble("prediction").toFloat()
                    resultList.add(name to prediction)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
        }
        return resultList
    }

    private fun getCurrentWeekId(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.WEEK_OF_MONTH)) {
            1 -> "1"
            2 -> "2"
            3 -> "3"
            else -> "4"
        }
    }

    private fun showChart(data: List<Pair<String, Float>>) {
        val entries = data.mapIndexed { index, pair -> BarEntry(index.toFloat(), pair.second) }
        val dataSet = BarDataSet(entries, "Prediksi Sayuran")
        dataSet.colors = getColors(data.size)
        val barData = BarData(dataSet)
        barData.barWidth = 0.9f

        barChart.data = barData
        barChart.setFitBars(true)

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = -45f

        barChart.axisRight.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f

        val description = Description()
        description.text = "Data Minggu Ini"
        description.textSize = 12f
        barChart.description = description

        barChart.animateY(1500)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
