package com.veggievision.lokatani.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "veggievision_data")
data class VeggieEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val weight: String,
    val timestamp: String
)