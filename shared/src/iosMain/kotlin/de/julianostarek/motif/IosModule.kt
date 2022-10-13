package de.julianostarek.motif

import com.russhwolf.settings.AppleSettings
import de.julianostarek.motif.persist.DriverFactory
import platform.Foundation.NSUserDefaults

fun iosStartKoin() {
    val driverFactory = DriverFactory()
    val loginDefaults = NSUserDefaults("login")
    sharedStartKoin(driverFactory, AppleSettings(loginDefaults))
}