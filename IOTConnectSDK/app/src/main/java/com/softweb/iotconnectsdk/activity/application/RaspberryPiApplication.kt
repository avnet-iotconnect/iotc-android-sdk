package com.softweb.iotconnectsdk.activity.application

import android.app.Application
import android.content.Context

import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.crashes.Crashes



class RaspberryPiApplication : Application() {
    init {
        raspberryPiApplication = this
    }

    override fun onCreate() {
        super.onCreate()

        raspberryPiApplication = this


        AppCenter.start(
            raspberryPiApplication, "34d6d4dd-5362-483e-8ea8-11d4e60663ca",
            Crashes::class.java
        )

       // Stetho.initializeWithDefaults(this)

    }

    companion object {

        internal var raspberryPiApplication: RaspberryPiApplication? = null

        val appContext: Context
            get() {
                if (raspberryPiApplication == null) {
                    raspberryPiApplication = RaspberryPiApplication()
                }
                return raspberryPiApplication!!
            }
    }
}
