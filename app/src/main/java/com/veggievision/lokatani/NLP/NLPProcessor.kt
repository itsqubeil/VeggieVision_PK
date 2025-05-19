package com.veggievision.lokatani.NLP

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class NLPProcessor(private val dataManager: SayurDataManager) {

    private val vegetablePattern = Pattern.compile(
        "\\b(ba[yie]am|baym|byam|baeam|baiam|bayem|bayaam|" +
                "kangkung|kankung|kangkon|kankung|kangkun|" +
                "pakcoy|pak coy|pakchoy|pakoj|pakoy|pakcoi|pakchoi|pokcoy|bok choy)\\b"
    )

    private val multiVegetablePattern = Pattern.compile(
        "(ba[yie]am|baym|byam|baeam|baiam|bayem|bayaam|" +
                "kangkung|kankung|kangkon|kankung|kangkun|" +
                "pakcoy|pak coy|pakchoy|pakoj|pakoy|pakcoi|pakchoi|pokcoy|bok choy)" +
                "(?:\\s*(?:dan|\\+|&|,|\\s+)\\s*" +
                "(ba[yie]am|baym|byam|baeam|baiam|bayem|bayaam|" +
                "kangkung|kankung|kangkon|kankung|kangkun|" +
                "pakcoy|pak coy|pakchoy|pakoj|pakoy|pakcoi|pakchoi|pokcoy|bok choy))+"
    )

    private val todayPattern = Pattern.compile("\\b(hari ini|hri ini|hr ini|sekarang)\\b")
    private val yesterdayPattern = Pattern.compile("\\b(kemarin|kmrn|kemaren|kmaren)\\b")
    private val thisWeekPattern = Pattern.compile("\\b(minggu ini|mingu ini|minggu yg ini|pekan ini|minggu skrg|minggu skrang|minggu sekarang)\\b")
    private val lastWeekPattern = Pattern.compile("\\b(minggu lalu|minggu kemarin|minggu yg lalu|pekan lalu|mingu lalu|minggu kmrn|minggu yg lewat|minggu sebelumnya)\\b")
    private val lastMonthPattern = Pattern.compile("\\b(bulan lalu|bulan kemarin|bulan yg lalu|bulan kmrn|bulan sebelumnya|bulan yg lewat)\\b")
    private val dayOfWeekPattern = Pattern.compile("\\b(senin|selasa|rabu|kamis|jumat|jum'at|sabtu|minggu)\\b")
    private val monthPattern = Pattern.compile("\\b(januari|jan|februari|feb|maret|mar|april|apr|mei|juni|jun|juli|jul|agustus|agt|agst|aug|september|sept|sep|oktober|okt|oct|november|nov|desember|des|dec)(?:\\s*(\\d{4}))?\\b")
    private val datePattern = Pattern.compile("\\b(?:tanggal|tgl)\\s*(\\d{1,2})\\s*(?:januari|jan|februari|feb|maret|mar|april|apr|mei|juni|jun|juli|jul|agustus|agt|agst|aug|september|sept|sep|oktober|okt|oct|november|nov|desember|des|dec)\\s*(\\d{4})?\\b")
    private val yearPattern = Pattern.compile("\\b(tahun|thn|thn\\.|th)\\s*(\\d{4})\\b")
    private val lastYearPattern = Pattern.compile("\\b(tahun|thn|thn\\.|th)\\s*(lalu|kemarin|kmrn|sebelumnya|yg lalu)\\b")
    private val thisYearPattern = Pattern.compile("\\b(tahun|thn|thn\\.|th)\\s*(ini|skrg|sekarang)\\b")
    private val monthYearPattern = Pattern.compile("\\b(januari|jan|februari|feb|maret|mar|april|apr|mei|juni|jun|juli|jul|agustus|agt|agst|aug|september|sept|sep|oktober|okt|oct|november|nov|desember|des|dec)\\s+(tahun|thn|thn\\.|th)\\s+(lalu|kemarin|kmrn|sebelumnya|yg lalu)\\b")

    private val weightQueryPattern = Pattern.compile("\\b(berat|total|jumlah|berapa|kilo|gram|gram|ditimbang|timbangan|data)\\b")

    fun processQuery(query: String): String {
        val normalizedQuery = query.lowercase().replace(Regex("\\s+"), " ")

        if (!weightQueryPattern.matcher(normalizedQuery).find()) {
            return "Maaf, fitur ini hanya untuk melakukan pengecekan terhadap berat yang sudah ditimbang. Contoh: 'berapa berat bayam hari ini?'"
        }

        if (isGeneralStatsQuery(normalizedQuery)) {
            return processGeneralStatsQuery(normalizedQuery)
        }

        val multiVegMatcher = multiVegetablePattern.matcher(normalizedQuery)
        if (multiVegMatcher.find()) {
            return processMultiVegetableQuery(normalizedQuery)
        }

        val vegetableMatcher = vegetablePattern.matcher(normalizedQuery)
        val vegetableType = if (vegetableMatcher.find()) {
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
            return "Maaf, saya hanya mengenali jenis sayuran: bayam, kangkung, dan pakcoy."
        }

        val monthYearMatcher = monthYearPattern.matcher(normalizedQuery)
        if (monthYearMatcher.find()) {
            return processMonthLastYearQuery(normalizedQuery, vegetableType)
        }

        val yearMatcher = yearPattern.matcher(normalizedQuery)
        if (yearMatcher.find()) {
            val year = yearMatcher.group(2).toInt()
            return processSpecificYearQuery(vegetableType, year)
        }

        if (normalizedQuery.contains("bulan ini")) {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id"))
            val currentYear = calendar.get(Calendar.YEAR)
            return processMonthQuery("bulan $currentMonth $currentYear", vegetableType)
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
            lastYearPattern.matcher(normalizedQuery).find() -> {
                processLastYearQuery(vegetableType)
            }
            thisYearPattern.matcher(normalizedQuery).find() -> {
                processThisYearQuery(vegetableType)
            }
            else -> {
                "Maaf, mohon sertakan periode waktu yang jelas (misal: 'total bayam september 2024' atau 'bayam tahun 2023')"
            }
        }
    }

    private fun processMultiVegetableQuery(query: String): String {
        val isAllVegetables = query.contains("semua sayur") ||
                query.contains("seluruh sayur") ||
                query.contains("semua jenis") ||
                (!query.contains("bayam") &&
                        !query.contains("kangkung") &&
                        !query.contains("pakcoy"))

        val vegetables = if (isAllVegetables) {
            listOf("bayam", "kangkung", "pakcoy")
        } else {
            val matcher = multiVegetablePattern.matcher(query)
            val detectedVegs = mutableListOf<String>()

            while (matcher.find()) {
                for (i in 1..matcher.groupCount()) {
                    matcher.group(i)?.let { veg ->
                        val normalizedVeg = when {
                            veg.contains("bay") || veg.contains("bai") || veg.contains("bae") -> "bayam"
                            veg.contains("kang") || veg.contains("kank") -> "kangkung"
                            veg.contains("pak") || veg.contains("pok") || veg.contains("bok") -> "pakcoy"
                            else -> veg
                        }
                        if (!detectedVegs.contains(normalizedVeg)) {
                            detectedVegs.add(normalizedVeg)
                        }
                    }
                }
            }
            detectedVegs
        }

        if (vegetables.isEmpty()) {
            return "Maaf, tidak ada sayuran yang terdeteksi dalam pertanyaan."
        }

        val (timeFrame, startDate, endDate) = when {
            query.contains("tahun lalu") || lastYearPattern.matcher(query).find() -> {
                val calendar = Calendar.getInstance()
                val lastYear = calendar.get(Calendar.YEAR) - 1
                calendar.set(lastYear, Calendar.JANUARY, 1, 0, 0, 0)
                val start = calendar.time
                calendar.set(lastYear, Calendar.DECEMBER, 31, 23, 59, 59)
                val end = calendar.time
                Triple("tahun lalu", start, end)
            }
            query.contains("tahun ini") || thisYearPattern.matcher(query).find() -> {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
                val start = calendar.time
                calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
                val end = calendar.time
                Triple("tahun ini", start, end)
            }
            query.contains("bulan ini") -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.time
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val end = calendar.time
                Triple("bulan ini", start, end)
            }
            query.contains("minggu ini") || thisWeekPattern.matcher(query).find() -> {
                val (start, end) = dataManager.getCurrentWeekRange()
                Triple("minggu ini", start, end)
            }
            yearPattern.matcher(query).find() -> {
                val matcher = yearPattern.matcher(query)
                matcher.find()
                val year = matcher.group(2).toInt()
                val calendar = Calendar.getInstance()
                calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
                val start = calendar.time
                calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
                val end = calendar.time
                Triple("tahun $year", start, end)
            }
            else -> Triple("periode yang diminta", null, null)
        }

        var totalWeight = 0.0
        val results = mutableListOf<String>()

        for (veg in vegetables) {
            val data = if (startDate != null && endDate != null) {
                dataManager.getDataByTypeBetweenDates(veg, startDate, endDate)
            } else {
                emptyList()
            }

            val vegWeight = data.sumOf { it.weight }
            totalWeight += vegWeight

            if (data.isEmpty()) {
                results.add("- $veg: 0 gram (tidak ada data)")
            } else {
                results.add("- $veg: ${String.format("%.2f", vegWeight)} gram")
            }
        }

        return """
        Berat sayuran $timeFrame:
        ${results.joinToString("\n")}
        
        Total berat semua sayuran: ${String.format("%.2f", totalWeight)} gram
    """.trimIndent()
    }

    private fun isGeneralStatsQuery(query: String): Boolean {
        return !vegetablePattern.matcher(query).find() &&
                (query.contains("semua sayur") ||
                        query.contains("semua jenis") ||
                        query.contains("total keseluruhan") ||
                        query.contains("statistik") ||
                        query.contains("stat") ||
                        query.contains("rangkuman") ||
                        query.contains("rekap") ||
                        query.contains("sayuran apa saja") ||
                        query.contains("sayur apa saja") ||
                        query.contains("apa saja sayuran") ||
                        query.contains("apa saja sayur"))
    }




    private fun processTodayQuery(vegetableType: String): String {
        val data = dataManager.getDataByTypeForToday(vegetableType)

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi hari ini."
        }

        val totalWeight = data.sumOf { it.weight }
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
        val today = formatter.format(Date())

        return "Berat total $vegetableType hari ini ($today) adalah ${String.format("%.2f", totalWeight)} gram."
    }

    private fun processLastYearQuery(vegetableType: String): String {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val lastYear = currentYear - 1

        calendar.set(Calendar.YEAR, lastYear)
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.time

        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        val endDate = calendar.time

        val data = if (vegetableType == "semua") {
            dataManager.getAllDataBetweenDates(startDate, endDate)
        } else {
            dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)
        }

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi pada tahun $lastYear."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total $vegetableType pada tahun $lastYear adalah ${String.format("%.2f", totalWeight)} gram."
    }

    private fun processSpecificYearQuery(vegetableType: String, year: Int): String {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (year > currentYear) {
            return "Tahun $year belum selesai. Data hanya tersedia sampai tahun $currentYear."
        }
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.time

        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        val endDate = calendar.time

        val data = if (vegetableType == "semua") {
            dataManager.getAllDataBetweenDates(startDate, endDate)
        } else {
            dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)
        }
        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi pada tahun $year."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total $vegetableType pada tahun $year adalah ${String.format("%.2f", totalWeight)} gram."
    }

    private fun processMonthLastYearQuery(query: String, vegetableType: String): String {
        val monthYearMatcher = monthYearPattern.matcher(query)
        if (!monthYearMatcher.find()) {
            return "Maaf, saya tidak mengenali bulan atau tahun yang Anda maksud."
        }

        val monthName = monthYearMatcher.group(1)

        val monthMap = mapOf(
            "januari" to 0, "jan" to 0,
            "februari" to 1, "feb" to 1,
            "maret" to 2, "mar" to 2,
            "april" to 3, "apr" to 3,
            "mei" to 4,
            "juni" to 5, "jun" to 5,
            "juli" to 6, "jul" to 6,
            "agustus" to 7, "agt" to 7, "agst" to 7, "aug" to 7,
            "september" to 8, "sept" to 8, "sep" to 8,
            "oktober" to 9, "okt" to 9, "oct" to 9,
            "november" to 10, "nov" to 10,
            "desember" to 11, "des" to 11, "dec" to 11
        )

        val monthIndex = monthMap[monthName] ?: return "Bulan tidak valid."

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val lastYear = currentYear - 1

        calendar.set(Calendar.YEAR, lastYear)
        calendar.set(Calendar.MONTH, monthIndex)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startDate = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        val fullMonthName = when (monthIndex) {
            0 -> "Januari"
            1 -> "Februari"
            2 -> "Maret"
            3 -> "April"
            4 -> "Mei"
            5 -> "Juni"
            6 -> "Juli"
            7 -> "Agustus"
            8 -> "September"
            9 -> "Oktober"
            10 -> "November"
            11 -> "Desember"
            else -> monthName
        }

        val data = dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi pada bulan $fullMonthName tahun lalu ($lastYear)."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total $vegetableType pada bulan $fullMonthName tahun lalu ($lastYear) adalah ${String.format("%.2f", totalWeight)} gram."
    }

    private fun processThisYearQuery(vegetableType: String): String {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.time

        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        val endDate = calendar.time

        val data = if (vegetableType == "semua") {
            dataManager.getAllDataBetweenDates(startDate, endDate)
        } else {
            dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)
        }

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi pada tahun $currentYear."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total $vegetableType pada tahun $currentYear adalah ${String.format("%.2f", totalWeight)} gram."
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

        return "Berat total $vegetableType kemarin (${formatter.format(yesterday)}) adalah ${String.format("%.2f", totalWeight)} gram."
    }

    private fun processWeekQuery(vegetableType: String): String {
        val data = if (vegetableType == "semua") {
            dataManager.getDataForCurrentWeek()
        } else {
            dataManager.getDataByTypeForCurrentWeek(vegetableType)
        }

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi minggu ini."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total $vegetableType minggu ini adalah ${String.format("%.2f", totalWeight)} gram."
    }


    private fun processLastWeekQuery(vegetableType: String): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val lastWeekStart = calendar.time
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val lastWeekEnd = calendar.time

        val data = if (vegetableType == "semua") {
            dataManager.getAllDataBetweenDates(lastWeekStart, lastWeekEnd)
        } else {
            dataManager.getDataByTypeBetweenDates(vegetableType, lastWeekStart, lastWeekEnd)
        }

        if (data.isEmpty()) {
            return "Tidak ada data $vegetableType yang terdeteksi minggu lalu."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total $vegetableType minggu lalu adalah ${String.format("%.2f", totalWeight)} gram."
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
        return "Berat total $vegetableType pada hari $dayName adalah ${String.format("%.2f", totalWeight)} gram."
    }

    private fun processMonthQuery(query: String, vegetableType: String): String {
        val monthMatcher = monthPattern.matcher(query)
        if (!monthMatcher.find()) {
            return "Maaf, saya tidak mengenali bulan yang Anda maksud."
        }

        val monthName = monthMatcher.group(1) ?: return "Maaf, saya tidak mengenali bulan yang Anda maksud."
        val yearStr = monthMatcher.group(2)

        val monthMap = mapOf(
            "januari" to 0, "jan" to 0,
            "februari" to 1, "feb" to 1,
            "maret" to 2, "mar" to 2,
            "april" to 3, "apr" to 3,
            "mei" to 4,
            "juni" to 5, "jun" to 5,
            "juli" to 6, "jul" to 6,
            "agustus" to 7, "agt" to 7, "agst" to 7, "aug" to 7,
            "september" to 8, "sept" to 8, "sep" to 8,
            "oktober" to 9, "okt" to 9, "oct" to 9,
            "november" to 10, "nov" to 10,
            "desember" to 11, "des" to 11, "dec" to 11
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

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        val fullMonthName = when (monthIndex) {
            0 -> "Januari"
            1 -> "Februari"
            2 -> "Maret"
            3 -> "April"
            4 -> "Mei"
            5 -> "Juni"
            6 -> "Juli"
            7 -> "Agustus"
            8 -> "September"
            9 -> "Oktober"
            10 -> "November"
            11 -> "Desember"
            else -> monthName
        }

        val data = if (vegetableType == "semua") {
            dataManager.getAllDataBetweenDates(startDate, endDate)
        } else {
            dataManager.getDataByTypeBetweenDates(vegetableType, startDate, endDate)
        }

        if (data.isEmpty()) {
            return "Tidak ada data ${if (vegetableType == "semua") "sayuran" else vegetableType} yang terdeteksi pada bulan $fullMonthName $year."
        }

        val totalWeight = data.sumOf { it.weight }
        return "Berat total ${if (vegetableType == "semua") "semua sayuran" else vegetableType} pada bulan $fullMonthName $year adalah ${String.format("%.2f", totalWeight)} gram."
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
            "januari" to 0, "jan" to 0,
            "februari" to 1, "feb" to 1,
            "maret" to 2, "mar" to 2,
            "april" to 3, "apr" to 3,
            "mei" to 4,
            "juni" to 5, "jun" to 5,
            "juli" to 6, "jul" to 6,
            "agustus" to 7, "agt" to 7, "agst" to 7, "aug" to 7,
            "september" to 8, "sept" to 8, "sep" to 8,
            "oktober" to 9, "okt" to 9, "oct" to 9,
            "november" to 10, "nov" to 10,
            "desember" to 11, "des" to 11, "dec" to 11
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
        return "Berat total $vegetableType pada tanggal ${formatter.format(date)} adalah ${String.format("%.2f", totalWeight)} gram."
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
        return "Berat total ${if (vegetableType == "semua") "semua sayuran" else vegetableType} bulan lalu adalah ${String.format("%.2f", totalWeight)} gram."
    }

    private fun processGeneralStatsQuery(query: String): String {
        val allData = dataManager.getVegetableData()

        if (allData.isEmpty()) {
            return "Tidak ada data sayuran yang tersedia."
        }

        val timeFrameData = when {
            thisWeekPattern.matcher(query).find() -> dataManager.getDataForCurrentWeek()
            todayPattern.matcher(query).find() -> dataManager.getDataForToday()
            lastMonthPattern.matcher(query).find() -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.time
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = calendar.time
                dataManager.getAllDataBetweenDates(start, end)
            }
            lastYearPattern.matcher(query).find() -> {
                val year = Calendar.getInstance().get(Calendar.YEAR) - 1
                val cal = Calendar.getInstance()
                cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
                val start = cal.time
                cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
                val end = cal.time
                dataManager.getAllDataBetweenDates(start, end)
            }
            thisYearPattern.matcher(query).find() -> {
                val year = Calendar.getInstance().get(Calendar.YEAR)
                val cal = Calendar.getInstance()
                cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
                val start = cal.time
                cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
                val end = cal.time
                dataManager.getAllDataBetweenDates(start, end)
            }
            yearPattern.matcher(query).find() -> {
                val matcher = yearPattern.matcher(query)
                matcher.find()
                val year = matcher.group(2).toInt()
                val cal = Calendar.getInstance()
                cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
                val start = cal.time
                cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
                val end = cal.time
                dataManager.getAllDataBetweenDates(start, end)
            }
            else -> allData
        }

        if (timeFrameData.isEmpty()) {
            val timeFrameText = when {
                thisWeekPattern.matcher(query).find() -> "minggu ini"
                todayPattern.matcher(query).find() -> "hari ini"
                lastMonthPattern.matcher(query).find() -> "bulan lalu"
                lastYearPattern.matcher(query).find() -> "tahun lalu"
                thisYearPattern.matcher(query).find() -> "tahun ini"
                yearPattern.matcher(query).find() -> {
                    val matcher = yearPattern.matcher(query)
                    matcher.find()
                    "tahun ${matcher.group(2)}"
                }
                else -> "periode yang diminta"
            }
            return "Tidak ada data sayuran yang terdeteksi pada $timeFrameText."
        }

        val groupedByType = timeFrameData.groupBy { it.vegetableType }

        val timeFrameText = when {
            thisWeekPattern.matcher(query).find() -> "minggu ini"
            todayPattern.matcher(query).find() -> "hari ini"
            lastMonthPattern.matcher(query).find() -> "bulan lalu"
            lastYearPattern.matcher(query).find() -> "tahun lalu"
            thisYearPattern.matcher(query).find() -> "tahun ini"
            yearPattern.matcher(query).find() -> {
                val matcher = yearPattern.matcher(query)
                matcher.find()
                "tahun ${matcher.group(2)}"
            }
            else -> "keseluruhan"
        }

        if (query.contains("apa saja") || query.contains("apa aja") || query.contains("jenisnya")) {
            val totalWeight = timeFrameData.sumOf { it.weight }
            val vegetableDetailsList = groupedByType.entries
                .sortedByDescending { it.value.sumOf { item -> item.weight } }
                .joinToString("\n") { entry ->
                    val vegType = entry.key
                    val vegWeight = entry.value.sumOf { it.weight }
                    "- $vegType: ${String.format("%.2f", vegWeight)} gram"
                }

            return """
            Daftar sayuran $timeFrameText:
            $vegetableDetailsList
            
            Total berat keseluruhan: ${String.format("%.2f", totalWeight)} gram
            Total jenis sayuran: ${groupedByType.size} jenis
        """.trimIndent()
        }

        val totalWeight = timeFrameData.sumOf { it.weight }
        val mostCommonVegetable = groupedByType.maxByOrNull { it.value.size }?.key ?: "tidak ada"
        val heaviestVegetable = groupedByType.maxByOrNull { group -> group.value.sumOf { it.weight } }?.key ?: "tidak ada"

        return """
        Statistik $timeFrameText:
        - Total berat semua sayuran: ${String.format("%.2f", totalWeight)} gram
        - Jenis sayuran terbanyak terdeteksi: $mostCommonVegetable (${groupedByType[mostCommonVegetable]?.size ?: 0} kali)
        - Jenis sayuran dengan berat total tertinggi: $heaviestVegetable (${String.format("%.2f", groupedByType[heaviestVegetable]?.sumOf { it.weight } ?: 0.0)} gram)
        - Total jenis sayuran terdeteksi: ${groupedByType.size} jenis
    """.trimIndent()
    }

}