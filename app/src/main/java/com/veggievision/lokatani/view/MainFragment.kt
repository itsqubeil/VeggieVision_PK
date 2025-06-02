package com.veggievision.lokatani.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.veggievision.lokatani.R
import com.veggievision.lokatani.databinding.FragmentMainBinding
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private var allPredictionData: Map<String, List<Pair<String, Float>>> = emptyMap()
    private var allVegetables: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadAllPredictionData()

        if (allPredictionData.isNotEmpty() && allVegetables.isNotEmpty()) {
            setupFilter()
            val defaultVegetable = getHighestPredictionVegetableThisWeek().first
            setDefaultSpinnerSelection(defaultVegetable)
            updateAdditionalInfo()

            if (binding.filterSpinner.selectedItemPosition != AdapterView.INVALID_POSITION) {
                showChartForVegetable(allVegetables[binding.filterSpinner.selectedItemPosition], false)
            } else {
                showChartForVegetable(defaultVegetable, false)
            }
        } else {
            Toast.makeText(requireContext(), "Data prediksi tidak tersedia atau kosong.", Toast.LENGTH_LONG).show()
            binding.tvMostConsumedVegetable.text = "N/A"
            binding.tvCurrentWeekInfo.text = "N/A"
            binding.lineChart.visibility = View.GONE
            binding.filterSpinner.isEnabled = false
        }
    }

    private fun loadAllPredictionData() {
        val resultMap = mutableMapOf<String, List<Pair<String, Float>>>()
        try {
            val inputStream = requireContext().assets.open("vegetable_predictions_lookup.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            val weekKeys = jsonObject.keys().asSequence().toList().sorted()

            for (weekId in weekKeys) {
                if (jsonObject.has(weekId)) {
                    val weekJsonData = jsonObject.optJSONArray(weekId)
                    if (weekJsonData != null) {
                        val weekDataList = mutableListOf<Pair<String, Float>>()
                        for (i in 0 until weekJsonData.length()) {
                            val item = weekJsonData.getJSONObject(i)
                            val name = item.getString("sayur_name")
                            val prediction = item.getDouble("prediction").toFloat()
                            weekDataList.add(name to prediction)
                        }
                        resultMap[weekId] = weekDataList
                    }
                }
            }
            allPredictionData = resultMap

            allVegetables = allPredictionData.values.flatten()
                .map { it.first }
                .distinct()
                .sorted()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal memuat data prediksi: ${e.message}", Toast.LENGTH_LONG).show()
            allPredictionData = emptyMap()
            allVegetables = emptyList()
        }
    }

    private fun setupFilter() {
        if (allVegetables.isEmpty()) {
            binding.filterSpinner.adapter = null
            binding.filterSpinner.isEnabled = false
            return
        }
        binding.filterSpinner.isEnabled = true
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, allVegetables)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.filterSpinner.adapter = adapter

        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedVegetable = allVegetables[position]
                binding.lineChart.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        showChartForVegetable(selectedVegetable, true)
                    }
                    .start()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun getCurrentWeekOfMonth(): Int {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        return (dayOfMonth - 1) / 7 + 1
    }

    private fun getCurrentWeekId(): String {
        val weekOfMonth = getCurrentWeekOfMonth()
        return when {
            weekOfMonth <= 1 -> "1"
            weekOfMonth == 2 -> "2"
            weekOfMonth == 3 -> "3"
            else -> "4"
        }
    }

    private fun getHighestPredictionVegetableThisWeek(): Pair<String, Float> {
        val currentWeekKey = getCurrentWeekId()
        val currentWeekData = allPredictionData[currentWeekKey] ?: emptyList()

        return if (currentWeekData.isNotEmpty()) {
            currentWeekData.maxByOrNull { it.second } ?: Pair(allVegetables.firstOrNull() ?: "N/A", 0f)
        } else {
            Pair(allVegetables.firstOrNull() ?: "N/A", 0f)
        }
    }

    private fun setDefaultSpinnerSelection(vegetableName: String) {
        if (allVegetables.isEmpty() || vegetableName == "N/A") return
        val position = allVegetables.indexOf(vegetableName)
        if (position >= 0) {
            binding.filterSpinner.setSelection(position, false)
        }
    }

    private fun updateAdditionalInfo() {
        val (highestVegName, highestVegValue) = getHighestPredictionVegetableThisWeek()
        if (highestVegName != "N/A") {
            binding.tvMostConsumedVegetable.text = "$highestVegName"
        } else {
            binding.tvMostConsumedVegetable.text = "Data tidak tersedia"
        }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        val todayDateString = dateFormat.format(calendar.time)
        val weekOfMonthForDisplay = getCurrentWeekOfMonth()
        val currentWeekJsonId = getCurrentWeekId()
        binding.tvCurrentWeekLabel.text = "$todayDateString"
        binding.tvCurrentWeekInfo.text = "Minggu $weekOfMonthForDisplay"
    }


    private fun showChartForVegetable(vegetableName: String, animate: Boolean) {
        if (allPredictionData.isEmpty() || vegetableName == "N/A") {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            binding.lineChart.visibility = View.INVISIBLE

            return
        }
        binding.lineChart.visibility = View.VISIBLE

        val weekLabels = listOf("Minggu 1", "Minggu 2", "Minggu 3", "Minggu 4")
        val entries = mutableListOf<Entry>()

        for ((index, weekId) in listOf("1", "2", "3", "4").withIndex()) {
            val weekData = allPredictionData[weekId] ?: emptyList()
            val prediction = weekData.find { it.first == vegetableName }?.second ?: 0f
            entries.add(Entry(index.toFloat(), prediction))
        }

        if (entries.all { it.y == 0f } && entries.isNotEmpty()) {
             Toast.makeText(requireContext(), "Tidak ada data prediksi untuk $vegetableName pada minggu ini.", Toast.LENGTH_SHORT).show()
        }

        val colorPrimary = ContextCompat.getColor(requireContext(), R.color.md_theme_primary )
        val colorOnSurfaceVariant = ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant)
        val colorSurfaceContainerHighest = ContextCompat.getColor(requireContext(), R.color.md_theme_surfaceContainer)


        val dataSet = LineDataSet(entries, "Prediksi $vegetableName")
        dataSet.color = colorPrimary
        dataSet.setCircleColor(colorPrimary)
        dataSet.circleHoleColor = colorSurfaceContainerHighest
        dataSet.lineWidth = 2.5f
        dataSet.circleRadius = 5f
        dataSet.circleHoleRadius = 2.5f
        dataSet.setDrawCircleHole(true)
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = colorOnSurfaceVariant
        dataSet.setDrawFilled(true)
        dataSet.fillColor = colorPrimary
        dataSet.fillAlpha = 60
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.cubicIntensity = 0.2f
        dataSet.setDrawValues(true)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${String.format(Locale.GERMAN, "%.1f", value)} kg"
            }
        }
        dataSet.highLightColor = ContextCompat.getColor(requireContext(), R.color.md_theme_secondary)
        dataSet.setDrawHighlightIndicators(true)
        dataSet.highlightLineWidth = 1.5f

        val lineData = LineData(dataSet)
        binding.lineChart.data = lineData

        val xAxis = binding.lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(weekLabels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant)
        xAxis.gridLineWidth = 0.8f
        xAxis.textColor = colorOnSurfaceVariant
        xAxis.axisLineColor = colorOnSurfaceVariant

        binding.lineChart.axisRight.isEnabled = false
        val leftAxis = binding.lineChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant)
        leftAxis.gridLineWidth = 0.8f
        leftAxis.textColor = colorOnSurfaceVariant
        leftAxis.axisLineColor = colorOnSurfaceVariant
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()} kg"
            }
        }
        leftAxis.spaceTop = 30f

        binding.lineChart.description.isEnabled = false

        val legend = binding.lineChart.legend
        legend.isEnabled = true
        legend.textColor = colorOnSurfaceVariant
        legend.form = Legend.LegendForm.CIRCLE
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)


        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.isDragEnabled = true
        binding.lineChart.setScaleEnabled(true)
        binding.lineChart.setPinchZoom(true)
        binding.lineChart.setDrawGridBackground(false)
        binding.lineChart.setDrawBorders(false)
        binding.lineChart.setBackgroundColor(Color.TRANSPARENT)
        binding.lineChart.setExtraOffsets(5f, 5f, 5f, 10f)

        binding.lineChart.invalidate()

        if (animate) {
            binding.lineChart.animateXY(800, 800, Easing.EaseInOutCubic)
            binding.lineChart.alpha = 0f
            binding.lineChart.animate().alpha(1f).setDuration(600).startDelay = 200
        } else {
            binding.lineChart.alpha = 1f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}