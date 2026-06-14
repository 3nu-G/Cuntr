package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.SessionRepository

class MainApplication : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: SessionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "countr_database"
        )
        .fallbackToDestructiveMigration(true)
        .build()
        repository = SessionRepository(database.sessionDao())
    }
}
