package com.veggievision.lokatani.NLP

import java.util.*
import java.util.Calendar

data class VegetableData(
    val id : Int,
    val vegetableType: String,
    val weight: Double,
    val timestamp: Date
)

data class TimeFrameOutputDetails(
    val timeFrameDesc: String,
    val startDate: Date?,
    val endDate: Date?,
    val timeFrameFullDescForOutput: String
)

class SayurDataManager {
    private val vegetableDataList = mutableListOf<VegetableData>()

    fun addVegetableData(data: VegetableData) {
        vegetableDataList.add(data)
    }

    fun getVegetableData(): List<VegetableData> {
        return vegetableDataList.toList()
    }

    fun clearData() {
        vegetableDataList.clear()
    }

    fun getDataByType(vegetableType: String): List<VegetableData> {
        return vegetableDataList.filter {
            it.vegetableType.equals(vegetableType, ignoreCase = true)
        }
    }

    fun getDataForToday(): List<VegetableData> {
        val calendar = Calendar.getInstance()
        return getDataForDate(calendar.time)
    }

    fun getDataForDate(date: Date): List<VegetableData> {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val startOfNextDay = calendar.time

        return vegetableDataList.filter {
            it.timestamp >= startOfDay && it.timestamp < startOfNextDay
        }
    }

    fun getCurrentWeekRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.time

        calendar.add(Calendar.DAY_OF_MONTH, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.time

        return Pair(start, end)
    }

    fun getDataForCurrentWeek(): List<VegetableData> {
        val (startOfWeek, endOfWeek) = getCurrentWeekRange()
        return vegetableDataList.filter {
            it.timestamp >= startOfWeek && it.timestamp <= endOfWeek
        }
    }

    fun getDataBetweenDates(startDate: Date, endDate: Date): List<VegetableData> {
        val calendarEnd = Calendar.getInstance()
        calendarEnd.time = endDate
        if (calendarEnd.get(Calendar.HOUR_OF_DAY) == 0 &&
            calendarEnd.get(Calendar.MINUTE) == 0 &&
            calendarEnd.get(Calendar.SECOND) == 0) {
            calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
            calendarEnd.set(Calendar.MINUTE, 59)
            calendarEnd.set(Calendar.SECOND, 59)
            calendarEnd.set(Calendar.MILLISECOND, 999)
        }
        val effectiveEndDate = calendarEnd.time

        val calendarStart = Calendar.getInstance()
        calendarStart.time = startDate
        if (calendarStart.get(Calendar.HOUR_OF_DAY) == 0 &&
            calendarStart.get(Calendar.MINUTE) == 0 &&
            calendarStart.get(Calendar.SECOND) == 0) {
        }


        return vegetableDataList.filter {
            it.timestamp >= calendarStart.time && it.timestamp <= effectiveEndDate
        }
    }

    fun getAllDataBetweenDates(startDate: Date, endDate: Date): List<VegetableData> {
        return getDataBetweenDates(startDate, endDate)
    }

    fun getDataByTypeForDayOfWeek(vegetableType: String, dayName: String): List<VegetableData> {
        val dayMapping = mapOf(
            "senin" to Calendar.MONDAY, "selasa" to Calendar.TUESDAY, "rabu" to Calendar.WEDNESDAY,
            "kamis" to Calendar.THURSDAY, "jumat" to Calendar.FRIDAY, "sabtu" to Calendar.SATURDAY,
            "minggu" to Calendar.SUNDAY, "monday" to Calendar.MONDAY, "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY, "thursday" to Calendar.THURSDAY, "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY, "sunday" to Calendar.SUNDAY
        )
        val targetDay = dayMapping[dayName.lowercase()] ?: return emptyList()

        val baseData = if (vegetableType.equals("semua", ignoreCase = true)) {
            getVegetableData()
        } else {
            getDataByType(vegetableType)
        }

        return baseData.filter { data ->
            val calendar = Calendar.getInstance()
            calendar.time = data.timestamp
            calendar.get(Calendar.DAY_OF_WEEK) == targetDay
        }
    }


    fun getDataByTypeForToday(vegetableType: String): List<VegetableData> {
        val todayData = getDataForToday()
        return if (vegetableType.equals("semua", ignoreCase = true)) {
            todayData
        } else {
            todayData.filter { it.vegetableType.equals(vegetableType, ignoreCase = true) }
        }
    }

    fun getDataByTypeForCurrentWeek(vegetableType: String): List<VegetableData> {
        val weekData = getDataForCurrentWeek()
        return if (vegetableType.equals("semua", ignoreCase = true)) {
            weekData
        } else {
            weekData.filter { it.vegetableType.equals(vegetableType, ignoreCase = true) }
        }
    }

    fun getDataByTypeBetweenDates(
        vegetableType: String,
        startDate: Date,
        endDate: Date
    ): List<VegetableData> {
        val dateRangeData = getDataBetweenDates(startDate, endDate)
        return if (vegetableType.equals("semua", ignoreCase = true)) {
            dateRangeData
        } else {
            dateRangeData.filter { it.vegetableType.equals(vegetableType, ignoreCase = true) }
        }
    }

    fun getDataByTypeAndDate(vegetableType: String, date: Date): List<VegetableData> {
        val dateData = getDataForDate(date)
        return if (vegetableType.equals("semua", ignoreCase = true)) {
            dateData
        } else {
            dateData.filter { it.vegetableType.equals(vegetableType, ignoreCase = true) }
        }
    }
}