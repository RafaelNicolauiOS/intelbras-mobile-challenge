package dev.rafaelnicolau.devices.repository.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

const val DATABASE_FILE_NAME = "devices.db"

@Database(entities = [DeviceEntity::class, FavoriteEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
}

// Room generates the actual implementations for each platform.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** Same SQLite build on both platforms, so queries behave identically. */
fun RoomDatabase.Builder<AppDatabase>.buildAppDatabase(): AppDatabase =
    setDriver(BundledSQLiteDriver()).build()
