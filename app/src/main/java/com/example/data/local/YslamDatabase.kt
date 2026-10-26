package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        SavedSnippetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class YslamDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun snippetDao(): SnippetDao

    companion object {
        @Volatile
        private var INSTANCE: YslamDatabase? = null

        fun getDatabase(context: Context): YslamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YslamDatabase::class.java,
                    "yslam_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
