package com.veggievision.lokatani.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VeggieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VeggieEntity)

    @Query("SELECT * FROM veggievision_data ORDER BY timestamp DESC")
    suspend fun getAll(): List<VeggieEntity>

    @Delete
    suspend fun deleteItems(items: List<VeggieEntity>)
}