package dev.mrbean.laundryledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Batch::class, BatchItem::class], version = 1, exportSchema = false)
abstract class LaundryDb : RoomDatabase() {
    abstract fun dao(): LaundryDao

    companion object {
        fun create(context: Context): LaundryDb =
            Room.databaseBuilder(context, LaundryDb::class.java, "laundry.db").build()
    }
}
