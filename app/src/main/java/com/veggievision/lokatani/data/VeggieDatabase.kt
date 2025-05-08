package com.veggievision.lokatani.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VeggieEntity::class], version = 1)
abstract class VeggieDatabase : RoomDatabase() {
    abstract fun veggieDao(): VeggieDao

    companion object {
        @Volatile
        private var INSTANCE: VeggieDatabase? = null

        fun getDatabase(context: Context): VeggieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VeggieDatabase::class.java,
                    "veggievision_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}