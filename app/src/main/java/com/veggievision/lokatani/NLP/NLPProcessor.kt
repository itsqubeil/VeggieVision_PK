package com.veggievision.lokatani.NLP

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class NLPProcessor(private val dataManager: SayurDataManager) {

    private val vegetablePattern = Pattern.compile("bayam|kangkung|pakcoy|tomat cerry|tomat")
    private val todayPattern = Pattern.compile("hari ini")
    private val yesterdayPattern = Pattern.compile("kemarin")
    private val thisWeekPattern = Pattern.compile("minggu ini")
    private val lastWeekPattern = Pattern.compile("minggu lalu")
    private val lastMonthPattern = Pattern.compile("bulan lalu")
    private val dayOfWeekPattern = Pattern.compile("\\b(senin|selasa|rabu|kamis|jumat|sabtu|minggu)\\b")
    private val monthPattern = Pattern.compile("(januari|februari|maret|april|mei|juni|juli|agustus|september|oktober|november|desember)(?:\\s*(\\d{4}))?")
    private val datePattern = Pattern.compile("tanggal (\\d{1,2})\\s*(?:januari|februari|maret|april|mei|juni|juli|agustus|september|oktober|november|desember)\\s*(\\d{4})?")

    fun processQuery(query: String): String {
        val normalizedQuery = query.lowercase().replace(Regex("\\s+"), " ")

        if (isGeneralStatsQuery(normalizedQuery)) {
            return processGeneralStatsQuery(normalizedQuery)
        }

        val vegetableMatcher = vegetablePattern.matcher(normalizedQuery)
        val vegetableType = if (vegetableMatcher.find()) {
            when (vegetableMatcher.group(0)) {
                "tomat" -> if (normalizedQuery.contains("tomat cerry")) "tomat cerry" else "tomat"
                else -> vegetableMatcher.group(0)
            }
        } else if (normalizedQuery.contains("sayur")) {
            "semua"
        } else {
            return "Maaf, saya hanya mengenali jenis sayuran: bayam, kangkung, pakcoy, dan tomat cerry."
        }

        return when {
            monthPattern.matcher(normalizedQuery).find() -> {
                processMonthQuery(normalizedQuery, vegetableType)
            }

            datePattern.matcher(normalizedQuery).find() -> {
                processDateQuery(normalizedQuery, vegetableType)
            }

            lastMonthPattern.matcher(normalizedQuery).find() -> {
                processLastMonthQuery(vegetableType)
            }

            lastWeekPattern.matcher(normalizedQuery).find() -> {
                processLastWeekQuery(vegetableType)
            }

            thisWeekPattern.matcher(normalizedQuery).find() -> {
                processWeekQuery(vegetableType)
            }

            dayOfWeekPattern.matcher(normalizedQuery).find() -> {
                processDayOfWeekQuery(normalizedQuery, vegetableType)
            }

            yesterdayPattern.matcher(normalizedQuery).find() -> {
                processYesterdayQuery(vegetableType)
            }

            todayPattern.matcher(normalizedQuery).find() -> {
                processTodayQuery(vegetableType)
            }

            else -> {
                "Maaf, mohon sertakan periode waktu yang jelas (misal: 'total bayam september 2024')"
            }
        }
    }

    private fun isGeneralStatsQuery(query: String): Boolean {
        return !vegetablePattern.matcher(query).find() &&
                (query.contains("semua sayur") ||
                        query.contains("semua jenis") ||
                        query.contains("total keseluruhan") ||
                        query.contains("statistik") ||
                        query.contains("rangkuman"))
    }

    private fun processTodayQuery(vegetableType: String): String {
        val data = dataManager.getDataByTypeForToday(vegetableType)

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi hari ini."
        }

        val totalWeight = data.sumOf { it.weight }
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
        val today = formatter.format(Date())

        return "Berat total $vegetableType hari ini ($today) adalah ${String.format("%.2f", totalWeight)} kg."
    }

    private fun processYesterdayQuery(vegetableType: String): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = calendar.time

        val data = dataManager.getDataByTypeAndDate(vegetableType, yesterday)

        if (data.isEmpty()) {
            val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
            return "Tidak ada data $vegetableType yang terdeteksi kemarin (${formatter.format(yesterday)})."
        }

        val totalWeight = data.sumOf { it.weight }
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id"))

        return "Berat total $vegetableType kemarin (${formatter.format(yesterday)}) adalah ${String.format("%.2f", totalWeight)} kg."
    }

    private fun processWeekQuery(vegetableType: String): String {
        val data = dataManager.getDataByTypeForCurrentWeek(vegetableType)

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi minggu ini."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total $vegetableType minggu ini adalah ${String.format("%.2f", totalWeight)} kg."
    }

    private fun processLastWeekQuery(vegetableType: String): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val lastWeekStart = calendar.time
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val lastWeekEnd = calendar.time

        val data = dataManager.getDataByTypeBetweenDates(vegetableType, lastWeekStart, lastWeekEnd)

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi minggu lalu."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total $vegetableType minggu lalu adalah ${String.format("%.2f", totalWeight)} kg."
    }

    private fun processDayOfWeekQuery(query: String, vegetableType: String): String {
        val dayMatcher = dayOfWeekPattern.matcher(query)
        if (!dayMatcher.find()) {
            return "Maaf, saya tidak mengenali hari yang Anda maksud."
        }

        val dayName = dayMatcher.group(0) ?: return "Maaf, saya tidak mengenali hari yang Anda maksud."
        val data = dataManager.getDataByTypeForDayOfWeek(vegetableType, dayName)

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi pada hari $dayName."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total $vegetableType pada hari $dayName adalah ${String.format("%.2f", totalWeight)} kg."
    }

    private fun processMonthQuery(query: String, vegetableType: String): String {
        val monthMatcher = monthPattern.matcher(query)
        if (!monthMatcher.find()) {
            return "Maaf, saya tidak mengenali bulan yang Anda maksud."
        }

        val monthName = monthMatcher.group(1) ?: return "Maaf, saya tidak mengenali bulan yang Anda maksud."
        val yearStr = monthMatcher.group(2)

        val monthMap = mapOf(
            "januari" to 0, "februari" to 1, "maret" to 2, "april" to 3,
            "mei" to 4, "juni" to 5, "juli" to 6, "agustus" to 7,
            "september" to 8, "oktober" to 9, "november" to 10, "desember" to 11
        )

        val monthIndex = monthMap[monthName] ?: return "Bulan tidak valid."

        val year = yearStr?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, monthIndex)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startDate = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time

        val data = dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi pada bulan $monthName $year."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total $vegetableType pada bulan $monthName $year adalah ${String.format("%.2f", totalWeight)} kg."
    }

    private fun processDateQuery(query: String, vegetableType: String): String {
        val dateMatcher = datePattern.matcher(query)
        if (!dateMatcher.find()) {
            return "Maaf, saya tidak mengenali tanggal yang Anda maksud."
        }

        val dayStr = dateMatcher.group(1) ?: return "Maaf, saya tidak mengenali tanggal yang Anda maksud."
        val day = dayStr.toInt()

        val monthMatcher = monthPattern.matcher(query)
        if (!monthMatcher.find()) {
            return "Maaf, saya tidak mengenali bulan yang Anda maksud."
        }

        val monthName = monthMatcher.group(1) ?: return "Maaf, saya tidak mengenali bulan yang Anda maksud."
        val yearStr = monthMatcher.group(2)

        val monthMap = mapOf(
            "januari" to 0, "februari" to 1, "maret" to 2, "april" to 3,
            "mei" to 4, "juni" to 5, "juli" to 6, "agustus" to 7,
            "september" to 8, "oktober" to 9, "november" to 10, "desember" to 11
        )
        val monthIdx = monthMap[monthName] ?: Calendar.getInstance().get(Calendar.MONTH)
        val year = yearStr?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, monthIdx)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val date = calendar.time

        val data = dataManager.getDataByTypeAndDate(vegetableType, date)

        if (data.isEmpty()) {
            val formatter = SimpleDateFormat("d MMMM yyyy", Locale("id"))
            return "Tidak ada data $vegetableType yang terdeteksi pada tanggal ${formatter.format(date)}."
        }

        val totalWeight = data.sumOf { it.weight }
        val formatter = SimpleDateFormat("d MMMM yyyy", Locale("id"))
        return "Berat total $vegetableType pada tanggal ${formatter.format(date)} adalah ${String.format("%.2f", totalWeight)} kg."
    }

    private fun processLastMonthQuery(vegetableType: String): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfLastMonth = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endOfLastMonth = calendar.time

        val data = if (vegetableType == "semua") {
            dataManager.getAllDataBetweenDates(startOfLastMonth, endOfLastMonth)
        } else {
            dataManager.getDataByTypeBetweenDates(vegetableType, startOfLastMonth, endOfLastMonth)
        }

        if (data.isEmpty()) {
            return "Tidak ada data ${if (vegetableType == "semua") "sayuran" else vegetableType} yang terdeteksi bulan lalu."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total ${if (vegetableType == "semua") "semua sayuran" else vegetableType} bulan lalu adalah ${String.format("%.2f", totalWeight)} kg."
    }

    private fun processGeneralStatsQuery(query: String): String {
        val allData = dataManager.getVegetableData()

        if (allData.isEmpty()) {
            return "Tidak ada data sayuran yang tersedia."
        }

        val groupedByType = allData.groupBy { it.vegetableType }

        val timeFrameData = when {
            thisWeekPattern.matcher(query).find() -> dataManager.getDataForCurrentWeek()
            todayPattern.matcher(query).find() -> dataManager.getDataForToday()
            else -> allData
        }

        val timeFrameText = when {
            thisWeekPattern.matcher(query).find() -> "minggu ini"
            todayPattern.matcher(query).find() -> "hari ini"
            else -> "keseluruhan"
        }

        val totalWeight = timeFrameData.sumOf { it.weight }
        val mostCommonVegetable = groupedByType.maxByOrNull { it.value.size }?.key ?: "tidak ada"
        val heaviestVegetable = groupedByType.maxByOrNull { group -> group.value.sumOf { it.weight } }?.key ?: "tidak ada"

        return """
            Statistik $timeFrameText:
            - Total berat semua sayuran: ${String.format("%.2f", totalWeight)} kg
            - Jenis sayuran terbanyak terdeteksi: $mostCommonVegetable (${groupedByType[mostCommonVegetable]?.size ?: 0} kali)
            - Jenis sayuran dengan berat total tertinggi: $heaviestVegetable (${String.format("%.2f", groupedByType[heaviestVegetable]?.sumOf { it.weight } ?: 0.0)} kg)
            - Total jenis sayuran terdeteksi: ${groupedByType.size} jenis
        """.trimIndent()
    }
}