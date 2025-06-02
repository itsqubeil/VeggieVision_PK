package com.veggievision.lokatani.NLP

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import android.util.Log

class NLPProcessor(private val dataManager: SayurDataManager) {

    private val vegetablePattern = Pattern.compile(
        "\\b(ba[yie]am|baym|byam|baeam|baiam|bayem|bayaam|" +
                "kangkung|kankung|kangkon|kangkun|" +
                "pakcoy|pak coy|pakchoy|pakoj|pakoy|pakcoi|pakchoi|pokcoy|bok choy)\\b"
    )

    private val multiVegetablePattern = Pattern.compile(
        "(ba[yie]am|baym|byam|baeam|baiam|bayem|bayaam|" +
                "kangkung|kankung|kangkon|kangkun|" +
                "pakcoy|pak coy|pakchoy|pakoj|pakoy|pakcoi|pakchoi|pokcoy|bok choy)" +
                "(?:\\s*(?:dan|\\+|&|,|\\s+)\\s*" +
                "(ba[yie]am|baym|byam|baeam|baiam|bayem|bayaam|" +
                "kangkung|kankung|kangkon|kangkun|" +
                "pakcoy|pak coy|pakchoy|pakoj|pakoy|pakcoi|pakchoi|pokcoy|bok choy))+"
    )
    private val todayPattern = Pattern.compile("\\b(hari ini|hri ini|hr ini|sekarang)\\b")
    private val yesterdayPattern = Pattern.compile("\\b(kemarin|kmrn|kemaren|kmaren)\\b")
    private val thisWeekPattern = Pattern.compile("\\b(minggu ini|mingu ini|minggu yg ini|pekan ini|minggu skrg|minggu skrang|minggu sekarang)\\b")
    private val lastWeekPattern = Pattern.compile("\\b(minggu lalu|minggu kemarin|minggu yg lalu|pekan lalu|mingu lalu|minggu kmrn|minggu yg lewat|minggu sebelumnya)\\b")
    private val thisMonthPattern = Pattern.compile("\\b(bulan ini|bln ini|bulan sekarang|bulan skrg)\\b")
    private val lastMonthPattern = Pattern.compile("\\b(bulan lalu|bulan kemarin|bulan yg lalu|bulan kmrn|bulan sebelumnya|bulan yg lewat)\\b")
    private val datePattern = Pattern.compile("\\b(?:(?:tanggal|tgl)\\s*)?(\\d{1,2})\\s+(januari|jan|februari|feb|maret|mar|april|apr|mei|juni|jun|juli|jul|agustus|agt|agst|aug|september|sept|sep|oktober|okt|oct|november|nov|desember|des|dec)(?:\\s+(\\d{4}))?\\b")
    private val monthPattern = Pattern.compile("\\b(januari|jan|februari|feb|maret|mar|april|apr|mei|juni|jun|juli|jul|agustus|agt|agst|aug|september|sept|sep|oktober|okt|oct|november|nov|desember|des|dec)(?:\\s*(\\d{4}))?\\b")
    private val yearPattern = Pattern.compile("\\b(tahun|thn|thn\\.|th)\\s*(\\d{4})\\b")
    private val lastYearPattern = Pattern.compile("\\b(tahun|thn|thn\\.|th)\\s*(lalu|kemarin|kmrn|sebelumnya|yg lalu)\\b")
    private val thisYearPattern = Pattern.compile("\\b(tahun|thn|thn\\.|th)\\s*(ini|skrg|sekarang)\\b")
    private val monthYearPattern = Pattern.compile("\\b(januari|jan|februari|feb|maret|mar|april|apr|mei|juni|jun|juli|jul|agustus|agt|agst|aug|september|sept|sep|oktober|okt|oct|november|nov|desember|des|dec)\\s+(tahun|thn|thn\\.|th)\\s+(lalu|kemarin|kmrn|sebelumnya|yg lalu)\\b")
    private val weightQueryPattern = Pattern.compile("\\b(berat|total|jumlah|berapa|ditimbang|timbangan|data|statistik|stat|rangkuman|rekap)\\b")
    private val averagePattern = Pattern.compile("\\b(rata-rata|average|avg)\\b")
    private val maxWeightPattern = Pattern.compile("\\b(max|maksimum|terberat|paling berat)\\b")
    private val minWeightPattern = Pattern.compile("\\b(min|minimum|teringan|paling ringan)\\b")


    private fun isGeneralStatsQuery(query: String): Boolean {
        if (vegetablePattern.matcher(query).find()) {
            return false
        }
        return query.contains("statistik") ||
                query.contains("stat") ||
                query.contains("rangkuman") ||
                query.contains("rekap") ||
                query.contains("sayuran apa saja") ||
                query.contains("sayur apa saja") ||
                query.contains("apa saja sayuran") ||
                query.contains("apa saja sayur") ||
                query.contains("jenisnya") ||
                query.contains("total keseluruhan")
    }

    private fun determineVegetableType(normalizedQuery: String): String? {
        val vegetableMatcher = vegetablePattern.matcher(normalizedQuery)
        return if (vegetableMatcher.find()) {
            val matchedVeg = vegetableMatcher.group(0)
            when {
                matchedVeg.contains("bay") || matchedVeg.contains("bai") || matchedVeg.contains("bae") -> "bayam"
                matchedVeg.contains("kang") || matchedVeg.contains("kank") -> "kangkung"
                matchedVeg.contains("pak") || matchedVeg.contains("pok") || matchedVeg.contains("bok") -> "pakcoy"
                else -> matchedVeg
            }
        } else if (normalizedQuery.contains("sayur") || normalizedQuery.contains("sayuran") || normalizedQuery.contains("syr")) {
            "semua"
        } else {
            null
        }
    }


    fun processQuery(query: String): String {
        val normalizedQuery = query.lowercase().replace(Regex("\\s+"), " ")
        Log.d(TAG, "User Input: '$query' (lowercase: '$normalizedQuery')")

        val vegetableType = determineVegetableType(normalizedQuery)
        if (vegetableType == null && !isGeneralStatsQuery(normalizedQuery) && !multiVegetablePattern.matcher(normalizedQuery).find()) {
            if (!averagePattern.matcher(normalizedQuery).find() &&
                !maxWeightPattern.matcher(normalizedQuery).find() &&
                !minWeightPattern.matcher(normalizedQuery).find() &&
                !weightQueryPattern.matcher(normalizedQuery).find()) {
                return "Maaf, saya tidak mengerti pertanyaan Anda. Mohon gunakan format yang benar."
            }
            return "Mohon sebutkan jenis sayuran (bayam, kangkung, pakcoy) atau 'semua sayuran'."
        }


        val statType: String? = when {
            averagePattern.matcher(normalizedQuery).find() -> "rata-rata"
            maxWeightPattern.matcher(normalizedQuery).find() -> "maksimum"
            minWeightPattern.matcher(normalizedQuery).find() -> "minimum"
            else -> null
        }

        if (statType == null && !weightQueryPattern.matcher(normalizedQuery).find()) {
            return "Maaf, fitur ini untuk pengecekan berat atau statistik (rata-rata, min, max). Contoh: 'berapa berat bayam hari ini?' atau 'rata-rata bayam minggu ini'."
        }

        if (isGeneralStatsQuery(normalizedQuery) && vegetableType != "semua" && !vegetablePattern.matcher(normalizedQuery).find()) {
            return processGeneralStatsQuery(normalizedQuery, statType)
        }


        val multiVegMatcher = multiVegetablePattern.matcher(normalizedQuery)
        if (multiVegMatcher.find()) {
            return processMultiVegetableQuery(normalizedQuery, statType)
        }

        if (vegetableType == null) {
            return "Mohon sebutkan jenis sayuran (bayam, kangkung, pakcoy) atau 'semua sayuran'."
        }

        val monthYearMatcher = monthYearPattern.matcher(normalizedQuery)
        if (monthYearMatcher.find()) {
            return processMonthLastYearQuery(normalizedQuery, vegetableType, statType)
        }

        val yearMatcher = yearPattern.matcher(normalizedQuery)
        if (yearMatcher.find()) {
            val year = yearMatcher.group(2).toInt()
            return processSpecificYearQuery(vegetableType, year, statType)
        }

        if (thisMonthPattern.matcher(normalizedQuery).find()) {
            return processThisMonthQuery(vegetableType, statType)
        }

        return when {
            datePattern.matcher(normalizedQuery).find() -> {
                processDateQuery(normalizedQuery, vegetableType, statType)
            }
            monthPattern.matcher(normalizedQuery).find() -> {
                processMonthQuery(normalizedQuery, vegetableType, statType)
            }
            lastMonthPattern.matcher(normalizedQuery).find() -> {
                processLastMonthQuery(vegetableType, statType)
            }
            lastWeekPattern.matcher(normalizedQuery).find() -> {
                processLastWeekQuery(vegetableType, statType)
            }
            thisWeekPattern.matcher(normalizedQuery).find() -> {
                processWeekQuery(vegetableType, statType)
            }
            yesterdayPattern.matcher(normalizedQuery).find() -> {
                processYesterdayQuery(vegetableType, statType)
            }
            todayPattern.matcher(normalizedQuery).find() -> {
                processTodayQuery(vegetableType, statType)
            }
            lastYearPattern.matcher(normalizedQuery).find() -> {
                processLastYearQuery(vegetableType, statType)
            }
            thisYearPattern.matcher(normalizedQuery).find() -> {
                processThisYearQuery(vegetableType, statType)
            }
            else -> {
                if (statType != null) {
                    "Untuk statistik '$statType', mohon sertakan periode waktu yang jelas (misal: '$statType $vegetableType hari ini')."
                } else if (vegetableType == "semua") {
                    "Mohon sertakan periode waktu yang jelas untuk permintaan data semua sayuran (misal: 'berat semua sayuran hari ini')."
                } else {
                    "Maaf, mohon sertakan periode waktu yang jelas (misal: 'total $vegetableType september 2024' atau '$vegetableType tahun 2023')"
                }
            }
        }
    }

    private fun formatStatOutput(
        data: List<VegetableData>,
        vegetableType: String,
        timeFrameFullDesc: String,
        statType: String?
    ): String {
        val vegOutputNameGlobal = if (vegetableType == "semua") "semua sayuran" else vegetableType

        if (data.isEmpty()) {
            return "Tidak ada data $vegOutputNameGlobal yang terdeteksi pada $timeFrameFullDesc."
        }

        val overallTotalWeight = data.sumOf { it.weight }
        val overallCount = data.size
        val overallAverageWeight = if (overallCount > 0) overallTotalWeight / overallCount else 0.0
        val overallMinWeight = data.minOfOrNull { it.weight }
        val overallMaxWeight = data.maxOfOrNull { it.weight }

        return when (statType) {
            "rata-rata" -> "Rata-rata berat $vegOutputNameGlobal pada $timeFrameFullDesc adalah ${String.format("%.2f", overallAverageWeight)} gram."
            "maksimum" -> "Berat maksimum $vegOutputNameGlobal pada $timeFrameFullDesc adalah ${String.format("%.2f", overallMaxWeight ?: 0.0)} gram."
            "minimum" -> "Berat minimum $vegOutputNameGlobal pada $timeFrameFullDesc adalah ${String.format("%.2f", overallMinWeight ?: 0.0)} gram."
            else -> {
                val response = StringBuilder()
                response.append("Detail $vegOutputNameGlobal pada $timeFrameFullDesc:\n")

                if (vegetableType == "semua" && data.isNotEmpty()) {
                    val groupedData = data.groupBy { it.vegetableType }
                    if (groupedData.size > 1 || groupedData.keys.firstOrNull() != "semua") {
                        response.append("Rincian per jenis sayur:\n")
                        for ((type, typeData) in groupedData.entries.sortedBy { it.key }) {
                            val typeTotalWeight = typeData.sumOf { it.weight }
                            val typeCount = typeData.size
                            val typeAverageWeight = if (typeCount > 0) typeTotalWeight / typeCount else 0.0
                            val typeMinWeight = typeData.minOfOrNull { it.weight }
                            val typeMaxWeight = typeData.maxOfOrNull { it.weight }

                            response.append("- Jenis: $type\n")
                            response.append("  - Total berat: ${String.format("%.2f", typeTotalWeight)} gram\n")
                            response.append("  - Jumlah data: $typeCount\n")
                            response.append("  - Rata-rata: ${String.format("%.2f", typeAverageWeight)} gram\n")
                            response.append("  - Min: ${if (typeMinWeight != null) String.format("%.2f", typeMinWeight) + "g" else "N/A"}, Max: ${if (typeMaxWeight != null) String.format("%.2f", typeMaxWeight) + "g" else "N/A"}\n")
                        }
                        response.append("\nStatistik Gabungan untuk Semua Sayuran Tersebut:\n")
                    }
                }

                response.append("- Total berat keseluruhan: ${String.format("%.2f", overallTotalWeight)} gram\n")
                response.append("- Jumlah data keseluruhan: $overallCount\n")
                response.append("- Rata-rata berat keseluruhan: ${String.format("%.2f", overallAverageWeight)} gram\n")
                response.append("- Berat minimum keseluruhan: ${if (overallMinWeight != null) String.format("%.2f", overallMinWeight) + " gram" else "N/A"}\n")
                response.append("- Berat maksimum keseluruhan: ${if (overallMaxWeight != null) String.format("%.2f", overallMaxWeight) + " gram" else "N/A"}\n")

                response.toString().trim().replaceFirst("Detail $vegOutputNameGlobal pada $timeFrameFullDesc:\n", "Detail $vegOutputNameGlobal pada $timeFrameFullDesc:\n")
            }
        }
    }

    private fun processTodayQuery(vegetableType: String, statType: String?): String {
        val timeFrameSimpleDesc = "hari ini"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameSimpleDesc")
        val data = dataManager.getDataByTypeForToday(vegetableType)
        val todayDateString = SimpleDateFormat("dd MMMM yyyy", Locale("id")).format(Date())
        val timeFrameFullDesc = "$timeFrameSimpleDesc ($todayDateString)"
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }

    private fun processThisMonthQuery(vegetableType: String, statType: String?): String {
        val timeFrameSimpleDesc = "bulan ini"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameSimpleDesc")

        val calendar = Calendar.getInstance()
        val currentMonthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id"))
        val currentYear = calendar.get(Calendar.YEAR)
        val timeFrameFullDesc = "$timeFrameSimpleDesc ($currentMonthName $currentYear)"

        calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val startDate = calendar.time
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)); calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time
        val data = dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }

    private fun processDateQuery(queryString: String, vegetableType: String, statType: String?): String {
        val dateMatcher = datePattern.matcher(queryString)
        if (!dateMatcher.find()) { return "Format tanggal tidak valid." }
        val dayStr = dateMatcher.group(1)!!; val monthNameInput = dateMatcher.group(2)!!; val yearStr = dateMatcher.group(3)
        val day = dayStr.toInt()
        val monthMap = mapOf("januari" to 0, "jan" to 0, "februari" to 1, "feb" to 1, "maret" to 2, "mar" to 2, "april" to 3, "apr" to 3, "mei" to 4, "juni" to 5, "jun" to 5, "juli" to 6, "jul" to 6, "agustus" to 7, "agt" to 7, "agst" to 7, "aug" to 7, "september" to 8, "sept" to 8, "sep" to 8, "oktober" to 9, "okt" to 9, "oct" to 9, "november" to 10, "nov" to 10, "desember" to 11, "des" to 11, "dec" to 11)
        val cal = Calendar.getInstance()
        val monthIndex = monthMap[monthNameInput.lowercase(Locale.ROOT)] ?: cal.get(Calendar.MONTH)
        val year = yearStr?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        cal.set(year, monthIndex, day)
        val date = cal.time
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
        val timeFrameFullDesc = "tanggal ${formatter.format(date)}"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameFullDesc")
        val data = dataManager.getDataByTypeAndDate(vegetableType, date)
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }

    private fun processMonthQuery(queryString: String, vegetableType: String, statType: String?): String {
        val monthMatcher = monthPattern.matcher(queryString)
        if (!monthMatcher.find()) { return "Format bulan tidak valid." }
        val monthName = monthMatcher.group(1)!!; val yearStr = monthMatcher.group(2)
        val monthMap = mapOf("januari" to 0, "jan" to 0, "februari" to 1, "feb" to 1, "maret" to 2, "mar" to 2, "april" to 3, "apr" to 3, "mei" to 4, "juni" to 5, "jun" to 5, "juli" to 6, "jul" to 6, "agustus" to 7, "agt" to 7, "agst" to 7, "aug" to 7, "september" to 8, "sept" to 8, "sep" to 8, "oktober" to 9, "okt" to 9, "oct" to 9, "november" to 10, "nov" to 10, "desember" to 11, "des" to 11, "dec" to 11)
        val cal = Calendar.getInstance()
        val monthIndex = monthMap[monthName.lowercase(Locale.ROOT)] ?: return "Nama bulan tidak dikenal: $monthName"
        val year = yearStr?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val fullMonthName = Calendar.getInstance().apply { set(Calendar.MONTH, monthIndex) }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id"))
        val timeFrameFullDesc = "bulan $fullMonthName $year"

        if (!thisMonthPattern.matcher(queryString).find() || !queryString.contains(currentMonthNameForLogHack(monthIndex, year))) {
            Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameFullDesc")
        }

        cal.set(year, monthIndex, 1); cal.set(Calendar.HOUR_OF_DAY,0); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0);
        val startDate = cal.time
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); cal.set(Calendar.HOUR_OF_DAY,23); cal.set(Calendar.MINUTE,59); cal.set(Calendar.SECOND,59);
        val endDate = cal.time
        val data = dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }
    private fun currentMonthNameForLogHack(monthIndex: Int, year: Int): String {
        val cal = Calendar.getInstance()
        if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == monthIndex) {
            return cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id")).lowercase(Locale.ROOT)
        }
        return "###impossible_match###"
    }

    private fun processYesterdayQuery(vegetableType: String, statType: String?): String {
        val timeFrameSimpleDesc = "kemarin"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameSimpleDesc")
        val calendar = Calendar.getInstance(); calendar.add(Calendar.DAY_OF_MONTH, -1); val yesterday = calendar.time
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
        val timeFrameFullDesc = "$timeFrameSimpleDesc (${formatter.format(yesterday)})"
        val data = dataManager.getDataByTypeAndDate(vegetableType, yesterday)
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }

    private fun processWeekQuery(vegetableType: String, statType: String?): String {
        val timeFrameSimpleDesc = "minggu ini"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameSimpleDesc")
        val data = dataManager.getDataByTypeForCurrentWeek(vegetableType)
        val (start, end) = dataManager.getCurrentWeekRange()
        val formatter = SimpleDateFormat("d MMM", Locale("id"))
        val timeFrameFullDesc = "$timeFrameSimpleDesc (${formatter.format(start)} - ${formatter.format(end)} ${Calendar.getInstance().get(Calendar.YEAR)})"
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }

    private fun processLastWeekQuery(vegetableType: String, statType: String?): String {
        val timeFrameSimpleDesc = "minggu lalu"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameSimpleDesc")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -1); calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); val lastWeekStart = calendar.time
        val startCalForDesc = calendar.clone() as Calendar
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); val lastWeekEnd = calendar.time
        val data = dataManager.getDataByTypeBetweenDates(vegetableType, lastWeekStart, lastWeekEnd)
        val formatter = SimpleDateFormat("d MMM", Locale("id"))
        val timeFrameFullDesc = "$timeFrameSimpleDesc (${formatter.format(lastWeekStart)} - ${formatter.format(lastWeekEnd)} ${startCalForDesc.get(Calendar.YEAR)})"
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }

    private fun processLastMonthQuery(vegetableType: String, statType: String?): String {
        val timeFrameSimpleDesc = "bulan lalu"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameSimpleDesc")
        val calendar = Calendar.getInstance(); calendar.add(Calendar.MONTH, -1)
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id"))
        val year = calendar.get(Calendar.YEAR)
        val timeFrameFullDesc = "$timeFrameSimpleDesc ($monthName $year)"
        calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); val startOfLastMonth = calendar.time
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)); calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); val endOfLastMonth = calendar.time
        val data = dataManager.getDataByTypeBetweenDates(vegetableType, startOfLastMonth, endOfLastMonth)
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }

    private fun processSpecificYearQuery(vegetableType: String, year: Int, statType: String?): String {
        val timeFrameFullDesc = "tahun $year"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameFullDesc")
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (year > currentYear) return "Tahun $year belum selesai. Data hanya tersedia sampai tahun $currentYear."
        val calendar = Calendar.getInstance(); calendar.set(year, Calendar.JANUARY, 1,0,0,0); val startDate = calendar.time
        calendar.set(year, Calendar.DECEMBER, 31,23,59,59); val endDate = calendar.time
        val data = dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }

    private fun processLastYearQuery(vegetableType: String, statType: String?): String {
        val lastYear = Calendar.getInstance().get(Calendar.YEAR) - 1
        val timeFrameFullDesc = "tahun lalu ($lastYear)"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameFullDesc")
        val calendar = Calendar.getInstance(); calendar.set(lastYear, Calendar.JANUARY, 1,0,0,0); val startDate = calendar.time
        calendar.set(lastYear, Calendar.DECEMBER, 31,23,59,59); val endDate = calendar.time
        val data = dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }
    private fun processThisYearQuery(vegetableType: String, statType: String?): String {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val timeFrameFullDesc = "tahun ini ($currentYear)"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameFullDesc")
        val calendar = Calendar.getInstance(); calendar.set(currentYear, Calendar.JANUARY, 1,0,0,0); val startDate = calendar.time
        calendar.set(currentYear, Calendar.DECEMBER, 31,23,59,59); val endDate = calendar.time
        val data = dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }
    private fun processMonthLastYearQuery(queryString: String, vegetableType: String, statType: String?): String {
        val monthYearMatcher = monthYearPattern.matcher(queryString)
        if (!monthYearMatcher.find()) return "Format bulan tahun lalu tidak valid."
        val monthName = monthYearMatcher.group(1)!!
        val monthMap = mapOf("januari" to 0, "jan" to 0, "februari" to 1, "feb" to 1, "maret" to 2, "mar" to 2, "april" to 3, "apr" to 3, "mei" to 4, "juni" to 5, "jun" to 5, "juli" to 6, "jul" to 6, "agustus" to 7, "agt" to 7, "agst" to 7, "aug" to 7, "september" to 8, "sept" to 8, "sep" to 8, "oktober" to 9, "okt" to 9, "oct" to 9, "november" to 10, "nov" to 10, "desember" to 11, "des" to 11, "dec" to 11)
        val cal = Calendar.getInstance()
        val monthIndex = monthMap[monthName.lowercase(Locale.ROOT)] ?: return "Nama bulan tidak dikenal: $monthName"
        val lastYear = cal.get(Calendar.YEAR) - 1
        val fullMonthName = Calendar.getInstance().apply { set(Calendar.MONTH, monthIndex) }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id"))
        val timeFrameFullDesc = "bulan $fullMonthName tahun lalu ($lastYear)"
        Log.d(TAG, "sayur: $vegetableType ${statType ?: "berat"} waktu: $timeFrameFullDesc")

        cal.set(Calendar.YEAR, lastYear); cal.set(Calendar.MONTH, monthIndex)
        cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); val startDate = cal.time
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); val endDate = cal.time
        val data = dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)
        return formatStatOutput(data, vegetableType, timeFrameFullDesc, statType)
    }

    private fun processMultiVegetableQuery(query: String, statType: String?): String {

        val isAllQueryVegs = query.contains("semua sayur") || query.contains("seluruh sayur") || query.contains("semua jenis") || (!query.contains("bayam") && !query.contains("kangkung") && !query.contains("pakcoy"))
        val vegetables = if (isAllQueryVegs && multiVegetablePattern.matcher(query).find().not()) {
            listOf("semua")
        } else {
            val matcher = multiVegetablePattern.matcher(query)
            val detectedVegs = mutableSetOf<String>()
            if (matcher.find()) {
                var i = 1
                while (i <= matcher.groupCount()) {
                    matcher.group(i)?.let { veg ->
                        val normalizedVeg = when {
                            veg.contains("bay") || veg.contains("bai") || veg.contains("bae") -> "bayam"
                            veg.contains("kang") || veg.contains("kank") -> "kangkung"
                            veg.contains("pak") || veg.contains("pok") || veg.contains("bok") -> "pakcoy"
                            else -> veg.trim()
                        }
                        if (normalizedVeg.isNotBlank()) detectedVegs.add(normalizedVeg)
                    }
                    i++
                }
            }
            if (detectedVegs.isEmpty() && (query.contains("sayur") || query.contains("sayuran"))) {
                detectedVegs.add("semua")
            }
            detectedVegs.toList()
        }


        if (vegetables.isEmpty() || (vegetables.size == 1 && vegetables.first() == "semua" && !isAllQueryVegs)) {
            return "Mohon sebutkan jenis sayuran yang valid untuk permintaan multi-sayuran."
        }

        val (timeFrameDesc, startDate, endDate, timeFrameFullDescForOutput) = determineTimeFrameForMulti(query)

        if (startDate == null || endDate == null) {
            return "Mohon sertakan periode waktu yang jelas untuk permintaan beberapa sayuran."
        }

        val logVegName = if (vegetables.size == 1 && vegetables.first() == "semua") "semua" else vegetables.joinToString(",")
        Log.d(TAG, "sayur: $logVegName ${statType ?: "berat"} waktu: $timeFrameDesc")

        val allSelectedData = mutableListOf<VegetableData>()
        val individualResults = mutableListOf<String>()
        var grandTotalWeight = 0.0

        if (vegetables.size == 1 && vegetables.first() == "semua") {
            val data = dataManager.getDataByTypeBetweenDates("semua", startDate, endDate)
            allSelectedData.addAll(data)
        } else {
            for (veg in vegetables) {
                if (veg == "semua") continue
                val data = dataManager.getDataByTypeBetweenDates(veg, startDate, endDate)
                allSelectedData.addAll(data)
                val vegWeight = data.sumOf { it.weight }
                grandTotalWeight += vegWeight
                if (data.isNotEmpty()) {
                    individualResults.add("- $veg: ${String.format("%.2f", vegWeight)} gram (${data.size} data)")
                } else {
                    individualResults.add("- $veg: 0 gram (tidak ada data pada $timeFrameDesc)")
                }
            }
        }


        if (allSelectedData.isEmpty()) {
            return "Tidak ada data sayuran (${vegetables.joinToString(", ")}) yang ditemukan pada $timeFrameDesc."
        }

        if (statType != null) {
            return formatStatOutput(allSelectedData, vegetables.joinToString(" & "), timeFrameFullDescForOutput, statType)
        }

        val response = StringBuilder("Detail berat sayuran pada $timeFrameFullDescForOutput:\n")
        if (individualResults.isNotEmpty() && !(vegetables.size == 1 && vegetables.first() == "semua")) {
            response.append(individualResults.joinToString("\n")).append("\n\n")
        }

        response.append(formatStatOutput(allSelectedData, "gabungan (${vegetables.joinToString(", ")})", timeFrameFullDescForOutput, null)
            .replaceFirst("Detail gabungan .*? pada $timeFrameFullDescForOutput:\n", "Statistik Gabungan:\n"))


        if (!(vegetables.size == 1 && vegetables.first() == "semua") && vegetables.size > 1) {
            response.append("\n\nTotal berat keseluruhan (${vegetables.joinToString(", ")}): ${String.format("%.2f", grandTotalWeight)} gram")
        }


        return response.toString().trim()
    }

    private fun determineTimeFrameForMulti(query: String): TimeFrameOutputDetails {
        var startDate: Date? = null
        var endDate: Date? = null
        var timeFrameDesc = "periode yang diminta"
        var timeFrameFullDescForOutput = "periode yang diminta"

        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("id"))


        when {
            todayPattern.matcher(query).find() -> {
                timeFrameDesc = "hari ini"
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(
                    Calendar.MINUTE,
                    0
                ); cal.set(Calendar.SECOND, 0); startDate = cal.time
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(
                    Calendar.MINUTE,
                    59
                ); cal.set(Calendar.SECOND, 59); endDate = cal.time
                timeFrameFullDescForOutput = "$timeFrameDesc (${dateFormat.format(startDate!!)})"
            }

            thisMonthPattern.matcher(query).find() -> {
                timeFrameDesc = "bulan ini"
                val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id"))
                val year = cal.get(Calendar.YEAR)
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(
                    Calendar.MINUTE,
                    0
                ); cal.set(Calendar.SECOND, 0); startDate = cal.time
                cal.set(
                    Calendar.DAY_OF_MONTH,
                    cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                ); cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(
                    Calendar.MINUTE,
                    59
                ); cal.set(Calendar.SECOND, 59); endDate = cal.time
                timeFrameFullDescForOutput = "$timeFrameDesc ($monthName $year)"
            }

            thisWeekPattern.matcher(query).find() -> {
                timeFrameDesc = "minggu ini"
                val (start, end) = dataManager.getCurrentWeekRange()
                startDate = start; endDate = end
                val weekFormat = SimpleDateFormat("d MMM", Locale("id"))
                timeFrameFullDescForOutput =
                    "$timeFrameDesc (${weekFormat.format(startDate!!)} - ${weekFormat.format(endDate!!)} ${
                        cal.get(Calendar.YEAR)
                    })"
            }

            lastYearPattern.matcher(query).find() -> {
                val lastYr = cal.get(Calendar.YEAR) - 1
                timeFrameDesc = "tahun lalu ($lastYr)"
                cal.set(lastYr, Calendar.JANUARY, 1, 0, 0, 0); startDate = cal.time
                cal.set(lastYr, Calendar.DECEMBER, 31, 23, 59, 59); endDate = cal.time
                timeFrameFullDescForOutput = timeFrameDesc
            }

            thisYearPattern.matcher(query).find() -> {
                val currentYr = cal.get(Calendar.YEAR)
                timeFrameDesc = "tahun ini ($currentYr)"
                cal.set(currentYr, Calendar.JANUARY, 1, 0, 0, 0); startDate = cal.time
                cal.set(currentYr, Calendar.DECEMBER, 31, 23, 59, 59); endDate = cal.time
                timeFrameFullDescForOutput = timeFrameDesc
            }

            yearPattern.matcher(query).find() -> {
                val matcher = yearPattern.matcher(query); matcher.find()
                val year = matcher.group(2).toInt()
                timeFrameDesc = "tahun $year"
                cal.set(year, Calendar.JANUARY, 1, 0, 0, 0); startDate = cal.time
                cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59); endDate = cal.time
                timeFrameFullDescForOutput = timeFrameDesc
            }

            monthPattern.matcher(query).find() -> {
                val monthMatcher = monthPattern.matcher(query); monthMatcher.find()
                val monthNameInput = monthMatcher.group(1)!!;
                val yearStr = monthMatcher.group(2)
                val monthMap = mapOf(
                    "januari" to 0,
                    "jan" to 0,
                    "februari" to 1,
                    "feb" to 1,
                    "maret" to 2,
                    "mar" to 2,
                    "april" to 3,
                    "apr" to 3,
                    "mei" to 4,
                    "juni" to 5,
                    "jun" to 5,
                    "juli" to 6,
                    "jul" to 6,
                    "agustus" to 7,
                    "agt" to 7,
                    "agst" to 7,
                    "aug" to 7,
                    "september" to 8,
                    "sept" to 8,
                    "sep" to 8,
                    "oktober" to 9,
                    "okt" to 9,
                    "oct" to 9,
                    "november" to 10,
                    "nov" to 10,
                    "desember" to 11,
                    "des" to 11,
                    "dec" to 11
                )
                val monthIdx =
                    monthMap[monthNameInput.lowercase(Locale.ROOT)] ?: cal.get(Calendar.MONTH)
                val yr = yearStr?.toIntOrNull() ?: cal.get(Calendar.YEAR)
                val fullMonthName = Calendar.getInstance().apply { set(Calendar.MONTH, monthIdx) }
                    .getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id"))
                timeFrameDesc = "bulan $fullMonthName $yr"

                cal.set(yr, monthIdx, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(
                    Calendar.MINUTE,
                    0
                ); cal.set(Calendar.SECOND, 0); startDate = cal.time
                cal.set(
                    Calendar.DAY_OF_MONTH,
                    cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                ); cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(
                    Calendar.MINUTE,
                    59
                ); cal.set(Calendar.SECOND, 59); endDate = cal.time
                timeFrameFullDescForOutput = timeFrameDesc
            }
        }
        return TimeFrameOutputDetails(timeFrameDesc, startDate, endDate, timeFrameFullDescForOutput)
    }


    private fun processGeneralStatsQuery(query: String, statType: String?): String {
        var timeFrameText = "keseluruhan"
        var timeFrameFullDesc = "keseluruhan"
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("id"))


        val timeFrameData: List<VegetableData> = when {
            thisWeekPattern.matcher(query).find() -> {
                timeFrameText = "minggu ini"
                val (start, end) = dataManager.getCurrentWeekRange()
                val weekFormat = SimpleDateFormat("d MMM", Locale("id"))
                timeFrameFullDesc = "$timeFrameText (${weekFormat.format(start)} - ${weekFormat.format(end)} ${cal.get(Calendar.YEAR)})"
                dataManager.getDataForCurrentWeek()
            }
            todayPattern.matcher(query).find() -> {
                timeFrameText = "hari ini"
                timeFrameFullDesc = "$timeFrameText (${dateFormat.format(cal.time)})"
                dataManager.getDataForToday()
            }
            thisMonthPattern.matcher(query).find() -> {
                timeFrameText = "bulan ini"
                timeFrameFullDesc = "$timeFrameText (${monthYearFormat.format(cal.time)})"
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY,0); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0); val start = cal.time
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); cal.set(Calendar.HOUR_OF_DAY,23); cal.set(Calendar.MINUTE,59); cal.set(Calendar.SECOND,59); val end = cal.time
                dataManager.getAllDataBetweenDates(start, end)
            }
            lastMonthPattern.matcher(query).find() -> {
                timeFrameText = "bulan lalu"
                cal.add(Calendar.MONTH, -1)
                timeFrameFullDesc = "$timeFrameText (${monthYearFormat.format(cal.time)})"
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY,0); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0); val start = cal.time
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); cal.set(Calendar.HOUR_OF_DAY,23); cal.set(Calendar.MINUTE,59); cal.set(Calendar.SECOND,59); val end = cal.time
                dataManager.getAllDataBetweenDates(start, end)
            }
            lastYearPattern.matcher(query).find() -> {
                val lastYr = cal.get(Calendar.YEAR) -1
                timeFrameText = "tahun lalu ($lastYr)"
                timeFrameFullDesc = timeFrameText
                cal.set(lastYr, Calendar.JANUARY, 1,0,0,0); val start = cal.time
                cal.set(lastYr, Calendar.DECEMBER, 31,23,59,59); val end = cal.time
                dataManager.getAllDataBetweenDates(start, end)
            }
            thisYearPattern.matcher(query).find() -> {
                val currentYr = cal.get(Calendar.YEAR)
                timeFrameText = "tahun ini ($currentYr)"
                timeFrameFullDesc = timeFrameText
                cal.set(currentYr, Calendar.JANUARY, 1,0,0,0); val start = cal.time
                cal.set(currentYr, Calendar.DECEMBER, 31,23,59,59); val end = cal.time
                dataManager.getAllDataBetweenDates(start, end)
            }
            yearPattern.matcher(query).find() -> {
                val matcher = yearPattern.matcher(query); matcher.find()
                val year = matcher.group(2).toInt()
                timeFrameText = "tahun $year"
                timeFrameFullDesc = timeFrameText
                cal.set(year, Calendar.JANUARY, 1,0,0,0); val start = cal.time
                cal.set(year, Calendar.DECEMBER, 31,23,59,59); val end = cal.time
                dataManager.getAllDataBetweenDates(start, end)
            }
            monthPattern.matcher(query).find() -> {
                val monthMatcher = monthPattern.matcher(query); monthMatcher.find()
                val monthNameInput = monthMatcher.group(1)!!; val yearStr = monthMatcher.group(2)
                val monthMap = mapOf("januari" to 0, "jan" to 0, "februari" to 1, "feb" to 1, "maret" to 2, "mar" to 2, "april" to 3, "apr" to 3, "mei" to 4, "juni" to 5, "jun" to 5, "juli" to 6, "jul" to 6, "agustus" to 7, "agt" to 7, "agst" to 7, "aug" to 7, "september" to 8, "sept" to 8, "sep" to 8, "oktober" to 9, "okt" to 9, "oct" to 9, "november" to 10, "nov" to 10, "desember" to 11, "des" to 11, "dec" to 11)
                val currentCal = Calendar.getInstance()
                val monthIdx = monthMap[monthNameInput.lowercase(Locale.ROOT)] ?: currentCal.get(Calendar.MONTH)
                val yr = yearStr?.toIntOrNull() ?: currentCal.get(Calendar.YEAR)
                val fullMonthName = Calendar.getInstance().apply{set(Calendar.MONTH, monthIdx)}.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id"))
                timeFrameText = "bulan $fullMonthName $yr"
                timeFrameFullDesc = timeFrameText

                currentCal.set(yr, monthIdx, 1); currentCal.set(Calendar.HOUR_OF_DAY,0); currentCal.set(Calendar.MINUTE,0); currentCal.set(Calendar.SECOND,0); val start = currentCal.time
                currentCal.set(Calendar.DAY_OF_MONTH, currentCal.getActualMaximum(Calendar.DAY_OF_MONTH)); currentCal.set(Calendar.HOUR_OF_DAY,23); currentCal.set(Calendar.MINUTE,59); currentCal.set(Calendar.SECOND,59); val end = currentCal.time
                dataManager.getAllDataBetweenDates(start, end)
            }

            else -> {
                dataManager.getVegetableData()
            }
        }

        Log.d(TAG, "sayur: statistik_umum ${statType ?: "berat"} waktu: $timeFrameText")

        if (timeFrameData.isEmpty()) {
            return "Tidak ada data sayuran yang terdeteksi pada $timeFrameText."
        }

        if (statType != null) {
            return formatStatOutput(timeFrameData, "semua sayuran", timeFrameFullDesc, statType)
        }

        val groupedByType = timeFrameData.groupBy { it.vegetableType }
        if (query.contains("apa saja") || query.contains("apa aja") || query.contains("jenisnya")) {
            val totalWeight = timeFrameData.sumOf { it.weight }
            val vegetableDetailsList = groupedByType.entries
                .sortedByDescending { it.value.sumOf { item -> item.weight } }
                .joinToString("\n") { entry ->
                    val vegType = entry.key
                    val vegWeight = entry.value.sumOf { it.weight }
                    "- $vegType: ${String.format("%.2f", vegWeight)} gram (${entry.value.size} data)"
                }
            return """
            Daftar sayuran pada $timeFrameFullDesc:
            $vegetableDetailsList
            
            Total berat keseluruhan: ${String.format("%.2f", totalWeight)} gram
            Total jenis sayuran: ${groupedByType.size} jenis
            """.trimIndent().replaceFirst("\n","")
        }

        val baseStatsOutput = formatStatOutput(timeFrameData, "semua sayuran", timeFrameFullDesc, null)
            .replaceFirst("Detail semua sayuran", "Statistik semua sayuran")


        val mostCommonVegetable = groupedByType.maxByOrNull { it.value.size }?.key ?: "N/A"
        val mostCommonCount = groupedByType[mostCommonVegetable]?.size ?: 0
        val heaviestVegetable = groupedByType.maxByOrNull { group -> group.value.sumOf { it.weight } }?.key ?: "N/A"
        val heaviestWeight = groupedByType[heaviestVegetable]?.sumOf { it.weight } ?: 0.0

        return """
        $baseStatsOutput
        - Jenis sayuran terbanyak: $mostCommonVegetable ($mostCommonCount data)
        - Jenis sayuran terberat (total): $heaviestVegetable (${String.format("%.2f", heaviestWeight)} gram)
        - Total jenis sayuran terdeteksi: ${groupedByType.size} jenis
        """.trimIndent().replaceFirst("\n", "")
    }


    companion object {
        private const val TAG = "NLPProcessor"
    }
}