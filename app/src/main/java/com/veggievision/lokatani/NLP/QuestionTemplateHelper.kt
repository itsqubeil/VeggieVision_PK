package com.veggievision.lokatani.NLP

import java.util.*
import kotlin.random.Random

class QuestionTemplateHelper {
    private val vegetables = listOf("bayam", "pakcoy", "kangkung")
    private val months = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

    private val templateQueries = listOf(
        "Berapa berat [sayuran] yang sudah dicatat hari ini?",
        "Berapa berat seluruh sayuran yang sudah dicatat hari ini?",
        "Berapa berat [sayuran] yang sudah dicatat minggu ini?",
        "Berapa berat seluruh sayuran yang sudah dicatat minggu ini?",
        "Berapa berat seluruh sayuran yang sudah dicatat pada [bulan] [tahun]?",
        "Berapa berat seluruh sayuran yang sudah dicatat pada tahun [tahun]?",
        "Berapa berat [sayuran] yang sudah dicatat pada [bulan] [tahun]?",
        "Berapa berat seluruh sayuran yang sudah dicatat pada [bulan] [tahun]?",
        "Berapa berat [sayuran] yang sudah dicatat pada tahun lalu?",
        "Berapa berat seluruh sayuran yang sudah dicatat pada tahun lalu?",
        "Berapa berat [sayuran] yang sudah dicatat pada bulan lalu?",
        "Berapa berat seluruh sayuran yang sudah dicatat pada bulan lalu?",
        "Berapa berat [sayuran] yang sudah dicatat pada tahun ini?",
        "Berapa berat seluruh sayuran yang sudah dicatat pada tahun ini?",
        "Berapa berat [sayuran] yang sudah dicatat pada bulan ini?",
        "Berapa berat seluruh sayuran yang sudah dicatat pada bulan ini?",
        "Berapa total berat bayam dan kangkung yang sudah dicatat bulan ini?",
        "Berapa total berat bayam dan pakcoy yang sudah dicatat bulan ini?",
        "Berapa total berat pakcoy dan kangkung yang sudah dicatat bulan ini?",
        "Berapa total berat pakcoy dan bayam yang sudah dicatat minggu ini?",
        "Berapa total berat pakcoy dan kangkung yang sudah dicatat minggu ini?",
        "Berapa total berat kangkung dan bayam yang sudah dicatat minggu ini?",
        "Berapa total berat kangkung dan pakcoy yang sudah dicatat hari ini?",
        "Berapa total berat kangkung dan bayam yang sudah dicatat hari ini?",
        "Berapa total berat bayam dan pakcoy yang sudah dicatat hari ini?",
    )

    private val usedTemplates = mutableSetOf<String>()

    fun getRandomTemplates(count: Int = 3): List<String> {
        val availableTemplates = templateQueries.filter { it !in usedTemplates }.toMutableList()

        if (availableTemplates.size < count) {
            usedTemplates.clear()
            availableTemplates.addAll(templateQueries)
        }

        val selectedTemplates = mutableListOf<String>()

        for (i in 0 until count) {
            if (availableTemplates.isEmpty()) break

            val randomIndex = Random.nextInt(availableTemplates.size)
            val templateBase = availableTemplates.removeAt(randomIndex)

            val processedTemplate = processTemplate(templateBase)
            selectedTemplates.add(processedTemplate)
            usedTemplates.add(templateBase)
        }

        return selectedTemplates
    }

    private fun processTemplate(template: String): String {
        if (template.contains("total berat")) {
            return template
        }

        var processed = template

        if (processed.contains("[sayuran]")) {
            val randomVegetable = vegetables.random()
            processed = processed.replace("[sayuran]", randomVegetable)
        }

        if (processed.contains("[bulan]")) {
            val randomMonth = months.random()
            processed = processed.replace("[bulan]", randomMonth)
        }

        if (processed.contains("[tahun]")) {
            val randomYear = currentYear - Random.nextInt(0, 2)
            processed = processed.replace("[tahun]", randomYear.toString())
        }

        return processed
    }
}