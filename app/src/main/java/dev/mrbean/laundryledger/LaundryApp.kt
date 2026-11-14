package dev.mrbean.laundryledger

import android.app.Application
import dev.mrbean.laundryledger.data.LaundryDb
import dev.mrbean.laundryledger.data.Prefs
import dev.mrbean.laundryledger.update.Updater

class LaundryApp : Application() {
    val db by lazy { LaundryDb.create(this) }
    val prefs by lazy { Prefs(this) }
    val updater by lazy { Updater(this) }
}
