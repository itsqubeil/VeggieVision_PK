package com.veggievision.lokatani.NLP

import java.util.*
import java.util.Calendar

data class VegetableData(
    val id : Int,
    val vegetableType: String,
    val weight: Double,
    val timestamp: Date
)

class SayurDataManager {
    private val vegetableDataList = mutableListOf<VegetableData>()

    fun addVegetableData(data: VegetableData) {
        vegetableDataList.add(data)
    }

    fun getVegetableData(): List<VegetableData> {
        return vegetableDataList
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

        // Tambah 6 hari ke depan untuk dapatkan akhir minggu
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.time

        return Pair(start, end)
    }

    fun getDataForCurrentWeek(): List<VegetableData> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.time

        calendar.add(Calendar.DAY_OF_WEEK, 7)
        val startOfNextWeek = calendar.time

        return vegetableDataList.filter {
            it.timestamp >= startOfWeek && it.timestamp < startOfNextWeek
        }
    }

    fun getDataBetweenDates(startDate: Date, endDate: Date): List<VegetableData> {
        return vegetableDataList.filter {
            it.timestamp >= startDate && it.timestamp <= endDate
        }
    }

    fun getAllDataBetweenDates(startDate: Date, endDate: Date): List<VegetableData> {
        return getDataBetweenDates(startDate, endDate)
    }

    fun getDataByTypeForDayOfWeek(vegetableType: String, dayName: String): List<VegetableData> {
        val dataForType = getDataByType(vegetableType)
        val dayMapping = mapOf(
            "senin" to Calendar.MONDAY,
            "selasa" to Calendar.TUESDAY,
            "rabu" to Calendar.WEDNESDAY,
            "kamis" to Calendar.THURSDAY,
            "jumat" to Calendar.FRIDAY,
            "sabtu" to Calendar.SATURDAY,
            "minggu" to Calendar.SUNDAY,
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY
        )

        val targetDay = dayMapping[dayName.lowercase()] ?: return emptyList()

        return dataForType.filter { data ->
            val calendar = Calendar.getInstance()
            calendar.time = data.timestamp
            calendar.get(Calendar.DAY_OF_WEEK) == targetDay
        }
    }

    fun getDataByTypeForToday(vegetableType: String): List<VegetableData> {
        val dataForType = getDataByType(vegetableType)
        val todayData = getDataForToday()
        return dataForType.filter { todayData.contains(it) }
    }

    fun getDataByTypeForCurrentWeek(vegetableType: String): List<VegetableData> {
        val dataForType = getDataByType(vegetableType)
        val weekData = getDataForCurrentWeek()
        return dataForType.filter { weekData.contains(it) }
    }

    fun getDataByTypeBetweenDates(
        vegetableType: String,
        startDate: Date,
        endDate: Date
    ): List<VegetableData> {
        val dataForType = getDataByType(vegetableType)
        val dateRangeData = getDataBetweenDates(startDate, endDate)
        return dataForType.filter { dateRangeData.contains(it) }
    }

    fun getDataByTypeAndDate(vegetableType: String, date: Date): List<VegetableData> {
        val dataForType = getDataByType(vegetableType)
        val dateData = getDataForDate(date)
        return dataForType.filter { dateData.contains(it) }
    }
}