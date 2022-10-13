package de.julianostarek.motif

import android.content.Context
import com.russhwolf.settings.AndroidSettings
import de.julianostarek.motif.persist.DriverFactory

fun androidStartKoin(context: Context) {
    val driverFactory = DriverFactory(context)
    val loginPreferences = context.getSharedPreferences("login", Context.MODE_PRIVATE)

    sharedStartKoin(driverFactory, AndroidSettings(loginPreferences))
}